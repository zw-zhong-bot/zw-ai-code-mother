package com.zw.zwaicodemother.ai.provider;

import com.zw.zwaicodemother.model.enums.ModelProviderEnum;

import java.util.List;

/**
 * 图像生成提供方。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
public interface ImageGenProvider {

    /**
     * 本实现负责的提供方
     *
     * @return 提供方枚举
     */
    ModelProviderEnum provider();

    /**
     * 根据提示词生成图片
     *
     * @param options 模型参数，尺寸与张数从 extra 中读取（size、n）
     * @param prompt  提示词
     * @return 图片结果列表
     * @throws Exception 调用异常
     */
    List<ImageItem> generate(ModelOptions options, String prompt) throws Exception;
}
