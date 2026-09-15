package com.ai.kust.server.media.service;

import com.ai.kust.server.media.dto.MediaTaskResponse;


public interface MediaService {
    String generateImage(String prompt, String size);

    MediaTaskResponse submitVideo(String prompt);

    MediaTaskResponse queryVideoTask(String taskId);
}
