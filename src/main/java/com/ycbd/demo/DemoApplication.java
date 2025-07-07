package com.ycbd.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.ycbd.demo.plugin.PluginEngine;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class DemoApplication {

    @Autowired
    private PluginEngine pluginEngine;

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initPlugins() {
        // 应用启动后初始化系统内置插件
        pluginEngine.initializeBuiltinPlugins();
    }
}
