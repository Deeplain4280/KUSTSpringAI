package com.ai.kust.server.qwen.controller;

import com.ai.kust.common.result.Result;
import com.ai.kust.server.qwen.models.entity.ChatConversation;
import com.ai.kust.server.qwen.models.entity.vo.ChatMessageResponse;
import com.ai.kust.server.qwen.service.ChatMemoryService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/chat")
@RestController
public class ChatMemoryController {
    @Resource
    private ChatMemoryService chatService;

    @PostMapping("/create")
    public Result<Void> create(@RequestBody ChatConversation body) {
        chatService.createConversation(body.getId(), body.getUserEmail(), body.getTitle());
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<ChatConversation>> list(@RequestParam String userEmail) {
        return Result.success(chatService.listConversation(userEmail));
    }

    @GetMapping("/messages")
    public  Result<List<ChatMessageResponse>> messages(@RequestParam String conversationId) {
        return Result.success((chatService.getMessages(conversationId)));
    }
}
