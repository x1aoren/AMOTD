package cn.mcobs;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MOTDListener implements Listener {
    
    private final AMOTD plugin;
    private List<CachedServerIcon> serverIcons;
    private Random random;
    private final AdvancedMOTDManager motdManager;
    
    public MOTDListener(AMOTD plugin) {
        this.plugin = plugin;
        this.serverIcons = new ArrayList<>();
        this.random = new Random();
        this.motdManager = new AdvancedMOTDManager(plugin);
        // 加载服务器图标
        loadServerIcons();
    }
    
    @EventHandler
    public void onServerPing(ServerListPingEvent event) {
        // 获取消息格式类型
        String formatType = plugin.getConfig().getString("message_format", "legacy");
        boolean useMinimessage = "minimessage".equalsIgnoreCase(formatType);
        
        String line1, line2;
        
        if (useMinimessage) {
            // 从minimessage部分获取配置
            line1 = plugin.getConfig().getString("minimessage.line1", "<green>默认的第一行MOTD</green>");
            line2 = plugin.getConfig().getString("minimessage.line2", "<yellow>默认的第二行MOTD</yellow>");
        } else {
            // 从legacy部分获取配置
            line1 = plugin.getConfig().getString("legacy.line1", "&a默认的第一行MOTD");
            line2 = plugin.getConfig().getString("legacy.line2", "&e默认的第二行MOTD");
        }
        
        // 使用新的管理器处理MOTD
        line1 = motdManager.processMOTD(line1, useMinimessage);
        line2 = motdManager.processMOTD(line2, useMinimessage);
        
        // 设置MOTD
        event.setMotd(line1 + "\n" + line2);
        
        // 添加人数修改功能
        if (plugin.getConfig().getBoolean("player_count.enabled", false)) {
            int maxPlayers = plugin.getConfig().getInt("player_count.max_players", 100);
            event.setMaxPlayers(maxPlayers);
        }
        
        // 处理服务器图标
        if (plugin.getConfig().getBoolean("enable_server_icon", true) && !serverIcons.isEmpty()) {
            try {
                // 随机选择一个图标
                CachedServerIcon icon = serverIcons.get(random.nextInt(serverIcons.size()));
                event.setServerIcon(icon);
            } catch (Exception e) {
                plugin.getLogger().warning("设置服务器图标时出错: " + e.getMessage());
            }
        }
    }
    
    /**
     * 加载服务器图标
     */
    private void loadServerIcons() {
        // 清空当前图标列表
        serverIcons.clear();
        
        // 检查是否启用了图标功能
        if (!plugin.getConfig().getBoolean("enable_server_icon", true)) {
            return;
        }
        
        // 获取icons文件夹
        File iconsFolder = new File(plugin.getDataFolder(), "icons");
        if (!iconsFolder.exists() || !iconsFolder.isDirectory()) {
            return;
        }
        
        // 获取所有png文件
        File[] iconFiles = iconsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (iconFiles == null || iconFiles.length == 0) {
            plugin.getLogger().info("没有找到PNG图标文件");
            return;
        }
        
        // 加载每个图标
        for (File iconFile : iconFiles) {
            try {
                BufferedImage image = ImageIO.read(iconFile);
                CachedServerIcon icon = plugin.getServer().loadServerIcon(image);
                serverIcons.add(icon);
                plugin.getLogger().info("已加载图标: " + iconFile.getName());
            } catch (Exception e) {
                plugin.getLogger().warning("加载图标时出错 " + iconFile.getName() + ": " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("成功加载了 " + serverIcons.size() + " 个服务器图标");
    }
    
    /**
     * 重新加载服务器图标
     */
    public void reloadServerIcons() {
        loadServerIcons();
    }
} 