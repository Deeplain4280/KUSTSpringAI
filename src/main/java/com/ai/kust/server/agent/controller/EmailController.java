package com.ai.kust.server.agent.controller;

import com.ai.kust.server.agent.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("api/email")
@RequiredArgsConstructor
public class EmailController {
    private final EmailService emailService;

    @GetMapping("/send")
    public Flux<String> stream (@RequestParam String userInput, @RequestParam String sessionId) {
        return emailService.chatStream(userInput, sessionId);
    }
}
