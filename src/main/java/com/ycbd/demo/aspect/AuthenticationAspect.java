package com.ycbd.demo.aspect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ycbd.demo.security.UserContext;
import com.ycbd.demo.service.BaseService;
import com.ycbd.demo.service.JwtService;
import com.ycbd.demo.service.OperationLogService;
import com.ycbd.demo.service.TokenFieldConfigService;
import com.ycbd.demo.utils.ApiResponse;
import com.ycbd.demo.utils.ResultCode;

import cn.hutool.core.map.MapUtil;
import cn.hutool.jwt.JWT;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Aspect
@Component
public class AuthenticationAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationAspect.class);

    @Autowired
    private JwtService jwtService;
    @Autowired
    private BaseService baseService;
    @Autowired
    private TokenFieldConfigService tokenFieldConfigService;
    @Autowired
    private OperationLogService operationLogService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 用于存储紧急情况下的默认白名单
    private final String[] defaultWhiteListPatterns = {
        "/api/core/register",
        "/api/core/login",
        "/h2-console/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/api-docs/**"
    };

    @PostConstruct
    public void init() {
        // 初始化时确保默认白名单存在于数据库中
        ensureDefaultWhitelistExists();
    }

    /**
     * 确保默认白名单存在于数据库中
     */
    private void ensureDefaultWhitelistExists() {
        try {
            for (String pattern : defaultWhiteListPatterns) {
                Map<String, Object> params = new HashMap<>();
                params.put("type", "WHITELIST");
                params.put("pattern", pattern);

                // 检查是否已存在
                List<Map<String, Object>> existing = baseService.queryList(
                        "security_config", 0, 1, null, params, null, null, null);

                // 如果不存在，则添加
                if (existing == null || existing.isEmpty()) {
                    params.put("is_active", true);
                    baseService.save("security_config", params);
                    logger.info("添加默认白名单路径: {}", pattern);
                }
            }
        } catch (Exception e) {
            logger.error("初始化默认白名单失败", e);
        }
    }

    @Pointcut("within(com.ycbd.demo.controller..*)")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();

        long startTime = System.currentTimeMillis();
        Object result = null;
        Map<String, Object> currentUser = null;
        try {
            String requestURI = request.getRequestURI();
            boolean whiteListed = isWhiteListed(requestURI);
            if (!whiteListed) {
                String token = request.getHeader("Authorization");
                if (token == null || !token.startsWith("Bearer ")) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.of(ResultCode.UNAUTHORIZED, "Authorization header is missing or invalid")));
                    return null;
                }

                token = token.substring(7);
                JWT jwt = jwtService.verifyAndDecode(token);

                if (jwt == null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.of(ResultCode.UNAUTHORIZED, "Invalid or expired token")));
                    return null;
                }

                currentUser = jwt.getPayload().getClaimsJson();

                if (!currentUser.containsKey("userId") || !currentUser.containsKey("username")) {
                    logger.warn("Token缺少必要的用户信息字段");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.of(ResultCode.UNAUTHORIZED, "Token missing required fields")));
                    return null;
                }

                Map<String, Boolean> enabledFields = tokenFieldConfigService.getEnabledTokenFields();
                currentUser.keySet().removeIf(key -> !enabledFields.getOrDefault(key, false));

                UserContext.setUser(currentUser);

                if (logger.isDebugEnabled()) {
                    logger.debug("Token验证成功，用户ID: {}, 用户名: {}", currentUser.get("userId"), currentUser.get("username"));
                }
            }

            // 执行目标方法
            result = joinPoint.proceed();
            return result;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            try {
                operationLogService.log(request, currentUser != null ? currentUser : UserContext.getUser(), response, duration, result);
            } catch (Exception e) {
                logger.error("保存操作日志失败", e);
            }
            UserContext.clear();
        }
    }

    @Cacheable(value = "security_whitelist", key = "#uri")
    public boolean isWhiteListed(String uri) {
        logger.info("检查URI '{}' 是否在白名单中 (DB Query)", uri);

        try {
            // 从数据库获取白名单配置
            Map<String, Object> params = new HashMap<>();
            params.put("type", "WHITELIST");
            params.put("is_active", true);

            List<Map<String, Object>> whitelistConfigs = baseService.queryList(
                    "security_config", 0, 100, null, params, null, null, null);

            // 检查URI是否匹配任何白名单模式
            for (Map<String, Object> config : whitelistConfigs) {
                String pattern = MapUtil.getStr(config, "pattern");
                if (pattern != null && pathMatcher.match(pattern, uri)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            logger.error("从数据库获取白名单失败，使用默认白名单", e);

            // 数据库查询失败时，使用默认白名单作为备用
            for (String pattern : defaultWhiteListPatterns) {
                if (pathMatcher.match(pattern, uri)) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * 清除白名单缓存
     */
    @CacheEvict(value = "security_whitelist", allEntries = true)
    public void clearWhitelistCache() {
        logger.info("清除白名单缓存");
    }
}
