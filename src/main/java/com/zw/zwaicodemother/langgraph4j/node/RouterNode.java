package com.zw.zwaicodemother.langgraph4j.node;

import com.zw.zwaicodemother.ai.AiCodeGenTypeRoutingService;
import com.zw.zwaicodemother.ai.AiCodeGenTypeRoutingServiceFactory;
import com.zw.zwaicodemother.ai.enums.CodeGenTypeEnum;
import com.zw.zwaicodemother.langgraph4j.state.WorkflowContext;
import com.zw.zwaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/*
* 智能路由节点
* */

@Slf4j
public class RouterNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 智能路由");

            CodeGenTypeEnum generationType;
            try{
                //获取Ai路由服务（工厂内部按模型版本自动重建，支持模型热切换）
                AiCodeGenTypeRoutingService routingService = SpringContextUtil
                        .getBean(AiCodeGenTypeRoutingServiceFactory.class)
                        .getAiCodeGenTypeRoutingService();
                //根据原始提示词进行智能路由
                generationType = routingService.routeCodeGenType(context.getOriginalPrompt());
                log.info("AI智能路由完成，选择类型: {} ({})", generationType.getValue(), generationType.getText());
            }catch (Exception e){
                log.error("AI智能路由失败，使用默认HTML类型: {}", e.getMessage());
                generationType = CodeGenTypeEnum.HTML;
            }
            // 更新状态
            context.setCurrentStep("智能路由");
            context.setGenerationType(generationType);
            return WorkflowContext.saveContext(context);
        });
    }
}
