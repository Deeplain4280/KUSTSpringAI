package com.ai.kust.server.qwen.service.impl;

import com.ai.kust.common.exception.BusinessException;
import com.ai.kust.common.result.ResultCode;
import com.ai.kust.server.qwen.service.QwenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class QwenServiceImpl implements QwenService {

    private final ChatClient chatClient;
    private final Executor aiExecutor;
    private final MessageChatMemoryAdvisor memoryAdvisor;

    public QwenServiceImpl (@Qualifier("chatClient") ChatClient chatClient,
                            @Qualifier("aiExecutor") Executor aiExecutor,
                             ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.aiExecutor = aiExecutor;
        this.memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    public Flux<String> stream(String userInput, String sessionId) {
        log.info("AI记忆对话已处理，用户对话： {}, 用户标识：{}",userInput, sessionId);
        return chatClient.prompt()
                .user(userInput)
                .advisors(memoryAdvisor)//添加到上下文记忆窗口
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))//独立ID
                .stream()//流式输出
                .content()
                .timeout(Duration.ofSeconds(30))
                .subscribeOn(Schedulers.fromExecutor(aiExecutor))
                .doOnError(e -> log.error("AI流式对话失败, 用户标识：{}", sessionId, e))
                .onErrorMap(e -> new BusinessException(ResultCode.AI_CALL_FAILED));
    }
    public Flux<String> stream(String userInput, String sessionId, String role) {
        log.info("AI记忆对话已处理，用户对话： {}, 用户标识：{}, 设置模型角色：{}",userInput, sessionId, role);
        ChatClient.ChatClientRequestSpec promptSpec = chatClient.prompt();
        if (StringUtils.hasText(role)) {
            promptSpec = promptSpec.system(role);
        }
        return promptSpec.user(userInput)
                .advisors(memoryAdvisor)//添加到上下文记忆窗口
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))//独立ID
                .stream()//流式输出
                .content()
                .timeout(Duration.ofSeconds(30))
                .subscribeOn(Schedulers.fromExecutor(aiExecutor))
                .doOnError(e -> log.error("AI流式对话失败, 用户标识：{}, 角色提示词：{}", sessionId, role, e))
                .onErrorMap(e -> new BusinessException(ResultCode.AI_CALL_FAILED));

}


}
