package com.ai.kust.server.agent.auth.service;

public interface AuthService {
    //发送验证码
    String sendCode(String email);

    void verifyCode(String email, String code);
}
