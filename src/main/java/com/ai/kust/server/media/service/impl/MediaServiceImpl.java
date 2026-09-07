package com.ai.kust.server.media.service.impl;

import com.ai.kust.common.exception.BusinessException;
import com.ai.kust.common.result.ResultCode;
import com.ai.kust.config.MediaProperties;
import com.ai.kust.server.media.dto.MediaTaskResponse;
import com.ai.kust.server.media.entity.MediaVideoTask;
import com.ai.kust.server.media.mapper.MediaVideoTaskMapper;
import com.ai.kust.server.media.service.MediaService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import tools.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class MediaServiceImpl implements MediaService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCEEDED = "SUCCEEDED";
    public static final String STATUS_FAILED = "FAILED";

    private final MediaProperties props;
    private final MediaVideoTaskMapper taskMapper;
    private final Executor aiExecutor;
    private final RestClient dashscopeClient;

    public MediaServiceImpl(MediaProperties props,
                            @Value("${media.dashscope.base-url:https://dashscope.aliyuncs.com}") String dashscopeBaseUrl,
                            @Value("${spring.ai.openai.api-key:}") String dashscopeApiKey,
                            MediaVideoTaskMapper taskMapper,
                            @Qualifier("aiExecutor") Executor aiExecutor) {
        this.props = props;
        this.taskMapper = taskMapper;
        this.aiExecutor = aiExecutor;

        // 直连百炼原生 DashScope API（OpenAI兼容网关不支持文生图/文生视频）
        // 直接 @Value 注入，避免依赖 @ConfigurationProperties 绑定与 ${...} 占位符解析
        String baseUrl = dashscopeBaseUrl == null ? "" : dashscopeBaseUrl.trim();
        String apiKey = dashscopeApiKey == null ? "" : dashscopeApiKey.trim();
        log.info("DashScope 媒体服务初始化: baseUrl={}, apiKey={}, imageModel={}, videoModel={}",
                baseUrl, maskKey(apiKey), props.getImageModel(), props.getVideoModel());

        if (!StringUtils.hasText(baseUrl)) {
            log.error("media.dashscope.base-url 为空，文生图/文生视频将无法调用百炼 API");
        }
        if (!StringUtils.hasText(apiKey)) {
            // 只告警不阻断启动（开发期便于先起服务）；key 为空时生成接口会返回明确的业务错误
            log.error("spring.ai.openai.api-key 为空！文生图/文生视频将无法调用百炼 API，请检查 application.properties");
        }

        this.dashscopeClient = RestClient.builder()
                .baseUrl(StringUtils.hasText(baseUrl) ? baseUrl : "https://dashscope.aliyuncs.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /** 只显示 key 首尾各 4 位，避免日志泄露完整密钥 */
    private static String maskKey(String key) {
        if (key == null || key.length() <= 8) {
            return "***";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }

    // ==================== 文生图（提交异步任务 + 轮询，调用方同步等待） ====================

    @Override
    public String generateImage(String prompt, String size) {
        try {
            log.info("文生图请求受理, prompt={}", prompt);
            String resultUrl = CompletableFuture.supplyAsync(() -> {
                // 同步模式会让百炼挂住连接直到出图完成（超过客户端读超时），
                // 因此加 X-DashScope-Async 头走异步任务，再自行轮询
                JsonNode submitResp = dashscopeClient.post()
                        .uri("/api/v1/services/aigc/text2image/image-synthesis")
                        .header("X-DashScope-Async", "enable")
                        .body(Map.of(
                                "model", props.getImageModel(),
                                "input", Map.of("prompt", prompt),
                                "parameters", Map.of("size",
                                        StringUtils.hasText(size) ? size : props.getImageSize())))
                        .retrieve()
                        .body(JsonNode.class);

                String taskId = submitResp.path("output").path("task_id").asText("");
                String imageUrl;
                if (StringUtils.hasText(taskId)) {
                    imageUrl = pollDashscopeTask(taskId, "文生图", IMAGE_MAX_WAIT_SECONDS);
                } else {
                    // 个别模型/情形下可能直接同步返回结果
                    imageUrl = extractResultUrl(submitResp, "文生图");
                }
                // 直接返回百炼生成的图片URL，不做本地转存
                return imageUrl;
            }, aiExecutor).join();

            log.info("文生图完成, prompt={}", prompt);
            return resultUrl;
        } catch (Exception e) {
            log.error("文生图失败, prompt={}", prompt, e);
            if (e instanceof BusinessException be) {
                throw be;
            }
            throw new BusinessException(ResultCode.MEDIA_GENERATION_FAILED, e);
        }
    }

    // ==================== 文生视频（异步任务 + MySQL持久化） ====================

    @Override
    public MediaTaskResponse submitVideo(String prompt) {
        String taskId = UUID.randomUUID().toString().replace("-", "");

        // 1. 落库一条 PENDING 任务（时间手动填，项目里 MyMetaObjectHandler 的 insertFill 是空的）
        MediaVideoTask task = new MediaVideoTask();
        task.setTaskId(taskId);
        task.setPrompt(prompt);
        task.setStatus(STATUS_PENDING);
        LocalDateTime now = LocalDateTime.now();
        task.setCreateTime(now);
        task.setUpdateTime(now);
        taskMapper.insert(task);
        log.info("视频任务已入库, taskId={}", taskId);

        // 2. 后台线程：提交百炼 → 轮询 → 更新状态
        aiExecutor.execute(() -> processVideoTask(task.getId()));

        return toResponse(task);
    }

    /** 后台执行单个视频任务；任何异常都不能逃逸，否则任务永远停在 PENDING */
    private void processVideoTask(String id) {
        MediaVideoTask task = taskMapper.selectById(id);
        try {
            // 2.1 提交百炼文生视频任务
            JsonNode submitResp = dashscopeClient.post()
                    .uri("/api/v1/services/aigc/video-generation/video-synthesis")
                    .body(Map.of(
                            "model", props.getVideoModel(),
                            "input", Map.of("prompt", task.getPrompt()),
                            "parameters", Map.of(
                                    "size", props.getVideoSize(),
                                    "duration", props.getVideoDuration())))
                    .retrieve()
                    .body(JsonNode.class);

            String dashscopeTaskId = submitResp.path("output").path("task_id").asText("");
            String remoteVideoUrl;

            if (StringUtils.hasText(dashscopeTaskId)) {
                // 正常情况：异步任务，先回写百炼任务ID，再轮询
                task.setDashscopeId(dashscopeTaskId);
                task.setUpdateTime(LocalDateTime.now());
                taskMapper.updateById(task);
                remoteVideoUrl = pollDashscopeTask(dashscopeTaskId, "文生视频", props.getVideoMaxWaitSeconds());
            } else {
                // 个别模型可能直接同步返回结果
                remoteVideoUrl = extractResultUrl(submitResp, "文生视频");
            }

            // 2.2 成功：直接使用百炼返回的视频URL（不做本地转存），更新为 SUCCEEDED
            task.setStatus(STATUS_SUCCEEDED);
            task.setResultUrl(remoteVideoUrl);
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);
            log.info("视频生成成功, taskId={}, url={}", task.getTaskId(), remoteVideoUrl);
        } catch (Exception e) {
            // 2.3 失败：更新为 FAILED 并记录原因（error_msg 列限长，截断到500）
            log.error("视频生成失败, taskId={}", task.getTaskId(), e);
            task.setStatus(STATUS_FAILED);
            task.setErrorMsg(truncate(e instanceof BusinessException be
                    ? be.getMessage()
                    : "视频生成异常: " + e.getMessage(), 500));
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    /** 文生图最长等待秒数（百炼 t2i 通常在 60s 内出图） */
    private static final int IMAGE_MAX_WAIT_SECONDS = 120;

    /** 轮询百炼任务直到 SUCCEEDED/FAILED/超时，成功时返回结果URL（图片/视频通用） */
    private String pollDashscopeTask(String dashscopeTaskId, String bizName, int maxWaitSeconds) {
        long deadline = System.currentTimeMillis() + maxWaitSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            JsonNode resp = dashscopeClient.get()
                    .uri("/api/v1/tasks/{taskId}", dashscopeTaskId)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode output = resp.path("output");
            String status = output.path("task_status").asText("");
            if (STATUS_SUCCEEDED.equals(status)) {
                // 老版本接口返回 output.video_url，新版本返回 output.results[0].url，两种都兼容
                return output.hasNonNull("video_url")
                        ? output.path("video_url").asText()
                        : extractResultUrl(resp, bizName);
            }
            if (STATUS_FAILED.equals(status)) {
                throw new BusinessException(ResultCode.MEDIA_GENERATION_FAILED,
                        bizName + "失败: " + output.path("message").asText("未知原因"));
            }
            try {
                Thread.sleep(props.getVideoPollIntervalSeconds() * 1000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ResultCode.MEDIA_GENERATION_FAILED, bizName + "生成被中断");
            }
        }
        throw new BusinessException(ResultCode.MEDIA_GENERATION_FAILED, bizName + "生成超时，请稍后重试");
    }

    @Override
    public MediaTaskResponse queryVideoTask(String taskId) {
        // 按业务唯一键 task_id 查询，而非主键 id
        MediaVideoTask task = taskMapper.selectOne(new LambdaQueryWrapper<MediaVideoTask>()
                .eq(MediaVideoTask::getTaskId, taskId));
        if (task == null) {
            throw new BusinessException(ResultCode.MEDIA_TASK_NOT_FOUND);
        }
        return toResponse(task);
    }

    // ==================== 私有工具 ====================

    private MediaTaskResponse toResponse(MediaVideoTask task) {
        return new MediaTaskResponse(task.getTaskId(), task.getStatus(),
                task.getResultUrl(), task.getErrorMsg());
    }

    /** 从百炼同步响应取 output.results[0].url */
    private String extractResultUrl(JsonNode resp, String bizName) {
        String taskStatus = resp.path("output").path("task_status").asText("");
        if (STATUS_FAILED.equals(taskStatus)) {
            throw new BusinessException(ResultCode.MEDIA_GENERATION_FAILED,
                    bizName + "失败: " + resp.path("output").path("message").asText("未知原因"));
        }
        JsonNode results = resp.path("output").path("results");
        if (results.isArray() && !results.isEmpty()) {
            String url = results.get(0).path("url").asText("");
            if (StringUtils.hasText(url)) {
                return url;
            }
        }
        throw new BusinessException(ResultCode.MEDIA_GENERATION_FAILED, bizName + "响应缺少结果");
    }

    /** 截断超长错误信息，防止超出 error_msg 列宽 */
    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}