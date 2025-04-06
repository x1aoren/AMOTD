package cn.mcobs.bukkit;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import cn.mcobs.SimpleMiniMessage;
import cn.mcobs.bukkit.BukkitMiniMessageHandler;

public class MOTDListener implements Listener {
    
    private final AMOTD plugin;
    
    public MOTDListener(AMOTD plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onServerPing(ServerListPingEvent event) {
        // 获取消息格式类型
        String formatType = plugin.getConfig().getString("message_format", "legacy");
        boolean useMinimessage = "minimessage".equalsIgnoreCase(formatType);
        
        String line1, line2;
        
        // 检测服务器版本
        boolean isLegacyVersion = isLegacyVersion();
        
        if (useMinimessage) {
            // 从minimessage部分获取配置
            line1 = plugin.getConfig().getString("minimessage.line1", "<green>默认的第一行MOTD</green>");
            line2 = plugin.getConfig().getString("minimessage.line2", "<yellow>默认的第二行MOTD</yellow>");
            
            // 根据版本使用不同的解析方法
            if (isLegacyVersion) {
                // 低版本使用SimpleMiniMessage
                line1 = SimpleMiniMessage.parseMiniMessage(line1);
                line2 = SimpleMiniMessage.parseMiniMessage(line2);
            } else {
                // 高版本使用新的处理类
                line1 = BukkitMiniMessageHandler.parse(line1);
                line2 = BukkitMiniMessageHandler.parse(line2);
            }
        } else {
            // 从legacy部分获取配置
            line1 = plugin.getConfig().getString("legacy.line1", "&a默认的第一行MOTD");
            line2 = plugin.getConfig().getString("legacy.line2", "&e默认的第二行MOTD");
            
            // 颜色代码转换在任何版本都能工作
            line1 = line1.replace('&', '§');
            line2 = line2.replace('&', '§');
        }
        
        // 设置MOTD
        event.setMotd(line1 + "\n" + line2);
        
        // 其余代码保持不变...
    }

    // 检测是否为旧版本
    private boolean isLegacyVersion() {
        String version = plugin.getServer().getBukkitVersion();
        return version.contains("1.8") || version.contains("1.9") || 
               version.contains("1.10") || version.contains("1.11") || 
               version.contains("1.12") || version.contains("1.13") || 
               version.contains("1.14") || version.contains("1.15");
    }
} 