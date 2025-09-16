package com.zw.zwaicodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.zw.zwaicodemother.exception.BusinessException;
import com.zw.zwaicodemother.exception.ErrorCode;
import com.zw.zwaicodemother.exception.ThrowUtils;
import com.zw.zwaicodemother.service.ProjectDownloadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

@Service
@Slf4j
public class ProjectDownloadServiceImpl implements ProjectDownloadService {
    /**
     * 需要过滤的文件和目录名称
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules",
            ".git",
            "dist",
            "build",
            ".DS_Store",
            ".env",
            "target",
            ".mvn",
            ".idea",
            ".vscode"
    );
    /**
     * 需要过滤的文件扩展名
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log",
            ".tmp",
            ".cache"
    );

    /**
     * 检查路径是否允许包含在压缩包中
     *
     * @param projectRoot 项目根目录
     * @param fullPath    完整路径
     * @return 是否允许
     */
    @Override
    public boolean isPathAllowed(Path projectRoot, Path fullPath) {
        //获取相对路径
        Path relativePath = projectRoot.relativize(fullPath);
        //检查路径中的每一部分
        for (Path part :relativePath) {
            String partName = part.toString();
            //检查文件名是否在忽略列表中
            if (IGNORED_NAMES.contains(partName)) {
                return false;
            }
            //检查文件扩展名是否在忽略列表中
            if (IGNORED_EXTENSIONS.stream().anyMatch(partName::endsWith)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response) {
        //基础校验
        ThrowUtils.throwIf(StrUtil.isBlank(projectPath), ErrorCode.PARAMS_ERROR,"项目路径不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(downloadFileName), ErrorCode.PARAMS_ERROR,"下载文件名不能为空");
        File projectDir = new File(projectPath);
        ThrowUtils.throwIf(!projectDir.exists(), ErrorCode.PARAMS_ERROR,"项目路径不存在");
        ThrowUtils.throwIf(!projectDir.isDirectory(), ErrorCode.PARAMS_ERROR,"项目路径不是目录");
        log.info("开始下载项目压缩包，项目路径: {}, 下载文件名: {}.zip", projectPath, downloadFileName);
        //设置HTTP响应头
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",
                String.format("attachment; filename=\"%s.zip\"", downloadFileName));
        //定义文件过滤器
        FileFilter fileFilter = file  -> isPathAllowed(projectDir.toPath(), file.toPath());
        try {
            //使用Hutool的ZipUtil 直接将过滤后的目录压缩到响应输出流
            ZipUtil.zip(response.getOutputStream(), StandardCharsets.UTF_8,false,
                    fileFilter,projectDir);
            log.info("项目打包下载完成: {}", downloadFileName);
        } catch (Exception e) {
            log.error("下载项目压缩包失败，项目路径: {}, 下载文件名: {}.zip", projectPath, downloadFileName, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"下载项目压缩包失败");
        }

        }
}

/*
下载压缩包核心方法
*
* */


