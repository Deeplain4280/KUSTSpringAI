package com.ai.kust.server.chat.service;

public interface ChatService {
    public abstract String chat(String userInput);

    public abstract String chat(String userInput, String role)
}
