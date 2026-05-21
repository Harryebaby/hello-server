package org.example.helloserver.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRecord {
    private String sessionId;
    private String userMessage;
    private String assistantMessage;
    private LocalDateTime createTime;
}
