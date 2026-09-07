package com.ai.kust.server.agent.media.service;

import reactor.core.publisher.Flux;

public interface MediaAgentService {

    /**
     * 图片创作 Agent 对话（含工具调用能力，带会话记忆）
     */
    Flux<String> chatStream(String userInput, String sessionId);
}
