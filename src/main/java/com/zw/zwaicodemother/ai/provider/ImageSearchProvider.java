package com.zw.zwaicodemother.ai.provider;

import com.zw.zwaicodemother.model.enums.ModelProviderEnum;

import java.util.List;

/**
 * 图片搜索提供方。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
public interface ImageSearchProvider {

    /**
     * 本实现负责的提供方
     *
     * @return 提供方枚举
     */
    ModelProviderEnum provider();

    /**
     * 按关键词搜索图片
     *
     * @param options 模型参数，接口地址与密钥从其中读取
     * @param query   搜索关键词
     * @param count   期望返回数量
     * @return 图片结果列表
     * @throws Exception 调用异常
     */
    List<ImageItem> search(ModelOptions options, String query, int count) throws Exception;
}
