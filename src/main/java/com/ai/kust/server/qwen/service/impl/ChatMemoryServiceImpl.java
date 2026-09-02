package com.ai.kust.server.qwen.service.impl;

import com.ai.kust.common.exception.BusinessException;
import com.ai.kust.common.result.ResultCode;
import com.ai.kust.server.qwen.mapper.ChatConversationMapper;
import com.ai.kust.server.qwen.mapper.SpringAIChatMemoryMapper;
import com.ai.kust.server.qwen.models.entity.ChatConversation;
import com.ai.kust.server.qwen.models.entity.SpringAIChatMemory;
import com.ai.kust.server.qwen.models.entity.vo.ChatMessageResponse;
import com.ai.kust.server.qwen.service.ChatMemoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ChatMemoryServiceImpl implements ChatMemoryService {
    private final ChatConversationMapper conversationMapper;
    private final SpringAIChatMemoryMapper memoryMapper;

    @Override
    public void createConversation(String id, String useremail, String title) {
        ChatConversation chatConversation = new ChatConversation();
        chatConversation.setId(id);
        chatConversation.setUserEmail(useremail);
        chatConversation.setTitle(title);
        conversationMapper.insert(chatConversation);
    }

    @Override
    public List<ChatConversation> listConversation(String userEmail) {
        return conversationMapper.selectList(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getUserEmail, userEmail)
                .orderByDesc(ChatConversation::getCreateTime));
    }

    @Override
    public List<ChatMessageResponse> getMessages(String conversationId) {
        checkExists(conversationId);
        return memoryMapper.selectList( new LambdaQueryWrapper<SpringAIChatMemory>()
                     .eq(SpringAIChatMemory::getConversationId, conversationId)
                     .in(SpringAIChatMemory::getType, "USER", "ASSISTANT")
                     .orderByAsc(SpringAIChatMemory::getSequenceId))
                .stream()
                .map(m -> new ChatMessageResponse("USER".equalsIgnoreCase(m.getType()) ? "user" : "ai",
                        m.getContent()))
                .toList();
    }



    private void checkExists(String conversationId) {
        if (conversationMapper.selectById(conversationId) == null) {
            throw new BusinessException(ResultCode.CHAT_CONVERSATION_NOT_FOUND);
        }
    }
}
