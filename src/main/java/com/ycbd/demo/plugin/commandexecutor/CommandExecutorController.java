package com.ycbd.demo.plugin.commandexecutor;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ycbd.demo.utils.ApiResponse;

/**
 * 命令执行控制器，暴露 REST 接口供外部调用。
 */
@RestController
@RequestMapping("/api/command")
public class CommandExecutorController {

    private CommandExecutionService commandService;

    public void setCommandService(CommandExecutionService commandService) {
        this.commandService = commandService;
    }

    /**
     * 执行命令
     *
     * @param params {"command":"ls -l"}
     * @return 统一响应
     */
    @PostMapping("/execute")
    public ApiResponse<Map<String, Object>> executeCommand(@RequestBody Map<String, String> params) {
        String command = params.get("command");
        if (command == null || command.trim().isEmpty()) {
            return ApiResponse.failed("命令不能为空");
        }

        Map<String, Object> result = commandService.executeCommand(command);
        return ApiResponse.success(result);
    }
}
