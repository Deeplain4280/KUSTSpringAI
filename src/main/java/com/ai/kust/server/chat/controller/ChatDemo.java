package com.ai.kust.server.chat.controller;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
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
    //创建聊天客户端，接受用户输入，经过大模型，实现聊天内容
    //注入聊天大模型，声明聊天变量
    private final ChatClient chatClient;

    public ChatDemo(ChatClient.Builder chatBuilderClient) {
        this.chatClient = chatBuilderClient.build();
    }

    //实现基础对话
    @GetMapping(value = "/chat")
    public String generation(String userInput) {
        ChatClient.ChatClientRequestSpec prompt = chatClient.prompt();

        ChatClient.ChatClientRequestSpec user = prompt.user(userInput);
        //System.out.println(user);

        ChatClient.CallResponseSpec call = user.call();
        //System.out.println(call);

        String content = call.content();//大模型的返回信息
        System.out.println(content);
        return content;
    }

}
