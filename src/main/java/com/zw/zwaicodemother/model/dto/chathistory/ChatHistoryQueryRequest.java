package com.zw.zwaicodemother.model.dto.chathistory;

import com.zw.zwaicodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话历史查询请求DTO
 * 
 * 功能：封装对话历史查询的请求参数
 * 参数：包含应用ID、消息类型、用户ID、时间范围等查询条件
 * 返回值：无（DTO类）
 * 异常：无
 *
 * @author ZW
 * @since 2025-01-08
 * @version 1.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatHistoryQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 消息类型（user/ai）
     */
    private String messageType;

    /**
     * 应用id
     */
    private Long appId;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 游标查询 - 最后一条记录的创建时间
     * 用于分页查询，获取早于此时间的记录
     */
    private LocalDateTime lastCreateTime;

    private static final long serialVersionUID = 1L;
}