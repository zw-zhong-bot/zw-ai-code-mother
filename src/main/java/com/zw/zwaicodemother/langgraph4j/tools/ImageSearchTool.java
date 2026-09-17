package com.zw.zwaicodemother.langgraph4j.tools;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.zw.zwaicodemother.ai.ModelRegistry;
import com.zw.zwaicodemother.ai.provider.ImageItem;
import com.zw.zwaicodemother.ai.provider.ImageSearchProvider;
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
 * 图片搜索工具（根据关键词搜索图片）。
 * <p>
 * 接口地址与密钥在每次调用时向 ModelRegistry 解析，管理端修改搜索配置后无需重启即可生效。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Slf4j
@Component
public class ImageSearchTool {

    /**
     * 单次搜索返回的图片数量
     */
    private static final int SEARCH_COUNT = 12;

    @Resource
    private ModelRegistry modelRegistry;

    @Tool("搜索内容相关的图片，用于网站内容展示")
    public List<ImageResource> searchContentImages(@P("搜索关键词") String query) {
        List<ImageResource> imageList = new ArrayList<>();
        try {
            ProviderBinding<ImageSearchProvider> binding = modelRegistry.getImageSearch();
            List<ImageItem> imageItems = binding.getProvider()
                    .search(binding.getOptions(), query, SEARCH_COUNT);
            if (CollUtil.isEmpty(imageItems)) {
                // 明确告警，避免配置错误时静默返回空列表
                log.warn("图片搜索未返回结果，配置：{}，关键词：{}",
                        binding.getOptions().getConfigName(), query);
                return imageList;
            }
            for (ImageItem imageItem : imageItems) {
                if (StrUtil.isBlank(imageItem.getUrl())) {
                    continue;
                }
                imageList.add(ImageResource.builder()
                        .category(ImageCategoryEnum.CONTENT)
                        .description(StrUtil.blankToDefault(imageItem.getDescription(), query))
                        .url(imageItem.getUrl())
                        .build());
            }
        } catch (Exception e) {
            log.error("图片搜索失败: {}", e.getMessage(), e);
        }
        return imageList;
    }
}
