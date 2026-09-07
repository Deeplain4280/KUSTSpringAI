package com.ai.kust.server.media.dto;

public record MediaTaskResponse(
        String taskId,
        String status,
        String mediaUrl,
        String errorMsg
) {
}
