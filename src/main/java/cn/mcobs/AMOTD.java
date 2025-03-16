package cn.mcobs;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class AMOTD extends JavaPlugin {

    private MOTDListener motdListener;

    @Override
    public void onEnable() {
        // 保存默认配置
        saveDefaultConfig();
        
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
} 