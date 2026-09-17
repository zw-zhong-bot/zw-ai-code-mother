package com.zw.zwaicodemother.service;

import com.mybatisflex.core.service.IService;
import com.zw.zwaicodemother.model.dto.modelconfig.ModelConfigAddRequest;
import com.zw.zwaicodemother.model.dto.modelconfig.ModelConfigQueryRequest;
import com.zw.zwaicodemother.model.dto.modelconfig.ModelConfigTestRequest;
import com.zw.zwaicodemother.model.dto.modelconfig.ModelConfigUpdateRequest;
import com.zw.zwaicodemother.model.entity.ModelConfig;
import com.zw.zwaicodemother.model.entity.User;
import com.zw.zwaicodemother.model.enums.ModelCapabilityEnum;
import com.zw.zwaicodemother.model.vo.ModelConfigTestVO;
import com.zw.zwaicodemother.model.vo.ModelConfigVO;

import java.util.List;

/**
 * 模型配置服务层。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
public interface ModelConfigService extends IService<ModelConfig> {

    /**
     * 新增模型配置
     *
     * @param addRequest 新增请求
     * @param loginUser  当前登录用户
     * @return 新配置 id
     */
    long addModelConfig(ModelConfigAddRequest addRequest, User loginUser);

    /**
     * 修改模型配置，apiKey 留空表示不修改
     *
     * @param updateRequest 修改请求
     * @param loginUser     当前登录用户
     * @return 是否成功
     */
    boolean updateModelConfig(ModelConfigUpdateRequest updateRequest, User loginUser);

    /**
     * 删除模型配置（逻辑删除）
     *
     * @param id 配置 id
     * @return 是否成功
     */
    boolean deleteModelConfig(long id);

    /**
     * 设为该能力下的默认配置，同时取消同能力其他配置的默认标记
     *
     * @param id 配置 id
     * @return 是否成功
     */
    boolean setDefault(long id);

    /**
     * 启用或停用配置
     *
     * @param id     配置 id
     * @param status 1 启用 0 停用
     * @return 是否成功
     */
    boolean toggleStatus(long id, Integer status);

    /**
     * 查询配置列表
     *
     * @param queryRequest 查询请求
     * @return 配置列表（密钥脱敏）
     */
    List<ModelConfigVO> listModelConfig(ModelConfigQueryRequest queryRequest);

    /**
     * 获取指定能力的启用默认配置
     *
     * @param capability 能力
     * @return 配置，不存在时返回 null
     */
    ModelConfig getEnabledDefaultConfig(ModelCapabilityEnum capability);

    /**
     * 解密配置中的密钥
     *
     * @param config 配置
     * @return 密钥明文，未配置时返回 null
     */
    String decryptApiKey(ModelConfig config);

    /**
     * 测试配置连通性
     *
     * @param testRequest 测试请求
     * @return 测试结果
     */
    ModelConfigTestVO testConnection(ModelConfigTestRequest testRequest);

    /**
     * 通知全量缓存刷新
     */
    void refreshCache();
}
