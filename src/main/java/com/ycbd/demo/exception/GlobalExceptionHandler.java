package com.ycbd.demo.exception;

import java.nio.charset.MalformedInputException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.ycbd.demo.utils.ApiResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public ApiResponse<Object> handle(Exception e) {
        logger.error("捕获到全局异常: ", e);
        return ApiResponse.failed("服务器内部错误: " + e.getMessage());
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    @ResponseBody
    public ApiResponse<Object> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("捕获到非法参数异常: {}", e.getMessage());
        return ApiResponse.failed(e.getMessage());
    }

    @ExceptionHandler(value = DataIntegrityViolationException.class)
    @ResponseBody
    public ApiResponse<Object> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        logger.warn("数据库约束异常: {}", e.getMostSpecificCause().getMessage());
        return ApiResponse.failed("数据完整性错误: " + e.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(value = MalformedInputException.class)
    @ResponseBody
    public ApiResponse<Object> handleMalformedInput(MalformedInputException e) {
        logger.warn("字符编码处理异常: {}", e.getMessage());
        return ApiResponse.failed("文件编码错误，请确认脚本文件为UTF-8或使用ASCII字符");
    }

    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    @ResponseBody
    public ApiResponse<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        logger.warn("缺少必要请求参数: {}", e.getParameterName());
        return ApiResponse.failed("缺少必要参数: " + e.getParameterName());
    }

    @ExceptionHandler(value = NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ApiResponse<Object> handleNoResourceFound(NoResourceFoundException e) {
        logger.info("静态资源未找到: {}", e.getResourcePath());
        return ApiResponse.failed("资源不存在: " + e.getResourcePath());
    }
}
