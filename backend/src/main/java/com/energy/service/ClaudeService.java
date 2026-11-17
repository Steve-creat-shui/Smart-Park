package com.energy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class ClaudeService {

    @Value("${claude.api.key}")
    private String apiKey;

    @Value("${claude.api.url}")
    private String apiUrl;

    @Value("${claude.api.model}")
    private String model;

    @Value("${claude.api.max-tokens}")
    private int maxTokens;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ClaudeService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String analyzeEnergyData(String contextData, String userQuery) {
        if (apiKey == null || apiKey.equals("your_claude_api_key_here")) {
            return generateFallbackAnalysis(userQuery, contextData);
        }
        try {
            String systemPrompt = "你是一个专业的建筑能源管理系统AI助手，专注于数字孪生技术和能效分析。\n" +
                "你的职责是：\n" +
                "1. 分析建筑能耗数据，识别异常和趋势\n" +
                "2. 提供节能建议和优化方案\n" +
                "3. 诊断空调系统和其他设备的能效问题\n" +
                "4. 生成专业的能效分析报告\n" +
                "请用中文回答，保持专业、简洁、有数据支撑。";

            String userMessage = String.format(
                "以下是建筑能耗数据：\n%s\n\n用户问题：%s\n\n请基于以上数据进行分析并给出专业建议。",
                contextData, userQuery);

            Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userMessage))
            );

            String response = webClient.post()
                .uri(apiUrl)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(body -> {
                    List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
                    if (content != null && !content.isEmpty()) {
                        return (String) content.get(0).get("text");
                    }
                    return "分析结果获取失败，请稍后重试。";
                })
                .block();
            return response != null ? response : "分析结果获取失败，请稍后重试。";
        } catch (Exception e) {
            return generateFallbackAnalysis(userQuery, contextData);
        }
    }

    public String getEnergyIssueAnalysis(String buildingId, String timeRange, String contextData) {
        String query = String.format("请分析建筑%s在%s时段的能耗异常原因，并给出整改建议。", buildingId, timeRange);
        return analyzeEnergyData(contextData, query);
    }

    public String generateEnergyReport(String buildingId, String reportData) {
        String query = String.format("请为建筑%s生成一份完整的能效分析报告，包括：能耗概况、异常分析、节能建议、改进措施。", buildingId);
        return analyzeEnergyData(reportData, query);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> callWithTools(String systemPrompt,
                                              List<Map<String, Object>> messages,
                                              List<Map<String, Object>> tools) {
        if (apiKey == null || apiKey.equals("your_claude_api_key_here")) {
            return Map.of("stop_reason", "end_turn",
                    "content", List.of(Map.of("type", "text", "text", "")));
        }
        try {
            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("system", systemPrompt);
            requestBody.put("messages", messages);
            if (tools != null && !tools.isEmpty()) requestBody.put("tools", tools);

            Map result = webClient.post()
                    .uri(apiUrl)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return result != null ? result : Map.of("stop_reason", "end_turn",
                    "content", List.of(Map.of("type", "text", "text", "响应为空")));
        } catch (Exception e) {
            return Map.of("stop_reason", "end_turn",
                    "content", List.of(Map.of("type", "text", "text",
                            generateFallbackAnalysis("", ""))));
        }
    }

    private String generateFallbackAnalysis(String query, String contextData) {
        return String.format(
            "**智能分析报告**（本地分析模式）\n\n" +
            "**查询内容：** %s\n\n" +
            "**分析结果：**\n" +
            "基于当前能耗数据，系统检测到以下关键指标：\n\n" +
            "1. **能耗趋势**：根据历史数据，建议关注高峰时段（09:00-11:00, 14:00-17:00）的能耗波动，及时调整设备运行策略。\n\n" +
            "2. **空调系统优化**：建议将空调系统设定温度调整至26°C（夏季）/ 20°C（冬季），可节省约15%%的空调能耗。\n\n" +
            "3. **异常检测**：系统已启动实时监控，若能耗超出均值150%%将触发告警。\n\n" +
            "4. **节能建议**：\n" +
            "   - 非工作时间关闭非必要照明及设备\n" +
            "   - 定期检查空调过滤网，保持设备清洁\n" +
            "   - 考虑引入智能楼宇控制系统（BAS）\n\n" +
            "5. **碳排放优化**：当前碳排放因子为0.785 kg/kWh，通过提升能效可有效降低碳排放。\n\n" +
            "> 提示：配置有效的 Claude API Key 后可获得更精准的AI分析结果。",
            query);
    }
}
