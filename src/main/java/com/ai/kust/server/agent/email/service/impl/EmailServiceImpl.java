package com.ai.kust.server.agent.email.service.impl;

import com.ai.kust.common.exception.BusinessException;
import com.ai.kust.common.result.ResultCode;
import com.ai.kust.server.agent.email.service.EmailService;
import com.ai.kust.server.agent.email.tool.EmailSendTool;
import com.ai.kust.server.agent.email.tool.SQLQueryTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class EmailServiceImpl  implements EmailService {

    private final ChatClient emailChatClient;
    private final Executor aiExecutor;
    private final EmailSendTool emailSendTool;
    private final SQLQueryTool sqlQueryTool;
    private MessageChatMemoryAdvisor memoryAdvisor;

    private final String SYSTEM_EMAIL_PROMPT = """
            你是一个专业的邮件发送和撰写助手，工作流程如下：
            1)理解用户的意图和真实想法，理解用户的邮件需求，帮助用户提取邮件主题，写作邮件正文
            2)在写邮件正文的过程中，不调用邮件发送工具，写作完成后，将起草的邮件内容以邮件预览的格式展示给用户
            3)当用户明确回答“确认发送”， “发送”等等后，才调用邮件发送工具sendEmail进行发送
            4）如果用户提供的信息不完整（比如缺少收件人邮箱、收件人姓名等关键信息）时，主动追问用户，不要猜测
            """;

    private String buildPrompt(String userInput) {
        String prompt = """
                
                """.formatted(userInput);
        return prompt;
    }

    public EmailServiceImpl(ChatClient.Builder chatBuilder,
                            @Qualifier("aiExecutor") Executor aiExecutor,
                            ChatMemory chatMemory,
                            EmailSendTool emailSendTool,
                            SQLQueryTool sqlQueryTool
    ) {
        this.aiExecutor = aiExecutor;
        this.memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        this.emailSendTool = emailSendTool;
        this.sqlQueryTool = sqlQueryTool;
        this.emailChatClient = chatBuilder.defaultSystem(SYSTEM_EMAIL_PROMPT)
                .defaultTools(emailSendTool, sqlQueryTool)
                .build();


    }

    public Flux<String> chatStream(String userInput, String sessionId) {
        log.info("调用发送工具，用户输入：{}， 用户标识：{}", userInput, sessionId);
        Boolean isLikelyToolCall = isLikelyToolCall(userInput);
        if (isLikelyToolCall) {
            log.info("调用工具，将流式输出降级为非流式输出，用户标识： {}", sessionId);
            return callNoStream(userInput, sessionId);
        }else {
            log.info("不调用工具，流式输出，用户标识： {}", sessionId);
            return callStream(userInput, sessionId);
        }

    }

    private boolean isLikelyToolCall(String userInput) {
        if (userInput == null) {
            return false;
        }
        String lower = userInput.toLowerCase();
        return lower.contains("确认发送") || lower.contains("可以发送") || lower.contains("可以发了") ||
                lower.contains("发吧") || lower.contains("发送") || lower.contains("send") || lower.contains("confirm");
    }

    private Flux<String> callStream(String userInput, String sessionId) {
        log.info("AI流式对话已处理，用户对话： {}, 用户标识：{}",userInput, sessionId);
        return emailChatClient.prompt()
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

    private Flux<String> callNoStream(String userInput, String sessionId) {
        return Flux.defer(() -> {
            try {
               ChatResponse chatResponse = emailChatClient.prompt()
                       .user(userInput)
                       .advisors(memoryAdvisor)
                       .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))
                       .call()
                       .chatResponse();
               System.out.println("调用工具返回值：" + chatResponse);
               String content = chatResponse.getResult().getOutput().getText();
               if (content == null || content.isBlank()) {
                   content = "邮件发送完成";
               }
               return Flux.just(content);
            }catch (Exception e){
                log.error("AI邮件发送工具非流式调用失败，用户标识： {}", sessionId,e);
                return Flux.error(new BusinessException(ResultCode.AI_CALL_FAILED, e));
            }
        }).subscribeOn(Schedulers.fromExecutor(aiExecutor));
    }
}
