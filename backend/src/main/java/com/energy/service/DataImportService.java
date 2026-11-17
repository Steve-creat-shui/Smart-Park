package com.energy.service;

import com.energy.entity.Building;
import com.energy.entity.EnergyConsumption;
import com.energy.entity.ImportHistory;
import com.energy.repository.BuildingRepository;
import com.energy.repository.EnergyConsumptionRepository;
import com.energy.repository.ImportHistoryRepository;
import com.energy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
public class DataImportService {

    private final EnergyConsumptionRepository energyRepo;
    private final ImportHistoryRepository importHistoryRepo;
    private final UserRepository userRepo;
    private final BuildingRepository buildingRepo;

    private static final DateTimeFormatter[] FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    };

    public Map<String, Object> importFile(MultipartFile file) throws Exception {
        return importFileForEnterprise(file, null, null);
    }

    public Map<String, Object> importFileForEnterprise(MultipartFile file, Long enterpriseId, Long userId) throws Exception {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        // 创建导入历史记录
        ImportHistory history = new ImportHistory();
        history.setEnterpriseId(enterpriseId != null ? enterpriseId : 0L);
        history.setUserId(userId != null ? userId : 0L);
        history.setFileName(file.getOriginalFilename());
        history.setFileSize(file.getSize());
        history.setStatus("processing");
        importHistoryRepo.save(history);

        try {
            List<EnergyConsumption> records;
            if (filename.endsWith(".csv")) {
                records = parseCsv(file);
            } else if (filename.endsWith(".xlsx")) {
                records = parseExcel(file, false);
            } else if (filename.endsWith(".xls")) {
                records = parseExcel(file, true);
            } else {
                throw new RuntimeException("不支持的文件格式，请上传 .csv 或 .xlsx 文件");
            }

            // 验证并统计
            int validCount = 0;
            int skipCount = 0;
            List<String> errors = new ArrayList<>();
            for (int i = 0; i < records.size(); i++) {
                EnergyConsumption e = records.get(i);
                if (e.getBuildingId() == null || e.getBuildingId().isEmpty()) {
                    skipCount++;
                    errors.add("行" + (i + 2) + ": 建筑ID为空");
                    continue;
                }
                if (e.getTimestamp() == null) {
                    skipCount++;
                    errors.add("行" + (i + 2) + ": 时间戳无效");
                    continue;
                }
                validCount++;
            }

            // 过滤有效记录
            List<EnergyConsumption> validRecords = records.stream()
                .filter(e -> e.getBuildingId() != null && !e.getBuildingId().isEmpty() && e.getTimestamp() != null)
                .collect(java.util.stream.Collectors.toList());

            // 建筑名称 → 建筑ID 自动映射（为企业/个人用户自动创建建筑）
            Map<String, String> nameToId = new LinkedHashMap<>();
            if (enterpriseId != null && enterpriseId != 0) {
                List<Building> existing = buildingRepo.findByEnterpriseId(enterpriseId);
                for (Building b : existing) {
                    if (b.getBuildingName() != null) nameToId.put(b.getBuildingName(), b.getBuildingId());
                    nameToId.put(b.getBuildingId(), b.getBuildingId());
                }
                int seq = existing.size() + 1;
                // 个人用户用P前缀，企业用E前缀
                String prefix = enterpriseId < 0 ? "P" + Math.abs(enterpriseId) + "B" : "E" + enterpriseId + "B";
                java.util.Set<String> names = validRecords.stream()
                    .map(EnergyConsumption::getBuildingId)
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
                for (String name : names) {
                    if (!nameToId.containsKey(name)) {
                        String bid = prefix + String.format("%03d", seq++);
                        Building b = new Building();
                        b.setBuildingId(bid);
                        b.setBuildingName(name);
                        b.setBuildingType(name);
                        b.setEnterpriseId(enterpriseId);
                        b.setStatus("active");
                        buildingRepo.save(b);
                        nameToId.put(name, bid);
                    }
                }
                validRecords.forEach(e -> {
                    e.setBuildingId(nameToId.getOrDefault(e.getBuildingId(), e.getBuildingId()));
                    e.setEnterpriseId(enterpriseId);
                });
            }
            energyRepo.saveAll(validRecords);

            // 更新历史记录
            history.setRecordCount(validCount);
            history.setStatus("completed");
            history.setCompletedAt(LocalDateTime.now());
            if (!errors.isEmpty()) {
                history.setErrorMessage(String.join("; ", errors.subList(0, Math.min(20, errors.size()))));
            }
            importHistoryRepo.save(history);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("imported", validCount);
            result.put("skipped", skipCount);
            result.put("total", records.size());
            result.put("errors", errors.size() > 20 ? errors.subList(0, 20) : errors);
            result.put("historyId", history.getId());
            result.put("message", "成功导入 " + validCount + " 条记录" + (skipCount > 0 ? "，跳过 " + skipCount + " 条" : ""));
            return result;
        } catch (Exception ex) {
            history.setStatus("failed");
            history.setErrorMessage(ex.getMessage());
            history.setCompletedAt(LocalDateTime.now());
            importHistoryRepo.save(history);
            throw ex;
        }
    }

    public List<ImportHistory> getImportHistory(Long enterpriseId) {
        if (enterpriseId != null && enterpriseId != 0) {
            List<ImportHistory> list = importHistoryRepo.findByEnterpriseIdOrderByCreatedAtDesc(enterpriseId);
            enrichHistory(list);
            return list;
        }
        List<ImportHistory> list = importHistoryRepo.findAllByOrderByCreatedAtDesc();
        enrichHistory(list);
        return list;
    }

    private void enrichHistory(List<ImportHistory> list) {
        list.forEach(h -> userRepo.findById(h.getUserId()).ifPresent(u ->
            h.setUserName(u.getRealName() != null ? u.getRealName() : u.getNickname())
        ));
    }

    public List<Map<String, Object>> previewFile(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        List<EnergyConsumption> records;
        if (filename.endsWith(".csv")) {
            records = parseCsv(file);
        } else if (filename.endsWith(".xlsx")) {
            records = parseExcel(file, false);
        } else if (filename.endsWith(".xls")) {
            records = parseExcel(file, true);
        } else {
            throw new RuntimeException("不支持的文件格式");
        }
        List<Map<String, Object>> preview = new ArrayList<>();
        for (int i = 0; i < Math.min(10, records.size()); i++) {
            EnergyConsumption e = records.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("buildingId", e.getBuildingId());
            row.put("timestamp", e.getTimestamp() != null ? e.getTimestamp().toString() : "");
            row.put("powerConsumptionKwh", e.getPowerConsumptionKwh());
            row.put("waterConsumptionM3", e.getWaterConsumptionM3());
            row.put("carbonEmissionKg", e.getCarbonEmissionKg());
            row.put("acConsumptionKwh", e.getAcConsumptionKwh());
            row.put("status", e.getStatus());
            preview.add(row);
        }
        return preview;
    }

    private List<EnergyConsumption> parseCsv(MultipartFile file) throws Exception {
        List<EnergyConsumption> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"))) {
            String header = br.readLine();
            if (header == null) return list;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cols = line.split(",", -1);
                EnergyConsumption e = buildFromCols(cols);
                if (e != null) list.add(e);
            }
        }
        return list;
    }

    private List<EnergyConsumption> parseExcel(MultipartFile file, boolean isXls) throws Exception {
        List<EnergyConsumption> list = new ArrayList<>();
        Workbook wb = isXls ? new HSSFWorkbook(file.getInputStream())
                            : new XSSFWorkbook(file.getInputStream());
        Sheet sheet = wb.getSheetAt(0);
        boolean first = true;
        for (Row row : sheet) {
            if (first) { first = false; continue; }
            String[] cols = new String[row.getLastCellNum()];
            for (int i = 0; i < cols.length; i++) {
                Cell cell = row.getCell(i);
                cols[i] = cell == null ? "" : getCellString(cell);
            }
            EnergyConsumption e = buildFromCols(cols);
            if (e != null) list.add(e);
        }
        wb.close();
        return list;
    }

    private EnergyConsumption buildFromCols(String[] cols) {
        if (cols.length < 3) return null;
        try {
            EnergyConsumption e = new EnergyConsumption();
            e.setBuildingId(safe(cols, 0));
            e.setTimestamp(parseDateTime(safe(cols, 1)));
            e.setPowerConsumptionKwh(parseDouble(safe(cols, 2)));
            e.setWaterConsumptionM3(parseDouble(safe(cols, 3)));
            e.setCarbonEmissionKg(parseDouble(safe(cols, 4)));
            e.setAcConsumptionKwh(parseDouble(safe(cols, 5)));
            e.setLightingConsumptionKwh(parseDouble(safe(cols, 6)));
            e.setOtherConsumptionKwh(parseDouble(safe(cols, 7)));
            e.setStatus(safe(cols, 8).isEmpty() ? "normal" : safe(cols, 8));
            if (e.getBuildingId() == null || e.getBuildingId().isEmpty()) return null;
            return e;
        } catch (Exception ex) {
            return null;
        }
    }

    private String safe(String[] cols, int idx) {
        return idx < cols.length ? cols[idx].trim() : "";
    }

    private double parseDouble(String s) {
        if (s == null || s.isEmpty()) return 0.0;
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isEmpty()) return LocalDateTime.now();
        for (DateTimeFormatter f : FORMATTERS) {
            try { return LocalDateTime.parse(s, f); } catch (DateTimeParseException ignored) {}
        }
        return LocalDateTime.now();
    }

    private String getCellString(Cell cell) {
        switch (cell.getCellType()) {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    java.util.Date d = cell.getDateCellValue();
                    return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
                }
                double v = cell.getNumericCellValue();
                if (v == Math.floor(v) && !Double.isInfinite(v)) {
                    return String.valueOf((long) v);
                }
                return String.valueOf(v);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return String.valueOf(cell.getNumericCellValue()); }
                catch (Exception e) { return cell.getStringCellValue().trim(); }
            case BLANK: return "";
            default: return cell.getStringCellValue().trim();
        }
    }
}
