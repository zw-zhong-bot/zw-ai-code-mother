package com.zw.zwaicodemother.langgraph4j.ai;

import com.zw.zwaicodemother.ai.ModelRegistry;
import com.zw.zwaicodemother.model.enums.ModelCapabilityEnum;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 代码质量检查服务工厂。
 * <p>
 * 服务实例在模型参数版本变化时自动重建，保证管理端切换模型后无需重启即生效。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Slf4j
@Component
public class CodeQualityCheckServiceFactory {

    @Resource
    private ModelRegistry modelRegistry;

    private volatile CodeQualityCheckService qualityCheckService;

    private volatile String modelVersion;

    /**
     * 获取代码质量检查 AI 服务，模型配置变更后自动重建
     *
     * @return 质量检查服务
     */
    public CodeQualityCheckService getCodeQualityCheckService() {
        String version = modelRegistry.currentVersion(ModelCapabilityEnum.CHAT);
        CodeQualityCheckService current = qualityCheckService;
        if (current != null && version.equals(modelVersion)) {
            return current;
        }
        synchronized (this) {
            if (qualityCheckService == null || !version.equals(modelVersion)) {
                qualityCheckService = AiServices.builder(CodeQualityCheckService.class)
                        .chatModel(modelRegistry.getChatModel())
                        .build();
                modelVersion = version;
                log.info("代码质量检查服务已重建，模型版本：{}", version);
            }
            return qualityCheckService;
        }
    }
}
