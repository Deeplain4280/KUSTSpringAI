package com.ai.kust.server.qwen.service;

import com.ai.kust.server.qwen.models.entity.ChatConversation;
import com.ai.kust.server.qwen.models.entity.vo.ChatMessageResponse;

import java.util.List;

public interface ChatMemoryService {

    void createConversation(String id, String email, String title);
    /** 侧边栏对话列表 */
    List<ChatConversation> listConversation(String userEmail);
    //读取历史对话
    List<ChatMessageResponse> getMessages(String conversationId);

    void updateTitle(String id, String title);

    public void deleteConversation(String conversationId);

    void clearAll(String userEmail);

    void clearMemory(String sessionId);
}
