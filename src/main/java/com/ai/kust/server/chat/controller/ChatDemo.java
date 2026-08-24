package com.ai.kust.server.chat.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api")

public class ChatDemo {
    @GetMapping(value = "/say")
    public String sat() {
        return "你好，我是你的智能助手";
    }
}
