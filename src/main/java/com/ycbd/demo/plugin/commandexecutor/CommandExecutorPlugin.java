package com.ycbd.demo.plugin.commandexecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ycbd.demo.plugin.IPlugin;

/**
 * 命令行执行插件，提供跨平台的命令执行能力。
 */
@Component
public class CommandExecutorPlugin implements IPlugin, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(CommandExecutorPlugin.class);

    @Autowired
    private ApplicationContext applicationContext;

    private CommandExecutorController controller;
    private CommandExecutionService commandService;
    private boolean isInitialized = false;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public String getName() {
        return "CommandExecutor";
    }

    @Override
    public void initialize() {
        if (isInitialized) {
            logger.info("命令执行器插件已初始化，跳过");
            return;
        }

        logger.info("初始化命令执行器插件");

        // 创建服务和控制器
        commandService = new CommandExecutionService();
        controller = new CommandExecutorController();
        controller.setCommandService(commandService);

        try {
            ConfigurableApplicationContext configurableContext = (ConfigurableApplicationContext) applicationContext;

            // 注册Bean
            if (!configurableContext.getBeanFactory().containsSingleton("commandExecutionService")) {
                configurableContext.getBeanFactory().registerSingleton("commandExecutionService", commandService);
                logger.info("注册commandExecutionService成功");
            }

            if (!configurableContext.getBeanFactory().containsSingleton("commandExecutorController")) {
                configurableContext.getBeanFactory().registerSingleton("commandExecutorController", controller);
                logger.info("注册commandExecutorController成功");

                // 注意：Spring Boot会自动扫描并注册标有@RestController的类
                // 由于我们是动态注册的Bean，可能需要手动触发映射更新
                // 这里不再尝试手动更新映射，而是依赖Spring的自动扫描机制
                logger.info("控制器已注册，请求路径: {}",
                        controller.getClass().getAnnotation(RequestMapping.class).value()[0]);
            }

            isInitialized = true;
            logger.info("命令执行器准备就绪");
        } catch (Exception e) {
            logger.error("初始化命令执行器时出错", e);
        }

        logger.info("命令执行器插件初始化完成");
    }

    @Override
    public void shutdown() {
        logger.info("关闭命令执行器插件");
        isInitialized = false;
    }

    public CommandExecutionService getCommandService() {
        if (commandService == null) {
            commandService = new CommandExecutionService();
        }
        return commandService;
    }

    public CommandExecutorController getController() {
        return controller;
    }

    // 直接提供一个执行命令的方法，让控制器调用
    public Object executeCommand(String command) {
        logger.info("执行命令: {}", command);
        if (commandService == null) {
            logger.error("命令服务为空，重新初始化");
            commandService = new CommandExecutionService();
        }
        return commandService.executeCommand(command);
    }
}
