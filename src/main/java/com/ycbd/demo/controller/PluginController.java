package com.ycbd.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ycbd.demo.plugin.commandexecutor.CommandExecutorPlugin;
import com.ycbd.demo.utils.ApiResponse;

@RestController
@RequestMapping("/api/system/plugins")
public class PluginController {

    @Autowired
    private CommandExecutorPlugin commandExecutorPlugin;

    private boolean pluginInitialized = false;

    @PostMapping("/load")
    public ApiResponse<String> loadPlugin(@RequestParam String pluginName) {
        try {
            if ("CommandExecutor".equals(pluginName)) {
                if (!pluginInitialized) {
                    commandExecutorPlugin.initialize();
                    pluginInitialized = true;
                    return ApiResponse.success("命令执行器插件加载成功");
                } else {
                    return ApiResponse.success("命令执行器插件已加载");
                }
            } else {
                return ApiResponse.failed("不支持的插件: " + pluginName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.failed("加载插件失败: " + e.getMessage());
        }
    }

    @PostMapping("/unload")
    public ApiResponse<String> unloadPlugin(@RequestParam String pluginName) {
        try {
            if ("CommandExecutor".equals(pluginName)) {
                if (pluginInitialized) {
                    commandExecutorPlugin.shutdown();
                    pluginInitialized = false;
                    return ApiResponse.success("命令执行器插件已卸载");
                } else {
                    return ApiResponse.success("命令执行器插件未加载");
                }
            } else {
                return ApiResponse.failed("不支持的插件: " + pluginName);
            }
        } catch (Exception e) {
            return ApiResponse.failed("卸载插件失败: " + e.getMessage());
        }
    }

    @PostMapping("/status")
    public ApiResponse<String> pluginStatus(@RequestParam String pluginName) {
        if ("CommandExecutor".equals(pluginName)) {
            return ApiResponse.success(pluginInitialized ? "已加载" : "未加载");
        } else {
            return ApiResponse.failed("不支持的插件: " + pluginName);
        }
    }
}
