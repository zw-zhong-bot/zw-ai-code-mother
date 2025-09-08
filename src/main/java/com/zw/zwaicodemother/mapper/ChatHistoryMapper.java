package com.zw.zwaicodemother.mapper;

import com.mybatisflex.core.BaseMapper;
import com.zw.zwaicodemother.model.entity.ChatHistory;

/**
 * 对话历史映射层
 * 
 * 功能：提供对话历史的数据访问方法
 * 方法：包含基础CRUD操作和自定义查询方法
 * 
 * @author ZW
 * @since 2025-01-08
 * @version 1.0
 */

public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

//    /**
//     * 根据应用ID查询最新的对话历史（分页）
//     *
//     * @param appId 应用ID
//     * @param page 分页参数
//     * @return 对话历史分页结果
//     */
//    Page<ChatHistory> selectLatestByAppId(@Param("appId") Long appId, Page<ChatHistory> page);
//
//    /**
//     * 根据应用ID和游标ID查询历史消息（向前加载）
//     *
//     * @param appId 应用ID
//     * @param cursorId 游标ID（获取此ID之前的消息）
//     * @param limit 限制数量
//     * @return 对话历史列表
//     */
//    List<ChatHistory> selectByAppIdAndCursor(@Param("appId") Long appId,
//                                           @Param("cursorId") Long cursorId,
//                                           @Param("limit") Integer limit);
//
//    /**
//     * 根据应用ID查询最新N条消息
//     *
//     * @param appId 应用ID
//     * @param limit 限制数量
//     * @return 对话历史列表
//     */
//    List<ChatHistory> selectLatestByAppIdWithLimit(@Param("appId") Long appId,
//                                                 @Param("limit") Integer limit);
//
//    /**
//     * 根据应用ID删除所有对话历史（级联删除）
//     *
//     * @param appId 应用ID
//     * @return 删除的记录数
//     */
//    int deleteByAppId(@Param("appId") Long appId);
//
//    /**
//     * 查询所有应用的对话历史（管理员使用，按时间降序）
//     *
//     * @param page 分页参数
//     * @return 对话历史分页结果
//     */
//    Page<ChatHistory> selectAllOrderByCreateTimeDesc(Page<ChatHistory> page);
//
//    /**
//     * 根据用户ID和应用ID查询对话历史
//     *
//     * @param userId 用户ID
//     * @param appId 应用ID
//     * @param page 分页参数
//     * @return 对话历史分页结果
//     */
//    Page<ChatHistory> selectByUserIdAndAppId(@Param("userId") Long userId,
//                                           @Param("appId") Long appId,
//                                           Page<ChatHistory> page);
//
//    /**
//     * 统计应用的对话消息数量
//     *
//     * @param appId 应用ID
//     * @return 消息数量
//     */
//    Long countByAppId(@Param("appId") Long appId);
}
