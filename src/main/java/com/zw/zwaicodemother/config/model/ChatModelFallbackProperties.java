package com.zw.zwaicodemother.config.model;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 对话模型 yml 兜底配置。
 * <p>
 * 绑定原有的 langchain4j.open-ai 配置块，数据库中没有对应能力的启用配置时，由此提供参数，
 * 保证改造后不配数据库也能按原有行为运行。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Data
@Component
@ConfigurationProperties(prefix = "langchain4j.open-ai")
public class ChatModelFallbackProperties {

    /**
     * 同步对话模型（对应能力 CHAT）
     */
    private ChatParams chatModel = new ChatParams();

    /**
     * 流式对话模型（对应能力 STREAM_CHAT）
     */
    private ChatParams streamingChatModel = new ChatParams();

    /**
     * 推理流式模型（对应能力 REASONING），未配置时回退到 chatModel
     */
    private ChatParams reasoningChatModel = new ChatParams();

    /**
     * 对话模型参数
     */
    @Data
    public static class ChatParams {

        private String baseUrl;

        private String apiKey;

        private String modelName;

        private Integer maxTokens;

        private Double temperature;

        private Duration timeout;

        private Boolean logRequests;

        private Boolean logResponses;

        private Boolean enabled;
    }

    /**
     * 取推理模型参数，未单独配置时回退到同步对话模型参数
     *
     * @return 推理模型参数
     */
    public ChatParams resolveReasoning() {
        if (reasoningChatModel != null && StrUtil.isNotBlank(reasoningChatModel.getModelName())) {
            return reasoningChatModel;
        }
        return chatModel;
    }
}
