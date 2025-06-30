package com.ycbd.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ycbd.demo.plugin.PluginEngine;
import com.ycbd.demo.security.UserContext;
import com.ycbd.demo.utils.ApiResponse;
import com.ycbd.demo.utils.ResultCode;

/**
 * 兼容旧版测试脚本中使用的 /api/plugin/* 路径。 新功能全部转发到 {@link PluginController} 所在的
 * PluginEngine。
 */
@RestController
@RequestMapping("/api/plugin")
public class LegacyPluginController {

    @Autowired
    private PluginEngine pluginEngine;

    /**
     * 加载插件（兼容旧路径）
     */
    @PostMapping("/load")
    public ApiResponse<String> loadPlugin(@RequestParam String pluginName) {
        if (!UserContext.getRoles().contains("ADMIN")) {
            return ApiResponse.of(ResultCode.FORBIDDEN, "没有权限加载插件");
        }
        String result = pluginEngine.loadPlugin(pluginName);
        return ApiResponse.success(result);
    }

    /**
     * 卸载插件（兼容旧路径）
     */
    @PostMapping("/unload")
    public ApiResponse<String> unloadPlugin(@RequestParam String pluginName) {
        if (!UserContext.getRoles().contains("ADMIN")) {
            return ApiResponse.of(ResultCode.FORBIDDEN, "没有权限卸载插件");
        }
        String result = pluginEngine.unloadPlugin(pluginName);
        return ApiResponse.success(result);
    }
}
