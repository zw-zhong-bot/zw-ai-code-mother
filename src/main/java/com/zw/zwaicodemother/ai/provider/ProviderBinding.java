package com.zw.zwaicodemother.ai.provider;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Provider 与参数的绑定结果。
 * <p>
 * 消费方拿到绑定后直接调用 provider 方法，参数在每次调用时解析，配置变更即时生效。
 *
 * @param <T> Provider 类型
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Getter
@AllArgsConstructor
public class ProviderBinding<T> {

    /**
     * Provider 实现
     */
    private final T provider;

    /**
     * 模型参数
     */
    private final ModelOptions options;
}
