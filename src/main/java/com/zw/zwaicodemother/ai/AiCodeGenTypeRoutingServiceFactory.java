package com.zw.zwaicodemother.ai;

import com.zw.zwaicodemother.model.enums.ModelCapabilityEnum;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI 代码生成类型路由服务工厂。
 * <p>
 * 服务实例在模型参数版本变化时自动重建，保证管理端切换模型后无需重启即生效。
 *
 * @author yupi
 */
@Slf4j
@Component
public class AiCodeGenTypeRoutingServiceFactory {

    @Resource
    private ModelRegistry modelRegistry;

    private volatile AiCodeGenTypeRoutingService routingService;

    private volatile String modelVersion;

    /**
     * 获取路由服务实例，模型配置变更后自动重建
     *
     * @return 路由服务
     */
    public AiCodeGenTypeRoutingService getAiCodeGenTypeRoutingService() {
        String version = modelRegistry.currentVersion(ModelCapabilityEnum.CHAT);
        AiCodeGenTypeRoutingService current = routingService;
        if (current != null && version.equals(modelVersion)) {
            return current;
        }
        synchronized (this) {
            if (routingService == null || !version.equals(modelVersion)) {
                routingService = AiServices.builder(AiCodeGenTypeRoutingService.class)
                        .chatModel(modelRegistry.getChatModel())
                        .build();
                modelVersion = version;
                log.info("AI 代码生成类型路由服务已重建，模型版本：{}", version);
            }
            return routingService;
        }
    }
}
