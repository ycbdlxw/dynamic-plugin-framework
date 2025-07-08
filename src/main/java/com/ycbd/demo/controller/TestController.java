package com.ycbd.demo.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.ycbd.demo.plugin.IPlugin;
import com.ycbd.demo.plugin.PluginEngine;
import com.ycbd.demo.plugin.commandexecutor.CommandExecutionService;
import com.ycbd.demo.plugin.commandexecutor.CommandExecutorPlugin;
import com.ycbd.demo.utils.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.ycbd.demo.plugin.TestServicePlugin;
import com.ycbd.demo.plugin.TestVerificationService;

/**
 * 测试控制器 此控制器在启动时自动加载TestService插件，并将其API请求转发到插件
 */
@RestController
@RequestMapping("/api/test")
@DependsOn("pluginEngine")
@Tag(name = "测试服务接口", description = "提供自动化接口测试功能")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private PluginEngine pluginEngine;

    // 命令执行服务，优先使用插件提供的，如果插件未加载则使用本地实例
    private CommandExecutionService commandService;

    @Autowired
    private TestVerificationService verificationService;

    @PostConstruct
    public void init() {
        logger.info("初始化测试控制器，尝试加载TestService插件");
        try {
            String result = pluginEngine.loadPlugin("TestService");
            logger.info(result);

            // 尝试加载命令执行器插件
            try {
                String cmdResult = pluginEngine.loadPlugin("CommandExecutor");
                logger.info(cmdResult);

                // 获取命令执行器插件
                IPlugin cmdPlugin = pluginEngine.getPlugin("CommandExecutor");
                if (cmdPlugin != null && cmdPlugin instanceof CommandExecutorPlugin) {
                    CommandExecutorPlugin executorPlugin = (CommandExecutorPlugin) cmdPlugin;
                    commandService = executorPlugin.getCommandService();
                    logger.info("成功获取命令执行器插件的服务");
                }
            } catch (Exception e) {
                logger.error("加载命令执行器插件失败", e);
            }

            // 如果插件加载失败，创建本地服务实例
            if (commandService == null) {
                logger.info("使用本地CommandExecutionService");
                commandService = new CommandExecutionService();
            }
        } catch (Exception e) {
            logger.error("加载TestService插件失败", e);
        }
    }

    /**
     * 执行测试脚本（新版API）
     */
    @PostMapping("/execute")
    @Operation(summary = "执行测试脚本", description = "执行指定路径的测试脚本并返回结果")
    public ApiResponse<?> executeScript(
            @RequestParam String scriptPath,
            @RequestParam(required = false, defaultValue = "src/main/resources/test/test_results") String resultDir,
            @RequestParam(required = false, defaultValue = "false") boolean useCurrentDir) {
        
        // 获取TestService插件
        IPlugin plugin = pluginEngine.getPlugin("TestService");
        if (plugin == null || !(plugin instanceof TestServicePlugin)) {
            return ApiResponse.failed("TestService插件未加载或不可用");
        }
        
        TestServicePlugin testPlugin = (TestServicePlugin) plugin;
        if (!testPlugin.isInitialized()) {
            return ApiResponse.failed("TestService插件未初始化");
        }
        
        // 调用插件的execute方法
        return testPlugin.getController().executeScript(scriptPath, resultDir, useCurrentDir);
    }

    /**
     * 执行测试脚本（旧版API，已废弃）
     */
    @PostMapping("/run")
    @Deprecated
    @Operation(summary = "执行测试脚本（已废弃）", description = "此方法已废弃，请使用/execute")
    public Object runScript(
            @RequestParam String scriptPath,
            @RequestParam(required = false, defaultValue = "src/main/resources/test/test_results") String resultDir,
            @RequestParam(required = false, defaultValue = "false") boolean useCurrentDir,
            RedirectAttributes attributes) {
        
        // 重定向到新的API
        attributes.addAttribute("scriptPath", scriptPath);
        attributes.addAttribute("resultDir", resultDir);
        attributes.addAttribute("useCurrentDir", useCurrentDir);
        
        return new RedirectView("/api/test/execute");
    }

    @PostMapping("/command")
    @Operation(summary = "执行命令", description = "执行系统命令并返回结果")
    public ApiResponse<Object> executeCommand(@RequestBody Map<String, String> params) {
        String command = params.get("command");
        if (command == null || command.trim().isEmpty()) {
            return ApiResponse.failed("命令不能为空");
        }

        // 执行命令
        try {
            // 确保commandService不为空
            if (commandService == null) {
                logger.error("commandService为空，创建新实例");
                commandService = new CommandExecutionService();
            }

            Map<String, Object> result = commandService.executeCommand(command);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("执行命令失败", e);
            return ApiResponse.failed("执行命令失败: " + e.getMessage());
        }
    }

    /** 完整验证：报告+数据库 */
    @PostMapping("/verify")
    @Operation(summary = "验证测试结果", description = "检查报告目录与数据库状态")
    public ApiResponse<Object> verifyAll(@RequestParam(required = false, defaultValue = "src/main/resources/test/test_results") String resultDir) {
        List<String> reportIssues = verificationService.verifyReports(resultDir);
        List<String> dbIssues = verificationService.verifyDatabase();

        boolean success = reportIssues.isEmpty() && dbIssues.isEmpty();
        Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("reportIssues", reportIssues);
        resp.put("dbIssues", dbIssues);

        if (success) {
            return ApiResponse.success(resp);
        } else {
            return ApiResponse.failed("ReportIssues=" + reportIssues + "; DbIssues=" + dbIssues);
        }
    }
}
