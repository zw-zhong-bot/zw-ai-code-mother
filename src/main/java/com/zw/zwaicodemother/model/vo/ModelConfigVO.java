package com.zw.zwaicodemother.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模型配置视图对象（密钥脱敏，不向前端返回明文）。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Data
public class ModelConfigVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置 id
     */
    private Long id;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 能力类型
     */
    private String capability;

    /**
     * 能力类型描述
     */
    private String capabilityText;

    /**
     * 提供方
     */
    private String provider;

    /**
     * 提供方描述
     */
    private String providerText;

    /**
     * 接口地址
     */
    private String baseUrl;

    /**
     * 密钥掩码，如 sk-****3f7a；未配置时为空
     */
    private String apiKeyMasked;

    /**
     * 是否已配置密钥
     */
    private Boolean apiKeyConfigured;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 扩展参数 JSON
     */
    private String params;

    /**
     * 是否该能力下的默认配置：0 否 1 是
     */
    private Integer isDefault;

    /**
     * 状态：1 启用 0 停用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
