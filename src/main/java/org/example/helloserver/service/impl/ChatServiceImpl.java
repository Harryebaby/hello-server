package org.example.helloserver.service.impl;

import org.example.helloserver.config.DeepSeekProperties;
import org.example.helloserver.service.ChatService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private static final String SYSTEM_PROMPT = "你是一名专业、友好、简洁的中文智能助手，请根据用户问题直接给出有帮助的回答。";

    private final RestTemplate restTemplate;
    private final DeepSeekProperties deepSeekProperties;

    public ChatServiceImpl(RestTemplate restTemplate, DeepSeekProperties deepSeekProperties) {
        this.restTemplate = restTemplate;
        this.deepSeekProperties = deepSeekProperties;
    }

    @Override
    public String chat(String message) {
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("聊天内容不能为空");
        }
        if (!StringUtils.hasText(deepSeekProperties.getApiKey())) {
            throw new IllegalStateException("请先配置 DEEPSEEK_API_KEY 环境变量");
        }

        DeepSeekChatRequest request = new DeepSeekChatRequest(
                deepSeekProperties.getModel(),
                List.of(
                        new ChatMessage("system", SYSTEM_PROMPT),
                        new ChatMessage("user", message)
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
