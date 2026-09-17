package com.zw.zwaicodemother.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.symmetric.AES;
import com.zw.zwaicodemother.exception.BusinessException;
import com.zw.zwaicodemother.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * 模型密钥加解密工具。
 * <p>
 * 主密钥取自环境变量 MODEL_CONFIG_AES_KEY（缺失时使用内置开发密钥并告警）。
 * 密文带 enc: 前缀，用于与历史明文数据兼容区分。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Slf4j
public final class AesUtils {

    /**
     * 主密钥环境变量名
     */
    private static final String AES_KEY_ENV = "MODEL_CONFIG_AES_KEY";

    /**
     * 开发环境兜底主密钥（仅用于本地开发，生产必须通过环境变量覆盖）
     */
    private static final String DEV_DEFAULT_KEY = "zw-ai-code-mother-dev-aes-key";

    /**
     * 密文前缀
     */
    private static final String CIPHER_PREFIX = "enc:";

    private static final AES AES_INSTANCE;

    static {
        String keyText = System.getenv(AES_KEY_ENV);
        boolean usingDefault = StrUtil.isBlank(keyText);
        if (usingDefault) {
            keyText = DEV_DEFAULT_KEY;
            log.warn("未设置环境变量 {}，模型密钥将使用内置开发密钥加密，生产环境请务必配置", AES_KEY_ENV);
        }
        byte[] digest = DigestUtil.sha256(keyText);
        byte[] keyBytes = Arrays.copyOf(digest, 32);
        byte[] ivBytes = Arrays.copyOf(digest, 16);
        AES_INSTANCE = new AES(Mode.CBC, Padding.PKCS5Padding, keyBytes, ivBytes);
    }

    private AesUtils() {
    }

    /**
     * 加密密钥
     *
     * @param plainText 明文
     * @return 带前缀的密文，明文为空时返回 null
     */
    public static String encrypt(String plainText) {
        if (StrUtil.isBlank(plainText)) {
            return null;
        }
        return CIPHER_PREFIX + AES_INSTANCE.encryptBase64(plainText);
    }

    /**
     * 解密密钥，兼容历史明文数据
     *
     * @param cipherText 密文或明文
     * @return 明文，入参为空时返回 null
     */
    public static String decrypt(String cipherText) {
        if (StrUtil.isBlank(cipherText)) {
            return null;
        }
        if (!cipherText.startsWith(CIPHER_PREFIX)) {
            // 兼容未加密的历史数据
            return cipherText;
        }
        try {
            return AES_INSTANCE.decryptStr(cipherText.substring(CIPHER_PREFIX.length()));
        } catch (Exception e) {
            log.error("模型密钥解密失败，请检查 {} 是否与加密时一致", AES_KEY_ENV, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模型密钥解密失败，请检查主密钥配置");
        }
    }

    /**
     * 生成密钥掩码，如 sk-****3f7a
     *
     * @param apiKey 密钥明文
     * @return 掩码，未配置时返回空串
     */
    public static String mask(String apiKey) {
        if (StrUtil.isBlank(apiKey)) {
            return "";
        }
        int length = apiKey.length();
        if (length <= 8) {
            return "****";
        }
        return apiKey.substring(0, 3) + "****" + apiKey.substring(length - 4);
    }
}
