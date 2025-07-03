package com.ycbd.demo.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ycbd.demo.config.AppProperties;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;

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

        logger.debug("生成Token，包含字段: {}", payload.keySet());

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
            cn.hutool.jwt.JWT jwt = cn.hutool.jwt.JWT.of(token);
            if (!jwt.setKey(appProperties.getJwt().getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8)).verify()) {
                return null;
            }
            return jwt.getPayloads();
        } catch (Exception e) {
            logger.warn("Token验证失败: {}", e.getMessage());
            return null;
        }
    }
}
