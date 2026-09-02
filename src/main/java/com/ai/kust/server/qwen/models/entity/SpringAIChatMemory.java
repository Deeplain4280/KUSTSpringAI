package com.ai.kust.server.qwen.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalTime;

@TableName(value = "spring_ai_chat_memory")
@Data
public class SpringAIChatMemory {

    @TableField(value = "conversation_id")
    private String conversationId;

    @TableField("content")
    private String content;

    @TableField("type")
    private String type;

    @TableField("timestamp")
    private LocalTime timeStamp;

    @TableId(value = "sequence_id", type = IdType.ASSIGN_ID)
    private Integer sequenceId;

}
