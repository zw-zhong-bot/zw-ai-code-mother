package com.zw.zwaicodemother.ai.tool;

import com.github.xiaoymin.knife4j.annotations.Ignore;
import com.mybatisflex.core.paginate.Page;
import com.zw.zwaicodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件删除工具
 * 支持 AI 通过工具调用的方式删除文件
 */
@Slf4j
public class FileDeleteTool {

    @Tool("删除指定路径的文件")
    public String delete(
            @P("文件的相对路径") String relativeFilePath,
            @ToolMemoryId Long appId) {
       try {
           Path path= Paths.get(
                   relativeFilePath
           );
           if (!path.isAbsolute()){
               String projectDirName  = "vue_project_"+appId;
               Path projectRoot=Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR,projectDirName);
               path=projectRoot.resolve(relativeFilePath);
           }
           if(!Files.exists(path)){
               return "警告：文件不存在，无需删除---"+relativeFilePath;
           }
           if (!Files.isRegularFile(path)) {
               return "警告：指定路径路径不是文件，无需删除---"+relativeFilePath;
           }
           //安全检查:避免重复删除重要文件
           String fileName=path.getFileName().toString();
           if (isImportantFile(fileName)){
               return "警告：删除重要文件被拒绝---"+fileName;
           }
           //删除文件
           Files.delete(path);
           log.info("文件删除成功：{}",path.toAbsolutePath());
           return "文件删除成功:"+relativeFilePath;
       }catch (IOException e){
           String errorMessage="文件删除失败:"+relativeFilePath+",异常信息:"+e.getMessage();
           log.error(errorMessage,e);
           return errorMessage;
       }
    }
    /**
     * 检查文件是否为重要文件
     * @param fileName 文件名
     * @return 是否为重要文件
     */
    private boolean isImportantFile(String fileName){
        String[] importantFiles={
                "package.json", "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
                "vite.config.js", "vite.config.ts", "vue.config.js",
                "tsconfig.json", "tsconfig.app.json", "tsconfig.node.json",
                "index.html", "main.js", "main.ts", "App.vue", ".gitignore", "README.md"
        };
        for (String importan:importantFiles){
            if (importan.equalsIgnoreCase(fileName)){
                return true;
            }
        }
        return false;
    }
}
