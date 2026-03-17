package com.zw.zwaicodemother.langgraph4j.node;

import com.zw.zwaicodemother.ai.enums.CodeGenTypeEnum;
import com.zw.zwaicodemother.core.builder.VueProjectBuilder;
import com.zw.zwaicodemother.exception.BusinessException;
import com.zw.zwaicodemother.exception.ErrorCode;
import com.zw.zwaicodemother.langgraph4j.state.WorkflowContext;
import com.zw.zwaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.apache.bcel.classfile.Code;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/*项目构建节点
* */
@Slf4j
public class ProjectBuilderNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 项目构建");
            //获取必要的参数
            String generatedCodeDir = context.getGeneratedCodeDir();
            CodeGenTypeEnum genTypeEnum = context.getGenerationType();
            String buildResultDir;
            //vue项目：使用VueProjectBuilder构建
            if (genTypeEnum==CodeGenTypeEnum.VUE_PROJECT){
                try{
                    VueProjectBuilder vueProjectBuilder = SpringContextUtil.getBean(VueProjectBuilder.class);
                    //执行Vue项目构建（npm install + npm run build）
                    boolean buildSuccess = vueProjectBuilder.buildProject(generatedCodeDir);
                    if (buildSuccess){
                        //构建成功，返回dist目录
                        buildResultDir = generatedCodeDir+ File.separator+"dist";
                        log.info("Vue 项目构建成功，dist 目录: {}", buildResultDir);
                    }else {
                        throw  new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败");
                    }
                }catch (Exception e){
                    log.error("Vue 项目构建异常: {}", e.getMessage(), e);
                    buildResultDir = generatedCodeDir; // 异常时返回原路径
                }
            }else {
                // HTML 和 MULTI_FILE 代码生成时已经保存了，直接使用生成的代码目录
                buildResultDir = generatedCodeDir;
            }
            
            // 更新状态
            context.setCurrentStep("项目构建");
            context.setBuildResultDir(buildResultDir);
            log.info("项目构建节点完成，最终目录: {}", buildResultDir);
            return WorkflowContext.saveContext(context);
        });
    }
}
