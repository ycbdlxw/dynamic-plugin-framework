package com.ycbd.demo.plugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.ycbd.demo.service.BaseService;

@Component
public class PluginEngine implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(PluginEngine.class);

    private ApplicationContext applicationContext;
    private final Map<String, IPlugin> loadedPlugins = new ConcurrentHashMap<>();
    private final Map<String, URLClassLoader> pluginClassLoaders = new ConcurrentHashMap<>();

    @Autowired
    private BaseService baseService;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 获取已加载的插件
     */
    public IPlugin getPlugin(String pluginName) {
        return loadedPlugins.get(pluginName);
    }

    public String loadPlugin(String pluginName) {
        try {
            // 1. 检查插件是否已加载
            if (loadedPlugins.containsKey(pluginName)) {
                return "插件 " + pluginName + " 已经加载，无需重复加载";
            }

            // 2. 从数据库获取插件配置
            Map<String, Object> params = new HashMap<>();
            params.put("plugin_name", pluginName);
            Map<String, Object> pluginConfig = baseService.getOne("plugin_config", params);

            if (pluginConfig == null) {
                return "插件 " + pluginName + " 配置不存在";
            }

            Boolean isActive = (Boolean) pluginConfig.get("is_active");
            if (isActive == null || !isActive) {
                return "插件 " + pluginName + " 未激活";
            }

            String className = (String) pluginConfig.get("class_name");
            return loadPluginByClassName(className);

        } catch (Exception e) {
            logger.error("加载插件失败: " + pluginName, e);
            return "加载插件失败: " + e.getMessage();
        }
    }

    /**
     * 初始化系统内置插件 在系统启动时调用，加载所有激活的插件
     */
    public void initializeBuiltinPlugins() {
        try {
            logger.info("初始化系统内置插件");

            // 从数据库获取所有激活的插件配置
            Map<String, Object> params = new HashMap<>();
            params.put("is_active", true);
            List<Map<String, Object>> activePlugins = baseService.queryList("plugin_config", 0, 100, null, params, null, null, null);

            if (activePlugins == null || activePlugins.isEmpty()) {
                logger.info("没有找到激活的插件配置");
                return;
            }

            for (Map<String, Object> plugin : activePlugins) {
                String pluginName = (String) plugin.get("plugin_name");
                String className = (String) plugin.get("class_name");

                try {
                    logger.info("加载内置插件: {}", pluginName);
                    loadPluginByClassName(className);
                } catch (Exception e) {
                    logger.error("加载内置插件失败: " + pluginName, e);
                }
            }
        } catch (Exception e) {
            logger.error("初始化系统内置插件失败", e);
        }
    }

    private String loadPluginByClassName(String className) {
        try {
            // 1. 尝试从应用类加载器加载（内置插件）
            Class<?> pluginClass;
            try {
                pluginClass = getClass().getClassLoader().loadClass(className);
                logger.info("从应用类加载器加载插件类: {}", className);
            } catch (ClassNotFoundException e) {
                // 2. 如果内置插件加载失败，尝试从插件目录加载
                logger.info("应用类加载器中未找到类 {}，尝试从插件目录加载", className);

                // 创建插件目录
                File pluginDir = new File("plugins");
                if (!pluginDir.exists() || !pluginDir.isDirectory()) {
                    if (!pluginDir.mkdirs()) {
                        return "插件目录不存在且无法创建";
                    }
                }

                URL[] urls = new URL[]{pluginDir.toURI().toURL()};
                try (URLClassLoader classLoader = new URLClassLoader(urls, getClass().getClassLoader())) {
                    pluginClass = classLoader.loadClass(className);
                }
            }

            // 3. 实例化插件
            IPlugin plugin = (IPlugin) pluginClass.getDeclaredConstructor().newInstance();

            // 4. 注入依赖
            AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
            beanFactory.autowireBean(plugin);

            // 5. 主动注入 ApplicationContext 以避免插件内部无法获取
            if (plugin instanceof ApplicationContextAware aware) {
                aware.setApplicationContext(applicationContext);
            }

            // 6. 初始化插件
            plugin.initialize();
            String pluginName = plugin.getName();
            loadedPlugins.put(pluginName, plugin);

            // 7. 只有外部插件才需要保存类加载器
            if (pluginClass.getClassLoader() instanceof URLClassLoader) {
                pluginClassLoaders.put(pluginName, (URLClassLoader) pluginClass.getClassLoader());
            }

            return "插件 " + pluginName + " 加载成功";
        } catch (Exception e) {
            logger.error("加载插件失败: " + className, e);
            return "加载插件失败: " + e.getMessage();
        }
    }

    public String unloadPlugin(String pluginName) {
        try {
            IPlugin plugin = loadedPlugins.get(pluginName);
            if (plugin == null) {
                return "插件 " + pluginName + " 未加载";
            }

            // 1. 关闭插件
            plugin.shutdown();

            // 2. 移除插件
            loadedPlugins.remove(pluginName);

            // 3. 关闭类加载器
            URLClassLoader classLoader = pluginClassLoaders.get(pluginName);
            if (classLoader != null) {
                classLoader.close();
                pluginClassLoaders.remove(pluginName);
            }

            return "插件 " + pluginName + " 卸载成功";
        } catch (Exception e) {
            logger.error("卸载插件失败: " + pluginName, e);
            return "卸载插件失败: " + e.getMessage();
        }
    }
}
