package com.zw.zwaicodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话历史视图对象VO
 * 
 * 功能：封装返回给前端的对话历史数据
 * 属性：包含对话历史的基本信息和关联的用户、应用信息
 * 方法：标准的getter/setter方法，通过Lombok自动生成
 * 
 * @author ZW
 * @since 2025-01-08
 * @version 1.0
 */
@Data
public class ChatHistoryVO implements Serializable {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 消息类型（user/ai/system/error）
     */
    private String messageType;

    /**
     * 消息类型描述
     */
    private String messageTypeText;

    /**
     * 关联应用ID
     */
    private Long appId;

    /**
     * 关联应用信息
     */
    private AppVO app;

    /**
     * 创建用户ID
     */
    private Long userId;

    /**
     * 创建用户信息
     */
    private UserVO user;

    /**
     * 父消息ID
     */
    private Long parentId;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误代码
     */
    private String errorCode;

    /**
     * 消息状态（0-发送中，1-成功，2-失败）
     */
    private Integer status;

    /**
     * 消息状态描述
     */
    private String statusText;

    /**
     * 消息序号
     */
    private Integer messageOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}