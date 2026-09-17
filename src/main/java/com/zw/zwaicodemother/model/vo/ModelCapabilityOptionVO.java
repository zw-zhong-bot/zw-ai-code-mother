package com.zw.zwaicodemother.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 模型能力选项（供管理页面渲染能力与提供方下拉框）。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelCapabilityOptionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 能力值
     */
    private String capability;

    /**
     * 能力描述
     */
    private String capabilityText;

    /**
     * 该能力支持的提供方
     */
    private List<ProviderOption> providers;

    /**
     * 提供方选项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderOption implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 提供方值
         */
        private String value;

        /**
         * 提供方描述
         */
        private String text;
    }
}
