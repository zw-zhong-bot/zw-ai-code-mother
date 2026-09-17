package com.zw.zwaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zw.zwaicodemother.ai.ModelRegistry;
import com.zw.zwaicodemother.ai.event.ModelConfigChangedEvent;
import com.zw.zwaicodemother.ai.provider.ImageItem;
import com.zw.zwaicodemother.ai.provider.ModelOptions;
import com.zw.zwaicodemother.exception.BusinessException;
import com.zw.zwaicodemother.exception.ErrorCode;
import com.zw.zwaicodemother.exception.ThrowUtils;
import com.zw.zwaicodemother.mapper.ModelConfigMapper;
import com.zw.zwaicodemother.model.dto.modelconfig.ModelConfigAddRequest;
import com.zw.zwaicodemother.model.dto.modelconfig.ModelConfigQueryRequest;
import com.zw.zwaicodemother.model.dto.modelconfig.ModelConfigTestRequest;
import com.zw.zwaicodemother.model.dto.modelconfig.ModelConfigUpdateRequest;
import com.zw.zwaicodemother.model.entity.ModelConfig;
import com.zw.zwaicodemother.model.entity.User;
import com.zw.zwaicodemother.model.enums.ModelCapabilityEnum;
import com.zw.zwaicodemother.model.enums.ModelProviderEnum;
import com.zw.zwaicodemother.model.vo.ModelConfigTestVO;
import com.zw.zwaicodemother.model.vo.ModelConfigVO;
import com.zw.zwaicodemother.service.ModelConfigService;
import com.zw.zwaicodemother.utils.AesUtils;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 模型配置服务实现。
 * <p>
 * 负责配置的增删改查、默认项切换与连通性测试；任何写操作都会发布配置变更事件，
 * 使模型实例缓存与 AI 服务缓存失效，从而实现不重启热切换。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Slf4j
@Service
public class ModelConfigServiceImpl extends ServiceImpl<ModelConfigMapper, ModelConfig> implements ModelConfigService {

    /**
     * 启用
     */
    private static final Integer STATUS_ENABLED = 1;

    /**
     * 停用
     */
    private static final Integer STATUS_DISABLED = 0;

    /**
     * 是默认配置
     */
    private static final Integer DEFAULT_YES = 1;

    /**
     * 非默认配置
     */
    private static final Integer DEFAULT_NO = 0;

    /**
     * 连通性测试等待上限（秒）
     */
    private static final long TEST_TIMEOUT_SECONDS = 60L;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Resource
    private ModelRegistry modelRegistry;

    @Override
    public long addModelConfig(ModelConfigAddRequest addRequest, User loginUser) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);
        ModelCapabilityEnum capability = checkCapabilityAndProvider(addRequest.getCapability(), addRequest.getProvider());
        checkRequiredParams(capability, addRequest.getProvider(), addRequest.getBaseUrl(),
                addRequest.getApiKey(), addRequest.getModelName());
        String configName = addRequest.getConfigName().trim();
        checkConfigNameUnique(configName, null);

        ModelConfig config = new ModelConfig();
        BeanUtil.copyProperties(addRequest, config);
        config.setId(null);
        config.setConfigName(configName);
        config.setApiKey(AesUtils.encrypt(addRequest.getApiKey()));
        boolean wantDefault = DEFAULT_YES.equals(addRequest.getIsDefault());
        config.setIsDefault(wantDefault ? DEFAULT_YES : DEFAULT_NO);
        config.setStatus(STATUS_DISABLED.equals(addRequest.getStatus()) ? STATUS_DISABLED : STATUS_ENABLED);
        config.setUserId(loginUser == null ? null : loginUser.getId());

        // 该能力尚无启用默认配置时自动提升为默认，避免"保存了却没生效"
        boolean autoPromoted = false;
        if (!wantDefault && STATUS_ENABLED.equals(config.getStatus())
                && getEnabledDefaultConfig(capability) == null) {
            config.setIsDefault(DEFAULT_YES);
            autoPromoted = true;
        }
        if (DEFAULT_YES.equals(config.getIsDefault())) {
            clearOtherDefault(capability, null);
        }
        boolean saved = this.save(config);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "新增模型配置失败");
        publishChangedEvent();
        if (autoPromoted) {
            log.info("能力 {} 此前无启用默认配置，已将新配置 {} 自动设为默认", capability, configName);
        }
        log.info("新增模型配置：configName={}, capability={}, provider={}, model={}",
                configName, capability, config.getProvider(), config.getModelName());
        return config.getId();
    }

    @Override
    public boolean updateModelConfig(ModelConfigUpdateRequest updateRequest, User loginUser) {
        ThrowUtils.throwIf(updateRequest == null || updateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        ModelConfig config = this.getById(updateRequest.getId());
        ThrowUtils.throwIf(config == null, ErrorCode.NOT_FOUND_ERROR, "模型配置不存在");

        String capabilityValue = StrUtil.blankToDefault(updateRequest.getCapability(), config.getCapability());
        String providerValue = StrUtil.blankToDefault(updateRequest.getProvider(), config.getProvider());
        ModelCapabilityEnum capability = checkCapabilityAndProvider(capabilityValue, providerValue);
        // apiKey 留空表示沿用原值，校验时用原值兜底
        String apiKey = StrUtil.isBlank(updateRequest.getApiKey()) ? decryptApiKey(config) : updateRequest.getApiKey();
        checkRequiredParams(capability, providerValue,
                StrUtil.blankToDefault(updateRequest.getBaseUrl(), config.getBaseUrl()), apiKey,
                StrUtil.blankToDefault(updateRequest.getModelName(), config.getModelName()));
        if (StrUtil.isNotBlank(updateRequest.getConfigName())) {
            checkConfigNameUnique(updateRequest.getConfigName().trim(), config.getId());
        }

        // 逐字段覆盖，未传的字段保持原值，避免部分更新误清空
        if (StrUtil.isNotBlank(updateRequest.getConfigName())) {
            config.setConfigName(updateRequest.getConfigName().trim());
        }
        if (StrUtil.isNotBlank(updateRequest.getBaseUrl())) {
            config.setBaseUrl(updateRequest.getBaseUrl());
        }
        if (StrUtil.isNotBlank(updateRequest.getModelName())) {
            config.setModelName(updateRequest.getModelName());
        }
        if (updateRequest.getParams() != null) {
            config.setParams(updateRequest.getParams());
        }
        // 密钥为空时保留原密文
        if (StrUtil.isNotBlank(updateRequest.getApiKey())) {
            config.setApiKey(AesUtils.encrypt(updateRequest.getApiKey()));
        }
        if (updateRequest.getIsDefault() != null) {
            config.setIsDefault(DEFAULT_YES.equals(updateRequest.getIsDefault()) ? DEFAULT_YES : DEFAULT_NO);
        }
        if (updateRequest.getStatus() != null) {
            config.setStatus(STATUS_DISABLED.equals(updateRequest.getStatus()) ? STATUS_DISABLED : STATUS_ENABLED);
        }
        if (updateRequest.getRemark() != null) {
            config.setRemark(updateRequest.getRemark());
        }
        config.setCapability(capabilityValue);
        config.setProvider(providerValue);
        config.setUserId(loginUser == null ? null : loginUser.getId());
        // 实体携带的是旧值，若不显式刷新会写回旧时间，使 DB 的 on update CURRENT_TIMESTAMP 失效
        config.setUpdateTime(LocalDateTime.now());

        if (DEFAULT_YES.equals(config.getIsDefault())) {
            clearOtherDefault(capability, config.getId());
        }
        boolean updated = this.updateById(config);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "修改模型配置失败");
        publishChangedEvent();
        log.info("修改模型配置：id={}, configName={}, capability={}", config.getId(), config.getConfigName(), capability);
        return true;
    }

    @Override
    public boolean deleteModelConfig(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        ModelConfig old = this.getById(id);
        ThrowUtils.throwIf(old == null, ErrorCode.NOT_FOUND_ERROR, "模型配置不存在");
        boolean removed = this.removeById(id);
        ThrowUtils.throwIf(!removed, ErrorCode.OPERATION_ERROR, "删除模型配置失败");
        publishChangedEvent();
        log.info("删除模型配置：id={}, configName={}", id, old.getConfigName());
        return true;
    }

    @Override
    public boolean setDefault(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        ModelConfig config = this.getById(id);
        ThrowUtils.throwIf(config == null, ErrorCode.NOT_FOUND_ERROR, "模型配置不存在");
        ModelCapabilityEnum capability = ModelCapabilityEnum.getEnumByValue(config.getCapability());
        ThrowUtils.throwIf(capability == null, ErrorCode.PARAMS_ERROR, "能力类型非法");
        // 停用的配置不能作为默认生效项
        ThrowUtils.throwIf(STATUS_DISABLED.equals(config.getStatus()), ErrorCode.OPERATION_ERROR,
                "停用的配置不能设为默认，请先启用");

        clearOtherDefault(capability, id);
        config.setIsDefault(DEFAULT_YES);
        config.setUpdateTime(LocalDateTime.now());
        boolean updated = this.updateById(config);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "设为默认失败");
        publishChangedEvent();
        log.info("模型配置已设为默认：capability={}, configName={}", capability, config.getConfigName());
        return true;
    }

    @Override
    public boolean toggleStatus(long id, Integer status) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        ModelConfig config = this.getById(id);
        ThrowUtils.throwIf(config == null, ErrorCode.NOT_FOUND_ERROR, "模型配置不存在");
        Integer target = STATUS_DISABLED.equals(status) ? STATUS_DISABLED : STATUS_ENABLED;
        config.setStatus(target);
        config.setUpdateTime(LocalDateTime.now());
        boolean updated = this.updateById(config);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新状态失败");
        publishChangedEvent();
        log.info("模型配置状态更新：id={}, status={}", id, target);
        return true;
    }

    @Override
    public List<ModelConfigVO> listModelConfig(ModelConfigQueryRequest queryRequest) {
        ModelConfigQueryRequest request = queryRequest == null ? new ModelConfigQueryRequest() : queryRequest;
        return this.list().stream()
                .filter(config -> StrUtil.isBlank(request.getCapability())
                        || request.getCapability().equals(config.getCapability()))
                .filter(config -> StrUtil.isBlank(request.getProvider())
                        || request.getProvider().equals(config.getProvider()))
                .filter(config -> request.getStatus() == null || request.getStatus().equals(config.getStatus()))
                .filter(config -> StrUtil.isBlank(request.getConfigName())
                        || StrUtil.containsIgnoreCase(config.getConfigName(), request.getConfigName()))
                .sorted(Comparator
                        .comparingInt((ModelConfig config) -> capabilityOrder(config.getCapability()))
                        .thenComparing(ModelConfig::getId))
                .map(this::toVO)
                .toList();
    }

    @Override
    public ModelConfig getEnabledDefaultConfig(ModelCapabilityEnum capability) {
        if (capability == null) {
            return null;
        }
        return this.list().stream()
                .filter(config -> capability.getValue().equals(config.getCapability()))
                .filter(config -> STATUS_ENABLED.equals(config.getStatus()))
                .filter(config -> DEFAULT_YES.equals(config.getIsDefault()))
                .min(Comparator.comparing(ModelConfig::getId))
                .orElse(null);
    }

    @Override
    public String decryptApiKey(ModelConfig config) {
        if (config == null) {
            return null;
        }
        return AesUtils.decrypt(config.getApiKey());
    }

    @Override
    public ModelConfigTestVO testConnection(ModelConfigTestRequest testRequest) {
        ThrowUtils.throwIf(testRequest == null, ErrorCode.PARAMS_ERROR);
        ModelConfig config = testRequest.getId() == null ? null : this.getById(testRequest.getId());
        if (config == null && StrUtil.isNotBlank(testRequest.getConfigName())) {
            String configName = testRequest.getConfigName().trim();
            config = this.list().stream()
                    .filter(item -> configName.equals(item.getConfigName()))
                    .findFirst()
                    .orElse(null);
        }
        ThrowUtils.throwIf(config == null, ErrorCode.NOT_FOUND_ERROR, "模型配置不存在");
        ModelCapabilityEnum capability = ModelCapabilityEnum.getEnumByValue(config.getCapability());
        ThrowUtils.throwIf(capability == null, ErrorCode.PARAMS_ERROR, "能力类型非法");

        ModelOptions options = modelRegistry.resolveOptions(config);
        long start = System.currentTimeMillis();
        try {
            String message = switch (capability) {
                case CHAT -> {
                    ChatModel chatModel = modelRegistry.buildChatModel(options);
                    String reply = chatModel.chat("ping");
                    yield "模型回复：" + StrUtil.maxLength(StrUtil.blankToDefault(reply, "(空)"), 60);
                }
                case STREAM_CHAT, REASONING -> testStreaming(options);
                case IMAGE_GEN -> {
                    List<ImageItem> items = modelRegistry.generateImage(options, "a simple red circle icon");
                    yield "生成图片 " + items.size() + " 张，首张地址：" + firstUrl(items);
                }
                case IMAGE_SEARCH -> {
                    List<ImageItem> items = modelRegistry.searchImage(options, "nature", 1);
                    yield "搜索到图片 " + items.size() + " 张，首张地址：" + firstUrl(items);
                }
            };
            long elapsed = System.currentTimeMillis() - start;
            log.info("模型配置连通性测试成功：configName={}, capability={}, 耗时={}ms",
                    config.getConfigName(), capability, elapsed);
            return new ModelConfigTestVO(true, elapsed, message);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("模型配置连通性测试失败：configName={}, capability={}", config.getConfigName(), capability, e);
            return new ModelConfigTestVO(false, elapsed, "调用失败：" + e.getMessage());
        }
    }

    @Override
    public void refreshCache() {
        publishChangedEvent();
        log.info("已按管理员请求刷新模型缓存");
    }

    /**
     * 流式模型测试：等待首个 token
     *
     * @param options 模型参数
     * @return 测试说明
     * @throws Exception 调用异常
     */
    private String testStreaming(ModelOptions options) throws Exception {
        StreamingChatModel streamingChatModel = modelRegistry.buildStreamingChatModel(options);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> firstChunk = new AtomicReference<>();
        AtomicReference<String> errorMessage = new AtomicReference<>();
        streamingChatModel.chat("ping", new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                firstChunk.compareAndSet(null, partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                errorMessage.set(error.getMessage());
                latch.countDown();
            }
        });
        if (!latch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException("等待流式响应超时（" + TEST_TIMEOUT_SECONDS + "s）");
        }
        if (StrUtil.isNotBlank(errorMessage.get())) {
            throw new IllegalStateException(errorMessage.get());
        }
        return "流式响应正常，首个分片：" + StrUtil.blankToDefault(firstChunk.get(), "(空)");
    }

    /**
     * 取首个图片地址用于展示
     *
     * @param items 图片列表
     * @return 地址
     */
    private String firstUrl(List<ImageItem> items) {
        if (items == null || items.isEmpty()) {
            return "(无)";
        }
        return StrUtil.maxLength(StrUtil.blankToDefault(items.get(0).getUrl(), "(空)"), 80);
    }

    /**
     * 发布配置变更事件（统一全量失效，写操作频率极低，代价可忽略）
     */
    private void publishChangedEvent() {
        eventPublisher.publishEvent(new ModelConfigChangedEvent(null));
    }

    /**
     * 取消同能力下其他配置的默认标记
     *
     * @param capability 能力
     * @param exceptId   不处理的配置 id，可为 null
     */
    private void clearOtherDefault(ModelCapabilityEnum capability, Long exceptId) {
        for (ModelConfig item : this.list()) {
            boolean sameCapability = capability.getValue().equals(item.getCapability());
            boolean isDefault = DEFAULT_YES.equals(item.getIsDefault());
            boolean skipSelf = exceptId != null && exceptId.equals(item.getId());
            if (sameCapability && isDefault && !skipSelf) {
                item.setIsDefault(DEFAULT_NO);
                item.setUpdateTime(LocalDateTime.now());
                this.updateById(item);
            }
        }
    }

    /**
     * 校验能力与提供方，并校验两者匹配关系
     *
     * @param capabilityValue 能力值
     * @param providerValue   提供方值
     * @return 能力枚举
     */
    private ModelCapabilityEnum checkCapabilityAndProvider(String capabilityValue, String providerValue) {
        ModelCapabilityEnum capability = ModelCapabilityEnum.getEnumByValue(capabilityValue);
        ThrowUtils.throwIf(capability == null, ErrorCode.PARAMS_ERROR, "能力类型非法：" + capabilityValue);
        ModelProviderEnum provider = ModelProviderEnum.getEnumByValue(providerValue);
        ThrowUtils.throwIf(provider == null, ErrorCode.PARAMS_ERROR, "提供方非法：" + providerValue);
        ThrowUtils.throwIf(!provider.support(capability), ErrorCode.PARAMS_ERROR,
                "提供方 " + provider.getText() + " 不支持能力 " + capability.getText());
        return capability;
    }

    /**
     * 校验不同能力的必填参数
     *
     * @param capability 能力
     * @param provider   提供方
     * @param baseUrl    接口地址
     * @param apiKey     密钥
     * @param modelName  模型名称
     */
    private void checkRequiredParams(ModelCapabilityEnum capability, String provider,
                                     String baseUrl, String apiKey, String modelName) {
        ThrowUtils.throwIf(StrUtil.isBlank(apiKey), ErrorCode.PARAMS_ERROR, "密钥不能为空");
        if (ModelProviderEnum.OPENAI_COMPATIBLE.getValue().equals(provider)) {
            ThrowUtils.throwIf(StrUtil.isBlank(baseUrl), ErrorCode.PARAMS_ERROR, "接口地址不能为空");
        }
        if (capability != ModelCapabilityEnum.IMAGE_SEARCH) {
            ThrowUtils.throwIf(StrUtil.isBlank(modelName), ErrorCode.PARAMS_ERROR, "模型名称不能为空");
        }
    }

    /**
     * 校验配置名称唯一
     *
     * @param configName 配置名称
     * @param exceptId   排除的配置 id，用于修改场景
     */
    private void checkConfigNameUnique(String configName, Long exceptId) {
        boolean duplicated = this.list().stream()
                .anyMatch(item -> configName.equals(item.getConfigName())
                        && (exceptId == null || !exceptId.equals(item.getId())));
        ThrowUtils.throwIf(duplicated, ErrorCode.PARAMS_ERROR, "配置名称已存在：" + configName);
    }

    /**
     * 转换为视图对象（密钥脱敏）
     *
     * @param config 配置
     * @return 视图对象
     */
    private ModelConfigVO toVO(ModelConfig config) {
        ModelConfigVO vo = new ModelConfigVO();
        BeanUtil.copyProperties(config, vo);
        vo.setApiKeyConfigured(StrUtil.isNotBlank(config.getApiKey()));
        vo.setApiKeyMasked(AesUtils.mask(decryptApiKey(config)));
        ModelCapabilityEnum capability = ModelCapabilityEnum.getEnumByValue(config.getCapability());
        vo.setCapabilityText(capability == null ? config.getCapability() : capability.getText());
        ModelProviderEnum provider = ModelProviderEnum.getEnumByValue(config.getProvider());
        vo.setProviderText(provider == null ? config.getProvider() : provider.getText());
        return vo;
    }

    /**
     * 能力排序，保证列表按能力分组展示
     *
     * @param capabilityValue 能力值
     * @return 排序序号
     */
    private int capabilityOrder(String capabilityValue) {
        ModelCapabilityEnum capability = ModelCapabilityEnum.getEnumByValue(capabilityValue);
        return capability == null ? Integer.MAX_VALUE : capability.ordinal();
    }
}
