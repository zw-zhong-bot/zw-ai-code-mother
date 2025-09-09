package com.zw.zwaicodemother.ai.tool;

import com.zw.zwaicodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Slf4j
public class FileWriteTool {
    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件的相对路径")String relativeFilePath,
            @P("文件内容")String content,
            @ToolMemoryId Long appId) {
        //具体实现
        try {
            Path  path= Paths.get(relativeFilePath);
            if (!path.isAbsolute()){
                String projectDirName  = "Vue_project_"+appId;
                Path projectRoot=Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR,projectDirName);
                path=projectRoot.resolve(relativeFilePath);
            }
            //创建父目录
            Path  parentDir=path.getParent();
            if (parentDir !=null){
                Files.createDirectories(parentDir);
            }
            //写入文件内容
            Files.write(path,content.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("写入文件到指定路径，路径：{}，内容：{}", relativeFilePath, content);
            // 注意要返回相对路径，不能让 AI 把文件绝对路径返回给用户
            return "文件写入成功"+relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }
}
