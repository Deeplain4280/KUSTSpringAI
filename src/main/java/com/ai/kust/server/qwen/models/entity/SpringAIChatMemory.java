package com.ai.kust.server.qwen.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalTime;

// 注意：必须与 Spring AI JDBC 仓库实际写入的表名一致（官方 schema 为大写）。
// Linux MySQL 表名区分大小写，写小写会导致"写入大写表、读取小写表"两张表问题。
@TableName(value = "SPRING_AI_CHAT_MEMORY")
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
