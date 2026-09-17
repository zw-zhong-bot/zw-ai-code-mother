package com.zw.zwaicodemother.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 模型配置连通性测试结果。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigTestVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否连通成功
     */
    private Boolean success;

    /**
     * 耗时（毫秒）
     */
    private Long elapsedMs;

    /**
     * 结果说明或错误信息
     */
    private String message;
}
