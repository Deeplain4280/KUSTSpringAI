package com.ai.kust.server.qwen.service.impl;

import com.ai.kust.server.qwen.mapper.ChatConversationMapper;
import com.ai.kust.server.qwen.mapper.SpringAIChatMemoryMapper;
import com.ai.kust.server.qwen.models.entity.ChatConversation;
import com.ai.kust.server.qwen.service.ChatMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ChatMemoryServiceImpl implements ChatMemoryService {
    private final ChatConversationMapper conversationMapper;
    private final SpringAIChatMemoryMapper MemoryMapper;

    @Override
    public void createConversation(String id, String useremail, String title) {
        ChatConversation chatConversation = new ChatConversation();
        chatConversation.setId(id);
        chatConversation.setUserEmail(useremail);
        chatConversation.setTitle(title);
        conversationMapper.insert(chatConversation);
    }
}
