package com.ai.kust.server.agent.image2prompt.service;

import reactor.core.publisher.Flux;

public interface ImagePromptAgentService {

    /**
     * 识图反推提示词 Agent（流式）
     *
     * @param imageBytes  图片字节；null/空 表示本轮是纯文字（多轮追问、闲聊）
     * @param contentType 图片 Content-Type（image/jpeg、image/png…）；无图时为 null
     * @param userInput   用户指令，如"识别这张图，反推提示词"
     * @param sessionId   会话ID；同一会话自动记忆多轮对话
     */

    Flux<String> chatStream (byte[] imageBytes, String contentType, String userInput, String sessionId);
}
