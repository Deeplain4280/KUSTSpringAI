package com.ai.kust.server.agent.auth.tool;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//需要先在配置文件中配置验证码前缀和过期时间
//再在依赖中到导入redis依赖
@Component
@Slf4j
@RequiredArgsConstructor
public class AuthEmailCodeTool {
    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String setFrom;

    @Value("${auth.code.key-prefix}")
    private String keyprefix;

    @Value("${auth.code.expire-minutes}")
    private int expire;

    private static final SecureRandom RANDOM = new SecureRandom();//创建一个随机生成对象，赋值给一个静态的常量

    @Tool(description = """
            向指定的邮箱地址中发送登录验证码，
            当用户输入的邮箱地址有效时，才调用此工具发送，
            返回一个发送成功或者失败的描述
            """)
    public Map<String, Object> sendVerifyCode(@ToolParam(description = """
            接受用户输入的邮箱地址
            """) String email) {
        log.info("调用验证码发送工具，验证码邮箱地址：{}", email);
        //生成验证码
        String code = String.format("%06d", RANDOM.nextInt(1000000));
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(setFrom);
            helper.setTo(email);
            helper.setSubject("登陆验证 邮箱验证码");
            String text = buildEmailHtml(code);
            helper.setText(text, true);
            mailSender.send(message);
            log.info("邮件登录验证码发送成功，邮箱地址:{}", email);

            String key = keyprefix + email;
            redisTemplate.opsForValue().set(key, code, expire, TimeUnit.MINUTES);
            log.info("验证码存入redis成功， key = {} | 过期时间 = {}", key, expire);
        }catch (MessagingException e){
            log.error("验证码发送失败，请稍后重试， 地址为：{}， 验证码不存入redis",email);
            return Map.of(
                    "success", false,
                    "message", "邮件发送失败"
            );
        }
        return Map.of(
                "success", true,
                "message", "验证码已发送到"+email+","+expire+"分钟内有效",
                "expireMinutes", expire
        );
    }
    private String buildEmailHtml(String code) {
        String text = String.format("""
                <div style="margin:0;padding:0;background-color:#f4f6f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;">
                  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f9;padding:40px 20px;">
                    <tr>
                      <td align="center">
                        <!-- 主卡片 -->
                        <table role="presentation" cellpadding="0" cellspacing="0" style="max-width:420px;width:100%;background:#ffffff;border-radius:16px;box-shadow:0 4px 24px rgba(0,0,0,0.06);overflow:hidden;">
                
                          <!-- 顶部图标区 -->
                          <tr>
                            <td align="center" style="padding:36px 32px 0;">
                              <div style="width:56px;height:56px;border-radius:50%;background:linear-gradient(135deg,#4F8EF7,#6C5CE7);display:flex;align-items:center;justify-content:center;margin:0 auto;">
                                <span style="font-size:26px;line-height:56px;color:#fff;">🔐</span>
                              </div>
                            </td>
                          </tr>
                
                          <!-- 标题 -->
                          <tr>
                            <td align="center" style="padding:20px 32px 0;">
                              <h2 style="margin:0;font-size:22px;font-weight:700;color:#1a1a2e;letter-spacing:-0.3px;">邮箱登录验证</h2>
                            </td>
                          </tr>
                
                          <!-- 验证码区域 -->
                          <tr>
                            <td align="center" style="padding:28px 32px;">
                              <div style="background:#F0F4FF;border-radius:12px;padding:24px 20px;text-align:center;">
                                <p style="margin:0 0 8px;font-size:13px;color:#6b7280;font-weight:500;letter-spacing:0.5px;">您的验证码是</p>
                                <p style="margin:0;font-size:36px;font-weight:800;color:#4F46E5;letter-spacing:8px;line-height:1.2;font-variant-numeric:tabular-nums;">%s</p>
                              </div>
                            </td>
                          </tr>
                
                          <!-- 提示文字 -->
                          <tr>
                            <td align="center" style="padding:0 32px 32px;">
                              <p style="margin:0;font-size:12px;color:#9ca3af;line-height:1.6;">
                                ⏱ 验证码 <strong style="color:#6b7280;">%d</strong> 分钟内有效，请勿泄露给他人<br/>
                                如非本人操作，请忽略此邮件
                              </p>
                            </td>
                          </tr>
                
                          <!-- 底部分隔线 + 品牌区（可选） -->
                          <tr>
                            <td style="border-top:1px solid #f0f0f0;padding:20px 32px;text-align:center;">
                              <p style="margin:0;font-size:11px;color:#c0c0c0;">© Your App Name · Security Verification</p>
                            </td>
                          </tr>
                
                        </table>
                      </td>
                    </tr>
                  </table>
                </div>
                """, code, expire);
        return text;
    }

}
