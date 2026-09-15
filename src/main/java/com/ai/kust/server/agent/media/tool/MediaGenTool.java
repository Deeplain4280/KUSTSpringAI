package com.ai.kust.server.agent.media.tool;

import com.ai.kust.server.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MediaGenTool {

    private final MediaService mediaService;

    @Tool(description = """
            仅当用户明确要求生成/绘制一张图片（如画一只猫、生成一张海报、做个头像）时调用此工具。
            若用户只是在聊天中提图，并未要求生成图片，请勿调用。
            生成图片需要一些时间，请耐心等待结果。
            """)
    public String generateImage(
            @ToolParam(description = "要生成图片的详细内容描述，主体、风格、背景等越具体效果越好")
            String prompt) {
        try {
            log.info("AI触发文生图工具, prompt={}", prompt);
            String url = mediaService.generateImage(prompt, null);
            // 工具返回值就是模型能“看到”并复述给用户的话
            return "图片已生成，地址如下（约24小时有效，请提醒用户尽快查看或保存）：" + url;
        } catch (Exception e) {
            log.error("文生图工具调用失败, prompt={}", prompt, e);
            return "图片生成失败，请稍后重试或换个描述。原因：" + e.getMessage();
        }
    }
}