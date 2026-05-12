package org.example.helloserver.service;

import org.example.helloserver.dto.ChatRequestDTO;
import org.example.helloserver.vo.ChatResponseVO;

public interface ChatService {
    ChatResponseVO chat(ChatRequestDTO requestDTO);
}
