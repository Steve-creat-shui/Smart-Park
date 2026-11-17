package com.energy.service;

import com.energy.dto.EnergyAnalysisRequest;
import com.energy.dto.EnergyAnalysisResponse;
import com.energy.entity.AgentKnowledge;
import com.energy.entity.ConversationFeedback;
import com.energy.repository.AgentKnowledgeRepository;
import com.energy.repository.BuildingRepository;
import com.energy.repository.ConversationFeedbackRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    private final CozeService cozeService;
    private final ClaudeService claudeService;
    private final EnergyService energyService;
    private final BuildingRepository buildingRepo;
    private final AgentKnowledgeRepository knowledgeRepo;
    private final ConversationFeedbackRepository feedbackRepo;
    private final ObjectMapper objectMapper;

    public Map<String, Object> agentQuery(String buildingId, String query,
                                           List<Map<String, Object>> simpleHistory) {
        // 优先使用 Coze 智能体
        if (cozeService.isConfigured()) {
            return agentQueryViaCoze(buildingId, query, simpleHistory);
        }
        // 降级使用 Claude tool-calling
        return agentQueryViaClaude(buildingId, query, simpleHistory);
    }

    /* ── Coze 路径：预采集数据 → 合并上下文 → 一次调用 ── */
    private Map<String, Object> agentQueryViaCoze(String buildingId, String query,
                                                   List<Map<String, Object>> simpleHistory) {
        List<Map<String, Object>> toolCallRecords = new ArrayList<>();
        String finalResult = null;
        try {
            // 1. 预采集本地数据
            String energyData = executeTool("query_energy_data",
                    Map.of("building_id", buildingId, "days", 30), buildingId);
            toolCallRecords.add(Map.of("tool", "query_energy_data",
                    "input", buildingId, "output", shorten(energyData, 300)));

            String anomalyData = executeTool("get_anomalies",
                    Map.of("building_id", buildingId, "days", 30), buildingId);
            toolCallRecords.add(Map.of("tool", "get_anomalies",
                    "input", buildingId, "output", shorten(anomalyData, 300)));

            String buildingInfo = executeTool("get_building_info",
                    Map.of("building_id", buildingId), buildingId);
            toolCallRecords.add(Map.of("tool", "get_building_info",
                    "input", buildingId, "output", shorten(buildingInfo, 200)));

            // 2. 构建历史上下文（仅保留最近4条）
            StringBuilder historyCtx = new StringBuilder();
            if (simpleHistory != null) {
                int start = Math.max(0, simpleHistory.size() - 4);
                for (int idx = start; idx < simpleHistory.size(); idx++) {
                    Map<String, Object> h = simpleHistory.get(idx);
                    String content = str(h.get("content"));
                    if (content.length() > 200) content = content.substring(0, 200) + "...";
                    historyCtx.append(String.format("[%s]: %s\n", h.get("role"), content));
                }
            }

            // 3. 合并为精简消息发给 Coze
            String fullMessage = String.format(
                    "【数据上下文】\n建筑: %s\n%s\n能耗: %s\n异常: %s\n%s" +
                    "【问题】%s",
                    buildingInfo, "",
                    shorten(energyData, 400), shorten(anomalyData, 300),
                    historyCtx.length() > 0 ? "【对话历史】\n" + historyCtx + "\n" : "",
                    query);

            finalResult = cozeService.chat(fullMessage, "energy_agent_" + buildingId);
        } catch (Exception e) {
            log.error("Coze agent query failed: {}", e.getMessage());
        }

        if (finalResult == null) {
            finalResult = generateLocalFallback(buildingId, query);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("buildingId", buildingId);
        result.put("query", query);
        result.put("result", finalResult);
        result.put("toolCalls", toolCallRecords);
        return result;
    }

    /* ── Claude 路径：原有 tool-calling 循环（降级） ── */
    private Map<String, Object> agentQueryViaClaude(String buildingId, String query,
                                                     List<Map<String, Object>> simpleHistory) {
        List<AgentKnowledge> knowledgeList = knowledgeRepo.findAllByOrderByCreatedAtDesc();
        List<ConversationFeedback> examples = feedbackRepo.findByRatingOrderByCreatedAtDesc(1);
        if (examples.size() > 5) examples = examples.subList(0, 5);

        String systemPrompt = buildSystemPrompt(knowledgeList, examples);
        List<Map<String, Object>> tools = buildTools();

        List<Map<String, Object>> messages = new ArrayList<>();
        if (simpleHistory != null) {
            for (Map<String, Object> h : simpleHistory) {
                Object role = h.get("role");
                Object content = h.get("content");
                if (role != null && content != null) {
                    messages.add(Map.of("role", role.toString(), "content", content.toString()));
                }
            }
        }
        messages.add(Map.of("role", "user", "content",
                String.format("当前关注建筑: %s。问题: %s", buildingId, query)));

        List<Map<String, Object>> toolCallRecords = new ArrayList<>();
        String finalResult = null;

        for (int i = 0; i < 6; i++) {
            Map<String, Object> response = claudeService.callWithTools(systemPrompt, messages, tools);
            if (response == null) break;

            String stopReason = (String) response.get("stop_reason");
            Object rawContent = response.get("content");
            if (!(rawContent instanceof List)) break;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) rawContent;

            if ("tool_use".equals(stopReason)) {
                messages.add(Map.of("role", "assistant", "content", content));

                List<Map<String, Object>> toolResults = new ArrayList<>();
                for (Map<String, Object> block : content) {
                    if ("tool_use".equals(block.get("type"))) {
                        String toolName = (String) block.get("name");
                        String toolId = (String) block.get("id");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> input = (Map<String, Object>) block.get("input");

                        String toolOutput = executeTool(toolName, input, buildingId);
                        String shortOutput = toolOutput.length() > 600
                                ? toolOutput.substring(0, 600) + "..." : toolOutput;

                        toolCallRecords.add(Map.of(
                                "tool", toolName,
                                "input", inputToString(input),
                                "output", shortOutput
                        ));
                        toolResults.add(Map.of(
                                "type", "tool_result",
                                "tool_use_id", toolId,
                                "content", toolOutput
                        ));
                    }
                }
                messages.add(Map.of("role", "user", "content", toolResults));

            } else {
                finalResult = content.stream()
                        .filter(c -> "text".equals(c.get("type")))
                        .map(c -> (String) c.get("text"))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
                break;
            }
        }

        if (finalResult == null) {
            finalResult = generateLocalFallback(buildingId, query);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("buildingId", buildingId);
        result.put("query", query);
        result.put("result", finalResult);
        result.put("toolCalls", toolCallRecords);
        return result;
    }

    private String shorten(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private String buildKnowledgeContext(List<AgentKnowledge> kList, List<ConversationFeedback> examples) {
        StringBuilder sb = new StringBuilder();
        if (!kList.isEmpty()) {
            kList.forEach(k -> sb.append(String.format("[%s] %s: %s\n", k.getCategory(), k.getTitle(), k.getContent())));
        }
        if (!examples.isEmpty()) {
            sb.append("\n优质回答示例：\n");
            examples.forEach(f -> {
                String q = f.getQuestion().length() > 60 ? f.getQuestion().substring(0, 60) + "..." : f.getQuestion();
                String a = f.getAnswer().length() > 200 ? f.getAnswer().substring(0, 200) + "..." : f.getAnswer();
                sb.append(String.format("Q: %s\nA: %s\n\n", q, a));
            });
        }
        return sb.toString();
    }

    private String executeTool(String toolName, Map<String, Object> input, String defaultBid) {
        try {
            switch (toolName) {
                case "query_energy_data":   return execQueryEnergy(input, defaultBid);
                case "get_anomalies":       return execGetAnomalies(input, defaultBid);
                case "compare_buildings":   return execCompare(input);
                case "get_building_info":   return execBuildingInfo(input, defaultBid);
                default: return "未知工具: " + toolName;
            }
        } catch (Exception e) {
            log.warn("Tool {} failed: {}", toolName, e.getMessage());
            return "工具执行出错: " + e.getMessage();
        }
    }

    private String execQueryEnergy(Map<String, Object> input, String def) {
        String bid = str(input.getOrDefault("building_id", def));
        int days = num(input.getOrDefault("days", 30));
        EnergyAnalysisRequest req = makeReq(bid, days);
        EnergyAnalysisResponse r = energyService.analyzeEnergy(req);
        return String.format(
                "建筑%s近%d天能耗数据：\n总用电量: %.2f kWh\n总用水量: %.2f m³\n总碳排放: %.2f kg\n"
                + "空调能耗: %.2f kWh\n日均用电: %.2f kWh\n能效评分: %.1f\n能效等级: %s\n异常记录: %d条",
                bid, days, safe(r.getTotalPowerConsumption()), safe(r.getTotalWaterConsumption()),
                safe(r.getTotalCarbonEmission()), safe(r.getTotalAcConsumption()),
                safe(r.getAvgPowerConsumption()), safe(r.getEnergyEfficiencyScore()),
                r.getEnergyRating() != null ? r.getEnergyRating() : "N/A",
                r.getAnomalies() != null ? r.getAnomalies().size() : 0);
    }

    private String execGetAnomalies(Map<String, Object> input, String def) {
        String bid = str(input.getOrDefault("building_id", def));
        int days = num(input.getOrDefault("days", 30));
        EnergyAnalysisRequest req = makeReq(bid, days);
        EnergyAnalysisResponse r = energyService.analyzeEnergy(req);
        if (r.getAnomalies() == null || r.getAnomalies().isEmpty()) {
            return "建筑" + bid + "近" + days + "天未检测到能耗异常。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("建筑%s近%d天共%d条异常：\n", bid, days, r.getAnomalies().size()));
        r.getAnomalies().stream().limit(8).forEach(a ->
                sb.append(String.format("- %s: %.2f kWh (偏差 %.1f%%)\n",
                        a.getOrDefault("timestamp", "未知"),
                        toDouble(a.get("power")),
                        toDouble(a.get("deviationPercent")))));
        return sb.toString();
    }

    private String execCompare(Map<String, Object> input) {
        @SuppressWarnings("unchecked")
        List<String> bids = (List<String>) input.getOrDefault("building_ids", List.of("B001", "B002"));
        StringBuilder sb = new StringBuilder("多建筑近30天能耗对比：\n");
        for (String bid : bids) {
            EnergyAnalysisRequest req = makeReq(bid, 30);
            EnergyAnalysisResponse r = energyService.analyzeEnergy(req);
            sb.append(String.format("- %s: %.2f kWh | 评分%.1f(%s) | 异常%d条\n",
                    bid, safe(r.getTotalPowerConsumption()), safe(r.getEnergyEfficiencyScore()),
                    r.getEnergyRating() != null ? r.getEnergyRating() : "N/A",
                    r.getAnomalies() != null ? r.getAnomalies().size() : 0));
        }
        return sb.toString();
    }

    private String execBuildingInfo(Map<String, Object> input, String def) {
        String bid = str(input.getOrDefault("building_id", def));
        return buildingRepo.findById(bid)
                .map(b -> String.format(
                        "建筑%s：名称=%s, 类型=%s, 楼层=%d, 面积=%.0f m², 空调=%s, 能效=%s, 状态=%s",
                        b.getBuildingId(), b.getBuildingName(), b.getBuildingType(),
                        b.getFloorCount() != null ? b.getFloorCount() : 0,
                        b.getTotalAreaM2() != null ? b.getTotalAreaM2() : 0,
                        b.getAcSystemType(), b.getEnergyRating(), b.getStatus()))
                .orElse("未找到建筑: " + bid);
    }

    private String generateLocalFallback(String buildingId, String query) {
        try {
            String energyData = execQueryEnergy(Map.of("building_id", buildingId, "days", 30), buildingId);
            String anomalyData = execGetAnomalies(Map.of("building_id", buildingId, "days", 30), buildingId);
            return String.format(
                    "**智能体分析报告（本地模式）**\n\n**问题：** %s\n\n**能耗数据：**\n%s\n\n**异常检测：**\n%s\n\n"
                    + "**建议：**\n1. 高峰时段（09:00-17:00）适当调整设备运行策略\n"
                    + "2. 空调系统设定温度建议调整至26°C（夏季），可节省约15%%能耗\n"
                    + "3. 非工作时间关闭非必要照明及设备\n\n"
                    + "> 配置 Claude API Key 后智能体将自主推理并提供更精准分析",
                    query, energyData, anomalyData);
        } catch (Exception e) {
            return "智能体分析失败：" + e.getMessage();
        }
    }

    private String buildSystemPrompt(List<AgentKnowledge> kList, List<ConversationFeedback> examples) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个建筑能源数字孪生智能体（Agent）。原则：先调用工具获取真实数据，再基于数据进行专业分析。\n\n");
        if (!kList.isEmpty()) {
            sb.append("【知识库】\n");
            kList.forEach(k -> sb.append(String.format("[%s] %s: %s\n",
                    k.getCategory(), k.getTitle(), k.getContent())));
            sb.append("\n");
        }
        if (!examples.isEmpty()) {
            sb.append("【优质回答示例】\n");
            examples.forEach(f -> {
                String q = f.getQuestion().length() > 80 ? f.getQuestion().substring(0, 80) + "..." : f.getQuestion();
                String a = f.getAnswer().length() > 250 ? f.getAnswer().substring(0, 250) + "..." : f.getAnswer();
                sb.append(String.format("Q: %s\nA: %s\n\n", q, a));
            });
        }
        sb.append("用中文回答，专业简洁，必须有数据支撑，给出具体可操作建议。");
        return sb.toString();
    }

    private List<Map<String, Object>> buildTools() {
        return List.of(
            Map.of("name", "query_energy_data",
                "description", "查询指定建筑的能耗统计（总电量/水耗/碳排放/空调/能效评分）",
                "input_schema", Map.of("type", "object",
                    "properties", Map.of(
                        "building_id", Map.of("type", "string", "description", "建筑ID，如B001"),
                        "days", Map.of("type", "integer", "description", "查询近N天，默认30")),
                    "required", List.of("building_id"))),
            Map.of("name", "get_anomalies",
                "description", "获取指定建筑的能耗异常记录，识别超标时段",
                "input_schema", Map.of("type", "object",
                    "properties", Map.of(
                        "building_id", Map.of("type", "string", "description", "建筑ID"),
                        "days", Map.of("type", "integer", "description", "查询范围（天），默认30")),
                    "required", List.of("building_id"))),
            Map.of("name", "compare_buildings",
                "description", "比较多栋建筑近30天能耗表现和能效评分",
                "input_schema", Map.of("type", "object",
                    "properties", Map.of(
                        "building_ids", Map.of("type", "array",
                            "items", Map.of("type", "string"),
                            "description", "要对比的建筑ID列表")),
                    "required", List.of("building_ids"))),
            Map.of("name", "get_building_info",
                "description", "获取建筑基本信息（名称/类型/楼层/面积/空调系统）",
                "input_schema", Map.of("type", "object",
                    "properties", Map.of(
                        "building_id", Map.of("type", "string", "description", "建筑ID")),
                    "required", List.of("building_id")))
        );
    }

    private EnergyAnalysisRequest makeReq(String bid, int days) {
        EnergyAnalysisRequest req = new EnergyAnalysisRequest();
        req.setBuildingId(bid);
        req.setSkipAiAnalysis(true);
        req.setStartTime(LocalDateTime.now().minusDays(days));
        req.setEndTime(LocalDateTime.now());
        return req;
    }

    private double safe(Double d) { return d != null ? d : 0; }
    private double toDouble(Object o) { return o instanceof Number ? ((Number) o).doubleValue() : 0; }
    private String str(Object o) { return o != null ? o.toString() : ""; }
    private int num(Object o) { return o instanceof Number ? ((Number) o).intValue() : 30; }
    private String inputToString(Map<String, Object> m) {
        try { return objectMapper.writeValueAsString(m); } catch (Exception e) { return m.toString(); }
    }
}
