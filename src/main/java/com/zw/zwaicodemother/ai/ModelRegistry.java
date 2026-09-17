package com.zw.zwaicodemother.ai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.zw.zwaicodemother.ai.event.ModelConfigChangedEvent;
import com.zw.zwaicodemother.ai.provider.ChatModelProvider;
import com.zw.zwaicodemother.ai.provider.ImageGenProvider;
import com.zw.zwaicodemother.ai.provider.ImageItem;
import com.zw.zwaicodemother.ai.provider.ImageSearchProvider;
import com.zw.zwaicodemother.ai.provider.ModelOptions;
import com.zw.zwaicodemother.ai.provider.ProviderBinding;
import com.zw.zwaicodemother.ai.provider.StreamingChatModelProvider;
import com.zw.zwaicodemother.config.model.ChatModelFallbackProperties;
import com.zw.zwaicodemother.config.model.DashScopeFallbackProperties;
import com.zw.zwaicodemother.config.model.PexelsFallbackProperties;
import com.zw.zwaicodemother.exception.ErrorCode;
import com.zw.zwaicodemother.exception.ThrowUtils;
import com.zw.zwaicodemother.mapper.ModelConfigMapper;
import com.zw.zwaicodemother.model.entity.ModelConfig;
import com.zw.zwaicodemother.model.enums.ModelCapabilityEnum;
import com.zw.zwaicodemother.model.enums.ModelProviderEnum;
import com.zw.zwaicodemother.utils.AesUtils;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 模型注册表。
 * <p>
 * 模型的可插拔入口：按能力解析参数（数据库启用默认配置优先，缺失时回退 yml），
 * 按提供方选择 Provider 实现，并按参数版本缓存实例；配置变更事件到达时缓存失效重建。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Slf4j
@Component
public class ModelRegistry {

    /**
     * 配置读取走 Mapper 而非 Service：Service 层依赖本注册表（连通性测试），
     * 反向依赖会形成循环依赖，此处刻意保持在数据访问层
     */
    @Resource
    private ModelConfigMapper modelConfigMapper;

    @Resource
    private ChatModelFallbackProperties chatFallbackProperties;

    @Resource
    private DashScopeFallbackProperties dashScopeFallbackProperties;

    @Resource
    private PexelsFallbackProperties pexelsFallbackProperties;

    @Resource
    private ChatModelProvider[] chatProviderBeans;

    @Resource
    private StreamingChatModelProvider[] streamingChatProviderBeans;

    @Resource
    private ImageGenProvider[] imageGenProviderBeans;

    @Resource
    private ImageSearchProvider[] imageSearchProviderBeans;

    /**
     * 提供方到 Provider 实现的映射
     */
    private final Map<ModelProviderEnum, ChatModelProvider> chatProviders = new EnumMap<>(ModelProviderEnum.class);

    private final Map<ModelProviderEnum, StreamingChatModelProvider> streamingChatProviders =
            new EnumMap<>(ModelProviderEnum.class);

    private final Map<ModelProviderEnum, ImageGenProvider> imageGenProviders = new EnumMap<>(ModelProviderEnum.class);

    private final Map<ModelProviderEnum, ImageSearchProvider> imageSearchProviders =
            new EnumMap<>(ModelProviderEnum.class);

    /**
     * 能力到模型实例的缓存
     */
    private final Map<ModelCapabilityEnum, CachedModel> modelCache = new ConcurrentHashMap<>();

    /**
     * 已提示过兜底的能力，避免重复打日志
     */
    private final Set<ModelCapabilityEnum> fallbackLogged = ConcurrentHashMap.newKeySet();

    /**
     * 已提示过凭证异常的配置标识，避免重复打日志
     */
    private final Set<String> suspiciousWarned = ConcurrentHashMap.newKeySet();

    /**
     * 已知的无效占位符取值
     */
    private static final Set<String> SUSPICIOUS_API_KEYS = Set.of("001", "NOT_SET_IN_YAML", "REPLACE_ME", "CHANGE_ME");

    /**
     * 初始化提供方映射
     */
    @PostConstruct
    public void initProviders() {
        for (ChatModelProvider provider : chatProviderBeans) {
            chatProviders.put(provider.provider(), provider);
        }
        for (StreamingChatModelProvider provider : streamingChatProviderBeans) {
            streamingChatProviders.put(provider.provider(), provider);
        }
        for (ImageGenProvider provider : imageGenProviderBeans) {
            imageGenProviders.put(provider.provider(), provider);
        }
        for (ImageSearchProvider provider : imageSearchProviderBeans) {
            imageSearchProviders.put(provider.provider(), provider);
        }
        log.info("模型注册表初始化完成：对话 {} 个、流式 {} 个、图像生成 {} 个、图片搜索 {} 个",
                chatProviders.size(), streamingChatProviders.size(),
                imageGenProviders.size(), imageSearchProviders.size());
    }

    /**
     * 获取同步对话模型
     *
     * @return 对话模型
     */
    public ChatModel getChatModel() {
        ModelOptions options = resolveOptions(ModelCapabilityEnum.CHAT);
        return cachedOrCreate(ModelCapabilityEnum.CHAT, ChatModel.class, options,
                item -> requireProvider(chatProviders, item, ChatModelProvider.class).createChatModel(item));
    }

    /**
     * 获取流式对话模型
     *
     * @return 流式对话模型
     */
    public StreamingChatModel getStreamingChatModel() {
        ModelOptions options = resolveOptions(ModelCapabilityEnum.STREAM_CHAT);
        return cachedOrCreate(ModelCapabilityEnum.STREAM_CHAT, StreamingChatModel.class, options,
                item -> requireProvider(streamingChatProviders, item, StreamingChatModelProvider.class)
                        .createStreamingChatModel(item));
    }

    /**
     * 获取推理流式模型
     *
     * @return 推理流式模型
     */
    public StreamingChatModel getReasoningStreamingChatModel() {
        ModelOptions options = resolveOptions(ModelCapabilityEnum.REASONING);
        return cachedOrCreate(ModelCapabilityEnum.REASONING, StreamingChatModel.class, options,
                item -> requireProvider(streamingChatProviders, item, StreamingChatModelProvider.class)
                        .createStreamingChatModel(item));
    }

    /**
     * 获取图像生成绑定
     *
     * @return Provider 与参数的绑定
     */
    public ProviderBinding<ImageGenProvider> getImageGen() {
        ModelOptions options = resolveOptions(ModelCapabilityEnum.IMAGE_GEN);
        return new ProviderBinding<>(requireProvider(imageGenProviders, options, ImageGenProvider.class), options);
    }

    /**
     * 获取图片搜索绑定
     *
     * @return Provider 与参数的绑定
     */
    public ProviderBinding<ImageSearchProvider> getImageSearch() {
        ModelOptions options = resolveOptions(ModelCapabilityEnum.IMAGE_SEARCH);
        return new ProviderBinding<>(requireProvider(imageSearchProviders, options, ImageSearchProvider.class), options);
    }

    /**
     * 解析指定能力的模型参数：数据库启用默认配置优先，缺失时回退 yml
     *
     * @param capability 能力
     * @return 模型参数
     */
    public ModelOptions resolveOptions(ModelCapabilityEnum capability) {
        ModelConfig config = findEnabledDefaultConfig(capability);
        ModelOptions options = config != null ? fromConfig(config) : fromFallback(capability);
        warnIfSuspiciousApiKey(options);
        return options;
    }

    /**
     * 凭证兜底校验：命中空值或已知占位符时告警一次。
     * <p>
     * 这类问题原先只能在调用报错时从堆栈里发现，且容易被误判为代码缺陷。
     *
     * @param options 模型参数
     */
    private void warnIfSuspiciousApiKey(ModelOptions options) {
        String apiKey = options.getApiKey();
        boolean blank = StrUtil.isBlank(apiKey);
        boolean placeholder = !blank && SUSPICIOUS_API_KEYS.contains(apiKey.trim());
        if (!blank && !placeholder) {
            return;
        }
        if (!suspiciousWarned.add(options.getConfigName())) {
            return;
        }
        log.warn("模型凭证疑似无效（{}），配置：{}，模型：{}。"
                        + "若该值来自环境变量，请确认当前进程已加载最新值："
                        + "Windows 不会向已运行进程传播环境变量变更，需重启 IDEA；"
                        + "也可在 /admin/modelManage 中用数据库配置覆盖。",
                blank ? "为空" : "占位符 " + apiKey.trim(),
                options.getConfigName(), options.getModelName());
    }

    /**
     * 查询指定能力的启用默认配置
     *
     * @param capability 能力
     * @return 配置，不存在时返回 null
     */
    private ModelConfig findEnabledDefaultConfig(ModelCapabilityEnum capability) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("capability", capability.getValue())
                .eq("status", 1)
                .eq("isDefault", 1);
        List<ModelConfig> configs = modelConfigMapper.selectListByQuery(queryWrapper);
        if (configs == null || configs.isEmpty()) {
            return null;
        }
        if (configs.size() > 1) {
            log.warn("能力 {} 存在多份默认配置，已取 id 最小的一条", capability);
        }
        return configs.stream()
                .min(Comparator.comparing(ModelConfig::getId))
                .orElse(null);
    }

    /**
     * 清除缓存
     *
     * @param capability 能力，为空表示全部
     */
    public void refresh(ModelCapabilityEnum capability) {
        if (capability == null) {
            modelCache.clear();
            log.info("已清空全部模型实例缓存");
            return;
        }
        modelCache.remove(capability);
        log.info("已清除模型实例缓存，capability={}", capability);
    }

    /**
     * 接收配置变更事件
     *
     * @param event 变更事件
     */
    @EventListener
    public void onModelConfigChanged(ModelConfigChangedEvent event) {
        refresh(event.getCapability());
    }

    /**
     * 取指定能力当前参数版本，供消费方判断是否需要重建持有的服务实例
     *
     * @param capability 能力
     * @return 参数版本摘要
     */
    public String currentVersion(ModelCapabilityEnum capability) {
        return resolveOptions(capability).version();
    }

    /**
     * 由指定配置构建参数（用于连通性测试等指定配置场景）
     *
     * @param config 配置
     * @return 模型参数
     */
    public ModelOptions resolveOptions(ModelConfig config) {
        return fromConfig(config);
    }

    /**
     * 直接创建同步对话模型（不走缓存）
     *
     * @param options 模型参数
     * @return 对话模型
     */
    public ChatModel buildChatModel(ModelOptions options) {
        return requireProvider(chatProviders, options, ChatModelProvider.class).createChatModel(options);
    }

    /**
     * 直接创建流式对话模型（不走缓存）
     *
     * @param options 模型参数
     * @return 流式对话模型
     */
    public StreamingChatModel buildStreamingChatModel(ModelOptions options) {
        return requireProvider(streamingChatProviders, options, StreamingChatModelProvider.class)
                .createStreamingChatModel(options);
    }

    /**
     * 按指定参数生成图片（不走缓存）
     *
     * @param options 模型参数
     * @param prompt  提示词
     * @return 图片结果
     * @throws Exception 调用异常
     */
    public List<ImageItem> generateImage(ModelOptions options, String prompt)
            throws Exception {
        return requireProvider(imageGenProviders, options, ImageGenProvider.class).generate(options, prompt);
    }

    /**
     * 按指定参数搜索图片（不走缓存）
     *
     * @param options 模型参数
     * @param query   关键词
     * @param count   数量
     * @return 图片结果
     * @throws Exception 调用异常
     */
    public List<ImageItem> searchImage(ModelOptions options, String query, int count)
            throws Exception {
        return requireProvider(imageSearchProviders, options, ImageSearchProvider.class).search(options, query, count);
    }

    /**
     * 由数据库配置构建参数
     *
     * @param config 配置
     * @return 模型参数
     */
    private ModelOptions fromConfig(ModelConfig config) {
        Map<String, Object> extra = parseParams(config.getParams());
        return ModelOptions.builder()
                .configName(config.getConfigName())
                .provider(ModelProviderEnum.getEnumByValue(config.getProvider()))
                .baseUrl(config.getBaseUrl())
                .apiKey(AesUtils.decrypt(config.getApiKey()))
                .modelName(config.getModelName())
                .maxTokens(extraInt(extra, "maxTokens"))
                .temperature(extraDouble(extra, "temperature"))
                .timeout(extraDuration(extra, "timeout"))
                .logRequests(extraBoolean(extra, "logRequests", false))
                .logResponses(extraBoolean(extra, "logResponses", false))
                .extra(extra)
                .build();
    }

    /**
     * 由 yml 兜底配置构建参数
     *
     * @param capability 能力
     * @return 模型参数
     */
    private ModelOptions fromFallback(ModelCapabilityEnum capability) {
        if (fallbackLogged.add(capability)) {
            log.info("能力 {} 未配置数据库默认配置，使用 yml 兜底参数", capability);
        }
        return switch (capability) {
            case CHAT -> chatOptions("yml-fallback-chat", chatFallbackProperties.getChatModel());
            case STREAM_CHAT -> chatOptions("yml-fallback-stream-chat", chatFallbackProperties.getStreamingChatModel());
            case REASONING -> chatOptions("yml-fallback-reasoning", chatFallbackProperties.resolveReasoning());
            case IMAGE_GEN -> ModelOptions.builder()
                    .configName("yml-fallback-image-gen")
                    .provider(ModelProviderEnum.DASHSCOPE)
                    .apiKey(dashScopeFallbackProperties.getApiKey())
                    .modelName(dashScopeFallbackProperties.getImageModel())
                    .extra(Map.of("size", StrUtil.blankToDefault(dashScopeFallbackProperties.getImageSize(), "1024*1024")))
                    .build();
            case IMAGE_SEARCH -> ModelOptions.builder()
                    .configName("yml-fallback-image-search")
                    .provider(ModelProviderEnum.PEXELS)
                    .baseUrl(pexelsFallbackProperties.getBaseUrl())
                    .apiKey(pexelsFallbackProperties.getApiKey())
                    .extra(Map.of())
                    .build();
        };
    }

    /**
     * 由对话模型 yml 参数构建
     *
     * @param configName 配置标识
     * @param params     yml 参数
     * @return 模型参数
     */
    private ModelOptions chatOptions(String configName, ChatModelFallbackProperties.ChatParams params) {
        return ModelOptions.builder()
                .configName(configName)
                .provider(ModelProviderEnum.OPENAI_COMPATIBLE)
                .baseUrl(params.getBaseUrl())
                .apiKey(params.getApiKey())
                .modelName(params.getModelName())
                .maxTokens(params.getMaxTokens())
                .temperature(params.getTemperature())
                .timeout(params.getTimeout())
                .logRequests(params.getLogRequests())
                .logResponses(params.getLogResponses())
                .extra(Map.of())
                .build();
    }

    /**
     * 读取缓存，版本不一致或类型不匹配时重建
     *
     * @param capability 能力
     * @param type       期望类型
     * @param options    模型参数
     * @param creator    创建逻辑
     * @param <T>        模型类型
     * @return 模型实例
     */
    private <T> T cachedOrCreate(ModelCapabilityEnum capability, Class<T> type, ModelOptions options,
                                 Function<ModelOptions, T> creator) {
        String version = options.version();
        CachedModel cached = modelCache.get(capability);
        if (cached != null && cached.type() == type && cached.version().equals(version)) {
            return type.cast(cached.instance());
        }
        synchronized (modelCache) {
            cached = modelCache.get(capability);
            if (cached != null && cached.type() == type && cached.version().equals(version)) {
                return type.cast(cached.instance());
            }
            T instance = creator.apply(options);
            modelCache.put(capability, new CachedModel(version, type, instance));
            log.info("模型实例已创建，capability={}, config={}, provider={}, model={}",
                    capability, options.getConfigName(), options.getProvider(), options.getModelName());
            return instance;
        }
    }

    /**
     * 按提供方取实现，取不到时抛出明确异常
     *
     * @param pool     提供方映射
     * @param options  模型参数
     * @param type     接口类型
     * @param <P>      Provider 类型
     * @return Provider 实现
     */
    private <P> P requireProvider(Map<ModelProviderEnum, P> pool, ModelOptions options, Class<P> type) {
        ModelProviderEnum providerEnum = options.getProvider();
        P provider = providerEnum == null ? null : pool.get(providerEnum);
        ThrowUtils.throwIf(provider == null, ErrorCode.SYSTEM_ERROR,
                "没有可用的 " + type.getSimpleName() + " 实现，配置：" + options.getConfigName()
                        + "，提供方：" + providerEnum);
        return provider;
    }

    /**
     * 解析 params JSON
     *
     * @param params JSON 字符串
     * @return 键值对
     */
    private Map<String, Object> parseParams(String params) {
        if (StrUtil.isBlank(params)) {
            return Map.of();
        }
        return JSONUtil.parseObj(params);
    }

    private Integer extraInt(Map<String, Object> extra, String key) {
        Object value = extra.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(value).trim();
        return StrUtil.isBlank(text) ? null : Integer.valueOf(text);
    }

    private Double extraDouble(Map<String, Object> extra, String key) {
        Object value = extra.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        String text = String.valueOf(value).trim();
        return StrUtil.isBlank(text) ? null : Double.valueOf(text);
    }

    private Boolean extraBoolean(Map<String, Object> extra, String key, Boolean defaultValue) {
        Object value = extra.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.valueOf(String.valueOf(value).trim());
    }

    /**
     * 超时支持秒数与 ISO-8601 两种写法
     *
     * @param extra 扩展参数
     * @param key   参数名
     * @return 超时时间
     */
    private Duration extraDuration(Map<String, Object> extra, String key) {
        Object value = extra.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return Duration.ofSeconds(number.longValue());
        }
        String text = String.valueOf(value).trim();
        if (StrUtil.isBlank(text)) {
            return null;
        }
        if (text.matches("\\d+")) {
            return Duration.ofSeconds(Long.parseLong(text));
        }
        try {
            return Duration.parse(text);
        } catch (Exception e) {
            log.warn("无法解析超时参数，已忽略：{}", text);
            return null;
        }
    }

    /**
     * 模型实例缓存项
     *
     * @param version  参数版本
     * @param type     实例类型
     * @param instance 实例
     */
    private record CachedModel(String version, Class<?> type, Object instance) {
    }
}
