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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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
    private static final String DEFAULT_RESULT_DIR = "src/main/resources/test/test_results";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"|'token'\\s*:\\s*'([^']+)'");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 允许的命令前缀列表
    private static final List<String> ALLOWED_PREFIXES = Arrays.asList("curl ", "echo ", "#");

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

        /**
         * 执行测试脚本
         */
        @PostMapping("/execute")
        public ApiResponse<String> executeScript(
                @RequestParam String scriptPath,
                @RequestParam(required = false, defaultValue = DEFAULT_RESULT_DIR) String resultDir,
                @RequestParam(required = false, defaultValue = "false") boolean useCurrentDir) {
            try {
                logger.info("[TEST] 脚本路径: {}", scriptPath);
                logger.info("[TEST] 结果目录: {}", resultDir);
                // 检查脚本路径
                File scriptFile = new File(scriptPath);
                if (!scriptFile.exists() || !scriptFile.isFile()) {
                    logger.error("[TEST] 脚本文件不存在: {}", scriptPath);
                    return ApiResponse.failed("脚本文件不存在: " + scriptPath);
                }

                // 创建结果目录
                File resultDirFile = new File(resultDir);
                if (!resultDirFile.exists()) {
                    boolean mk = resultDirFile.mkdirs();
                    logger.info("[TEST] 结果目录不存在，尝试创建: {}，结果: {}", resultDirFile.getAbsolutePath(), mk);
                } else {
                    logger.info("[TEST] 结果目录已存在: {}", resultDirFile.getAbsolutePath());
                }

                // 验证脚本内容
                String validationError = validateScript(scriptFile);
                if (validationError != null) {
                    logger.error("[TEST] 脚本校验失败: {}", validationError);
                    return ApiResponse.failed(validationError);
                }

                // 生成结果文件名
                String resultFileName = scriptFile.getName().replace(".curl", "") + "_"
                        + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";
                File resultFile = new File(resultDirFile, resultFileName);
                logger.info("[TEST] 结果文件路径: {}", resultFile.getAbsolutePath());

                // 使用带token处理的executeScript方法
                try {
                    List<CommandResult> results = executeScript(scriptFile, resultFile);
                    int total = results.size();
                    long success = results.stream().filter(CommandResult::isSuccess).count();
                    long fail = total - success;
                    logger.info("[TEST] 执行总结: 总命令数:{} 成功:{} 失败:{}", total, success, fail);
                    return ApiResponse.success("脚本执行成功，结果保存在: " + resultFile.getAbsolutePath());
                } catch (IOException e) {
                    logger.error("[TEST] 执行脚本失败", e);
                    return ApiResponse.failed("执行脚本失败: " + e.getMessage());
                }
            } catch (Exception e) {
                logger.error("[TEST] 执行脚本失败", e);
                return ApiResponse.failed("执行脚本失败: " + e.getMessage());
            }
        }

        /**
         * 解析并执行脚本
         */
        private List<CommandResult> executeScript(File scriptFile, File resultFile) throws IOException {
            // 读取脚本（保持对不同编码的兼容）
            List<String> lines;
            try {
                lines = Files.readAllLines(scriptFile.toPath(), StandardCharsets.UTF_8);
            } catch (MalformedInputException mie) {
                lines = Files.readAllLines(scriptFile.toPath(), Charset.defaultCharset());
            }

            List<CommandResult> results = new ArrayList<>();
            String currentDesc = "";
            String token = null;

            // 判断脚本是否需要token
            boolean requiresToken = lines.stream().anyMatch(l -> l.contains("Bearer your-token-here") || l.contains("${TOKEN}") || l.contains("$TOKEN"));
            boolean firstCurl = true;

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile))) {
                for (String raw : lines) {
                    String line = raw.trim();

                    if (line.isEmpty()) {
                        continue;
                    }

                    // 注释作为描述
                    if (line.startsWith("#")) {
                        currentDesc = line.substring(1).trim();
                        continue;
                    }

                    // echo 行仅打印，不执行
                    if (line.toLowerCase().startsWith("echo ")) {
                        continue;
                    }

                    // 只接受以 curl 开头的单行命令
                    if (line.toLowerCase().startsWith("curl ")) {
                        CommandResult result = executeCommand(line, token);
                        results.add(result);
                        writeResult(writer, currentDesc, result);

                        // 如果脚本需要token且这是首条curl，则视为登录命令
                        if (requiresToken && firstCurl) {
                            firstCurl = false;

                            // 提取 token
                            if (result.getToken() != null && !result.getToken().isEmpty()) {
                                token = result.getToken();
                            } else {
                                String extracted = extractToken(result.getResult());
                                if (result.isSuccess() && extracted != null && !extracted.isEmpty()) {
                                    token = extracted;
                                }
                            }

                            // 登录失败，终止脚本
                            if (token == null || token.isEmpty()) {
                                writer.write("登录失败，未获取token，终止脚本执行\n");
                                logger.error("登录失败，未获取token，终止脚本执行");
                                break;
                            }
                        } else {
                            // 后续命令：若包含占位符则替换
                            if (token != null && !token.isEmpty()) {
                                // 已在 executeCommand 内替换 your-token-here
                            }
                        }

                        continue;
                    }

                    // 其余行视为非法
                    logger.error("脚本包含非法行: {}", line);
                    throw new IOException("脚本包含非法行: " + line);
                }

                // 总结
                writer.write("\n执行总结:\n");
                writer.write("总命令数: " + results.size() + "\n");
                long successCount = results.stream().filter(CommandResult::isSuccess).count();
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

            // 不主动追加 Authorization 头，只在命令中显式存在占位符时进行替换
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
                } else {
                    final String outStr = output.toString();

                    // 默认成功条件：进程退出0 & HTTP 2xx & 业务 code==200
                    boolean successFlag = true;

                    // 提取 Status 行
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("Status:\\s*(\\d{3})").matcher(outStr);
                    if (m.find()) {
                        int httpCode = Integer.parseInt(m.group(1));
                        if (httpCode >= 300) {
                            successFlag = false;
                        }
                    }

                    // 提取业务 code 字段
                    java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("\"code\"\\s*:\\s*(\\d+)").matcher(outStr);
                    if (m2.find()) {
                        int bizCode = Integer.parseInt(m2.group(1));
                        if (bizCode != 200) {
                            successFlag = false;
                        }
                    }

                    result.setSuccess(successFlag);
                    result.setResult(outStr);

                    // 如果失败，记录错误
                    if (!successFlag) {
                        result.setErrorMessage("请求返回非成功状态");
                    }

                    // 检查是否是登录命令，如果是，尝试提取token
                    if (command.contains("login")) {
                        String extractedToken = extractToken(outStr);
                        if (extractedToken != null && !extractedToken.isEmpty()) {
                            logger.info("从登录命令中提取到token: {}", extractedToken);
                            result.setToken(extractedToken);
                        }
                    }
                }

                // 如果仍未获取到token，且命令包含输出重定向，则尝试从文件读取
                if ((result.getToken() == null || result.getToken().isEmpty()) && command.contains(">")) {
                    try {
                        // 简单解析重定向文件名：取 '>' 之后的第一个非空字段
                        String[] parts = command.split(">");
                        if (parts.length > 1) {
                            String redirectPart = parts[1].trim().split(" ")[0];
                            java.io.File redirectFile = new java.io.File(redirectPart);
                            if (redirectFile.exists() && redirectFile.isFile()) {
                                String fileContent = new String(java.nio.file.Files.readAllBytes(redirectFile.toPath()));
                                String fileToken = extractToken(fileContent);
                                if (fileToken != null && !fileToken.isEmpty()) {
                                    logger.info("从重定向文件({})中提取到token", redirectPart);
                                    result.setToken(fileToken);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("尝试从重定向文件提取token失败", e);
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
         * 验证脚本是否符合规范
         */
        private String validateScript(File scriptFile) {
            try {
                List<String> lines = Files.readAllLines(scriptFile.toPath());

                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).trim();

                    // 空行或注释行
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    // echo命令行 - 不区分大小写，允许带引号
                    if (line.toLowerCase().startsWith("echo ")) {
                        continue;
                    }

                    // curl命令行 - 不区分大小写
                    if (line.toLowerCase().startsWith("curl ")) {
                        continue;
                    }

                    // rm命令行 - 允许清理临时文件
                    if (line.toLowerCase().startsWith("rm ")) {
                        continue;
                    }

                    // 不再允许变量赋值和shell控制行
                    // 不允许的行
                    logger.error("脚本验证失败，第 {} 行: {}", i + 1, line);
                    return "脚本第 " + (i + 1) + " 行含有非允许内容: " + line;
                }

                return null; // 通过校验
            } catch (IOException e) {
                logger.error("读取脚本文件失败", e);
                return "读取脚本文件失败: " + e.getMessage();
            }
        }

        /**
         * 检查行是否是允许的内容
         */
        private boolean isAllowedLine(String line) {
            if (line.isEmpty()) {
                return true;
            }

            // 检查是否以允许的前缀开头
            for (String prefix : ALLOWED_PREFIXES) {
                if (line.toLowerCase().startsWith(prefix.toLowerCase())) {
                    return true;
                }
            }

            return false;
        }

        /**
         * 读取文件内容
         */
        private List<String> readFileLines(File file) throws IOException {
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            return lines;
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
