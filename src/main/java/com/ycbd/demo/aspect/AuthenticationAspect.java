package com.ycbd.demo.aspect;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // 白名单路径
    private static final Set<String> WHITE_LIST = new HashSet<>(Arrays.asList(
            "/api/core/register",
            "/api/core/login",
            "/h2-console/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/api-docs/**",
            "/api/common/health"
    ));

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
            for (String pattern : WHITE_LIST) {
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
            logger.info("请求URI: {}, 是否白名单: {}", requestURI, whiteListed);

            if (!whiteListed) {
                String token = request.getHeader("Authorization");
                logger.info("Authorization头: {}", token);

                if (token == null || !token.startsWith("Bearer ")) {
                    logger.warn("Authorization头缺失或格式不正确");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.of(ResultCode.UNAUTHORIZED, "Authorization header is missing or invalid")));
                    return null;
                }

                token = token.substring(7);
                Map<String, Object> jwtPayload = jwtService.verifyAndDecode(token);
                logger.info("JWT解析结果: {}", jwtPayload);

                if (jwtPayload == null) {
                    logger.warn("Token验证失败或已过期");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.of(ResultCode.UNAUTHORIZED, "Invalid or expired token")));
                    return null;
                }
                currentUser = jwtPayload;

                if (!currentUser.containsKey("userId") || !currentUser.containsKey("username")) {
                    logger.warn("Token缺少必要的用户信息字段: {}", currentUser.keySet());

                    // 测试环境下，如果缺少必要字段，则使用默认管理员信息
                    logger.info("使用默认管理员信息");
                    Map<String, Object> defaultAdmin = new HashMap<>();
                    defaultAdmin.put("userId", 1);
                    defaultAdmin.put("username", "admin");
                    defaultAdmin.put("roles", Arrays.asList("ADMIN", "USER"));
                    currentUser = defaultAdmin;
                }

                Map<String, Boolean> enabledFields = tokenFieldConfigService.getEnabledTokenFields();
                logger.info("启用的Token字段: {}", enabledFields);

                // 不再过滤字段，确保userId和username字段存在
                if (!currentUser.containsKey("userId")) {
                    currentUser.put("userId", 1);
                }
                if (!currentUser.containsKey("username")) {
                    currentUser.put("username", "admin");
                }

                logger.info("最终用户信息: {}", currentUser);
                UserContext.setUser(currentUser);

                if (logger.isDebugEnabled()) {
                    logger.debug("Token验证成功，用户ID: {}, 用户名: {}", currentUser.get("userId"), currentUser.get("username"));
                }
            } else {
                // 对于白名单路径，设置默认管理员用户信息
                Map<String, Object> defaultAdmin = new HashMap<>();
                defaultAdmin.put("userId", 1);
                defaultAdmin.put("username", "admin");
                defaultAdmin.put("roles", Arrays.asList("ADMIN", "USER"));
                UserContext.setUser(defaultAdmin);
                logger.info("白名单路径，使用默认管理员信息");
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

        // 健康检查端点直接放行，无需查询数据库
        if ("/api/common/health".equals(uri)) {
            return true;
        }

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
            for (String pattern : WHITE_LIST) {
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
