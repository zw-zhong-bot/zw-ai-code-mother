package com.zw.zwaicodemother.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模型配置实体类。
 * <p>
 * 承载模型的可插拔参数（提供方、接口地址、模型名称、密钥），供管理端维护，
 * 由 ModelRegistry 在调用期解析为具体模型实例。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("model_config")
public class ModelConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 配置名称，全局唯一，如 chat-default
     */
    @Column("configName")
    private String configName;

    /**
     * 能力类型：CHAT/STREAM_CHAT/REASONING/IMAGE_GEN/IMAGE_SEARCH
     */
    @Column("capability")
    private String capability;

    /**
     * 提供方：OPENAI_COMPATIBLE/DASHSCOPE/PEXELS
     */
    @Column("provider")
    private String provider;

    /**
     * 接口地址
     */
    @Column("baseUrl")
    private String baseUrl;

    /**
     * 密钥（加密存储）
     */
    @Column("apiKey")
    private String apiKey;

    /**
     * 模型名称
     */
    @Column("modelName")
    private String modelName;

    /**
     * 扩展参数 JSON
     */
    @Column("params")
    private String params;

    /**
     * 同 capability 下的默认配置：0 否 1 是
     */
    @Column("isDefault")
    private Integer isDefault;

    /**
     * 状态：1 启用 0 停用
     */
    @Column("status")
    private Integer status;

    /**
     * 备注
     */
    @Column("remark")
    private String remark;

    /**
     * 最后修改人 id
     */
    @Column("userId")
    private Long userId;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
