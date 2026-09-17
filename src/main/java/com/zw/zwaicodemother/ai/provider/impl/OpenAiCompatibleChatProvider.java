package com.zw.zwaicodemother.ai.provider.impl;

import cn.hutool.core.util.StrUtil;
import com.zw.zwaicodemother.ai.provider.ChatModelProvider;
import com.zw.zwaicodemother.ai.provider.ModelOptions;
import com.zw.zwaicodemother.ai.provider.StreamingChatModelProvider;
import com.zw.zwaicodemother.exception.BusinessException;
import com.zw.zwaicodemother.exception.ErrorCode;
import com.zw.zwaicodemother.exception.ThrowUtils;
import com.zw.zwaicodemother.model.enums.ModelProviderEnum;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * OpenAI 兼容接口提供方。
 * <p>
 * 适配所有遵循 OpenAI 协议的端点（scnet、DeepSeek、OpenAI 等），同时支持同步与流式对话模型。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Slf4j
@Component
public class OpenAiCompatibleChatProvider implements ChatModelProvider, StreamingChatModelProvider {

    @Override
    public ModelProviderEnum provider() {
        return ModelProviderEnum.OPENAI_COMPATIBLE;
    }

    @Override
    public ChatModel createChatModel(ModelOptions options) {
        checkOptions(options);
        return OpenAiChatModel.builder()
                .baseUrl(options.getBaseUrl())
                .apiKey(options.getApiKey())
                .modelName(options.getModelName())
                .maxTokens(options.getMaxTokens())
                .temperature(options.getTemperature())
                .timeout(options.getTimeout())
                .logRequests(Boolean.TRUE.equals(options.getLogRequests()))
                .logResponses(Boolean.TRUE.equals(options.getLogResponses()))
                .build();
    }

    @Override
    public StreamingChatModel createStreamingChatModel(ModelOptions options) {
        checkOptions(options);
        return OpenAiStreamingChatModel.builder()
                .baseUrl(options.getBaseUrl())
                .apiKey(options.getApiKey())
                .modelName(options.getModelName())
                .maxTokens(options.getMaxTokens())
                .temperature(options.getTemperature())
                .timeout(options.getTimeout())
                .logRequests(Boolean.TRUE.equals(options.getLogRequests()))
                .logResponses(Boolean.TRUE.equals(options.getLogResponses()))
                .build();
    }

    /**
     * 校验创建模型必需的参数
     *
     * @param options 模型参数
     */
    private void checkOptions(ModelOptions options) {
        ThrowUtils.throwIf(options == null, new BusinessException(ErrorCode.SYSTEM_ERROR, "模型参数为空"));
        ThrowUtils.throwIf(StrUtil.isBlank(options.getApiKey()), ErrorCode.SYSTEM_ERROR,
                "模型配置缺少 apiKey：" + options.getConfigName());
        ThrowUtils.throwIf(StrUtil.isBlank(options.getModelName()), ErrorCode.SYSTEM_ERROR,
                "模型配置缺少 modelName：" + options.getConfigName());
    }
}
