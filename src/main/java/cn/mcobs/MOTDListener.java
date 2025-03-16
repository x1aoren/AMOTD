package cn.mcobs;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

public class MOTDListener implements Listener {
    
    private final AMOTD plugin;
    
    public MOTDListener(AMOTD plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onServerPing(ServerListPingEvent event) {
        // 从配置文件中读取两行MOTD
        String line1 = plugin.getConfig().getString("line1", "默认的第一行MOTD");
        String line2 = plugin.getConfig().getString("line2", "默认的第二行MOTD");
        
        // 转换颜色代码
        line1 = ChatColor.translateAlternateColorCodes('&', line1);
        line2 = ChatColor.translateAlternateColorCodes('&', line2);
        
        // 设置MOTD
        event.setMotd(line1 + "\n" + line2);
    }
} 