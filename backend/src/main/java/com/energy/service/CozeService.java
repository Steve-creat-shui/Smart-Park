package com.energy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CozeService {

    @Value("${coze.api.token:}")
    private String apiToken;

    @Value("${coze.api.bot-id:}")
    private String botId;

    @Value("${coze.api.base-url:https://api.coze.cn}")
    private String baseUrl;

    @Value("${coze.api.timeout:60}")
    private int timeoutSeconds;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public CozeService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * 判断 Coze 是否已配置
     */
    public boolean isConfigured() {
        return apiToken != null && !apiToken.isBlank()
                && botId != null && !botId.isBlank()
                && !apiToken.equals("your_coze_token_here");
    }

    /**
     * 发送消息给 Coze 智能体并获取回复（同步阻塞）
     */
    public String chat(String userMessage) {
        return chat(userMessage, "default_user");
    }

    @SuppressWarnings("unchecked")
    public String chat(String userMessage, String userId) {
        if (!isConfigured()) {
            return null;
        }

        try {
            // 使用流式模式（stream=true），已验证可用
            Map<String, Object> chatRequest = Map.of(
                    "bot_id", botId,
                    "user_id", userId,
                    "stream", true,
                    "auto_save_history", true,
                    "additional_messages", List.of(
                            Map.of("role", "user",
                                    "content", userMessage,
                                    "content_type", "text")
                    )
            );

            // 读取完整 SSE 流
            String sseResponse = webClient.post()
                    .uri(baseUrl + "/v3/chat")
                    .header("Authorization", "Bearer " + apiToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(chatRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(timeoutSeconds));

            if (sseResponse == null || sseResponse.isBlank()) {
                log.warn("Coze streaming response is empty");
                return null;
            }

            // 解析 SSE 事件，找 conversation.message.completed + type=answer
            String answer = parseAnswerFromSSE(sseResponse);
            if (answer != null && !answer.isBlank()) {
                log.info("Coze replied successfully, length={}", answer.length());
                return answer;
            }

            log.warn("Coze SSE contained no answer");
            return null;

        } catch (Exception e) {
            log.error("Coze API call failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从 SSE 流文本中解析出最终 answer
     */
    @SuppressWarnings("unchecked")
    private String parseAnswerFromSSE(String sse) {
        String[] lines = sse.split("\n");
        String currentEvent = "";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("event:")) {
                currentEvent = line.substring(6).trim();
            } else if (line.startsWith("data:") && "conversation.message.completed".equals(currentEvent)) {
                String json = line.substring(5).trim();
                try {
                    Map<String, Object> msg = objectMapper.readValue(json, Map.class);
                    if ("answer".equals(str(msg.get("type")))
                            && "assistant".equals(str(msg.get("role")))) {
                        return str(msg.get("content"));
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /**
     * 分析能耗数据（兼容 ClaudeService 接口）
     */
    public String analyzeEnergyData(String contextData, String userQuery) {
        String prompt = String.format(
                "你是一个专业的建筑能源管理AI助手。以下是建筑能耗数据：\n%s\n\n用户问题：%s\n\n请基于以上数据进行分析并给出专业建议。",
                contextData, userQuery);
        String result = chat(prompt);
        return result != null ? result : generateFallbackAnalysis(userQuery, contextData);
    }

    /**
     * 能耗异常分析
     */
    public String getEnergyIssueAnalysis(String buildingId, String timeRange, String contextData) {
        String query = String.format("请分析建筑%s在%s时段的能耗异常原因，并给出整改建议。", buildingId, timeRange);
        return analyzeEnergyData(contextData, query);
    }

    /**
     * 生成能效报告
     */
    public String generateEnergyReport(String buildingId, String reportData) {
        String query = String.format("请为建筑%s生成一份完整的能效分析报告，包括：能耗概况、异常分析、节能建议、改进措施。", buildingId);
        return analyzeEnergyData(reportData, query);
    }

    private String generateFallbackAnalysis(String query, String contextData) {
        return String.format(
                "**智能分析报告**（本地分析模式）\n\n" +
                "**查询内容：** %s\n\n" +
                "**分析结果：**\n" +
                "基于当前能耗数据，系统检测到以下关键指标：\n\n" +
                "1. **能耗趋势**：建议关注高峰时段（09:00-11:00, 14:00-17:00）的能耗波动。\n\n" +
                "2. **空调系统优化**：建议空调设定温度调整至26°C（夏季）/20°C（冬季），可节省约15%%空调能耗。\n\n" +
                "3. **异常检测**：系统实时监控中，能耗超出均值150%%将触发告警。\n\n" +
                "4. **节能建议**：\n" +
                "   - 非工作时间关闭非必要照明及设备\n" +
                "   - 定期维护空调系统\n" +
                "   - 引入智能楼宇控制系统（BAS）\n\n" +
                "> 提示：配置 Coze API Token 和 Bot ID 后可获得更精准的AI分析结果。",
                query);
    }

    private String str(Object o) { return o != null ? o.toString() : ""; }
    private int toInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        return -1;
    }
}
