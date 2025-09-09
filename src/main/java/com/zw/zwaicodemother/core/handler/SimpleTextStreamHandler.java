package com.zw.zwaicodemother.core.handler;


import com.zw.zwaicodemother.model.entity.User;
import com.zw.zwaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.zw.zwaicodemother.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 简单文本流处理器
 * 处理 HTML 和 MULTI_FILE 类型的流式响应
 */
@Slf4j
public class SimpleTextStreamHandler {
    /**
     * 处理传统流（HTML, MULTI_FILE）
     * 直接收集完整的文本响应
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               Long appId,
                               User loginUser) {
        StringBuilder aiResponseBuilder = new StringBuilder();
        return originFlux.map(chunk->{
            //收集AI响应内容
            aiResponseBuilder.append(chunk);
            return chunk;
        }).doOnComplete(() -> {
            // 流式响应完成后，添加AI消息到对话历史
            String aiResponse = aiResponseBuilder.toString();
            chatHistoryService.addChatMessage(appId, aiResponse,
                    ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
        }).doOnError(error->{
            // 流式响应错误后，添加错误消息到对话历史
            String errorMessage = "AI回复异常:"+error.getMessage();
            chatHistoryService.addChatMessage(appId, errorMessage,
                    ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
        });
    }
}
