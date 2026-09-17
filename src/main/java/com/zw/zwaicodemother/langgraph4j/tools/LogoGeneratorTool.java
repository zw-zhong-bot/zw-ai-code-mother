package com.zw.zwaicodemother.langgraph4j.tools;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.zw.zwaicodemother.ai.ModelRegistry;
import com.zw.zwaicodemother.ai.provider.ImageGenProvider;
import com.zw.zwaicodemother.ai.provider.ImageItem;
import com.zw.zwaicodemother.ai.provider.ProviderBinding;
import com.zw.zwaicodemother.langgraph4j.model.ImageResource;
import com.zw.zwaicodemother.langgraph4j.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Logo 图片生成工具。
 * <p>
 * 模型参数在每次调用时向 ModelRegistry 解析，管理端切换图像模型后无需重启即可生效。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Slf4j
@Component
public class LogoGeneratorTool {

    @Resource
    private ModelRegistry modelRegistry;

    @Tool("根据描述生成 Logo 图片")
    public List<ImageResource> generateLogos(@P("Logo 设计描述，如名称、行业、风格等，尽量详细")
                                              String description) {
        List<ImageResource> logoList = new ArrayList<>();
        try {
            //构建Logo设计提示词
            String logoPrompt = String.format("生成 Logo，Logo 中禁止包含任何文字！Logo 介绍：%s", description);
            ProviderBinding<ImageGenProvider> binding = modelRegistry.getImageGen();
            List<ImageItem> imageItems = binding.getProvider().generate(binding.getOptions(), logoPrompt);
            if (CollUtil.isEmpty(imageItems)) {
                // 明确告警，避免配置错误时静默返回空列表
                log.warn("Logo 生成未返回图片，配置：{}，模型：{}",
                        binding.getOptions().getConfigName(), binding.getOptions().getModelName());
                return logoList;
            }
            for (ImageItem imageItem : imageItems) {
                if (StrUtil.isNotBlank(imageItem.getUrl())) {
                    logoList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.LOGO)
                            .description(description)
                            .url(imageItem.getUrl())
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("生成 Logo 失败: {}", e.getMessage(), e);
        }
        return logoList;
    }
}
