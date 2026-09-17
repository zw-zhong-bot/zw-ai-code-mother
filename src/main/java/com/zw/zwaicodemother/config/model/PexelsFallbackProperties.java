package com.zw.zwaicodemother.config.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Pexels 图片搜索 yml 兜底配置。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Data
@Component
@ConfigurationProperties(prefix = "pexels")
public class PexelsFallbackProperties {

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 接口地址，为空时用提供方默认地址
     */
    private String baseUrl;
}
