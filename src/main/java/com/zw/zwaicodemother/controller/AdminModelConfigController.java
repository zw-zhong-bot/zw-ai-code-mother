package com.zw.zwaicodemother.controller;

import com.zw.zwaicodemother.annotation.AuthCheck;
import com.zw.zwaicodemother.common.BaseResponse;
import com.zw.zwaicodemother.common.DeleteRequest;
import com.zw.zwaicodemother.common.ResultUtils;
import com.zw.zwaicodemother.constant.UserConstant;
import com.zw.zwaicodemother.exception.BusinessException;
import com.zw.zwaicodemother.exception.ErrorCode;
import com.zw.zwaicodemother.exception.ThrowUtils;
import com.zw.zwaicodemother.model.dto.modelconfig.ModelConfigAddRequest;
import com.zw.zwaicodemother.model.dto.modelconfig.ModelConfigQueryRequest;
import com.zw.zwaicodemother.model.dto.modelconfig.ModelConfigTestRequest;
import com.zw.zwaicodemother.model.dto.modelconfig.ModelConfigUpdateRequest;
import com.zw.zwaicodemother.model.entity.User;
import com.zw.zwaicodemother.model.enums.ModelCapabilityEnum;
import com.zw.zwaicodemother.model.enums.ModelProviderEnum;
import com.zw.zwaicodemother.model.vo.ModelCapabilityOptionVO;
import com.zw.zwaicodemother.model.vo.ModelConfigTestVO;
import com.zw.zwaicodemother.model.vo.ModelConfigVO;
import com.zw.zwaicodemother.service.ModelConfigService;
import com.zw.zwaicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 模型配置管理接口（仅管理员）。
 * <p>
 * 覆盖模型参数的可视化维护与热切换：新增、修改、删除、设默认、启停、连通性测试与缓存刷新。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@RestController
@RequestMapping("/admin/model")
public class AdminModelConfigController {

    @Resource
    private ModelConfigService modelConfigService;

    @Resource
    private UserService userService;

    /**
     * 查询配置列表（密钥脱敏）
     *
     * @param queryRequest 查询请求
     * @return 配置列表
     */
    @PostMapping("/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<ModelConfigVO>> listModelConfig(@RequestBody(required = false) ModelConfigQueryRequest queryRequest) {
        return ResultUtils.success(modelConfigService.listModelConfig(queryRequest));
    }

    /**
     * 查询能力与提供方选项
     *
     * @return 能力选项列表
     */
    @GetMapping("/capabilities")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<ModelCapabilityOptionVO>> listCapabilities() {
        List<ModelCapabilityOptionVO> options = Arrays.stream(ModelCapabilityEnum.values())
                .map(capability -> {
                    List<ModelCapabilityOptionVO.ProviderOption> providers = Arrays.stream(ModelProviderEnum.values())
                            .filter(provider -> provider.support(capability))
                            .map(provider -> new ModelCapabilityOptionVO.ProviderOption(provider.getValue(), provider.getText()))
                            .toList();
                    return new ModelCapabilityOptionVO(capability.getValue(), capability.getText(), providers);
                })
                .toList();
        return ResultUtils.success(options);
    }

    /**
     * 新增配置
     *
     * @param addRequest 新增请求
     * @param request    请求对象
     * @return 新配置 id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addModelConfig(@RequestBody ModelConfigAddRequest addRequest,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(modelConfigService.addModelConfig(addRequest, loginUser));
    }

    /**
     * 修改配置（apiKey 留空表示不修改）
     *
     * @param updateRequest 修改请求
     * @param request       请求对象
     * @return 是否成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateModelConfig(@RequestBody ModelConfigUpdateRequest updateRequest,
                                                   HttpServletRequest request) {
        ThrowUtils.throwIf(updateRequest == null || updateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(modelConfigService.updateModelConfig(updateRequest, loginUser));
    }

    /**
     * 删除配置（逻辑删除）
     *
     * @param deleteRequest 删除请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteModelConfig(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(modelConfigService.deleteModelConfig(deleteRequest.getId()));
    }

    /**
     * 设为该能力下的默认配置
     *
     * @param id 配置 id
     * @return 是否成功
     */
    @PostMapping("/setDefault")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> setDefault(@RequestParam long id) {
        return ResultUtils.success(modelConfigService.setDefault(id));
    }

    /**
     * 启用或停用配置
     *
     * @param id     配置 id
     * @param status 1 启用 0 停用
     * @return 是否成功
     */
    @PostMapping("/toggle")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> toggleStatus(@RequestParam long id, @RequestParam Integer status) {
        return ResultUtils.success(modelConfigService.toggleStatus(id, status));
    }

    /**
     * 连通性测试
     * <p>
     * 对话类会发送一次最小对话请求；图像生成类会实际生成一张图片（产生计费）。
     *
     * @param testRequest 测试请求
     * @return 测试结果
     */
    @PostMapping("/test")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<ModelConfigTestVO> testConnection(@RequestBody ModelConfigTestRequest testRequest) {
        ThrowUtils.throwIf(testRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(modelConfigService.testConnection(testRequest));
    }

    /**
     * 强制刷新模型缓存
     *
     * @return 是否成功
     */
    @PostMapping("/refresh")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> refreshCache() {
        modelConfigService.refreshCache();
        return ResultUtils.success(true);
    }
}
