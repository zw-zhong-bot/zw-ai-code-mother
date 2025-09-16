package com.zw.zwaicodemother.service;

import jakarta.servlet.http.HttpServletResponse;

import java.nio.file.Path;

public interface ProjectDownloadService {
    /**
     * 检查路径是否允许包含在压缩包中
     *
     * @param projectRoot 项目根目录
     * @param fullPath    完整路径
     * @return 是否允许
     */
    boolean isPathAllowed(Path projectRoot, Path fullPath);

    /**
     * 下载项目压缩包
     *
     * @param projectPath 项目根目录
     * @param downloadFileName 下载文件名
     * @param response HttpServletResponse
     */
    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
