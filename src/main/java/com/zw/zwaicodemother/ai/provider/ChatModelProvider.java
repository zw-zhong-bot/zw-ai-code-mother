package com.zw.zwaicodemother.ai.provider;

import com.zw.zwaicodemother.model.enums.ModelProviderEnum;
import dev.langchain4j.model.chat.ChatModel;

/**
 * 对话模型提供方（同步）。
 * <p>
 * 新增一个提供方只需实现本接口并声明其 provider() 枚举值，注册表会自动收录。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
public interface ChatModelProvider {

    /**
     * 本实现负责的提供方
     *
     * @return 提供方枚举
     */
    ModelProviderEnum provider();

    /**
     * 创建同步对话模型
     *
     * @param options 模型参数
     * @return 对话模型
     */
    ChatModel createChatModel(ModelOptions options);
}
