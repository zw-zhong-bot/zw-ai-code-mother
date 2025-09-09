package com.zw.zwaicodemother.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.autoconfigure.batch.BatchDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


/*
* 推理流式模型配置
* 推理流式模型（用于Vue项目生成，带工具调用）
* */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class ReasoningStreamingChatModelConfig {
    private String baseUrl;
    private String apiKey;
    //推理流式模型（用于Vue项目生成，带工具调用）
    @Bean
    public StreamingChatModel reasoningStreamingChatModel() {
        //为了测试是方便临时修改
        final String  modelName = "deepseek-chat";
        final int maxTokens = 8192;
        //生产环境使用：
//        final String  modelName = "deepseek-reasoner";
//        final int maxTokens = 32768;

        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
