package org.example.helloserver.service.impl;

import org.example.helloserver.config.DeepSeekProperties;
import org.example.helloserver.dto.ChatRequestDTO;
import org.example.helloserver.entity.ChatRecord;
import org.example.helloserver.service.ChatMemoryStore;
import org.example.helloserver.vo.ChatResponseVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ChatServiceImplTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private ChatMemoryStore chatMemoryStore;
    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        chatMemoryStore = new InMemoryChatMemoryStore();

        DeepSeekProperties properties = new DeepSeekProperties();
        properties.setApiKey("test-api-key");
        properties.setBaseUrl("http://deepseek.test/v1");
        properties.setModel("deepseek-test");
        properties.setTemperature(0.7);

        chatService = new ChatServiceImpl(restTemplate, properties, chatMemoryStore);
    }

    @Test
    void chatShouldSendSameSessionHistoryToModel() {
        expectDeepSeekRequest(null, "第一轮回答");
        expectDeepSeekRequest("第一轮回答", "第二轮回答");

        ChatResponseVO first = chatService.chat(request("test001", "这是服务端课程，你是谁？"));

        assertThat(first.getQuestion()).isEqualTo("这是服务端课程，你是谁？");
        assertThat(first.getAnswer()).isEqualTo("第一轮回答");

        ChatResponseVO second = chatService.chat(request("test001", "之前和你聊过什么？"));

        assertThat(second.getQuestion()).isEqualTo("之前和你聊过什么？");
        assertThat(second.getAnswer()).isEqualTo("第二轮回答");
        server.verify();
    }

    @Test
    void chatShouldOnlyKeepLatestThreeRecords() {
        for (int i = 1; i <= 4; i++) {
            expectDeepSeekRequest(null, "回答" + i);
        }

        for (int i = 1; i <= 4; i++) {
            chatService.chat(request("test002", "问题" + i));
        }

        List<ChatRecord> records = chatMemoryStore.findRecent("test002", 10);

        assertThat(records).extracting(ChatRecord::getUserMessage)
                .containsExactly("问题2", "问题3", "问题4");
        server.verify();
    }

    @Test
    void chatShouldRejectBlankSessionIdAndMessage() {
        assertThatThrownBy(() -> chatService.chat(request(" ", "你好")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("会话编号不能为空");

        assertThatThrownBy(() -> chatService.chat(request("test003", " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("聊天内容不能为空");
    }

    private void expectDeepSeekRequest(String expectedHistory, String answer) {
        var expectation = server.expect(requestTo("http://deepseek.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("当前用户问题")));

        if (expectedHistory != null) {
            expectation.andExpect(content().string(containsString(expectedHistory)));
        }

        expectation.andRespond(withSuccess("""
                {"choices":[{"message":{"role":"assistant","content":"%s"}}]}
                """.formatted(answer), MediaType.APPLICATION_JSON));
    }

    private ChatRequestDTO request(String sessionId, String message) {
        ChatRequestDTO requestDTO = new ChatRequestDTO();
        requestDTO.setSessionId(sessionId);
        requestDTO.setMessage(message);
        return requestDTO;
    }
}
