package org.example.helloserver.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.helloserver.entity.ChatRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisChatMemoryStoreTest {

    private StringRedisTemplate stringRedisTemplate;
    private ListOperations<String, String> listOperations;
    private RedisChatMemoryStore redisChatMemoryStore;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        listOperations = mock(ListOperations.class);
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        redisChatMemoryStore = new RedisChatMemoryStore(stringRedisTemplate, objectMapper);
    }

    @Test
    void appendShouldWriteRecordToSessionListKey() {
        ChatRecord record = new ChatRecord(
                "test001",
                "这是服务端课程，你是谁？",
                "我是你的课程助手。",
                LocalDateTime.of(2026, 5, 20, 21, 0)
        );

        redisChatMemoryStore.append(record);

        verify(listOperations).rightPush(eq("chat:session:test001"),
                eq("{\"sessionId\":\"test001\",\"userMessage\":\"这是服务端课程，你是谁？\",\"assistantMessage\":\"我是你的课程助手。\",\"createTime\":[2026,5,20,21,0]}"));
    }

    @Test
    void findRecentShouldReadLatestRecordsFromSessionListKey() {
        when(listOperations.range("chat:session:test001", -3, -1)).thenReturn(List.of(
                "{\"sessionId\":\"test001\",\"userMessage\":\"问题1\",\"assistantMessage\":\"回答1\",\"createTime\":[2026,5,20,21,0]}",
                "{\"sessionId\":\"test001\",\"userMessage\":\"问题2\",\"assistantMessage\":\"回答2\",\"createTime\":[2026,5,20,21,1]}"
        ));

        List<ChatRecord> records = redisChatMemoryStore.findRecent("test001", 3);

        assertThat(records).extracting(ChatRecord::getUserMessage)
                .containsExactly("问题1", "问题2");
    }

    @Test
    void trimToRecentShouldKeepOnlyLatestRecords() {
        redisChatMemoryStore.trimToRecent("test001", 3);

        verify(listOperations).trim("chat:session:test001", -3, -1);
    }
}
