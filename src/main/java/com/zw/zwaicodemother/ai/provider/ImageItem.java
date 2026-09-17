package com.zw.zwaicodemother.ai.provider;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图片结果项（生成或搜索出来的图片）。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Data
@AllArgsConstructor
public class ImageItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 图片访问地址
     */
    private String url;

    /**
     * 图片描述，生成场景下为 null
     */
    private String description;
}
