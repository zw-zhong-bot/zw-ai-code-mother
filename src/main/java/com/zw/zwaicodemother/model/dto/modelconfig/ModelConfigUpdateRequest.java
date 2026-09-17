package com.zw.zwaicodemother.model.dto.modelconfig;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 修改模型配置请求。
 * <p>
 * apiKey 传空（null 或空白）表示不修改原有密钥。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Data
public class ModelConfigUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置 id
     */
    private Long id;

    /**
     * 配置名称，全局唯一
     */
    private String configName;

    /**
     * 能力类型：CHAT/STREAM_CHAT/REASONING/IMAGE_GEN/IMAGE_SEARCH
     */
    private String capability;

    /**
     * 提供方：OPENAI_COMPATIBLE/DASHSCOPE/PEXELS
     */
    private String provider;

    /**
     * 接口地址
     */
    private String baseUrl;

    /**
     * 密钥明文，留空表示不修改
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 扩展参数 JSON 字符串
     */
    private String params;

    /**
     * 是否设为该能力下的默认配置：0 否 1 是
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
}
