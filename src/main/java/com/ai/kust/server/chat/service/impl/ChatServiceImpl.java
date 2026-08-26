package com.ai.kust.server.chat.service.impl;

import com.ai.kust.common.exception.BusinessException;
import com.ai.kust.common.result.ResultCode;
import com.ai.kust.server.chat.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {
    private final ChatClient chatClient;
    private final Executor aiExecutor;

    public ChatServiceImpl(@Qualifier("chatClient") ChatClient chatClient,
                          @Qualifier("aiExecutor") Executor aiExecutor) {
        this.chatClient = chatClient;
        this.aiExecutor = aiExecutor;
    }

    public String chat(String userInput) {
        try {
            log.debug("ai对话内容:{}",userInput);
            String content = CompletableFuture.supplyAsync(()->
                    chatClient.prompt().user(userInput).call().content(), aiExecutor
                    ).join();
            log.debug("AI响应对话内容：{}", content);
            return content;
        }catch (Exception e) {
            log.error("ai对话异常，用户输入：{}", userInput,e);
            throw new BusinessException(ResultCode.AI_CALL_FAILED, e);
        }
    }
}
