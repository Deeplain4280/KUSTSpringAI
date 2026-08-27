package com.ai.kust.server.qwen.service;

import reactor.core.publisher.Flux;

public interface QwenService {

    //记忆功能 流式 线程池 异常处理
    public abstract Flux<String> stream(String userInput, String sessionId);

    Flux<String> stream(String userInput, String sessionId, String role);
}
