package com.zw.zwaicodemother.core;

import com.zw.zwaicodemother.ai.AiCodeGeneratorService;
import com.zw.zwaicodemother.ai.enums.CodeGenTypeEnum;
import com.zw.zwaicodemother.ai.model.HtmlCodeResult;
import com.zw.zwaicodemother.ai.model.MultiFileCodeResult;
import com.zw.zwaicodemother.core.parser.CodeParserExecutor;
import com.zw.zwaicodemother.core.saver.CodeFileSaverExecutor;
import com.zw.zwaicodemother.exception.BusinessException;
import com.zw.zwaicodemother.exception.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {
    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    /**
     * 通用流式代码处理方法
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @return 流式响应
     */
    private Flux<String> processCodeStream (Flux<String> codeStream , CodeGenTypeEnum codeGenType, Long appId) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(chunk -> {
            //实时收集代码片段
            codeBuilder.append(chunk);
        }).doOnComplete(()->{
            //流式返回完成后保存代码
            try{
                String completeCode = codeBuilder.toString();
                //使用执行器解析代码
                Object paresdResult = CodeParserExecutor.executeParser(completeCode,codeGenType);
                //使用执行器保存代码
                File savedDir = CodeFileSaverExecutor.executeSaver(paresdResult,codeGenType,appId);
                log.info("保存成功，路径为："+savedDir.getAbsolutePath());
            }catch (Exception e){
                log.error("保存失败：{}" ,e.getMessage());
            }
        });
    }

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum ,Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }


    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage,
                                                  CodeGenTypeEnum codeGenTypeEnum,Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, codeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield  processCodeStream(codeStream, codeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 生成 HTML 模式的代码并保存（流式）
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
        Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
        // 字符串拼接器，用于当流式返回所有的代码之后，再保存代码
        StringBuilder codeBuilder = new StringBuilder();
        return result.doOnNext(chunk -> {
            // 实时收集代码片段
            codeBuilder.append(chunk);
        }).doOnComplete(() -> {
            // 流式返回完成后，保存代码
            try {
                String completeHtmlCode = codeBuilder.toString();
                // 解析代码为对象
                HtmlCodeResult htmlCodeResult = CodeParser.parseHtmlCode(completeHtmlCode);
                // 保存代码到文件
                File saveDir = CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
                log.info("保存成功，目录为：{}", saveDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存失败: {}", e.getMessage());
            }
        });
    }

    /**
     * 生成多文件模式的代码并保存（流式）
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage) {
        Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
        // 字符串拼接器，用于当流式返回所有的代码之后，再保存代码
        StringBuilder codeBuilder = new StringBuilder();
        return result.doOnNext(chunk -> {
            // 实时收集代码片段
            codeBuilder.append(chunk);
        }).doOnComplete(() -> {
            try {
                // 流式返回完成后，保存代码
                String completeMultiFileCode = codeBuilder.toString();
                // 解析代码为对象
                MultiFileCodeResult multiFileCodeResult = CodeParser.parseMultiFileCode(completeMultiFileCode);
                // 保存代码到文件
                File saveDir = CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult);
                log.info("保存成功，目录为：{}", saveDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存失败: {}", e.getMessage());
            }
        });
    }


    /**
     * 生成 HTML 模式的代码并保存
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private File generateAndSaveHtmlCode(String userMessage) {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
        return CodeFileSaver.saveHtmlCodeResult(result);
    }

    /**
     * 生成多文件模式的代码并保存
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private File generateAndSaveMultiFileCode(String userMessage) {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
        return CodeFileSaver.saveMultiFileCodeResult(result);
    }

}
