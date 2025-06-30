package com.ycbd.demo.plugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
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

            // 特殊处理TestService插件（直接加载，避免数据库查询）
            if ("TestService".equals(pluginName)) {
                return loadTestServicePlugin();
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

    private String loadTestServicePlugin() {
        try {
            // 直接加载TestServicePlugin类
            Class<?> pluginClass = TestServicePlugin.class;
            IPlugin plugin = (TestServicePlugin) pluginClass.getDeclaredConstructor().newInstance();

            // 注入依赖
            AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
            beanFactory.autowireBean(plugin);

            // 若插件实现 ApplicationContextAware，则手动注入 ApplicationContext，
            // 解决通过反射实例化导致 setApplicationContext 未被调用的问题
            if (plugin instanceof ApplicationContextAware aware) {
                aware.setApplicationContext(applicationContext);
            }

            // 初始化插件
            plugin.initialize();
            loadedPlugins.put("TestService", plugin);

            return "插件 TestService 加载成功";
        } catch (Exception e) {
            logger.error("加载TestService插件失败", e);
            return "加载TestService插件失败: " + e.getMessage();
        }
    }

    private String loadPluginByClassName(String className) {
        try {
            // 3. 创建类加载器
            File pluginDir = new File("plugins");
            if (!pluginDir.exists() || !pluginDir.isDirectory()) {
                // 尝试创建插件目录
                if (!pluginDir.mkdirs()) {
                    return "插件目录不存在且无法创建";
                }
            }

            URL[] urls = new URL[]{pluginDir.toURI().toURL()};
            URLClassLoader classLoader = new URLClassLoader(urls, getClass().getClassLoader());

            // 4. 加载插件主类
            Class<?> pluginClass;
            try {
                // 尝试从插件目录加载
                pluginClass = classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                // 如果插件目录中不存在，则尝试从应用类加载器加载
                logger.info("插件目录中未找到类 {}，尝试从应用类加载器加载", className);
                pluginClass = getClass().getClassLoader().loadClass(className);
            }

            IPlugin plugin = (IPlugin) pluginClass.getDeclaredConstructor().newInstance();

            // 5. 注入依赖
            AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
            beanFactory.autowireBean(plugin);

            // 主动注入 ApplicationContext 以避免插件内部无法获取
            if (plugin instanceof ApplicationContextAware aware) {
                aware.setApplicationContext(applicationContext);
            }

            // 6. 初始化插件
            plugin.initialize();
            String pluginName = plugin.getName();
            loadedPlugins.put(pluginName, plugin);
            pluginClassLoaders.put(pluginName, classLoader);

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
