package com.energy.service;

import com.energy.entity.EnergyConsumption;
import com.energy.repository.EnergyConsumptionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final EnergyConsumptionRepository energyRepo;
    private final CozeService cozeService;
    private final ClaudeService claudeService;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] generateExcelReport(String buildingId) throws IOException {
        List<EnergyConsumption> dataList = energyRepo.findByBuildingIdOrderByTimestampDesc(buildingId);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("能耗数据报告");

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("建筑能效分析报告 - " + buildingId);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

            Row infoRow = sheet.createRow(1);
            infoRow.createCell(0).setCellValue("生成时间: " + LocalDateTime.now().format(FMT));
            infoRow.createCell(4).setCellValue("建筑ID: " + buildingId);

            Row headerRow = sheet.createRow(3);
            String[] headers = {"监测时间", "建筑类型", "电力消耗(kWh)", "水能耗(m³)",
                    "碳排放(kg)", "空调能耗(kWh)", "照明能耗(kWh)", "温度(°C)", "湿度(%)", "状态"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            int rowIdx = 4;
            double sumPower = 0, sumWater = 0, sumCarbon = 0, sumAc = 0;
            for (EnergyConsumption d : dataList) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(d.getTimestamp() != null ? d.getTimestamp().format(FMT) : "");
                row.createCell(1).setCellValue(d.getBuildingType() != null ? d.getBuildingType() : "");
                setValue(row, 2, d.getPowerConsumptionKwh());
                setValue(row, 3, d.getWaterConsumptionM3());
                setValue(row, 4, d.getCarbonEmissionKg());
                setValue(row, 5, d.getAcConsumptionKwh());
                setValue(row, 6, d.getLightingConsumptionKwh());
                setValue(row, 7, d.getTemperatureCelsius());
                setValue(row, 8, d.getHumidityPercent());
                row.createCell(9).setCellValue(d.getStatus() != null ? d.getStatus() : "");

                sumPower += d.getPowerConsumptionKwh() != null ? d.getPowerConsumptionKwh() : 0;
                sumWater += d.getWaterConsumptionM3() != null ? d.getWaterConsumptionM3() : 0;
                sumCarbon += d.getCarbonEmissionKg() != null ? d.getCarbonEmissionKg() : 0;
                sumAc += d.getAcConsumptionKwh() != null ? d.getAcConsumptionKwh() : 0;
            }

            Row sumRow = sheet.createRow(rowIdx + 1);
            sumRow.createCell(0).setCellValue("汇总");
            sumRow.createCell(2).setCellValue(Math.round(sumPower * 100.0) / 100.0);
            sumRow.createCell(3).setCellValue(Math.round(sumWater * 100.0) / 100.0);
            sumRow.createCell(4).setCellValue(Math.round(sumCarbon * 100.0) / 100.0);
            sumRow.createCell(5).setCellValue(Math.round(sumAc * 100.0) / 100.0);

            Sheet analysisSheet = workbook.createSheet("AI智能分析");
            String contextData = String.format("建筑%s: 总电力%.2fkWh, 总水耗%.2fm³, 总碳排放%.2fkg, 空调%.2fkWh, 共%d条记录",
                    buildingId, sumPower, sumWater, sumCarbon, sumAc, dataList.size());
            String aiResult = cozeService.isConfigured()
                    ? cozeService.generateEnergyReport(buildingId, contextData)
                    : claudeService.generateEnergyReport(buildingId, contextData);

            Row aiTitleRow = analysisSheet.createRow(0);
            aiTitleRow.createCell(0).setCellValue("AI智能分析结果");
            analysisSheet.setColumnWidth(0, 20000);
            String[] lines = aiResult.split("\n");
            for (int i = 0; i < lines.length; i++) {
                Row r = analysisSheet.createRow(i + 1);
                r.createCell(0).setCellValue(lines[i]);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    private void setValue(Row row, int col, Double value) {
        Cell cell = row.createCell(col);
        if (value != null) cell.setCellValue(value);
        else cell.setCellValue("-");
    }
}
