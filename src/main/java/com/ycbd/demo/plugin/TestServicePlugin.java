package com.ycbd.demo.plugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ycbd.demo.utils.ApiResponse;

@Component
public class TestServicePlugin implements IPlugin {

    private static final Logger logger = LoggerFactory.getLogger(TestServicePlugin.class);
    private static final String DEFAULT_RESULT_DIR = "test_results";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"|'token'\\s*:\\s*'([^']+)'");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private TestServiceController controller;
    private boolean isInitialized = false;

    @Override
    public String getName() {
        return "TestService";
    }

    @Override
    public void initialize() {
        logger.info("初始化测试服务插件");
        controller = new TestServiceController();
        isInitialized = true;
    }

    @Override
    public void shutdown() {
        logger.info("关闭测试服务插件");
        isInitialized = false;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public TestServiceController getController() {
        return controller;
    }

    /**
     * 测试服务控制器
     */
    @RestController
    @RequestMapping("/api/test/execute")
    public static class TestServiceController {

        @PostMapping
        public ApiResponse<Map<String, Object>> runTest(
                @RequestParam String scriptPath,
                @RequestParam(required = false, defaultValue = "test_results") String resultDir,
                @RequestParam(required = false, defaultValue = "false") boolean useCurrentDir) {

            try {
                // 验证脚本路径
                File scriptFile = new File(scriptPath);
                if (!scriptFile.exists() || !scriptFile.isFile()) {
                    return ApiResponse.failed("脚本文件不存在: " + scriptPath);
                }

                // 确定结果目录
                String finalResultDir;
                if (useCurrentDir) {
                    // 使用脚本所在目录作为基准
                    File scriptParent = scriptFile.getParentFile();
                    finalResultDir = new File(scriptParent, resultDir).getAbsolutePath();
                } else {
                    // 使用绝对路径或相对于应用程序的路径
                    finalResultDir = resultDir;
                }

                // 创建结果目录
                File resultDirFile = new File(finalResultDir);
                if (!resultDirFile.exists()) {
                    if (!resultDirFile.mkdirs()) {
                        return ApiResponse.failed("无法创建结果目录: " + finalResultDir);
                    }
                }

                // 先检查脚本文件是否符合规范（仅注释或单行 curl）
                String validationMsg = validateScript(scriptFile);
                if (validationMsg != null) {
                    return ApiResponse.failed(validationMsg);
                }

                // 生成结果文件名
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String resultFileName = sdf.format(new Date()) + "_result.txt";
                File resultFile = new File(resultDirFile, resultFileName);

                // 解析并执行脚本
                List<CommandResult> results = executeScript(scriptFile, resultFile);

                // 统计结果
                int totalCommands = results.size();
                int successCount = 0;
                int failureCount = 0;

                for (CommandResult result : results) {
                    if (result.isSuccess()) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                }

                // 构建响应
                Map<String, Object> response = new HashMap<>();
                response.put("totalCommands", totalCommands);
                response.put("successCount", successCount);
                response.put("failureCount", failureCount);
                response.put("commandResults", results);
                response.put("resultFilePath", resultFile.getAbsolutePath());

                return ApiResponse.success(response);

            } catch (Exception e) {
                logger.error("执行测试脚本失败", e);
                return ApiResponse.failed("执行测试脚本失败: " + e.getMessage());
            }
        }

        /**
         * 解析并执行脚本
         */
        private List<CommandResult> executeScript(File scriptFile, File resultFile) throws IOException {
            List<CommandResult> results = new ArrayList<>();
            List<String> lines;
            try {
                lines = Files.readAllLines(scriptFile.toPath(), StandardCharsets.UTF_8);
            } catch (MalformedInputException mie) {
                // 尝试使用系统默认编码作为降级方案
                try {
                    logger.warn("使用UTF-8读取脚本失败，尝试使用系统默认编码({})", Charset.defaultCharset());
                    lines = Files.readAllLines(scriptFile.toPath(), Charset.defaultCharset());
                } catch (Exception e) {
                    // 最后降级为 ISO_8859_1，避免因编码问题彻底失败
                    logger.warn("使用系统默认编码读取脚本失败，尝试使用ISO_8859_1", e);
                    lines = Files.readAllLines(scriptFile.toPath(), StandardCharsets.ISO_8859_1);
                }
            }

            StringBuilder currentCommand = new StringBuilder();
            StringBuilder currentComment = new StringBuilder();
            String token = null;

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile))) {
                for (String line : lines) {
                    line = line.trim();

                    // 跳过空行
                    if (line.isEmpty()) {
                        continue;
                    }

                    // 处理注释
                    if (line.startsWith("#")) {
                        if (currentCommand.length() > 0) {
                            // 如果已经有命令，则执行它
                            CommandResult result = executeCommand(currentCommand.toString(), token);
                            results.add(result);
                            writeResult(writer, currentComment.toString(), result);
                            // 提取token
                            String extractedToken = extractToken(result.getResult());
                            if (result.isSuccess() && extractedToken != null && !extractedToken.isEmpty()) {
                                token = extractedToken;
                                System.out.println("[TestServicePlugin] 提取到token: " + token);
                                logger.info("[TestServicePlugin] 提取到token: {}", token);
                            }
                            currentCommand = new StringBuilder();
                        }

                        // 保存新的注释
                        currentComment = new StringBuilder(line.substring(1).trim());
                    } else {
                        // 处理命令
                        if (line.startsWith("curl")) {
                            if (currentCommand.length() > 0) {
                                // 如果已经有命令，则执行它
                                logger.info("执行命令: {}", currentCommand.toString());
                                CommandResult result = executeCommand(currentCommand.toString(), token);
                                results.add(result);
                                writeResult(writer, currentComment.toString(), result);

                                // 首先检查命令结果中是否有token
                                if (result.getToken() != null && !result.getToken().isEmpty()) {
                                    token = result.getToken();
                                    logger.info("[TestServicePlugin] 从命令结果中获取token: {}", token);
                                } else {
                                    // 尝试从响应中提取token
                                    String extractedToken = extractToken(result.getResult());
                                    if (result.isSuccess() && extractedToken != null && !extractedToken.isEmpty()) {
                                        token = extractedToken;
                                        logger.info("[TestServicePlugin] 从响应中提取token: {}", token);
                                    } else {
                                        logger.info("[TestServicePlugin] 未能从响应中提取token");
                                    }
                                }
                                currentCommand = new StringBuilder();
                            }
                        }

                        // 添加到当前命令
                        if (currentCommand.length() > 0 && !line.endsWith("\\")) {
                            currentCommand.append(" ");
                        }

                        // 移除行尾的反斜杠
                        if (line.endsWith("\\")) {
                            currentCommand.append(line.substring(0, line.length() - 1));
                        } else {
                            currentCommand.append(line);
                        }
                    }
                }

                // 处理最后一个命令
                if (currentCommand.length() > 0) {
                    logger.info("执行最后一个命令: {}", currentCommand.toString());
                    CommandResult result = executeCommand(currentCommand.toString(), token);
                    results.add(result);
                    writeResult(writer, currentComment.toString(), result);

                    // 首先检查命令结果中是否有token
                    if (result.getToken() != null && !result.getToken().isEmpty()) {
                        token = result.getToken();
                        logger.info("[TestServicePlugin] 从最后一个命令结果中获取token: {}", token);
                    } else {
                        // 尝试从响应中提取token
                        String extractedToken = extractToken(result.getResult());
                        if (result.isSuccess() && extractedToken != null && !extractedToken.isEmpty()) {
                            token = extractedToken;
                            logger.info("[TestServicePlugin] 从最后一个命令响应中提取token: {}", token);
                        } else {
                            // 仅当之前尚未成功获取token时才输出提示，避免干扰日志
                            if (token == null) {
                                logger.info("[TestServicePlugin] 未能从最后一个命令响应中提取token");
                            } else {
                                logger.debug("[TestServicePlugin] 最后一个命令未返回新的token，已忽略");
                            }
                        }
                    }
                    currentCommand = new StringBuilder();
                }

                // 写入总结
                writer.write("\n执行总结:\n");
                writer.write("总命令数: " + results.size() + "\n");

                int successCount = 0;
                for (CommandResult result : results) {
                    if (result.isSuccess()) {
                        successCount++;
                    }
                }

                writer.write("成功数: " + successCount + "\n");
                writer.write("失败数: " + (results.size() - successCount) + "\n");
            }

            return results;
        }

        /**
         * 执行命令
         */
        private CommandResult executeCommand(String command, String token) {
            CommandResult result = new CommandResult();
            result.setCommand(command);
            result.setCommandName(extractCommandName(command));

            // 替换token
            if (token != null) {
                // 替换常见的token占位符格式
                command = command.replace("Bearer your-token-here", "Bearer " + token);
                command = command.replace("Bearer ${TOKEN}", "Bearer " + token);
                command = command.replace("Bearer $TOKEN", "Bearer " + token);

                // 记录替换情况
                logger.debug("Token替换后的命令: {}", command);
            } else {
                logger.debug("未找到token，命令未修改: {}", command);
            }

            try {
                // 创建进程
                ProcessBuilder processBuilder = new ProcessBuilder();

                // 在Windows上使用cmd /c，在Unix/Linux上使用bash -c
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    processBuilder.command("cmd.exe", "/c", command);
                } else {
                    processBuilder.command("bash", "-c", command);
                }

                Process process = processBuilder.start();

                // 读取输出
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                // 读取错误
                StringBuilder error = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line).append("\n");
                    }
                }

                // 等待进程完成
                boolean completed = process.waitFor(30, TimeUnit.SECONDS);

                if (!completed) {
                    process.destroyForcibly();
                    result.setSuccess(false);
                    result.setErrorMessage("命令执行超时");
                } else if (process.exitValue() != 0) {
                    result.setSuccess(false);
                    result.setErrorMessage(error.toString());
                } else {
                    result.setSuccess(true);
                    result.setResult(output.toString());

                    // 检查是否是登录命令，如果是，尝试提取token
                    if (command.contains("login")) {
                        String extractedToken = extractToken(output.toString());
                        if (extractedToken != null && !extractedToken.isEmpty()) {
                            logger.info("从登录命令中提取到token: {}", extractedToken);
                            result.setToken(extractedToken); // 保存到结果中
                        }
                    }
                }

            } catch (Exception e) {
                result.setSuccess(false);
                result.setErrorMessage(e.getMessage());
            }

            return result;
        }

        /**
         * 从命令中提取名称
         */
        private String extractCommandName(String command) {
            // 尝试从URL中提取操作
            Pattern pattern = Pattern.compile("'https?://[^/]+/api/([^'\\s]+)'");
            Matcher matcher = pattern.matcher(command);
            if (matcher.find()) {
                return matcher.group(1);
            }

            // 如果没有找到，返回默认名称
            return "未命名命令";
        }

        /**
         * 从响应中提取token
         */
        private String extractToken(String response) {
            if (response == null) {
                return null;
            }
            Matcher matcher = TOKEN_PATTERN.matcher(response);
            if (matcher.find()) {
                // 检查第一个捕获组（双引号格式）
                String token = matcher.group(1);
                if (token == null || token.isEmpty()) {
                    // 如果第一个捕获组为空，尝试第二个捕获组（单引号格式）
                    token = matcher.group(2);
                }
                // 打印更详细的日志，帮助调试
                if (token != null && !token.isEmpty()) {
                    logger.debug("成功提取token: {}", token);
                } else {
                    logger.debug("未能从响应中提取token: {}", response);
                }
                return token;
            }
            logger.debug("未能匹配token模式，响应内容: {}", response);
            return null;
        }

        /**
         * 写入结果到文件
         */
        private void writeResult(BufferedWriter writer, String comment, CommandResult result) throws IOException {
            writer.write(comment + "结果:\n");
            if (result.isSuccess()) {
                writer.write(result.getResult());
            } else {
                writer.write("执行失败: " + result.getErrorMessage() + "\n");
            }
            writer.write("\n");
        }

        /**
         * 校验脚本文件是否符合测试规则： 1. 只允许空行、以 # 开头的注释、或以 curl 开头的单行命令。 2. 不允许出现反斜杠续行符或其它
         * shell 语句。
         *
         * @param scriptFile 脚本文件
         * @return 返回错误信息；若为 null 表示校验通过
         */
        private String validateScript(File scriptFile) {
            List<String> lines;
            try {
                lines = Files.readAllLines(scriptFile.toPath(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                return "读取脚本文件失败: " + e.getMessage();
            }

            for (int i = 0; i < lines.size(); i++) {
                String rawLine = lines.get(i);
                String line = rawLine.trim();

                // 跳过空行
                if (line.isEmpty()) {
                    continue;
                }

                // 注释行
                if (line.startsWith("#")) {
                    continue;
                }

                // curl 命令行，需保证不含续行符
                if (line.startsWith("curl")) {
                    if (line.endsWith("\\")) {
                        return String.format("脚本第 %d 行使用了续行符 \\\\，不符合单行 curl 要求: %s", i + 1, rawLine);
                    }
                    continue;
                }

                // 其它情况视为违规
                return String.format("脚本第 %d 行含有非允许内容: %s", i + 1, rawLine);
            }

            return null; // 通过校验
        }
    }

    /**
     * 命令执行结果
     */
    public static class CommandResult {

        private String commandName;
        private String command;
        private boolean success;
        private String result;
        private String errorMessage;
        private String token;

        public String getCommandName() {
            return commandName;
        }

        public void setCommandName(String commandName) {
            this.commandName = commandName;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
