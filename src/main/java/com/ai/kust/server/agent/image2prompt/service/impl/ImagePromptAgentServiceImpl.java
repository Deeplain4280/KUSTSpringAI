package com.ai.kust.server.agent.image2prompt.service.impl;

import com.ai.kust.common.exception.BusinessException;
import com.ai.kust.common.result.ResultCode;
import com.ai.kust.server.agent.image2prompt.service.ImagePromptAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class ImagePromptAgentServiceImpl implements ImagePromptAgentService {

    /**
     * ⚠️ 把你的"识图反推提示词"整段粘贴到下面这个 text block 里，替换示例内容。
     * 若你的提示词里含连续三个双引号，请拆成两段字符串拼接。
     */
    private static final String SYSTEM_IMAGE_PROMPT = """
            你是一名专业的AI提示词逆向工程师，专为AI图片创建大模型提供提示词。\s
            你会通过专业的规则或逻辑去反推用户提供的图片所需的提示词，进而让用户拿到提示词后去使用AI生成，所有提示词务必是中文简体。\s
            确立视觉基调，故事、主体与风格为了获得最佳效果并拥有更细腻的创意控制权，请在您的提示词(Prompt)中包含以下核心要素：\s
            
            主体 (Subject)： 画面中是谁或什么；\s
            构图 (Composition)： 镜头的取景方式是怎样的；\s
            动作 (Action)： 正在发生什么；\s
            地点 (Location)： 场景发生在哪里；\s
            风格 (Style)： 整体美学风格是什么；\s
            
            雕琢细节：相机、布光与格式虽然简单的提示词依然有效，但要获得专业级的效果，需要更具体的指令。\s
            在撰写提示词时，请超越基础描述，加入以下进阶要素：\s
            构图与宽高比 (Composition and aspect ratio)： 定义画布规格；\s
            相机与布光细节 (Camera and lighting details)： 像电影摄影师一样指导镜头(例如"低角度镜头、浅景深svg (f/1.8)"、"黄金时刻的逆光创造出长长的拖影"、"带有柔和青色调的电影感调色")；\s
            特定文本整合 (Specific text integration)： 清晰说明应该出现的文本及其外观；\s
            参考输入 (Reference inputs)： 使用上传的图片时，明确定义每一张的作用。
            """;

    /** 图片上限：百炼 qwen-vl 系列单图一般 ≤10MB */
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final List<String> SUPPORTED_CONTENT_TYPES =
            List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final ChatClient imagePromptChatClient;
    private final ChatMemory chatMemory;   // 与邮件/图片创作 Agent 共用同一份 JDBC 记忆
    private final Executor aiExecutor;

    public ImagePromptAgentServiceImpl(ChatClient.Builder chatBuilder,
                                       @Qualifier("aiExecutor") Executor aiExecutor,
                                       ChatMemory chatMemory) {
        this.aiExecutor = aiExecutor;
        this.chatMemory = chatMemory;
        this.imagePromptChatClient = chatBuilder
                .defaultSystem(SYSTEM_IMAGE_PROMPT)
                .build();
    }

    @Override
    public Flux<String> chatStream(byte[] imageBytes, String contentType, String userInput, String sessionId) {
        return Flux.defer(() -> {
            try {
                String text = StringUtils.hasText(userInput) ? userInput.trim() : "请分析这张图片";
                boolean hasImage = imageBytes != null && imageBytes.length > 0;
                if (hasImage) {
                    validateImage(imageBytes.length, contentType);
                }
                log.info("识图Agent对话受理, sessionId={}, hasImage={}, userInput={}", sessionId, hasImage, text);

                // 1. 历史记忆（只含纯文本消息，图片不落库）
                List<Message> history = chatMemory.get(sessionId);

                // 2. 组装本轮消息：带图则把图片作为 Media 附加到 user 消息
                UserMessage.Builder userBuilder = UserMessage.builder().text(text);
                if (hasImage) {
                    userBuilder.media(new Media(
                            MimeTypeUtils.parseMimeType(contentType),
                            new ByteArrayResource(imageBytes)));
                }
                UserMessage current = userBuilder.build();

                List<Message> messages = new ArrayList<>(history);
                messages.add(current);

                // 3. 多模态流式调用（该模型需支持图片输入）
                StringBuilder answer = new StringBuilder();
                return imagePromptChatClient.prompt()
                        .messages(messages)
                        .stream()
                        .content()
                        .doOnNext(answer::append)
                        .timeout(Duration.ofSeconds(60))   // 与 MVC async 默认超时(60s)保持一致
                        .doOnComplete(() -> saveMemory(sessionId, text, hasImage, answer.toString().trim()))
                        .subscribeOn(Schedulers.fromExecutor(aiExecutor))
                        .doOnError(e -> log.error("识图Agent流式对话失败, sessionId={}", sessionId, e));
                        // 注意：不在这里把异常包成固定文案的 BusinessException（会吞掉真实原因），
                        // 原样抛给 Controller，由 Controller 统一转成一条 SSE data 文本返回。
            } catch (BusinessException be) {
                return Flux.error(be);
            } catch (Exception e) {
                log.error("识图Agent受理异常, sessionId={}", sessionId, e);
                return Flux.error(e);
            }
        });
    }

    // ==================== 私有方法 ====================

    private void validateImage(long size, String contentType) {
        if (!StringUtils.hasText(contentType)
                || !SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持 jpeg/png/webp/gif 格式的图片");
        }
        if (size > MAX_IMAGE_BYTES) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片大小不能超过 10MB");
        }
    }

    /**
     * 只把文字写进记忆（图片剥离）：
     * 记忆表 content 列是 MySQL TEXT(64KB)，塞 base64 图片会溢出导致多轮对话报错。
     * 追问轮（如"改成水墨画"）模型凭上一轮提示词文本即可改写，不影响体验。
     */
    private void saveMemory(String sessionId, String userText, boolean hasImage, String assistantText) {
        try {
            List<Message> toStore = new ArrayList<>();
            // 加图片标记，让后续轮次知道这轮用户发过图
            toStore.add(new UserMessage(hasImage ? "[用户上传了一张图片] " + userText : userText));
            if (StringUtils.hasText(assistantText)) {
                toStore.add(new AssistantMessage(assistantText));
            }
            chatMemory.add(sessionId, toStore);
        } catch (Exception e) {
            // 记忆写入失败只告警，不阻断本次回答
            log.error("识图Agent记忆写入失败, sessionId={}", sessionId, e);
        }
    }
}