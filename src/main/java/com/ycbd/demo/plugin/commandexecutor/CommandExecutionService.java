package com.ycbd.demo.plugin.commandexecutor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 命令执行服务，提供统一的跨平台命令执行接口。
 */
public class CommandExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(CommandExecutionService.class);

    /**
     * 执行单条命令
     *
     * @param command 完整的命令字符串，例如 "ls -l" 或 "dir"
     * @return 执行结果 map，包含 exitCode、stdout、stderr
     */
    public Map<String, Object> executeCommand(String command) {
        logger.info("执行命令: {}", command);
        Map<String, Object> result = new HashMap<>();
        ProcessBuilder processBuilder = createProcessBuilder(command);

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        int exitCode = -1;

        try {
            Process process = processBuilder.start();

            // 读取标准输出
            try (BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = stdoutReader.readLine()) != null) {
                    stdout.append(line).append("\n");
                }
            }

            // 读取标准错误
            try (BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = stderrReader.readLine()) != null) {
                    stderr.append(line).append("\n");
                }
            }

            // 等待命令执行完成
            if (process.waitFor(30, TimeUnit.SECONDS)) {
                exitCode = process.exitValue();
            } else {
                process.destroyForcibly();
                stderr.append("命令执行超时");
            }

        } catch (IOException | InterruptedException e) {
            logger.error("命令执行失败", e);
            stderr.append("执行出错: ").append(e.getMessage());
        }

        result.put("exitCode", exitCode);
        result.put("stdout", stdout.toString());
        result.put("stderr", stderr.toString());

        return result;
    }

    private ProcessBuilder createProcessBuilder(String command) {
        ProcessBuilder processBuilder;
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Windows
            processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            // Linux/Unix/Mac
            processBuilder = new ProcessBuilder("sh", "-c", command);
        }

        processBuilder.redirectErrorStream(false);
        return processBuilder;
    }
}
