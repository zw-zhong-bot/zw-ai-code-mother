package com.zw.zwaicodemother.config.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云百炼（DashScope）yml 兜底配置。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Data
@Component
@ConfigurationProperties(prefix = "dashscope")
public class DashScopeFallbackProperties {

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 图像生成模型名称
     */
    private String imageModel;

    /**
     * 图像尺寸，如 1024*1024
     */
    private String imageSize;
}
