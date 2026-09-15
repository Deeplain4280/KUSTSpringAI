package com.ai.kust.server.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImageGenRequest(
        @NotBlank(message = "图片不能为空")
        @Size(max = 2000, message = "图片描述过长")
        String prompt,
        String size
) {
}
