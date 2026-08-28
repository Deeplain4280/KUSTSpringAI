package com.ai.kust.server.qwen.controller;

import com.ai.kust.server.qwen.service.QwenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/qwen")
@RequiredArgsConstructor//等同于@Resours
public class QwenController {

    private final QwenService QwenService;

    @GetMapping(value = "/stream/memory", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String userInput, @RequestParam String sessionId) {
        return QwenService.stream(userInput, sessionId);
    }

    @GetMapping(value = "/stream/memory/role", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String userInput, @RequestParam String sessionId,
                               @RequestParam(required = false) String role) {
        return QwenService.stream(userInput, sessionId, role);
    }
}
