package com.ai.kust.server.media.controller;

import com.ai.kust.common.result.Result;
import com.ai.kust.server.media.dto.ImageGenRequest;
import com.ai.kust.server.media.dto.MediaTaskResponse;
import com.ai.kust.server.media.service.MediaService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    @Resource
    private MediaService mediaService;

    /** 文生图：POST /api/media/image  body: {"prompt":"一只戴眼镜的猫","size":"1024*1024"} */
    @PostMapping("/image")
    public Result<String> generateImage(@RequestBody @Valid ImageGenRequest request) {
        return Result.success(mediaService.generateImage(request.prompt(), request.size()));
    }

    /** 文生视频：提交后立即返回任务，前端轮询查询接口拿结果 */
    @PostMapping("/video")
    public Result<MediaTaskResponse> submitVideo(@RequestBody @Valid ImageGenRequest request) {
        return Result.success(mediaService.submitVideo(request.prompt()));
    }

    /** 查询任务状态：GET /api/media/video/{taskId} */
    @GetMapping("/video/{taskId}")
    public Result<MediaTaskResponse> queryVideo(@PathVariable String taskId) {
        return Result.success(mediaService.queryVideoTask(taskId));
    }
}
