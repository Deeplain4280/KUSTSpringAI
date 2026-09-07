package com.ai.kust.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "media")
public class MediaProperties {

    /** 文生图模型 */
    private String imageModel = "wan2.2-t2i-flash";
    /** 出图尺寸，如 1024*1024 */
    private String imageSize = "1024*1024";
    /** 文生视频模型 */
    private String videoModel = "wan2.2-t2v-lite";
    private String videoSize = "1280*720";
    private int videoDuration = 5;
    private int videoPollIntervalSeconds = 5;
    private int videoMaxWaitSeconds = 600;
}
