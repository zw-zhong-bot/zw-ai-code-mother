package com.zw.zwaicodemother.core.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zw.zwaicodemother.ai.model.message.*;
import com.zw.zwaicodemother.model.entity.User;
import com.zw.zwaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.zw.zwaicodemother.service.ChatHistoryService;
import io.swagger.v3.core.util.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

/**
 * JSON 消息流处理器
 * 处理 VUE_PROJECT 类型的复杂流式响应，包含工具调用信息
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {
    /**
     * 处理 TokenStream（VUE_PROJECT）
     * 解析 JSON 消息并重组为完整的响应格式
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        //收集数据用于生成后端记忆格式
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        //用于跟踪已经见过的工具ID，判断是否时第一次调用
        Set<String> seenToolIds = new HashSet<>();
        return originFlux.map(chunk->{
            //解析每个JSON消息块
            String result = handleJsonMessageChunk(chunk,chatHistoryStringBuilder,seenToolIds);
            return result == null ? "" : result; // 避免返回null
        }).filter(StrUtil::isNotBlank)//过滤空字符
                .doOnComplete(()->{
                    //流式响应完成后，添加 AI 消息到对话历史
                    String aiRespomse=chatHistoryStringBuilder.toString();
                    chatHistoryService.addChatMessage(appId,aiRespomse, ChatHistoryMessageTypeEnum.AI.getValue(),loginUser.getId());
                }).doOnError(error->{
                    String errorMeassage="AI回复失败: "+error.getMessage();
                    chatHistoryService.addChatMessage(appId,errorMeassage, ChatHistoryMessageTypeEnum.AI.getValue(),loginUser.getId());
                });
    }
    /**
     * 解析并收集 TokenStream 数据
     */
    private String handleJsonMessageChunk(String chunk,
                                          StringBuilder chatHistoryStringBuilder,Set<String> seenToolIds){
        // 解析 JSON
        StreamMessage streamMessage= JSONUtil.toBean(chunk,StreamMessage.class);
        StreamMessageTypeEnum typeEnum =StreamMessageTypeEnum
                .getEnumByValue(streamMessage.getType());
        switch (typeEnum){
            case AI_RESPONSE->{
                AiResponseMessage aiResponseMessage = JSONUtil.toBean(chunk,AiResponseMessage.class);
                String data = aiResponseMessage.getData();
                //直接拼接响应
                chatHistoryStringBuilder.append(data);
                return data;
            }

            case TOOL_REQUEST-> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                //判断是否是第一次调用
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    //第一次调用这个工具，记录ID并完整返回工具信息
                    seenToolIds.add(toolId);
                    return "\n\n[选择工具] 写入文件\n\n";
                } else {
                    //不是第一次调用，只返回工具ID
                    return null;
                }
            }
            case TOOL_EXECUTED-> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk,ToolExecutedMessage.class);
                String arguments = toolExecutedMessage.getArguments();
                if (StrUtil.isBlank(arguments) || !JSONUtil.isTypeJSONObject(arguments)) {
                    log.warn("工具执行参数格式异常: {}", arguments);
                    return null;
                }
                JSONObject jsonObject = JSONUtil.parseObj(arguments);
                String relativeFilePath = jsonObject.getStr("relativeFilePath");
                String content = jsonObject.getStr("content");
                if (StrUtil.isBlank(relativeFilePath) || StrUtil.isBlank(content)) {
                    log.warn("工具执行参数缺失必要字段: {}", arguments);
                    return null;
                }
                String suffix = FileUtil.getSuffix(relativeFilePath);
                String result = String.format("""
                         [工具调用] 写入文件 %s
                        ```%s
                        %s
                        ```
                        """, relativeFilePath, suffix, content);
                //输出前端和要持久化的内容
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                return output;
            }
            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return " ";
            }
        }
    }
}