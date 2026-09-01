package com.ai.kust.server.agent.auth.service.impl;

import com.ai.kust.server.agent.auth.service.AuthService;
import com.ai.kust.server.agent.auth.tool.AuthEmailCodeTool;
import com.ai.kust.server.agent.auth.tool.EmailQueryTool;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl  implements AuthService {
    private final ChatClient.Builder chatClient;
    private final AuthEmailCodeTool authEmailCodeTool;
    private final EmailQueryTool emailQueryTool;


    private String buildSendCodePrompt(String email) {
        return """
                用户提供了登录标识: %s
                请严格按照以下的步骤进行操作/执行，不要跳过任何执行流程
                执行流程：
                第一步：调用 queryUser 工具，传入用户登录标识，查询用户信息
                第二步：分析查询的用户信息
                如果查找到用户信息，就判断用户的 status 字段的值
                   如果值为1 就调用 第三步流程
                   如果值为0，就直接回复："账户状态异常，请联系管理员"
                如果未找到用户信息或者用户返回值为空，直接回复："账户未注册，请注册后使用"
                第三步：调用 sendVerifyCode 工具传入邮箱地址，发送验证码
                第四步：根据发送结果，用一句话告知用户验证码已发送以及有效期
                
                注意：回答简洁，不要出现任何工具名称，也不要输出任何技术细节
                
                """;
    }
    public String sendCode(String email) {
        log.info("邮箱验证码已发送，邮箱为：{}", email);
        String content = chatClient.build().prompt()
                .user(buildSendCodePrompt(email))
                .tools(authEmailCodeTool, emailQueryTool)
                .call()
                .content();
        log.info("验证码发送成功，返回值为：{}", content);
        return content;
    }
}
