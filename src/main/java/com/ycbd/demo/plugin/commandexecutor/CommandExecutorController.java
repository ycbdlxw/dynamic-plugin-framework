package com.ycbd.demo.plugin.commandexecutor;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(CommandExecutorController.class);

    private CommandExecutionService commandService;

    public CommandExecutorController() {
        // 默认构造函数，确保实例化时不为空
        this.commandService = new CommandExecutionService();
        logger.info("CommandExecutorController已初始化，commandService已创建");
    }

    public void setCommandService(CommandExecutionService commandService) {
        this.commandService = commandService;
        logger.info("CommandExecutorController的commandService已设置");
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

        if (commandService == null) {
            logger.error("commandService为空，尝试重新初始化");
            commandService = new CommandExecutionService();
        }

        try {
            Map<String, Object> result = commandService.executeCommand(command);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("执行命令失败", e);
            return ApiResponse.failed("执行命令失败: " + e.getMessage());
        }
    }
}
