package org.example.helloserver.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.helloserver.entity.ChatRecord;
import org.example.helloserver.service.ChatMemoryStore;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Primary
@Service
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final String KEY_PREFIX = "chat:session:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatMemoryStore fallbackStore = new InMemoryChatMemoryStore();

    public RedisChatMemoryStore(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ChatRecord> findRecent(String sessionId, int limit) {
        if (!StringUtils.hasText(sessionId) || limit <= 0) {
            return List.of();
        }

        try {
            List<String> values = stringRedisTemplate.opsForList().range(key(sessionId), -limit, -1);
            if (values == null || values.isEmpty()) {
                return List.of();
            }

            List<ChatRecord> records = new ArrayList<>(values.size());
            for (String value : values) {
                records.add(objectMapper.readValue(value, ChatRecord.class));
            }
            return records;
        } catch (Exception e) {
            return fallbackStore.findRecent(sessionId, limit);
        }
    }

    @Override
    public void append(ChatRecord record) {
        if (record == null || !StringUtils.hasText(record.getSessionId())) {
            return;
        }

        try {
            stringRedisTemplate.opsForList().rightPush(key(record.getSessionId()), serialize(record));
        } catch (Exception e) {
            fallbackStore.append(record);
        }
    }

    @Override
    public void trimToRecent(String sessionId, int maxRecords) {
        if (!StringUtils.hasText(sessionId) || maxRecords <= 0) {
            return;
        }

        try {
            stringRedisTemplate.opsForList().trim(key(sessionId), -maxRecords, -1);
        } catch (Exception e) {
            fallbackStore.trimToRecent(sessionId, maxRecords);
        }
    }

    private String serialize(ChatRecord record) throws JsonProcessingException {
        return objectMapper.writeValueAsString(record);
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
