package org.example.helloserver.controller;

import org.example.helloserver.common.Result;
import org.example.helloserver.common.ResultCode;
import org.example.helloserver.dto.ChatRequestDTO;
import org.example.helloserver.service.ChatService;
import org.example.helloserver.vo.ChatResponseVO;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public Result<ChatResponseVO> chat(@RequestBody ChatRequestDTO requestDTO) {
        if (requestDTO == null || !StringUtils.hasText(requestDTO.getMessage())) {
            return Result.error(ResultCode.ERROR);
        }

        String answer = chatService.chat(requestDTO.getMessage());
        ChatResponseVO responseVO = new ChatResponseVO(requestDTO.getMessage(), answer);
        return Result.success(responseVO);
    }
}
