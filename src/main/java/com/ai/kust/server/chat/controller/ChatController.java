package com.ai.kust.server.chat.controller;

import com.ai.kust.common.result.Result;
import com.ai.kust.server.chat.service.ChatService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Resource
    private ChatService chatService;

    @GetMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<String>chat(@RequestParam @NotBlank(message = "用户输入不得为空") String userInput) {
        String content = chatService.chat(userInput);
        return Result.success(content);
    }
}
