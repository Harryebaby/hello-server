package org.example.helloserver.service;

import org.example.helloserver.entity.ChatRecord;

import java.util.List;

public interface ChatMemoryStore {
    List<ChatRecord> findRecent(String sessionId, int limit);

    void append(ChatRecord record);

    void trimToRecent(String sessionId, int maxRecords);
}
