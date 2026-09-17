package com.zw.zwaicodemother.langgraph4j.ai;

import com.zw.zwaicodemother.ai.ModelRegistry;
import com.zw.zwaicodemother.langgraph4j.tools.ImageSearchTool;
import com.zw.zwaicodemother.langgraph4j.tools.LogoGeneratorTool;
import com.zw.zwaicodemother.langgraph4j.tools.MermaidDiagramTool;
import com.zw.zwaicodemother.langgraph4j.tools.UndrawIllustrationTool;
import com.zw.zwaicodemother.model.enums.ModelCapabilityEnum;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 图片收集 AI 服务工厂。
 * <p>
 * 服务实例在模型参数版本变化时自动重建；工具内部的接口地址与密钥在每次调用时解析，
 * 因此图像模型或搜索配置变更后同样无需重启。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Slf4j
@Component
public class ImageCollectionServiceFactory {

    @Resource
    private ModelRegistry modelRegistry;

    @Resource
    private ImageSearchTool imageSearchTool;

    @Resource
    private UndrawIllustrationTool undrawIllustrationTool;

    @Resource
    private MermaidDiagramTool mermaidDiagramTool;

    @Resource
    private LogoGeneratorTool logoGeneratorTool;

    private volatile ImageCollectionService imageCollectionService;

    private volatile String modelVersion;

    /**
     * 获取图片收集 AI 服务，模型配置变更后自动重建
     *
     * @return 图片收集服务
     */
    public ImageCollectionService getImageCollectionService() {
        String version = modelRegistry.currentVersion(ModelCapabilityEnum.CHAT);
        ImageCollectionService current = imageCollectionService;
        if (current != null && version.equals(modelVersion)) {
            return current;
        }
        synchronized (this) {
            if (imageCollectionService == null || !version.equals(modelVersion)) {
                imageCollectionService = AiServices.builder(ImageCollectionService.class)
                        .chatModel(modelRegistry.getChatModel())
                        .tools(
                                imageSearchTool,
                                undrawIllustrationTool,
                                mermaidDiagramTool,
                                logoGeneratorTool
                        )
                        .build();
                modelVersion = version;
                log.info("图片收集服务已重建，模型版本：{}", version);
            }
            return imageCollectionService;
        }
    }
}
