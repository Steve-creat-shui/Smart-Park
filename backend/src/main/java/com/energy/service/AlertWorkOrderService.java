package com.energy.service;

import com.energy.entity.*;
import com.energy.repository.AlertRepository;
import com.energy.repository.BuildingRepository;
import com.energy.repository.DeviceRepository;
import com.energy.repository.UserRepository;
import com.energy.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertWorkOrderService {

    private final AlertRepository alertRepo;
    private final WorkOrderRepository workOrderRepo;
    private final DeviceRepository deviceRepo;
    private final BuildingRepository buildingRepo;
    private final UserRepository userRepo;
    private final CozeService cozeService;
    private final ClaudeService claudeService;

    private static final AtomicLong alertSeq = new AtomicLong(System.currentTimeMillis() % 100000);
    private static final AtomicLong orderSeq = new AtomicLong(System.currentTimeMillis() % 100000);

    // ==================== 告警级别 → 工单优先级 映射 ====================
    private static final Map<String, String> LEVEL_TO_PRIORITY = Map.of(
            "info", "low",
            "warning", "medium",
            "error", "high",
            "critical", "critical"
    );

    // ==================== 告警 CRUD ====================

    public List<Alert> listAlerts(String level, String status) {
        List<Alert> list;
        if (isNotBlank(level) && isNotBlank(status)) {
            list = alertRepo.findByLevelAndStatusOrderByCreatedAtDesc(level, status);
        } else if (isNotBlank(level)) {
            list = alertRepo.findByLevelOrderByCreatedAtDesc(level);
        } else if (isNotBlank(status)) {
            list = alertRepo.findByStatusOrderByCreatedAtDesc(status);
        } else {
            list = alertRepo.findAllByOrderByCreatedAtDesc();
        }
        enrichAlerts(list);
        return list;
    }

    public Map<String, Object> getAlertStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", alertRepo.count());
        stats.put("active", alertRepo.countByStatus("active"));
        stats.put("acknowledged", alertRepo.countByStatus("acknowledged"));
        stats.put("resolved", alertRepo.countByStatus("resolved"));
        return stats;
    }

    @Transactional
    public Alert createAlert(String level, String type, Integer buildingId, Integer deviceId, String message) {
        Alert alert = new Alert();
        alert.setAlertCode("ALT" + String.format("%05d", alertSeq.incrementAndGet()));
        alert.setLevel(level);
        alert.setType(type);
        alert.setBuildingId(buildingId);
        alert.setDeviceId(deviceId);
        alert.setMessage(message);
        alert.setStatus("active");
        alert.setTriggeredAt(LocalDateTime.now());
        alert.setCreatedAt(LocalDateTime.now());
        alert = alertRepo.save(alert);

        // 自动创建工单
        autoCreateWorkOrder(alert);
        return alert;
    }

    @Transactional
    public Alert acknowledgeAlert(Integer alertId) {
        Alert alert = alertRepo.findById(alertId)
                .orElseThrow(() -> new RuntimeException("告警不存在"));
        if (!"active".equals(alert.getStatus())) {
            throw new RuntimeException("只有活跃状态的告警可以确认");
        }
        alert.setStatus("acknowledged");
        return alertRepo.save(alert);
    }

    @Transactional
    public Alert resolveAlert(Integer alertId) {
        Alert alert = alertRepo.findById(alertId)
                .orElseThrow(() -> new RuntimeException("告警不存在"));
        alert.setStatus("resolved");
        alert.setResolvedAt(LocalDateTime.now());
        Alert saved = alertRepo.save(alert);

        // 同时解决关联工单
        workOrderRepo.findByAlertId(alertId).ifPresent(wo -> {
            if (!"completed".equals(wo.getStatus())) {
                wo.setStatus("completed");
                wo.setCompletedAt(LocalDateTime.now());
                wo.setResolutionNotes("告警已解决，工单自动完成");
                workOrderRepo.save(wo);
            }
        });
        return saved;
    }

    // ==================== 指派人员 ====================

    public List<Map<String, Object>> getAssignableUsers() {
        List<User> users = userRepo.findByStatusOrderByCreatedAtDesc("active");
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("nickname", u.getNickname());
            m.put("realName", u.getRealName());
            m.put("department", u.getDepartment());
            m.put("position", u.getPosition());
            m.put("phone", u.getPhone());
            m.put("role", u.getRole());
            result.add(m);
        }
        return result;
    }

    @Transactional
    public WorkOrder assignWorkOrder(Integer orderId, Integer assignedTo) {
        WorkOrder wo = workOrderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("工单不存在"));
        wo.setAssignedTo(assignedTo);
        log.info("工单 {} 已指派给用户 {}", wo.getOrderCode(), assignedTo);
        return workOrderRepo.save(wo);
    }

    // ==================== 工单 CRUD ====================

    public List<WorkOrder> listWorkOrders(String status, String priority) {
        List<WorkOrder> list;
        if (isNotBlank(status) && isNotBlank(priority)) {
            list = workOrderRepo.findByStatusAndPriorityOrderByCreatedAtDesc(status, priority);
        } else if (isNotBlank(status)) {
            list = workOrderRepo.findByStatusOrderByCreatedAtDesc(status);
        } else if (isNotBlank(priority)) {
            list = workOrderRepo.findByPriorityOrderByCreatedAtDesc(priority);
        } else {
            list = workOrderRepo.findAllByOrderByCreatedAtDesc();
        }
        enrichWorkOrders(list);
        return list;
    }

    public Map<String, Object> getWorkOrderStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", workOrderRepo.count());
        stats.put("pending", workOrderRepo.countByStatus("pending"));
        stats.put("processing", workOrderRepo.countByStatus("processing"));
        stats.put("completed", workOrderRepo.countByStatus("completed"));
        return stats;
    }

    @Transactional
    public WorkOrder startWorkOrder(Integer orderId) {
        WorkOrder wo = workOrderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("工单不存在"));
        if (!"pending".equals(wo.getStatus())) {
            throw new RuntimeException("只有待处理工单可以开始处理");
        }
        wo.setStatus("processing");
        wo.setStartedAt(LocalDateTime.now());

        // 同时确认关联告警
        if (wo.getAlertId() != null) {
            alertRepo.findById(wo.getAlertId()).ifPresent(alert -> {
                if ("active".equals(alert.getStatus())) {
                    alert.setStatus("acknowledged");
                    alertRepo.save(alert);
                }
            });
        }
        return workOrderRepo.save(wo);
    }

    @Transactional
    public WorkOrder completeWorkOrder(Integer orderId, String resolutionNotes) {
        WorkOrder wo = workOrderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("工单不存在"));
        if (!"processing".equals(wo.getStatus())) {
            throw new RuntimeException("只有处理中的工单可以完成");
        }
        wo.setStatus("completed");
        wo.setCompletedAt(LocalDateTime.now());
        wo.setResolutionNotes(resolutionNotes);

        // 同时解决关联告警
        if (wo.getAlertId() != null) {
            alertRepo.findById(wo.getAlertId()).ifPresent(alert -> {
                if (!"resolved".equals(alert.getStatus())) {
                    alert.setStatus("resolved");
                    alert.setResolvedAt(LocalDateTime.now());
                    alertRepo.save(alert);
                }
            });
        }
        return workOrderRepo.save(wo);
    }

    // ==================== 自动创建工单 (含AI建议) ====================

    @Transactional
    public WorkOrder autoCreateWorkOrder(Alert alert) {
        // 检查是否已有工单
        if (workOrderRepo.findByAlertId(alert.getId()).isPresent()) {
            return null;
        }

        String priority = LEVEL_TO_PRIORITY.getOrDefault(alert.getLevel(), "medium");

        // 生成AI建议
        String aiSuggestion = generateAiSuggestion(alert);

        WorkOrder wo = new WorkOrder();
        wo.setOrderCode("WO" + String.format("%05d", orderSeq.incrementAndGet()));
        wo.setAlertId(alert.getId());
        wo.setDeviceId(alert.getDeviceId());
        wo.setBuildingId(alert.getBuildingId() != null ? alert.getBuildingId() : 0);
        wo.setFaultDescription(buildFaultDescription(alert));
        wo.setAiSuggestion(aiSuggestion);
        wo.setPriority(priority);
        wo.setStatus("pending");
        wo.setCreatedAt(LocalDateTime.now());
        WorkOrder saved = workOrderRepo.save(wo);

        // 告警状态 → 已确认
        alert.setStatus("acknowledged");
        alertRepo.save(alert);

        log.info("自动创建工单 {} (优先级:{}) ← 告警 {} (级别:{})",
                wo.getOrderCode(), priority, alert.getAlertCode(), alert.getLevel());
        return saved;
    }

    private String buildFaultDescription(Alert alert) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(levelLabel(alert.getLevel())).append("] ");
        if (alert.getType() != null) sb.append(alert.getType()).append(" - ");
        sb.append(alert.getMessage() != null ? alert.getMessage() : "能耗异常告警");
        return sb.toString();
    }

    private String generateAiSuggestion(Alert alert) {
        try {
            String prompt = String.format(
                    "你是建筑能源管理专家。以下是一条能耗告警，请给出简明扼要的运维处理建议（100字以内）：\n" +
                    "告警级别：%s\n告警类型：%s\n告警信息：%s\n" +
                    "请直接给出处理建议，不要重复告警信息。",
                    levelLabel(alert.getLevel()),
                    alert.getType() != null ? alert.getType() : "未分类",
                    alert.getMessage() != null ? alert.getMessage() : "无"
            );
            String result = cozeService.isConfigured()
                    ? cozeService.chat(prompt)
                    : claudeService.analyzeEnergyData("告警数据", prompt);
            return result != null ? result : getDefaultSuggestion(alert.getLevel());
        } catch (Exception e) {
            log.warn("AI建议生成失败: {}", e.getMessage());
            return getDefaultSuggestion(alert.getLevel());
        }
    }

    private String getDefaultSuggestion(String level) {
        switch (level) {
            case "critical": return "紧急：请立即安排运维人员到场检查，切断异常设备电源，排查安全隐患。";
            case "error": return "请尽快派遣技术人员排查故障设备，检查传感器数据是否正常，必要时重启相关系统。";
            case "warning": return "建议在24小时内安排巡检，检查设备运行参数，确认是否存在异常工况。";
            default: return "建议在下次例行巡检时关注此项，记录相关数据以便后续趋势分析。";
        }
    }

    private String levelLabel(String level) {
        switch (level) {
            case "info": return "信息";
            case "warning": return "警告";
            case "error": return "错误";
            case "critical": return "严重";
            default: return level;
        }
    }

    // ==================== 辅助 ====================

    private void enrichAlerts(List<Alert> list) {
        Map<Integer, String> buildingNames = new HashMap<>();
        Map<Integer, String> deviceNames = new HashMap<>();
        for (Alert a : list) {
            if (a.getBuildingId() != null) {
                a.setBuildingName(buildingNames.computeIfAbsent(a.getBuildingId(),
                        id -> buildingRepo.findAll().stream()
                                .filter(b -> {
                                    try { return Integer.parseInt(b.getBuildingId().replace("B00", "").replace("B0", "")) == id; }
                                    catch (Exception e) { return false; }
                                })
                                .map(Building::getBuildingName)
                                .findFirst().orElse("建筑" + id)));
            }
            if (a.getDeviceId() != null) {
                a.setDeviceName(deviceNames.computeIfAbsent(a.getDeviceId(),
                        id -> deviceRepo.findById(id).map(Device::getDeviceName).orElse("设备" + id)));
            }
        }
    }

    private void enrichWorkOrders(List<WorkOrder> list) {
        Map<Integer, String> buildingNames = new HashMap<>();
        Map<Integer, String> deviceNames = new HashMap<>();
        Map<Integer, String> userNames = new HashMap<>();
        for (WorkOrder wo : list) {
            if (wo.getBuildingId() != null) {
                wo.setBuildingName(buildingNames.computeIfAbsent(wo.getBuildingId(),
                        id -> buildingRepo.findAll().stream()
                                .filter(b -> {
                                    try { return Integer.parseInt(b.getBuildingId().replace("B00", "").replace("B0", "")) == id; }
                                    catch (Exception e) { return false; }
                                })
                                .map(Building::getBuildingName)
                                .findFirst().orElse("建筑" + id)));
            }
            if (wo.getDeviceId() != null) {
                wo.setDeviceName(deviceNames.computeIfAbsent(wo.getDeviceId(),
                        id -> deviceRepo.findById(id).map(Device::getDeviceName).orElse("设备" + id)));
            }
            if (wo.getAlertId() != null) {
                wo.setAlertCode(alertRepo.findById(wo.getAlertId())
                        .map(Alert::getAlertCode).orElse(null));
            }
            if (wo.getAssignedTo() != null) {
                wo.setAssignedName(userNames.computeIfAbsent(wo.getAssignedTo(),
                        id -> userRepo.findById(Long.valueOf(id))
                                .map(u -> u.getRealName() != null ? u.getRealName() : u.getNickname())
                                .orElse("用户" + id)));
            }
        }
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isEmpty() && !"all".equalsIgnoreCase(s);
    }
}
