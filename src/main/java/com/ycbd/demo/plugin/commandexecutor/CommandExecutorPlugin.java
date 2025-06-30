package com.ycbd.demo.plugin.commandexecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

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
    private CommandExecutionService service = new CommandExecutionService();

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
        logger.info("初始化命令执行器插件");

        ConfigurableApplicationContext configurableContext = (ConfigurableApplicationContext) applicationContext;
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) configurableContext.getBeanFactory();

        // 创建服务和控制器
        controller = new CommandExecutorController();
        controller.setCommandService(service);

        // 注册Bean
        configurableContext.getBeanFactory().registerSingleton("commandExecutionService", service);
        configurableContext.getBeanFactory().registerSingleton("commandExecutorController", controller);

        // 刷新映射
        RequestMappingHandlerMapping requestMappingHandlerMapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
        requestMappingHandlerMapping.getHandlerMethods().forEach((key, value)
                -> logger.debug("已映射: {}", key));

        try {
            // 使用service来执行命令
            logger.info("命令执行器准备就绪");
        } catch (Exception e) {
            logger.error("初始化命令执行器时出错", e);
        }

        logger.info("命令执行器插件初始化完成");
    }

    @Override
    public void shutdown() {
        logger.info("关闭命令执行器插件");
        // 清理资源
    }

    // 直接提供一个执行命令的方法，让控制器调用
    public Object executeCommand(String command) {
        logger.info("执行命令: {}", command);
        return service.executeCommand(command);
    }
}
