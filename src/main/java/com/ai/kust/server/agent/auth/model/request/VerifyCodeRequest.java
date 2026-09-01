package com.ai.kust.server.agent.auth.model.request;


import lombok.Data;

@Data
public class VerifyCodeRequest {

    private String email;
    private String code;
}
