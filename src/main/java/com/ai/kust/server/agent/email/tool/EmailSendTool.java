package com.ai.kust.server.agent.email.tool;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailSendTool {
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String setFrom;

    @Tool(description = """
            当用户确认发送邮件时，才调用此工具发送邮件，
            如果用户只是起草或者修改邮件，请勿调用，
            邮件的参数必须保持完整：收件人邮箱，邮件主题，邮件内容
            """)
    public String sendEmail(@ToolParam(description = "收件人的邮箱必须为有效的Email邮箱格式") String to,
                            @ToolParam(description = "邮箱主题") String subject,
                            @ToolParam(description = "邮件内容，支持文本，HTML等格式") String content) {
        try {
            log.info("AI触发工具调用，收件人地址：{}, 邮件主题：{}",to, subject);
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(setFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content);
            javaMailSender.send(message);
            log.info("邮件已经发送成功，收件人：{}", to);
            return "邮件发送成功给" + to;
        }catch (MessagingException e){
            log.error("邮件发送失败，收件人：{}", to);
            return "邮件发送失败，原因：" + e;
        }
    }
}
