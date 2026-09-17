package com.zw.zwaicodemother.ai.provider.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationOutput;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.protocol.Protocol;
import com.zw.zwaicodemother.ai.provider.ImageGenProvider;
import com.zw.zwaicodemother.ai.provider.ImageItem;
import com.zw.zwaicodemother.ai.provider.ModelOptions;
import com.zw.zwaicodemother.model.enums.ModelProviderEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 阿里云百炼（DashScope）图像生成提供方。
 * <p>
 * 按模型名前缀路由到不同接口：qwen-image 系列走同步多模态接口（MultiModalConversation），
 * 其余模型走图片合成接口（ImageSynthesis，SDK 内部提交异步任务后轮询等待）。
 * <p>
 * 配置中的接口地址会被真正使用：SDK 端点已含版本前缀，故只填域名时自动补齐 {@code /api/v1}；
 * 留空则沿用 SDK 默认端点（https://dashscope.aliyuncs.com/api/v1）。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Slf4j
@Component
public class DashScopeImageGenProvider implements ImageGenProvider {

    /**
     * 走多模态同步接口的模型前缀
     */
    private static final String QWEN_IMAGE_PREFIX = "qwen-image";

    /**
     * 默认图片尺寸
     */
    private static final String DEFAULT_SIZE = "1024*1024";

    /**
     * 图片合成接口的任务名（与 SDK 默认值一致）
     */
    private static final String TEXT_TO_IMAGE_TASK = "text2image";

    /**
     * SDK 端点的版本前缀
     */
    private static final String API_VERSION_SUFFIX = "/api/v1";

    @Override
    public ModelProviderEnum provider() {
        return ModelProviderEnum.DASHSCOPE;
    }

    @Override
    public List<ImageItem> generate(ModelOptions options, String prompt) throws Exception {
        if (StrUtil.isBlank(options.getApiKey())) {
            throw new IllegalStateException("模型配置缺少 apiKey：" + options.getConfigName());
        }
        String baseUrl = resolveBaseUrl(options);
        String modelName = options.getModelName();
        if (modelName != null && modelName.startsWith(QWEN_IMAGE_PREFIX)) {
            return generateByMultiModal(options, baseUrl, prompt);
        }
        return generateByImageSynthesis(options, baseUrl, prompt);
    }

    /**
     * 解析配置中的接口地址，仅填域名时补齐版本前缀
     *
     * @param options 模型参数
     * @return 可作为 SDK baseHttpUrl 的地址；未配置时返回 null 表示用 SDK 默认端点
     */
    private String resolveBaseUrl(ModelOptions options) {
        String baseUrl = StrUtil.trim(options.getBaseUrl());
        if (StrUtil.isBlank(baseUrl)) {
            return null;
        }
        baseUrl = StrUtil.removeSuffix(baseUrl, "/");
        if (!baseUrl.contains(API_VERSION_SUFFIX)) {
            baseUrl = baseUrl + API_VERSION_SUFFIX;
        }
        return baseUrl;
    }

    /**
     * 图片合成接口（同步语义：SDK 内部提交异步任务并轮询结果）
     *
     * @param options 模型参数
     * @param baseUrl 接口地址，可为 null
     * @param prompt  提示词
     * @return 图片结果列表
     * @throws Exception 调用异常
     */
    private List<ImageItem> generateByImageSynthesis(ModelOptions options, String baseUrl, String prompt)
            throws Exception {
        ImageSynthesisParam param = ImageSynthesisParam.builder()
                .apiKey(options.getApiKey())
                .model(options.getModelName())
                .prompt(prompt)
                .size(options.extraString("size", DEFAULT_SIZE))
                .n(options.extraInt("n", 1))
                .build();
        ImageSynthesis imageSynthesis = baseUrl == null
                ? new ImageSynthesis()
                : new ImageSynthesis(TEXT_TO_IMAGE_TASK, baseUrl);
        ImageSynthesisResult result = imageSynthesis.call(param);
        if (result == null || result.getOutput() == null) {
            throw new IllegalStateException("图像生成返回为空，model=" + options.getModelName());
        }
        if (StrUtil.isNotBlank(result.getOutput().getCode())) {
            throw new IllegalStateException("图像生成失败：" + result.getOutput().getCode()
                    + " " + result.getOutput().getMessage());
        }
        List<Map<String, String>> results = result.getOutput().getResults();
        List<ImageItem> imageList = new ArrayList<>();
        if (results != null) {
            for (Map<String, String> item : results) {
                String url = item == null ? null : item.get("url");
                if (StrUtil.isNotBlank(url)) {
                    imageList.add(new ImageItem(url, null));
                }
            }
        }
        return imageList;
    }

    /**
     * 多模态同步接口（qwen-image 系列）
     *
     * @param options 模型参数
     * @param baseUrl 接口地址，可为 null
     * @param prompt  提示词
     * @return 图片结果列表
     * @throws Exception 调用异常
     */
    private List<ImageItem> generateByMultiModal(ModelOptions options, String baseUrl, String prompt)
            throws Exception {
        MultiModalMessage message = MultiModalMessage.builder()
                .role("user")
                .content(List.of(Collections.singletonMap("text", prompt)))
                .build();
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(options.getApiKey())
                .model(options.getModelName())
                .message(message)
                .build();
        // 注意：该构造函数仅在 protocol 为 http 时设置 baseHttpUrl（传 https 会写到 websocket 地址上）
        MultiModalConversation conversation = baseUrl == null
                ? new MultiModalConversation()
                : new MultiModalConversation(Protocol.HTTP.getValue(), baseUrl);
        MultiModalConversationResult result = conversation.call(param);
        if (result == null || result.getOutput() == null
                || result.getOutput().getChoices() == null
                || result.getOutput().getChoices().isEmpty()) {
            throw new IllegalStateException("图像生成返回为空，model=" + options.getModelName());
        }
        MultiModalConversationOutput.Choice choice = result.getOutput().getChoices().get(0);
        if (choice.getMessage() == null || choice.getMessage().getContent() == null
                || choice.getMessage().getContent().isEmpty()) {
            throw new IllegalStateException("图像生成未返回内容，model=" + options.getModelName());
        }
        Object image = choice.getMessage().getContent().get(0).get("image");
        if (image == null || StrUtil.isBlank(String.valueOf(image))) {
            throw new IllegalStateException("图像生成未返回图片地址，model=" + options.getModelName());
        }
        return List.of(new ImageItem(String.valueOf(image), null));
    }
}
