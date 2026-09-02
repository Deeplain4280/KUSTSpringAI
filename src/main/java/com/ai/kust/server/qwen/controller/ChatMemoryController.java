package com.ai.kust.server.qwen.controller;

import com.ai.kust.common.result.Result;
import com.ai.kust.server.qwen.models.entity.ChatConversation;
import com.ai.kust.server.qwen.service.ChatMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/chat")
@RestController
@RequiredArgsConstructor
public class ChatMemoryController {
    private ChatMemoryService chatService;

    public Result<Void> create(@RequestBody ChatConversation body) {
        chatService.createConversation(body.getId(), body.getUserEmail(), body.getTitle());
        return Result.success();
    }
}
