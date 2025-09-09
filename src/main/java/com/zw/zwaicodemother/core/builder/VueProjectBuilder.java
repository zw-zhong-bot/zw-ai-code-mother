package com.zw.zwaicodemother.core.builder;

import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VueProjectBuilder {
    /**
     * 异步构建项目（不阻塞主流程）
     *
     * @param projectPath 项目路径
     */
    public void buildProjectAsync(String projectPath) {
        Thread.ofVirtual().name("vue-project-builder"+System.currentTimeMillis()).start(() -> {
            try{
                buildProject(projectPath);
            }catch (Exception e){
                log.error("异步构建Vue项目，项目路径：{}，异常信息：{}", projectPath, e.getMessage());
            }
        });
    }
    /*
    * 构建Vue项目
    * @param projectPath 项目路径
    * @return 构建结果
    * */
    public  boolean buildProject(String projectPath){
        File projectDir = new File(projectPath);
        if (!projectDir.exists()||!projectDir.isDirectory()){
            log.error("构建Vue项目，项目路径：{}，异常信息：{}", projectPath, "项目路径不存在");
            return false;
        }
        //检查是否有package.json文件
        File packageJsonFile = new File(projectDir, "package.json");
        if (!packageJsonFile.exists()){
            log.error("项目目录中没有 package.json 文件：{}", packageJsonFile);
            return false;
        }
        log.info("开始构建Vue项目：{}",projectPath);
        //执行npm install命令
        if (!executeNpmInstall(projectDir)){
            log.error("npm install 命令执行失败:{}", projectPath);
            return false;
        }
        return false;
    }
    /**
     * 执行 npm install 命令
     */
    private boolean executeNpmInstall(File projectDir) {
        log.info("执行 npm install...");
        String command = String.format("%s install", buildCommand("npm"));
        return executeCommand(projectDir, command, 300); // 5分钟超时
    }
    /**
     * 执行 npm run build 命令
     */
    private boolean executeNpmBuild(File projectDir) {
        log.info("执行 npm run build...");
        String command = String.format("%s run build", buildCommand("npm"));
        return executeCommand(projectDir, command, 180); // 3分钟超时
    }
    /**
     * 执行命令
     *
     * @param workingDir     工作目录
     * @param command        命令字符串
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否执行成功
     */
    private boolean executeCommand(File workingDir,String command,int  timeoutSeconds){
        try {
            log.info("在目录{}执行命令：{}",workingDir.getAbsolutePath(),command);
            Process process= RuntimeUtil.exec(
                    null,workingDir,command.split("\\s+")//命令分割为数组
            );
            //等待进程完成时，设置超时
            boolean finished=process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished){
                log.error("命令执行超时：{},强制终止进程", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功：{}", command);
                return true;
            }else {
                log.error("命令执行失败：{},退出码：{}", command, exitCode);
                return false;
            }
        } catch (InterruptedException e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return false;
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }
}
