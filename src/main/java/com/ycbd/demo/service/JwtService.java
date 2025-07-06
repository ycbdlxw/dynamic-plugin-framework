package com.ycbd.demo.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ycbd.demo.config.AppProperties;

import cn.hutool.jwt.JWT;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private TokenFieldConfigService tokenFieldConfigService;

    /**
     * 生成JWT令牌
     *
     * @param userData 用户数据，包含所有可能需要的字段
     * @return JWT令牌字符串
     */
    public String generateToken(Map<String, Object> userData) {
        // 使用不可变的java.util.Date，避免可变DateTime导致iat被错误偏移
        java.util.Date now = new java.util.Date();
        long expireMillis = now.getTime() + appProperties.getJwt().getExpirationMinutes() * 60L * 1000L;
        java.util.Date expireTime = new java.util.Date(expireMillis);

        Map<String, Boolean> enabledFieldMap = tokenFieldConfigService.getEnabledTokenFields();
        logger.info("启用的Token字段配置: {}", enabledFieldMap);

        Map<String, Object> payload = new HashMap<>();
        for (String tokenField : enabledFieldMap.keySet()) {
            // tokenField 可能与userData字段名不同（如userId=>id），先直接取
            if (userData.containsKey(tokenField)) {
                payload.put(tokenField, userData.get(tokenField));
                continue;
            }
            // 特殊映射：userId 对应 id
            if ("userId".equals(tokenField) && userData.containsKey("id")) {
                payload.put("userId", userData.get("id"));
            }
        }

        // 兜底: 若缺少必需字段，仍尝试填充
        if (!payload.containsKey("userId") && userData.containsKey("id")) {
            payload.put("userId", userData.get("id"));
        }
        if (!payload.containsKey("username") && userData.containsKey("username")) {
            payload.put("username", userData.get("username"));
        }

        logger.info("生成Token，原始用户数据: {}", userData);
        logger.info("生成Token，包含字段: {}", payload);

        return JWT.create()
                .addPayloads(payload)
                .setIssuedAt(now)
                .setExpiresAt(expireTime)
                .setKey(appProperties.getJwt().getSecret().getBytes())
                .sign();
    }

    /**
     * 验证并解码JWT令牌
     *
     * @param token JWT令牌字符串
     * @return 解码后的JWT对象，验证失败时返回null
     */
    public Map<String, Object> verifyAndDecode(String token) {
        try {
            logger.info("开始验证Token: {}", token.substring(0, Math.min(20, token.length())) + "...");
            cn.hutool.jwt.JWT jwt = cn.hutool.jwt.JWT.of(token);
            boolean verified = jwt.setKey(appProperties.getJwt().getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8)).verify();
            logger.info("Token验证结果: {}", verified);

            if (!verified) {
                logger.warn("Token签名验证失败");
                return null;
            }

            Map<String, Object> payload = jwt.getPayloads();
            logger.info("Token解析成功，包含字段: {}", payload.keySet());
            return payload;
        } catch (Exception e) {
            logger.warn("Token验证失败: {}", e.getMessage());
            return null;
        }
    }
}
