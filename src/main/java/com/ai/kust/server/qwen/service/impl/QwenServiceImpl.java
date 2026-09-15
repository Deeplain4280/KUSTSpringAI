package com.ai.kust.server.qwen.service.impl;

import com.ai.kust.common.exception.BusinessException;
import com.ai.kust.common.result.ResultCode;
import com.ai.kust.server.qwen.service.QwenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class QwenServiceImpl implements QwenService {

    private final ChatClient chatClient;
    private final Executor aiExecutor;
    private final MessageChatMemoryAdvisor memoryAdvisor;
    private final ChatMemory chatMemory;

    public QwenServiceImpl (@Qualifier("chatClient") ChatClient chatClient,
                            @Qualifier("aiExecutor") Executor aiExecutor,
                             ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.aiExecutor = aiExecutor;
        this.chatMemory = chatMemory;
        this.memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    public Flux<String> stream(String userInput, String sessionId) {
        log.info("AI记忆对话已处理，用户对话： {}, 用户标识：{}",userInput, sessionId);
        StringBuilder reply = new StringBuilder();
        return chatClient.prompt()
                .user(userInput)
                .advisors(memoryAdvisor)//添加到上下文记忆窗口
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))//独立ID
                .stream()//流式输出
                .content()
                .timeout(Duration.ofSeconds(30))
                .subscribeOn(Schedulers.fromExecutor(aiExecutor))
                .doOnNext(reply::append)
                // 流式模式下 advisor 不会自动写库，这里手动持久化（否则侧边栏历史/记忆都会丢失）
                .doOnComplete(() -> persistMemory(sessionId, userInput, reply.toString()))
                .doOnError(e -> log.error("AI流式对话失败, 用户标识：{}", sessionId, e))
                .onErrorMap(e -> new BusinessException(ResultCode.AI_CALL_FAILED));
    }
    public Flux<String> stream(String userInput, String sessionId, String role) {
        log.info("AI记忆对话已处理，用户对话： {}, 用户标识：{}, 设置模型角色：{}",userInput, sessionId, role);
        ChatClient.ChatClientRequestSpec promptSpec = chatClient.prompt();
        if (StringUtils.hasText(role)) {
            promptSpec = promptSpec.system(role);
        }
        StringBuilder reply = new StringBuilder();
        return promptSpec.user(userInput)
                .advisors(memoryAdvisor)//添加到上下文记忆窗口
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))//独立ID
                .stream()//流式输出
                .content()
                .timeout(Duration.ofSeconds(30))
                .subscribeOn(Schedulers.fromExecutor(aiExecutor))
                .doOnNext(reply::append)
                .doOnComplete(() -> persistMemory(sessionId, userInput, reply.toString()))
                .doOnError(e -> log.error("AI流式对话失败, 用户标识：{}, 角色提示词：{}", sessionId, role, e))
                .onErrorMap(e -> new BusinessException(ResultCode.AI_CALL_FAILED));

    }

    /**
     * 流式对话结束后手动写入对话记忆（用户消息 + AI 回复），
     * 供侧边栏历史消息与多轮上下文使用。
     */
    private void persistMemory(String conversationId, String userInput, String assistantReply) {
        try {
            chatMemory.add(conversationId,
                    List.of(new UserMessage(userInput), new AssistantMessage(assistantReply)));
            log.info("对话记忆持久化成功, sessionId={}", conversationId);
        } catch (Exception e) {
            log.error("对话记忆持久化失败, sessionId={}", conversationId, e);
        }
    }


}
