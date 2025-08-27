package com.zw.zwaicodemother.model.dto.user;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

/**
 * 用户头像上传请求
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 */
@Data
public class UserAvatarUploadRequest implements Serializable {
    
    /**
     * 头像文件
     */
    private MultipartFile file;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    private static final long serialVersionUID = 1L;
}
