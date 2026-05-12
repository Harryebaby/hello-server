package org.example.helloserver.service.impl;

import org.example.helloserver.config.DeepSeekProperties;
import org.example.helloserver.dto.ChatRequestDTO;
import org.example.helloserver.entity.ChatRecord;
import org.example.helloserver.service.ChatMemoryStore;
import org.example.helloserver.service.ChatService;
import org.example.helloserver.vo.ChatResponseVO;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private static final String SYSTEM_PROMPT = "你是一名专业、友好、简洁的中文智能助手，请根据用户问题直接给出有帮助的回答。";
    private static final int MAX_HISTORY_RECORDS = 3;

    private final RestTemplate restTemplate;
    private final DeepSeekProperties deepSeekProperties;
    private final ChatMemoryStore chatMemoryStore;

    public ChatServiceImpl(RestTemplate restTemplate,
                           DeepSeekProperties deepSeekProperties,
                           ChatMemoryStore chatMemoryStore) {
        this.restTemplate = restTemplate;
        this.deepSeekProperties = deepSeekProperties;
        this.chatMemoryStore = chatMemoryStore;
    }

    @Override
    public ChatResponseVO chat(ChatRequestDTO requestDTO) {
        if (requestDTO == null || !StringUtils.hasText(requestDTO.getSessionId())) {
            throw new IllegalArgumentException("会话编号不能为空");
        }
        String sessionId = requestDTO.getSessionId();
        String message = requestDTO.getMessage();
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("聊天内容不能为空");
        }

        String finalPrompt = buildPrompt(chatMemoryStore.findRecent(sessionId, MAX_HISTORY_RECORDS), message);
        String answer = callModel(finalPrompt);

        chatMemoryStore.append(new ChatRecord(sessionId, message, answer, LocalDateTime.now()));
        chatMemoryStore.trimToRecent(sessionId, MAX_HISTORY_RECORDS);

        return new ChatResponseVO(message, answer);
    }

    private String buildPrompt(List<ChatRecord> records, String message) {
        String historyText = records == null || records.isEmpty()
                ? "（无）"
                : records.stream()
                .map(record -> "用户：" + record.getUserMessage() + "\n助手：" + record.getAssistantMessage())
                .collect(Collectors.joining("\n\n"));

        return """
                以下是历史对话：
                %s

                当前用户问题：
                %s
                """.formatted(historyText, message);
    }

    private String callModel(String prompt) {
        if (!StringUtils.hasText(deepSeekProperties.getApiKey())) {
            throw new IllegalStateException("请先配置 DEEPSEEK_API_KEY 环境变量");
        }

        DeepSeekChatRequest request = new DeepSeekChatRequest(
                deepSeekProperties.getModel(),
                List.of(
                        new ChatMessage("system", SYSTEM_PROMPT),
                        new ChatMessage("user", prompt)
                ),
                new Thinking("disabled"),
                deepSeekProperties.getTemperature(),
                false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepSeekProperties.getApiKey());

        try {
            ResponseEntity<DeepSeekChatResponse> response = restTemplate.exchange(
                    buildChatCompletionsUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    DeepSeekChatResponse.class
            );
            return extractAnswer(response.getBody());
        } catch (RestClientException e) {
            throw new IllegalStateException("调用 DeepSeek 模型失败：" + e.getMessage(), e);
        }
    }

    private String buildChatCompletionsUrl() {
        String baseUrl = deepSeekProperties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/chat/completions";
    }

    private String extractAnswer(DeepSeekChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("DeepSeek 未返回有效回答");
        }

        Choice firstChoice = response.choices().get(0);
        if (firstChoice == null || firstChoice.message() == null
                || !StringUtils.hasText(firstChoice.message().content())) {
            throw new IllegalStateException("DeepSeek 回答内容为空");
        }
        return firstChoice.message().content();
    }

    private record DeepSeekChatRequest(
            String model,
            List<ChatMessage> messages,
            Thinking thinking,
            Double temperature,
            Boolean stream
    ) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record Thinking(String type) {
    }

    private record DeepSeekChatResponse(List<Choice> choices) {
    }

    private record Choice(ChatMessage message) {
    }
}
