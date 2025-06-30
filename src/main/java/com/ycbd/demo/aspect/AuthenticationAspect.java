package com.ycbd.demo.aspect;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import cn.hutool.jwt.JWT;

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

        // 硬编码白名单，避免数据库查询错误
        String[] whiteListPatterns = {
            "/api/core/register",
            "/api/core/login",
            "/h2-console/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/api-docs/**",
            "/api/system/token-fields/enabled",
            "/api/system/token-fields/enabled-names",
            "/api/test/**",
            "/api/test-service/**",
            "/api/test/execute/**"
        };

        for (String pattern : whiteListPatterns) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }

        return false;
    }
}
