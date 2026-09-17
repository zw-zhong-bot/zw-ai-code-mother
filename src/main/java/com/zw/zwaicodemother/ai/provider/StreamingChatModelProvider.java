package com.zw.zwaicodemother.ai.provider;

import com.zw.zwaicodemother.model.enums.ModelProviderEnum;
import dev.langchain4j.model.chat.StreamingChatModel;

/**
 * 流式对话模型提供方。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
public interface StreamingChatModelProvider {

    /**
     * 本实现负责的提供方
     *
     * @return 提供方枚举
     */
    ModelProviderEnum provider();

    /**
     * 创建流式对话模型
     *
     * @param options 模型参数
     * @return 流式对话模型
     */
    StreamingChatModel createStreamingChatModel(ModelOptions options);
}
