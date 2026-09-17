package com.zw.zwaicodemother.model.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 模型能力类型枚举。
 * <p>
 * 一个能力对应一类使用场景，同一能力下可存在多份配置，由 isDefault 决定生效的那一份。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Getter
public enum ModelCapabilityEnum {

    /**
     * 对话模型（同步）
     */
    CHAT("CHAT", "对话模型"),

    /**
     * 流式对话模型
     */
    STREAM_CHAT("STREAM_CHAT", "流式对话模型"),

    /**
     * 推理流式模型（Vue 项目生成，带工具调用）
     */
    REASONING("REASONING", "推理流式模型"),

    /**
     * 图像生成
     */
    IMAGE_GEN("IMAGE_GEN", "图像生成"),

    /**
     * 图片搜索
     */
    IMAGE_SEARCH("IMAGE_SEARCH", "图片搜索");

    private final String value;

    private final String text;

    ModelCapabilityEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据值获取枚举
     *
     * @param value 枚举值
     * @return 枚举，不存在时返回 null
     */
    public static ModelCapabilityEnum getEnumByValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value))
                .findFirst()
                .orElse(null);
    }
}
