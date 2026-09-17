package com.zw.zwaicodemother.model.dto.modelconfig;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询模型配置请求（配置量小，不分页）。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Data
public class ModelConfigQueryRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 能力类型，为空表示查全部
     */
    private String capability;

    /**
     * 提供方，为空表示不限
     */
    private String provider;

    /**
     * 状态：1 启用 0 停用，为空表示不限
     */
    private Integer status;

    /**
     * 配置名称关键字，模糊匹配
     */
    private String configName;
}
