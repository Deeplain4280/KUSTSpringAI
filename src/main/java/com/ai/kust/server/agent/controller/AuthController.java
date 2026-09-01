package com.ai.kust.server.agent.controller;


import com.ai.kust.common.result.Result;
import com.ai.kust.server.agent.auth.model.request.SendCodeRequest;
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
        String message = authService.sendCode(request.getIndentity());
        return Result.success(message);

    }
}
