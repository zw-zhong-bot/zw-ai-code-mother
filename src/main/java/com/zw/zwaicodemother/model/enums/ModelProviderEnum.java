package com.zw.zwaicodemother.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 模型提供方枚举。
 * <p>
 * 提供方决定由哪一个 Provider 实现类去创建模型，并限定其可承载的能力范围。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Getter
public enum ModelProviderEnum {

    /**
     * OpenAI 兼容接口（scnet、DeepSeek、OpenAI 等）
     */
    OPENAI_COMPATIBLE("OPENAI_COMPATIBLE", "OpenAI 兼容接口",
            EnumSet.of(ModelCapabilityEnum.CHAT, ModelCapabilityEnum.STREAM_CHAT, ModelCapabilityEnum.REASONING)),

    /**
     * 阿里云百炼 DashScope
     */
    DASHSCOPE("DASHSCOPE", "阿里云百炼 DashScope",
            EnumSet.of(ModelCapabilityEnum.IMAGE_GEN)),

    /**
     * Pexels 图片搜索
     */
    PEXELS("PEXELS", "Pexels 图片搜索",
            EnumSet.of(ModelCapabilityEnum.IMAGE_SEARCH));

    private final String value;

    private final String text;

    /**
     * 该提供方支持的能力集合
     */
    private final Set<ModelCapabilityEnum> capabilities;

    ModelProviderEnum(String value, String text, Set<ModelCapabilityEnum> capabilities) {
        this.value = value;
        this.text = text;
        this.capabilities = Collections.unmodifiableSet(capabilities);
    }

    /**
     * 根据值获取枚举
     *
     * @param value 枚举值
     * @return 枚举，不存在时返回 null
     */
    public static ModelProviderEnum getEnumByValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断该提供方是否支持指定能力
     *
     * @param capability 能力
     * @return 是否支持
     */
    public boolean support(ModelCapabilityEnum capability) {
        return capability != null && capabilities.contains(capability);
    }
}
