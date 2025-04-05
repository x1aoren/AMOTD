package cn.mcobs;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

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
        
        // 处理玩家列表悬停文本
        if (plugin.getConfig().getBoolean("hover_player_list.enabled", true)) {
            // 保留原始玩家列表，以便在悬停时显示
            // 不做任何修改，默认会显示所有在线玩家
        } else {
            // 使用已经定义好的辅助方法处理不同服务器的兼容性
            setCustomPlayerSample(event, false);
        }
        
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
    
    /**
     * 尝试使用扩展API设置自定义玩家列表悬停文本
     * @param event Ping事件
     * @param enabled 是否启用悬停文本
     */
    private void setCustomPlayerSample(ServerListPingEvent event, boolean enabled) {
        if (!enabled) {
            try {
                // 尝试Paper API
                Class<?> paperServerListPingEventClass = Class.forName("com.destroystokyo.paper.event.server.PaperServerListPingEvent");
                if (paperServerListPingEventClass.isInstance(event)) {
                    Method setHidePlayers = paperServerListPingEventClass.getMethod("setHidePlayers", boolean.class);
                    setHidePlayers.invoke(event, true);
                    return;
                }
                
                // 尝试通过反射调用setPlayerSample方法
                Method setPlayerSampleMethod = event.getClass().getMethod("setPlayerSample", List.class);
                setPlayerSampleMethod.invoke(event, new ArrayList<>());
            } catch (Exception ignored) {
                // 不支持扩展API，使用基本Bukkit功能
                plugin.getLogger().fine("此服务器不支持高级玩家列表自定义功能");
            }
        } else {
            try {
                // 检查是否有getOnlinePlayers方法
                Method getOnlinePlayersMethod = event.getClass().getMethod("getOnlinePlayers");
                List<?> players = (List<?>) getOnlinePlayersMethod.invoke(event);
                
                // 如果没有玩家在线并且支持自定义玩家列表
                if (players.isEmpty()) {
                    String emptyMessage = plugin.getConfig().getString("hover_player_list.empty_message", "目前没有玩家在线");
                    
                    // 尝试Paper API设置自定义文本
                    try {
                        // 创建一个虚拟的玩家配置文件
                        Constructor<?> profileConstructor = Class.forName("com.mojang.authlib.GameProfile").getConstructor(UUID.class, String.class);
                        Object profile = profileConstructor.newInstance(UUID.randomUUID(), emptyMessage);
                        
                        // 创建自定义玩家样本
                        List<Object> playersList = new ArrayList<>();
                        playersList.add(profile);
                        
                        // 通过反射设置
                        Method setPlayerSample = event.getClass().getMethod("setPlayerSample", List.class);
                        setPlayerSample.invoke(event, playersList);
                    } catch (Exception e) {
                        // 不支持，使用默认行为
                    }
                }
            } catch (Exception ignored) {
                // 此服务器不支持getOnlinePlayers方法
            }
        }
    }
} 