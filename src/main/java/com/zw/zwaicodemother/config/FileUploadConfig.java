package com.zw.zwaicodemother.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import jakarta.servlet.MultipartConfigElement;

/**
 * 文件上传配置类
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 */
@Configuration
public class FileUploadConfig {

    /**
     * 配置MultipartResolver
     */
    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    /**
     * 配置MultipartConfigElement
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        
        // 设置单个文件最大大小
        factory.setMaxFileSize(DataSize.ofMegabytes(10));
        
        // 设置总请求最大大小
        factory.setMaxRequestSize(DataSize.ofMegabytes(10));
        
        // 设置文件写入磁盘的阈值
        factory.setFileSizeThreshold(DataSize.ofKilobytes(2));
        
        // 设置临时文件位置
        factory.setLocation(System.getProperty("java.io.tmpdir"));
        
        return factory.createMultipartConfig();
    }
}
