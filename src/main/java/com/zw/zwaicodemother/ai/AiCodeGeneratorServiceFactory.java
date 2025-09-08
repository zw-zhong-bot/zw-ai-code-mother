package com.zw.zwaicodemother.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zw.zwaicodemother.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


/**
 * AI 服务创建工厂
 */

@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel openstreamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<Long,AiCodeGeneratorService> serviceCache= Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.info("AI 服务实例被移除，appId: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return serviceCache.get(appId, this::createAiCodeGeneratorService);
    }

    /**
     * 创建新的 AI 服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(Long appId) {
        log.info("创建新的 AI 服务实例，appId: {}", appId);
        //根据appID构建独立的对话记忆
        MessageWindowChatMemory  chatMemory=MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId,chatMemory,20);
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(openstreamingChatModel)
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * 根据 appId 获取服务
     */
//    public AiCodeGeneratorService  getAiCodeGeneratorService(long appId){
//        //根据appID构建独立的对话记忆
//        MessageWindowChatMemory  chatMemory=MessageWindowChatMemory
//                .builder()
//                .id(appId)
//                .chatMemoryStore(redisChatMemoryStore)
//                .maxMessages(20)
//                .build();
//        return AiServices.builder(AiCodeGeneratorService.class)
//                .chatModel(chatModel)
//                .streamingChatModel(openstreamingChatModel)
//                .chatMemory(chatMemory)
//                .build();
//
//
//    }
    /**
     * 默认提供一个 Bean
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0L);
    }
    /*@Bean
    public AiCodeGeneratorService  aiCodeGeneratorService(){
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(openstreamingChatModel)
                //根据id构建独立的对话记忆
                .chatMemoryProvider(memoryID  -> MessageWindowChatMemory
                        .builder()
                        .id(memoryID)
                        .chatMemoryStore(redisChatMemoryStore)
                        .maxMessages(20)
                        .build()
                )
                .build();
    }*/




}