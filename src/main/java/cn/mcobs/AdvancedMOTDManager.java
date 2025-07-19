package cn.mcobs;

import org.bukkit.plugin.java.JavaPlugin;
import cn.mcobs.platform.PlatformDetector;
import cn.mcobs.platform.PlatformDetector.PlatformType;
import cn.mcobs.platform.PlatformManager;
import cn.mcobs.bukkit.AMOTD;
import cn.mcobs.folia.FoliaPlugin;

import java.lang.reflect.Method;

/**
 * 插件统一入口类，根据平台类型加载相应的实现
 */
public class AdvancedMOTDManager extends JavaPlugin {
    
    private JavaPlugin platformImplementation;
    private PlatformManager platformManager;
    
    @Override
    public void onEnable() {
        // 检测平台类型
        PlatformType platformType = PlatformDetector.detectPlatform();
        
        getLogger().info("检测到平台类型: " + platformType);
        
        // 创建平台管理器
        platformManager = new PlatformManager(this);
        
        // 根据平台类型加载相应的实现
        switch (platformType) {
            case FOLIA:
                getLogger().info("正在加载Folia版本的AMOTD...");
                loadFoliaImplementation();
                break;
                
            case PAPER:
            case BUKKIT:
                getLogger().info("正在加载Bukkit/Paper版本的AMOTD...");
                loadBukkitImplementation();
                break;
                
            case VELOCITY:
                getLogger().info("正在加载Velocity版本的AMOTD...");
                // Velocity实现在自己的主类中加载
                break;
                
            default:
                getLogger().severe("不支持的平台类型!");
                getServer().getPluginManager().disablePlugin(this);
                break;
        }
    }
    
    @Override
    public void onDisable() {
        // 如果平台实现已加载，调用其onDisable方法
        if (platformImplementation != null) {
            try {
                // 反射调用onDisable方法
                Method onDisableMethod = platformImplementation.getClass().getMethod("onDisable");
                onDisableMethod.invoke(platformImplementation);
            } catch (Exception e) {
                getLogger().severe("无法调用平台实现的onDisable方法: " + e.getMessage());
            }
        }
        
        getLogger().info("AMOTD插件已禁用");
    }
    
    /**
     * 加载Bukkit/Paper实现
     */
    private void loadBukkitImplementation() {
        try {
            // 创建Bukkit实现实例
            AMOTD bukkitPlugin = new AMOTD();
            
            // 使用反射设置必要的字段
            injectPluginFields(bukkitPlugin);
            
            // 调用onEnable方法
            bukkitPlugin.onEnable();
            
            // 保存实例
            platformImplementation = bukkitPlugin;
            
            getLogger().info("成功加载Bukkit/Paper版本的AMOTD");
        } catch (Exception e) {
            getLogger().severe("加载Bukkit/Paper实现失败: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    /**
     * 加载Folia实现
     */
    private void loadFoliaImplementation() {
        try {
            // 创建Folia实现实例
            FoliaPlugin foliaPlugin = new FoliaPlugin();
            
            // 使用反射设置必要的字段
            injectPluginFields(foliaPlugin);
            
            // 调用onEnable方法
            foliaPlugin.onEnable();
            
            // 保存实例
            platformImplementation = foliaPlugin;
            
            getLogger().info("成功加载Folia版本的AMOTD");
        } catch (Exception e) {
            getLogger().severe("加载Folia实现失败: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    /**
     * 注入插件字段
     * @param implementation 平台实现实例
     */
    private void injectPluginFields(JavaPlugin implementation) throws Exception {
        // 使用反射设置JavaPlugin必要的字段
        Class<?> pluginClass = JavaPlugin.class;
        
        // 设置logger
        Method getLoggerMethod = JavaPlugin.class.getDeclaredMethod("getLogger");
        Object logger = getLoggerMethod.invoke(this);
        
        // 设置dataFolder
        java.lang.reflect.Field dataFolderField = pluginClass.getDeclaredField("dataFolder");
        dataFolderField.setAccessible(true);
        dataFolderField.set(implementation, getDataFolder());
        
        // 设置pluginLoader
        java.lang.reflect.Field pluginLoaderField = pluginClass.getDeclaredField("loader");
        pluginLoaderField.setAccessible(true);
        pluginLoaderField.set(implementation, getPluginLoader());
        
        // 设置server
        java.lang.reflect.Field serverField = pluginClass.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(implementation, getServer());
        
        // 设置description
        java.lang.reflect.Field descriptionField = pluginClass.getDeclaredField("description");
        descriptionField.setAccessible(true);
        descriptionField.set(implementation, getDescription());
        
        // 设置configFile
        java.lang.reflect.Field configFileField = pluginClass.getDeclaredField("configFile");
        configFileField.setAccessible(true);
        configFileField.set(implementation, new java.io.File(getDataFolder(), "config.yml"));
    }
    
    /**
     * 获取平台管理器
     * @return 平台管理器
     */
    public PlatformManager getPlatformManager() {
        return platformManager;
    }
} 