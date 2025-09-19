package com.zw.zwaicodemother.ai.tool;


import cn.hutool.core.io.FileUtil;
import com.zw.zwaicodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

/**
 * 文件目录读取工具
 * 使用 Hutool 简化文件操作
 */
@Slf4j
public class FileDirReadTool {
    /**
     * 需要忽略的文件和目录
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules", ".git", "dist", "build", ".DS_Store",
            ".env", "target", ".mvn", ".idea", ".vscode", "coverage"
    );
    /**
     * 需要忽略的文件扩展名
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log", ".tmp", ".cache", ".lock"
    );

    @Tool("读取目录结构，获取指定目录下的所有文件和子目录信息")
    public String readDir(
            @P("目录的相对路径，为空则读取整个项目结构") String relativeDirPath,
            @ToolMemoryId Long appId){
        try {
            Path path = Paths.get(relativeDirPath==null?"":relativeDirPath);
            if (!path.isAbsolute()){
                String projectDirName="vue_project_"+appId;
                Path projectRoot  = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path=projectRoot.resolve(relativeDirPath==null?"":relativeDirPath);
            }
            File targetDir=path.toFile();
            if (!targetDir.exists()||!targetDir.isDirectory()){
                log.error("读取目录结构，目录不存在或不是目录：{}",targetDir);
                return "错误：目录不存在或不是目录 - " + relativeDirPath;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("项目目录结构：\n");
            //使用Hutool递归获取所有文件
            List<File> allFiles = FileUtil.loopFiles(targetDir,file->!shouldIgnore(file.getName()));
            // 按路径深度和名称排序显示
            allFiles.stream()
                    .sorted((f1, f2) -> {
                        int depth1 = getRelativeDepth(targetDir,f1);
                        int depth2 = getRelativeDepth(targetDir,f2);
                        if (depth1  != depth2){
                            return Integer.compare(depth1,depth2);
                        }
                        return f1.getPath().compareTo(f2.getPath());
                    })
                    .forEach(file -> {
                        int depth = getRelativeDepth(targetDir,file);
                        String indent = "  ".repeat(depth);
                        stringBuilder.append(indent).append(file.getName());
                    });
            return stringBuilder.toString();
        }catch (Exception e){
            String errorMessage = "读取目录结构失败: " + relativeDirPath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }

    }

    /**
     * 计算文件相对于根目录的深度
     */
    private  int  getRelativeDepth(File root,File file){
        Path rootPath = root.toPath();
        Path filePath  = file.toPath();
        return rootPath.relativize(filePath).getNameCount() - 1;
    }

    /**
     * 判断是否应该忽略该文件或目录
     */
    private boolean shouldIgnore(String fileName) {
        //检查是否在忽略名称列表中
        if(IGNORED_NAMES.contains(fileName)){
            return true;
        }
        //检查文件扩展名
        return IGNORED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
}
