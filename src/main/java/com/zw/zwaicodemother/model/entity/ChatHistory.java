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
 * 对话历史实体类
 * 
 * 功能：存储用户与AI的对话历史记录
 * 属性：包含消息内容、消息类型、关联应用、用户信息、错误信息等
 * 方法：标准的getter/setter方法，通过Lombok自动生成
 * 
 * @author ZW
 * @since 2025-01-08
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_history")
public class ChatHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 消息类型（user/ai/system/error）
     */
    @Column("messageType")
    private String messageType;

    /**
     * 关联应用ID
     */
    @Column("appId")
    private Long appId;

    /**
     * 创建用户ID
     */
    @Column("userId")
    private Long userId;

    /**
     * 父消息ID（用于上下文关联）
     */
    @Column("parentId")
    private Long parentId;


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
     * 是否删除（逻辑删除标记）
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
