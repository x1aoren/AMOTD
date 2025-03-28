package cn.mcobs;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public final class AMOTD extends JavaPlugin {

    private MOTDListener motdListener;

    @Override
    public void onEnable() {
        // 保存默认配置
        saveDefaultConfig();
        
        // 检查配置文件是否存在，如果不存在则重新生成
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getLogger().warning("未找到配置文件，正在重新生成默认配置...");
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
            getServer().setMaxPlayers(maxPlayers);
            getLogger().info("已应用真实最大人数限制: " + maxPlayers);
        }
        
        // 检查和警告配置兼容性问题
        checkConfigCompatibility();
        
        // 输出启动信息
        getLogger().info("AMOTD 插件已启用！");
    }

    @Override
    public void onDisable() {
        // 输出关闭信息
        getLogger().info("AMOTD 插件已禁用！");
    }
    
    /**
     * 获取MOTD监听器实例
     */
    public MOTDListener getMotdListener() {
        return motdListener;
    }
    
    /**
     * 创建icons文件夹
     */
    private void createIconsFolder() {
        File iconsFolder = new File(getDataFolder(), "icons");
        if (!iconsFolder.exists()) {
            iconsFolder.mkdirs();
            getLogger().info("已创建icons文件夹");
        }
    }

    // 检查和警告配置兼容性问题
    public void checkConfigCompatibility() {
        String formatType = getConfig().getString("message_format", "legacy");
        boolean useMinimessage = "minimessage".equalsIgnoreCase(formatType);
        
        if (useMinimessage) {
            boolean isPaper = false;
            try {
                Class.forName("io.papermc.paper.text.PaperComponents");
                isPaper = true;
            } catch (ClassNotFoundException e) {
                // 不是Paper服务器
            }
            
            if (!isPaper) {
                getLogger().warning("警告: 当前配置使用MiniMessage格式，但服务器不是Paper。某些功能可能受到限制。");
                getLogger().warning("建议对低版本服务器使用legacy格式，或升级到Paper服务器以获得全部功能。");
            }
        }
    }

    // 添加更新最大人数的方法，供重载配置时使用
    public void updateMaxPlayers() {
        if (getConfig().getBoolean("player_count.enabled", false) && 
            getConfig().getBoolean("player_count.apply_limit", false)) {
            int maxPlayers = getConfig().getInt("player_count.max_players", 100);
            getServer().setMaxPlayers(maxPlayers);
            getLogger().info("已更新真实最大人数限制: " + maxPlayers);
        }
    }
} 