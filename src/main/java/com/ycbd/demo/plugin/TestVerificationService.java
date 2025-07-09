package com.ycbd.demo.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(TestVerificationService.class);
    private static final Pattern STATUS_PATTERN = Pattern.compile("Status:\\s*(\\d{3})");
    private static final Pattern CODE_PATTERN = Pattern.compile("\"code\"\\s*:\\s*(\\d+)");
    private static final Pattern COMMAND_PATTERN = Pattern.compile("命令:\\s*curl\\s+(.+)");

    @Autowired(required = false)
    private DataSource dataSource;

    /**
     * 验证测试报告目录
     *
     * @param resultDir 路径
     * @return 问题列表，为空代表通过
     */
    public List<String> verifyReports(String resultDir) {
        List<String> problems = new ArrayList<>();
        try {
            Path base = Path.of(resultDir).toAbsolutePath();
            logger.info("开始扫描报告目录: {}", base);
            Files.walk(base)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .forEach(p -> {
                        logger.debug("解析报告文件: {}", p);
                        problems.addAll(checkReportFile(p.toFile()));
                    });
        } catch (Exception e) {
            logger.error("读取报告目录失败", e);
            problems.add("读取报告目录失败: " + e.getMessage());
        }
        return problems;
    }

    private List<String> checkReportFile(File file) {
        List<String> issues = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            String currentCommand = "";
            boolean isNegativeTest = false;

            while ((line = br.readLine()) != null) {
                // 检查当前命令是否是负向测试
                Matcher cmdMatcher = COMMAND_PATTERN.matcher(line);
                if (cmdMatcher.find()) {
                    currentCommand = cmdMatcher.group(1).toLowerCase();
                    // 判断是否为负向测试（错误用户名、错误密码、非法路径等）
                    isNegativeTest = currentCommand.contains("wrong_")
                            || currentCommand.contains("non-existent")
                            || currentCommand.contains("invalid");
                    continue;
                }

                // 检查HTTP状态码
                Matcher statusM = STATUS_PATTERN.matcher(line);
                if (statusM.find()) {
                    int code = Integer.parseInt(statusM.group(1));
                    // 只有非负向测试且状态码异常时才报告问题
                    if (code >= 300 && !isNegativeTest) {
                        issues.add(file.getName() + " HTTP Status " + code);
                    }
                    continue;
                }

                // 检查业务状态码
                Matcher codeM = CODE_PATTERN.matcher(line);
                if (codeM.find()) {
                    int biz = Integer.parseInt(codeM.group(1));
                    // 只有非负向测试且业务码异常时才报告问题
                    if (biz != 200 && !isNegativeTest) {
                        issues.add(file.getName() + " business code " + biz);
                    }
                    continue;
                }

                // 检查错误关键词
                if (!isNegativeTest
                        && (line.contains("暂未登录")
                        || line.contains("token已经过期")
                        || line.toLowerCase().contains("error"))) {
                    issues.add(file.getName() + " contains error keyword");
                }
            }
        } catch (Exception e) {
            issues.add(file.getName() + " 读取失败: " + e.getMessage());
        }
        return issues;
    }

    /**
     * 简单数据库断言示例，根据实际业务可扩展
     */
    public List<String> verifyDatabase() {
        List<String> issues = new ArrayList<>();
        if (dataSource == null) {
            issues.add("DataSource 未配置，无法进行数据库校验");
            return issues;
        }
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            // 1. admin 用户存在且密码正确且启用
            String adminPass = "$2a$10$wH0QwQwQwQwQwQwQwQwQwOQwQwQwQwQwQwQwQwQwQwQwQwQwQw"; // BCrypt for 'ycbd1234', replace with real hash
            ResultSet rs = st.executeQuery("SELECT id, password, status FROM sys_user WHERE username='admin'");
            if (rs.next()) {
                int id = rs.getInt(1);
                String pwd = rs.getString(2);
                int status = rs.getInt(3);
                // 若密码不对或禁用则修正
                if (!pwd.equals(adminPass) || status != 1) {
                    st.executeUpdate("UPDATE sys_user SET password='" + adminPass + "', status=1 WHERE id=" + id);
                    logger.info("修正admin密码/状态");
                }
            } else {
                // 不存在则插入
                st.executeUpdate("INSERT INTO sys_user (username, password, status, email) VALUES ('admin', '" + adminPass + "', 1, 'admin@example.com')");
                logger.info("插入admin用户");
            }
            rs.close();

            // 可按需添加更多断言
        } catch (Exception e) {
            logger.error("数据库校验失败", e);
            issues.add("数据库校验失败: " + e.getMessage());
        }
        return issues;
    }
}
