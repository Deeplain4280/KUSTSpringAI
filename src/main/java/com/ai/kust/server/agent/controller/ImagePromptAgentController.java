package com.ai.kust.server.agent.controller;

import com.ai.kust.common.exception.BusinessException;
import com.ai.kust.common.result.ResultCode;
import com.ai.kust.server.agent.image2prompt.service.ImagePromptAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/image-agent")
@RequiredArgsConstructor
public class ImagePromptAgentController {

    private final ImagePromptAgentService imagePromptAgentService;

    /**
     * 识图反推提示词 Agent（SSE 流式，POST multipart）
     * POST /api/image-agent/chat
     *   file      : 图片文件（jpeg/png/webp/gif，≤10MB，可选——多轮追问轮可不带）
     *   userInput : 用户指令，如"识别这张图，反推提示词"
     *   sessionId : 会话ID（同一会话自动记忆，支持多轮追问）
     */
    @PostMapping(value = "/chat",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestPart(value = "file", required = false) MultipartFile file,
                             @RequestParam String userInput,
                             @RequestParam String sessionId) {
        Flux<String> stream;
        try {
            boolean hasFile = file != null && !file.isEmpty();
            byte[] bytes = hasFile ? file.getBytes() : null;
            String contentType = hasFile ? file.getContentType() : null;
            stream = imagePromptAgentService.chatStream(bytes, contentType, userInput, sessionId);
        } catch (Exception e) {
            log.error("识图Agent请求读取文件失败, sessionId={}", sessionId, e);
            stream = Flux.error(new BusinessException(ResultCode.IMAGE_ANALYZE_FAILED, e));
        }

        /*
         * 关键：SSE 响应头（text/event-stream）一旦发出，流内错误不能再抛给全局 @ExceptionHandler——
         * 它返回 JSON 的 Result，但没有 text/event-stream 的转换器，会二次抛
         * "No converter ... Result"，把真实错误完全盖住（就是你看到的日志）。
         * 所以这里把所有错误转成一条 SSE data 文本发给前端，真实原因会直接展示出来，便于定位。
         */
        return stream.onErrorResume(e -> {
            log.error("识图Agent流式调用异常, sessionId={}, 根因：{}", sessionId, e.getMessage(), e);
            String reason = e instanceof BusinessException be && be.getMessage() != null
                    ? be.getMessage()
                    : (e.getMessage() != null ? e.getMessage() : e.toString());
            return Flux.just("\n[识别失败] " + reason);
        });
    }
}
