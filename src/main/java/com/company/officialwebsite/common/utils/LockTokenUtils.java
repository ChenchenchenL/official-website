package com.company.officialwebsite.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * LockTokenUtils：独占编辑锁 Token 生成、哈希摘要与脱敏工具类。
 */
public final class LockTokenUtils {

    private LockTokenUtils() {
    }

    /**
     * 生成安全随机的明文 Lock Token (64字符)。
     */
    public static String generateToken() {
        return (UUID.randomUUID().toString() + UUID.randomUUID().toString()).replace("-", "");
    }

    /**
     * 计算明文 Token 的 SHA-256 哈希值（用于物理数据库存储与检索，防止明文泄露）。
     */
    public static String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(rawToken.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 对 Lock Token 进行脱敏遮罩（日志与审计专用）。
     */
    public static String maskToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return "<EMPTY>";
        }
        String trimmed = rawToken.trim();
        if (trimmed.length() <= 6) {
            return "***";
        }
        return trimmed.substring(0, 6) + "***";
    }
}
