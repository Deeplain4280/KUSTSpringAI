package com.ai.kust.server.agent.controller;


import com.ai.kust.common.result.Result;
import com.ai.kust.common.result.ResultCode;
import com.ai.kust.server.agent.auth.model.request.SendCodeRequest;
import com.ai.kust.server.agent.auth.model.request.VerifyCodeRequest;
import com.ai.kust.server.agent.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/send-code")
    public Result<String> sendCode(@RequestBody SendCodeRequest request) {
        String message = authService.sendCode(request.getIdentity());
        return Result.success(message);

    }
    //登录按钮
    @PostMapping("/verify")
    public Result<String> verifyCode(@RequestBody VerifyCodeRequest request) {
        authService.verifyCode(request.getEmail(), request.getCode());
        return Result.success(ResultCode.VERIFY_CODE_SUCCESS.getMessage());
    }
}
