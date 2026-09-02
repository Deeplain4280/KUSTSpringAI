package com.ai.kust.server.qwen.models.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalTime;

@TableName(value = "chat_conversation")
@Data
public class ChatConversation {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @TableField(value = "user_email")
    private String userEmail;

    @TableField(value = "title")
    private String title;

    @TableField(value = "create_time")
    private LocalTime createTime;

}
