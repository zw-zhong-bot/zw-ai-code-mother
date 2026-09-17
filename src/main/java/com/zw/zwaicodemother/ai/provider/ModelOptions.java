package com.zw.zwaicodemother.ai.provider;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.zw.zwaicodemother.model.enums.ModelProviderEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;

/**
 * 模型参数载体。
 * <p>
 * 由 ModelRegistry 依据数据库配置或 yml 兜底值构建，是 Provider 创建模型实例的唯一入参；
 * 参数变更后 version() 随之变化，缓存据此重建实例。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Getter
@Builder
@AllArgsConstructor
public class ModelOptions {

    /**
     * 配置名称，或 yml-fallback 标识
     */
    private final String configName;

    /**
     * 提供方
     */
    private final ModelProviderEnum provider;

    /**
     * 接口地址
     */
    private final String baseUrl;

    /**
     * 密钥明文（仅存在于内存，不落日志）
     */
    private final String apiKey;

    /**
     * 模型名称
     */
    private final String modelName;

    /**
     * 最大生成 token 数（对话类）
     */
    private final Integer maxTokens;

    /**
     * 温度（对话类）
     */
    private final Double temperature;

    /**
     * 超时时间（对话类）
     */
    private final Duration timeout;

    /**
     * 是否打印请求日志
     */
    private final Boolean logRequests;

    /**
     * 是否打印响应日志
     */
    private final Boolean logResponses;

    /**
     * 扩展参数（来自 params JSON），如图像的 size、n
     */
    private final Map<String, Object> extra;

    /**
     * 读取整型扩展参数
     *
     * @param key          参数名
     * @param defaultValue 默认值
     * @return 参数值
     */
    public Integer extraInt(String key, Integer defaultValue) {
        Object value = extra == null ? null : extra.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(value);
        return StrUtil.isBlank(text) ? defaultValue : Integer.valueOf(text.trim());
    }

    /**
     * 读取字符串扩展参数
     *
     * @param key          参数名
     * @param defaultValue 默认值
     * @return 参数值
     */
    public String extraString(String key, String defaultValue) {
        Object value = extra == null ? null : extra.get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return StrUtil.isBlank(text) ? defaultValue : text;
    }

    /**
     * 计算配置版本，任一有效参数变化都会得到不同结果
     *
     * @return 版本摘要
     */
    public String version() {
        String raw = String.join("|",
                StrUtil.blankToDefault(configName, ""),
                provider == null ? "" : provider.getValue(),
                StrUtil.blankToDefault(baseUrl, ""),
                StrUtil.blankToDefault(apiKey, ""),
                StrUtil.blankToDefault(modelName, ""),
                maxTokens == null ? "" : String.valueOf(maxTokens),
                temperature == null ? "" : String.valueOf(temperature),
                timeout == null ? "" : String.valueOf(timeout.toMillis()),
                Boolean.toString(Boolean.TRUE.equals(logRequests)),
                Boolean.toString(Boolean.TRUE.equals(logResponses)),
                extra == null ? "" : new TreeMap<>(extra).toString());
        return DigestUtil.md5Hex(raw);
    }
}
