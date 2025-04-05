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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
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
        
        // 处理人数显示
        if (plugin.getConfig().getBoolean("player_count.enabled", false)) {
            int maxPlayers = plugin.getConfig().getInt("player_count.max_players", 100);
            event.setMaxPlayers(maxPlayers);
        }
        
        // 处理玩家列表悬停文本
        boolean enableHoverText = plugin.getConfig().getBoolean("hover_player_list.enabled", true);
        if (!enableHoverText) {
            try {
                // 方法1: 使用Paper API
                try {
                    Class<?> paperEventClass = Class.forName("com.destroystokyo.paper.event.server.PaperServerListPingEvent");
                    if (paperEventClass.isInstance(event)) {
                        Method setHidePlayersMethod = paperEventClass.getMethod("setHidePlayers", boolean.class);
                        setHidePlayersMethod.invoke(event, true);
                        plugin.getLogger().info("使用Paper API隐藏玩家悬停列表");
                        return;
                    }
                } catch (Exception ignored) {}
                
                // 方法2: 使用反射修改原版实现
                // 获取event内部的ping对象
                Field pingField = event.getClass().getDeclaredField("ping");
                pingField.setAccessible(true);
                Object ping = pingField.get(event);
                
                // 获取ping内部的players对象
                Field playersField = ping.getClass().getDeclaredField("players");
                playersField.setAccessible(true);
                Object players = playersField.get(ping);
                
                // 将sample设置为空数组
                if (players != null) {
                    Field sampleField = players.getClass().getDeclaredField("sample");
                    sampleField.setAccessible(true);
                    sampleField.set(players, null);
                    plugin.getLogger().info("使用反射清空玩家悬停列表");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("隐藏玩家悬停列表失败: " + e.getMessage());
            }
        } else {
            // 自定义玩家悬停文本
            String emptyMessage = plugin.getConfig().getString("hover_player_list.empty_message", "目前没有玩家在线");
            
            // 检查是否有玩家在线
            if (plugin.getServer().getOnlinePlayers().isEmpty()) {
                try {
                    // 只在Paper服务器上尝试使用高级API
                    try {
                        Class<?> paperEventClass = Class.forName("com.destroystokyo.paper.event.server.PaperServerListPingEvent");
                        if (paperEventClass.isInstance(event)) {
                            // 获取PlayerSample
                            Method getPlayerSampleMethod = paperEventClass.getMethod("getPlayerSample");
                            List<?> sample = (List<?>) getPlayerSampleMethod.invoke(event);
                            
                            if (sample != null) {
                                // 清空现有列表
                                sample.clear();
                                
                                // 创建自定义空消息
                                try {
                                    // 使用Paper的Profile API
                                    Class<?> profileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
                                    Object profile = profileClass.getConstructor(UUID.class, String.class)
                                            .newInstance(UUID.randomUUID(), emptyMessage);
                                    
                                    // 添加到样本中
                                    Method addMethod = sample.getClass().getMethod("add", Object.class);
                                    addMethod.invoke(sample, profile);
                                    plugin.getLogger().info("使用Paper API设置空玩家列表消息");
                                } catch (Exception ignored) {
                                    // 回退到基本功能
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                } catch (Exception e) {
                    plugin.getLogger().warning("设置空玩家列表消息失败: " + e.getMessage());
                }
            }
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
     * 自定义玩家列表悬停文本
     * @param event Ping事件
     */
    private void customizePlayerHoverList(ServerListPingEvent event) {
        String customMessage = plugin.getConfig().getString("hover_player_list.custom_message", "在线玩家信息已隐藏");
        
        try {
            // 1. 尝试Paper API
            try {
                Class<?> paperEventClass = Class.forName("com.destroystokyo.paper.event.server.PaperServerListPingEvent");
                if (paperEventClass.isInstance(event)) {
                    // 使用反射方法添加元素，避免通配符类型问题
                    Method getSampleMethod = paperEventClass.getMethod("getPlayerSample");
                    List<?> originalSample = (List<?>) getSampleMethod.invoke(event);
                    
                    if (originalSample != null) {
                        // 清空原始列表
                        originalSample.clear();
                        
                        // 创建一个新的GameProfile作为自定义消息
                        Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
                        Constructor<?> constructor = gameProfileClass.getConstructor(UUID.class, String.class);
                        Object profile = constructor.newInstance(UUID.randomUUID(), customMessage);
                        
                        // 使用反射添加到样本中，避免类型不兼容问题
                        Method addMethod = originalSample.getClass().getMethod("add", Object.class);
                        addMethod.invoke(originalSample, profile);
                        plugin.getLogger().fine("使用Paper API自定义玩家列表");
                    }
                    return;
                }
            } catch (Exception ignored) {
                // 非Paper服务器
            }
            
            // 2. 尝试Spigot API
            try {
                Method getPlayersMethod = event.getClass().getMethod("getPlayers");
                List<?> players = (List<?>) getPlayersMethod.invoke(event);
                
                if (players != null) {
                    players.clear();
                    
                    try {
                        // 获取玩家样本类
                        Class<?> serverPingPlayerSampleClass = Class.forName("org.bukkit.craftbukkit.util.CraftChatMessage");
                        Method fromStringMethod = serverPingPlayerSampleClass.getMethod("fromString", String.class);
                        Object nameComponent = fromStringMethod.invoke(null, customMessage);
                        
                        // 创建一个伪玩家条目
                        Object playerEntry = Class.forName("org.bukkit.craftbukkit.v1_16_R3.CraftPlayerProfile")
                                .getConstructor(UUID.class, String.class)
                                .newInstance(UUID.randomUUID(), customMessage);
                        
                        // 使用反射添加元素，避免通配符类型问题
                        Method addMethod = players.getClass().getMethod("add", Object.class);
                        addMethod.invoke(players, playerEntry);
                    } catch (Exception e) {
                        // 如果不支持定制，至少保留一个空列表，避免问号
                    }
                    plugin.getLogger().fine("使用Spigot API自定义玩家列表");
                    return;
                }
            } catch (Exception ignored) {
                // 不支持此API
            }
            
            // 3. 尝试反射注入自定义文本 (兜底方案)
            Class<?> serverPingClass = event.getClass();
            try {
                // 获取ServerPing实例
                Field serverPingField = serverPingClass.getDeclaredField("ping");
                serverPingField.setAccessible(true);
                Object serverPing = serverPingField.get(event);
                
                // 获取玩家信息字段
                Field playersField = serverPing.getClass().getDeclaredField("players");
                playersField.setAccessible(true);
                Object playerSample = playersField.get(serverPing);
                
                if (playerSample != null) {
                    // 获取sample数组字段
                    Field sampleField = playerSample.getClass().getDeclaredField("sample");
                    sampleField.setAccessible(true);
                    
                    // 创建伪玩家信息数组
                    Constructor<?> gameProfileConstructor = Class.forName("com.mojang.authlib.GameProfile")
                            .getConstructor(UUID.class, String.class);
                    Object[] newSample = new Object[1];
                    newSample[0] = gameProfileConstructor.newInstance(UUID.randomUUID(), customMessage);
                    
                    // 设置新数组
                    sampleField.set(playerSample, newSample);
                    plugin.getLogger().fine("通过反射自定义玩家列表");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("无法自定义玩家列表：" + e.getMessage());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("自定义玩家列表失败: " + e.getMessage());
        }
    }
} 