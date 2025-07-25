package cn.mcobs.bukkit;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import cn.mcobs.utils.LanguageManager;

import java.io.File;

public class AMOTD extends JavaPlugin {
    
    private MOTDListener motdListener;
    private LanguageManager languageManager;
    
    @Override
    public void onEnable() {
        // 检测是否为Folia服务端
        boolean isFolia = isFoliaServer();
        if (isFolia) {
            getLogger().info(languageManager != null ? 
                languageManager.getMessage("folia_detected") : 
                "检测到Folia服务端，将使用线程安全API");
        }
        
        // 保存默认配置
        saveDefaultConfig();
        
        // 避免循环依赖 - 先加载配置
        super.reloadConfig();
        
        // 初始化语言管理器
        languageManager = new LanguageManager(this);
        
        // 明确设置语言
        String language = getConfig().getString("language", "en");
        languageManager.setLanguage(language);
        
        // 生成中文配置文件
        saveChineseConfig();
        
        // 检查配置文件是否存在，如果不存在则重新生成
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getLogger().warning(languageManager.getMessage("config_not_found"));
            saveDefaultConfig();
        }
        
        // 创建icons文件夹
        createIconsFolder();
        
        // 创建并注册事件监听器
        motdListener = new MOTDListener(this);
        getServer().getPluginManager().registerEvents(motdListener, this);
        
        // 创建命令执行器实例
        AMOTDCommand commandExecutor = new AMOTDCommand(this);
        
        // 注册命令和Tab补全器
        getCommand("amotd").setExecutor(commandExecutor);
        getCommand("amotd").setTabCompleter(commandExecutor);
        
        // 如果启用了真实人数限制，应用最大人数设置
        if (getConfig().getBoolean("player_count.enabled", false) && 
            getConfig().getBoolean("player_count.apply_limit", false)) {
            int maxPlayers = getConfig().getInt("player_count.max_players", 100);
            
            if (isFolia) {
                // 使用反射调用Folia的线程安全API
                try {
                    // 获取全局区域调度器
                    Class<?> serverClass = getServer().getClass();
                    Object scheduler = serverClass.getMethod("getGlobalRegionScheduler").invoke(getServer());
                    
                    // 执行任务
                    scheduler.getClass().getMethod("execute", JavaPlugin.class, Runnable.class)
                            .invoke(scheduler, this, (Runnable) () -> {
                                getServer().setMaxPlayers(maxPlayers);
                                String message = (languageManager != null) ? 
                                    languageManager.getMessage("max_players_updated", maxPlayers) : 
                                    "已更新真实最大人数限制: " + maxPlayers;
                                getLogger().info(message);
                            });
                } catch (Exception e) {
                    getLogger().warning(languageManager != null ? 
                        languageManager.getMessage("folia_api_error", e.getMessage()) : 
                        "无法使用Folia API设置最大玩家数: " + e.getMessage());
                    // 回退到传统方法
                    getServer().setMaxPlayers(maxPlayers);
                    String message = (languageManager != null) ? 
                        languageManager.getMessage("max_players_updated", maxPlayers) : 
                        "已更新真实最大人数限制: " + maxPlayers;
                    getLogger().info(message);
                }
            } else {
                // 传统方法
                getServer().setMaxPlayers(maxPlayers);
                String message = (languageManager != null) ? 
                    languageManager.getMessage("max_players_updated", maxPlayers) : 
                    "已更新真实最大人数限制: " + maxPlayers;
                getLogger().info(message);
            }
        }
        
        // 输出启动信息
        getLogger().info((languageManager != null) ? 
            languageManager.getMessage("plugin_enable") : 
            "AMOTD 插件已启用！");
    }
    
    @Override
    public void onDisable() {
        getLogger().info((languageManager != null) ? 
            languageManager.getMessage("plugin_disable") : 
            "AMOTD 插件已禁用！");
    }
    
    @Override
    public void reloadConfig() {
        // 先加载配置
        super.reloadConfig();
        
        // 然后再重新加载语言设置
        if (languageManager != null) {
            String language = getConfig().getString("language", "en");
            languageManager.setLanguage(language);
        }
        
        // 更新最大玩家数量
        updateMaxPlayers();
    }
    
    // 创建icons文件夹
    private void createIconsFolder() {
        File iconsFolder = new File(getDataFolder(), "icons");
        if (!iconsFolder.exists()) {
            iconsFolder.mkdirs();
            if (getConfig().getBoolean("debug", false)) {
                getLogger().info(languageManager.getMessage("icons_folder_created"));
            }
        }
    }
    
    // 添加或修改updateMaxPlayers方法
    public void updateMaxPlayers() {
        if (getConfig().getBoolean("player_count.enabled", false) && 
            getConfig().getBoolean("player_count.apply_limit", false)) {
            int maxPlayers = getConfig().getInt("player_count.max_players", 100);
            
            boolean isFolia = isFoliaServer();
            if (isFolia) {
                // 使用反射调用Folia的线程安全API
                try {
                    // 获取全局区域调度器
                    Class<?> serverClass = getServer().getClass();
                    Object scheduler = serverClass.getMethod("getGlobalRegionScheduler").invoke(getServer());
                    
                    // 执行任务
                    scheduler.getClass().getMethod("execute", JavaPlugin.class, Runnable.class)
                            .invoke(scheduler, this, (Runnable) () -> {
                                getServer().setMaxPlayers(maxPlayers);
                                if (getConfig().getBoolean("debug", false)) {
                                    getLogger().info(languageManager.getMessage("max_players_updated", maxPlayers));
                                }
                            });
                    return;
                } catch (Exception e) {
                    if (getConfig().getBoolean("debug", false)) {
                        getLogger().warning("无法使用Folia API设置最大玩家数: " + e.getMessage());
                    }
                    // 回退到传统方法
                }
            }
            
            // 传统方法
            getServer().setMaxPlayers(maxPlayers);
            if (getConfig().getBoolean("debug", false)) {
                getLogger().info(languageManager.getMessage("max_players_updated", maxPlayers));
            }
        }
    }
    
    // 添加getter方法
    public MOTDListener getMotdListener() {
        return motdListener;
    }
    
    // 获取语言管理器
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
    
    private void saveChineseConfig() {
        File chineseConfigFile = new File(getDataFolder(), "config_zh.yml");
        if (!chineseConfigFile.exists()) {
            saveResource("config_zh.yml", false);
        }
    }
    
    /**
     * 检测是否为Folia服务端
     * @return 是否为Folia服务端
     */
    private boolean isFoliaServer() {
        try {
            // 尝试加载Folia特有的类
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
} 