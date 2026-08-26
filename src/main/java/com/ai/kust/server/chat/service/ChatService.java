package com.ai.kust.server.chat.service;

import reactor.core.publisher.Flux;

public interface ChatService {
    public abstract String chat(String userInput);

    public abstract String chat(String userInput, String role);

    public abstract Flux<String> chatStream(String userInput);

    public abstract Flux<String> chatStream(String userInput, String role);
}
