package cn.mcobs;

import org.bukkit.plugin.java.JavaPlugin;

public final class AMOTD extends JavaPlugin {

    @Override
    public void onEnable() {
        // 保存默认配置
        saveDefaultConfig();
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new MOTDListener(this), this);
        
        // 输出启动信息
        getLogger().info("AMOTD 插件已启用！");
    }

    @Override
    public void onDisable() {
        // 输出关闭信息
        getLogger().info("AMOTD 插件已禁用！");
    }
} 