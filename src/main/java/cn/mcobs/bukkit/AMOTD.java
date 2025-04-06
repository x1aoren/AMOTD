package cn.mcobs.bukkit;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

import java.io.File;

public class AMOTD extends JavaPlugin {
    
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
        
        // 输出启动信息
        getLogger().info("AMOTD 插件已启用！");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("AMOTD 插件已禁用！");
    }
    
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        
        // 更新最大玩家数量
        updateMaxPlayers();
    }
    
    // 创建icons文件夹
    private void createIconsFolder() {
        File iconsFolder = new File(getDataFolder(), "icons");
        if (!iconsFolder.exists()) {
            iconsFolder.mkdirs();
            if (getConfig().getBoolean("debug", false)) {
                getLogger().info("已创建icons文件夹");
            }
        }
    }
    
    // 添加或修改updateMaxPlayers方法
    public void updateMaxPlayers() {
        if (getConfig().getBoolean("player_count.enabled", false) && 
            getConfig().getBoolean("player_count.apply_limit", false)) {
            int maxPlayers = getConfig().getInt("player_count.max_players", 100);
            getServer().setMaxPlayers(maxPlayers);
            if (getConfig().getBoolean("debug", false)) {
                getLogger().info("已更新真实最大人数限制: " + maxPlayers);
            }
        }
    }
    
    // 添加getter方法
    public MOTDListener getMotdListener() {
        return motdListener;
    }
} 