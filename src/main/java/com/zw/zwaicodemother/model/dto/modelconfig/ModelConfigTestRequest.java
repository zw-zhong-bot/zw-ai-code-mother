package com.zw.zwaicodemother.model.dto.modelconfig;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 模型配置连通性测试请求。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Data
public class ModelConfigTestRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置 id
     */
    private Long id;

    /**
     * 配置名称（id 为空时按名称查）
     */
    private String configName;
}
