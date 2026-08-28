package com.ai.kust.server.agent.email.service;

import reactor.core.publisher.Flux;

public interface EmailService {

    Flux<String> chatStream(String userId, String sessionId);
}
