package org.example.helloserver.service.impl;

import org.example.helloserver.entity.ChatRecord;
import org.example.helloserver.service.ChatMemoryStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryChatMemoryStore implements ChatMemoryStore {

    private final Map<String, Deque<ChatRecord>> records = new ConcurrentHashMap<>();

    @Override
    public List<ChatRecord> findRecent(String sessionId, int limit) {
        if (!StringUtils.hasText(sessionId) || limit <= 0) {
            return List.of();
        }

        Deque<ChatRecord> sessionRecords = records.get(sessionId);
        if (sessionRecords == null) {
            return List.of();
        }

        synchronized (sessionRecords) {
            int skip = Math.max(0, sessionRecords.size() - limit);
            List<ChatRecord> result = new ArrayList<>();
            int index = 0;
            for (ChatRecord record : sessionRecords) {
                if (index >= skip) {
                    result.add(record);
                }
                index++;
            }
            return result;
        }
    }

    @Override
    public void append(ChatRecord record) {
        if (record == null || !StringUtils.hasText(record.getSessionId())) {
            return;
        }

        Deque<ChatRecord> sessionRecords = records.computeIfAbsent(record.getSessionId(), key -> new ArrayDeque<>());
        synchronized (sessionRecords) {
            sessionRecords.addLast(record);
        }
    }

    @Override
    public void trimToRecent(String sessionId, int maxRecords) {
        if (!StringUtils.hasText(sessionId) || maxRecords < 0) {
            return;
        }

        Deque<ChatRecord> sessionRecords = records.get(sessionId);
        if (sessionRecords == null) {
            return;
        }

        synchronized (sessionRecords) {
            while (sessionRecords.size() > maxRecords) {
                sessionRecords.removeFirst();
            }
        }
    }
}
