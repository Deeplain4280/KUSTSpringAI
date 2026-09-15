package com.ai.kust.server.agent.media.service.impl;

import com.ai.kust.common.exception.BusinessException;
import com.ai.kust.common.result.ResultCode;
import com.ai.kust.server.agent.media.service.MediaAgentService;
import com.ai.kust.server.agent.media.tool.MediaGenTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class MediaAgentServiceImpl implements MediaAgentService {

    private static final String SYSTEM_MEDIA_PROMPT = """
            你是一个活泼、专业的图片创作助手，工作流程如下：
            1) 先理解用户的图片需求，如果画面主体、风格等信息不完整，主动追问，不要臆测；
            2) 信息足够时，把用户的描述整理成详细、具体的提示词，调用生成图片工具；
            3) 图片生成后，用简洁的话告诉用户图片已生成，附上图片地址，并提醒地址约24小时有效、请尽快查看或保存；
            4) 如果用户要求生成视频，请礼貌说明：视频生成需要几分钟时间，请使用平台的视频生成接口
               （POST /api/media/video 提交任务，返回 taskId 后轮询 GET /api/media/video/{taskId}），
               本助手当前负责图片创作；
            5) 图片生成失败时，把原因用通俗的语言转述给用户，并建议更换或补充描述后重试；
            6) 全程不要出现工具名称，回答简洁友好。
            """;

    private final ChatClient mediaChatClient;
    private final Executor aiExecutor;
    private final MessageChatMemoryAdvisor memoryAdvisor;
    private final MediaGenTool mediaGenTool;

    public MediaAgentServiceImpl(ChatClient.Builder chatBuilder,
                                 @Qualifier("aiExecutor") Executor aiExecutor,
                                 ChatMemory chatMemory,
                                 MediaGenTool mediaGenTool) {
        this.aiExecutor = aiExecutor;
        this.mediaGenTool = mediaGenTool;
        this.memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        this.mediaChatClient = chatBuilder
                .defaultSystem(SYSTEM_MEDIA_PROMPT)
                .defaultTools(mediaGenTool)
                .build();
    }

    @Override
    public Flux<String> chatStream(String userInput, String sessionId) {
        log.info("媒体创作Agent对话受理, sessionId={}, userInput={}", sessionId, userInput);
        if (isLikelyToolCall(userInput)) {
            // 疑似要生成图片：走非流式，让模型完整执行工具调用后一次性返回结果
            return callNoStream(userInput, sessionId);
        }
        return callStream(userInput, sessionId);
    }

    /** 粗判用户是否在要求生成图片（命中时降级为非流式，工具调用链路更稳） */
    private boolean isLikelyToolCall(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return false;
        }
        String lower = userInput.toLowerCase();
        return lower.contains("画") || lower.contains("生成") || lower.contains("图片")
                || lower.contains("海报") || lower.contains("头像") || lower.contains("插画")
                || lower.contains("壁纸") || lower.contains("漫画") || lower.contains("出图")
                || lower.contains("logo");
    }

    private Flux<String> callNoStream(String userInput, String sessionId) {
        return Flux.defer(() -> {
            try {
                log.info("媒体创作Agent非流式调用, sessionId={}", sessionId);
                String content = mediaChatClient.prompt()
                        .user(userInput)
                        .advisors(memoryAdvisor)
                        .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))
                        .call()
                        .content();
                if (content == null || content.isBlank()) {
                    content = "处理完成，请查看上一条回复。";
                }
                return Flux.just(content);
            } catch (Exception e) {
                log.error("媒体创作Agent非流式调用失败, sessionId={}", sessionId, e);
                return Flux.error(new BusinessException(ResultCode.AI_CALL_FAILED, e));
            }
        }).subscribeOn(Schedulers.fromExecutor(aiExecutor));
    }

    private Flux<String> callStream(String userInput, String sessionId) {
        return mediaChatClient.prompt()
                .user(userInput)
                .advisors(memoryAdvisor)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .content()
                .timeout(Duration.ofSeconds(60))
                .subscribeOn(Schedulers.fromExecutor(aiExecutor))
                .doOnError(e -> log.error("媒体创作Agent流式对话失败, sessionId={}", sessionId, e))
                .onErrorMap(e -> new BusinessException(ResultCode.AI_CALL_FAILED));
    }
}
