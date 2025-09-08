package com.zw.zwaicodemother.controller;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zw.zwaicodemother.annotation.AuthCheck;
import com.zw.zwaicodemother.common.BaseResponse;
import com.zw.zwaicodemother.common.ResultUtils;
import com.zw.zwaicodemother.constant.UserConstant;
import com.zw.zwaicodemother.exception.ErrorCode;
import com.zw.zwaicodemother.exception.ThrowUtils;
import com.zw.zwaicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.zw.zwaicodemother.model.entity.ChatHistory;
import com.zw.zwaicodemother.model.entity.User;
import com.zw.zwaicodemother.service.ChatHistoryService;
import com.zw.zwaicodemother.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 对话历史控制层
 * 
 * 功能：提供对话历史相关的REST API接口
 * 方法：包含对话历史的增删改查、分页查询、权限控制等接口
 * 
 * @author ZW
 * @since 2025-01-08
 * @version 1.0
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private UserService userService;
    /**
     * 分页查询某个应用的对话历史（游标查询）
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request        请求
     * @return 对话历史分页
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>>  listAppChatHistoryByPage(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                              HttpServletRequest request){
        User loginUser=userService.getLoginUser(request);
        Page<ChatHistory> result  = chatHistoryService.listAppChatHistoryByPage(appId,pageSize,lastCreateTime,loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页查询所有对话历史
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史分页
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listChatHistoryByPage(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest){
        ThrowUtils.throwIf(
                chatHistoryQueryRequest==null,ErrorCode.PARAMS_ERROR
        );
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        //查询数据
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper=chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> result  = chatHistoryService.page(Page.of(pageNum,pageSize),queryWrapper);
        return ResultUtils.success(result);
    }




}
