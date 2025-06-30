package com.ycbd.demo.utils;

import java.util.HashMap;

public class ApiResponse<T> extends HashMap<String, Object> {

    private ApiResponse(int code, String message, T data) {
        this.put("code", code);
        this.put("message", message);
        if (data != null) {
            this.put("data", data);
        }
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> failed(String message) {
        return new ApiResponse<>(ResultCode.FAILED.getCode(), message, null);
    }

    public static <T> ApiResponse<T> failed() {
        return failed(ResultCode.FAILED.getMessage());
    }

    public static <T> ApiResponse<T> of(ResultCode code, T data) {
        return new ApiResponse<>(code.getCode(), code.getMessage(), data);
    }
}
