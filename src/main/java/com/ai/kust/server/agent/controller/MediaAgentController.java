package com.ai.kust.server.agent.controller;

import com.ai.kust.server.agent.media.service.MediaAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/media-agent")
@RequiredArgsConstructor
public class MediaAgentController {

    private final MediaAgentService mediaAgentService;

    /**
     * 图片创作 Agent（SSE 流式）
     * GET /api/media-agent/chat?userInput=帮我画一只戴眼镜的柴犬&sessionId=xxx
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String userInput, @RequestParam String sessionId) {
        return mediaAgentService.chatStream(userInput, sessionId);
    }
}
