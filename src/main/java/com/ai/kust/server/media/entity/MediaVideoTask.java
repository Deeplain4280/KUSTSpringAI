package com.ai.kust.server.media.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "media_video_task")
public class MediaVideoTask {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @TableField(value = "task_id")
    private String taskId;        // 对外暴露的任务ID（UUID去横线，业务唯一键）

    @TableField(value = "dashscope_id")
    private String dashscopeId;   // 百炼侧任务ID

    @TableField(value = "prompt")
    private String prompt;

    @TableField(value = "status")
    private String status;        // PENDING / SUCCEEDED / FAILED

    @TableField(value = "result_url")
    private String resultUrl;     // 转存后的本地访问URL

    @TableField(value = "error_msg")
    private String errorMsg;

    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}
