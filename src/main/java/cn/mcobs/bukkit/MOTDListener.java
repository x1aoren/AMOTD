package cn.mcobs.bukkit;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import cn.mcobs.SimpleMiniMessage;
import cn.mcobs.bukkit.BukkitMiniMessageHandler;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

public class MOTDListener implements Listener {
    
    private final AMOTD plugin;
    
    private List<org.bukkit.util.CachedServerIcon> serverIcons = new ArrayList<>();
    private final Random random = new Random();
    
    public MOTDListener(AMOTD plugin) {
        this.plugin = plugin;
        this.reloadServerIcons(); // 初始化时加载图标
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
            
            // 使用处理类解析MiniMessage格式
            line1 = BukkitMiniMessageHandler.parse(line1);
            line2 = BukkitMiniMessageHandler.parse(line2);
        } else {
            // 从legacy部分获取配置
            line1 = plugin.getConfig().getString("legacy.line1", "&a默认的第一行MOTD");
            line2 = plugin.getConfig().getString("legacy.line2", "&e默认的第二行MOTD");
            
            // 使用处理类解析传统格式
            line1 = BukkitMiniMessageHandler.parseLegacy(line1);
            line2 = BukkitMiniMessageHandler.parseLegacy(line2);
        }
        
        // 设置MOTD
        event.setMotd(line1 + "\n" + line2);
        
        // 修复1: 处理人数显示 - 从Velocity版本移植
        if (plugin.getConfig().getBoolean("player_count.enabled", false)) {
            int maxPlayers = plugin.getConfig().getInt("player_count.max_players", 100);
            event.setMaxPlayers(maxPlayers);
            
            // 如果需要应用真实限制
            if (plugin.getConfig().getBoolean("player_count.apply_limit", false)) {
                plugin.getServer().setMaxPlayers(maxPlayers);
            }
        }
        
        // 修复处理玩家列表悬停文本
        boolean enableHoverText = plugin.getConfig().getBoolean("hover_player_list.enabled", true);
        if (!enableHoverText) {
            try {
                // 尝试使用Paper API (如果可用)
                try {
                    Class<?> paperPingClass = Class.forName("com.destroystokyo.paper.event.server.PaperServerListPingEvent");
                    if (paperPingClass.isInstance(event)) {
                        Method hidePlayersMethod = paperPingClass.getMethod("setHidePlayers", boolean.class);
                        hidePlayersMethod.invoke(event, true);
                        return;
                    }
                } catch (ClassNotFoundException ignored) {
                    // 不是Paper服务器
                }
                
                // 使用反射清除玩家样本
                clearPlayerSamples(event);
            } catch (Exception e) {
                plugin.getLogger().warning("隐藏玩家列表失败: " + e.getMessage());
            }
        } else if (plugin.getServer().getOnlinePlayers().isEmpty()) {
            // 没有玩家在线时显示自定义消息
            String emptyMessage = plugin.getConfig().getString("hover_player_list.empty_message", "目前没有玩家在线");
            try {
                // 尝试Paper API (如果可用)
                try {
                    Class<?> paperPingClass = Class.forName("com.destroystokyo.paper.event.server.PaperServerListPingEvent");
                    if (paperPingClass.isInstance(event)) {
                        Method getPlayersMethod = paperPingClass.getMethod("getPlayerSample");
                        List<?> players = (List<?>) getPlayersMethod.invoke(event);
                        players.clear();
                        
                        // 修复: 使用正确的方式创建PlayerProfile
                        try {
                            // 尝试使用createProfile静态方法
                            Class<?> serverClass = Class.forName("org.bukkit.Bukkit");
                            Method createProfileMethod = serverClass.getMethod("createProfile", UUID.class, String.class);
                            Object profile = createProfileMethod.invoke(null, UUID.randomUUID(), emptyMessage);
                            
                            // 使用反射调用add方法
                            Method addMethod = players.getClass().getMethod("add", Object.class);
                            addMethod.invoke(players, profile);
                            return;
                        } catch (Exception e) {
                            // 如果createProfile方法不可用，直接使用GameProfile
                            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
                            Constructor<?> constructor = gameProfileClass.getConstructor(UUID.class, String.class);
                            Object gameProfile = constructor.newInstance(UUID.randomUUID(), emptyMessage);
                            
                            Method addMethod = players.getClass().getMethod("add", Object.class);
                            addMethod.invoke(players, gameProfile);
                            return;
                        }
                    }
                } catch (ClassNotFoundException ignored) {
                    // 不是Paper服务器
                }
                
                // 使用直接的GameProfile方法
                setCustomPlayerSample(event, emptyMessage);
            } catch (Exception e) {
                plugin.getLogger().warning("设置自定义玩家列表消息失败: " + e.getMessage());
            }
        }
        
        // 处理服务器图标
        if (plugin.getConfig().getBoolean("enable_server_icon", true) && !serverIcons.isEmpty()) {
            try {
                // 随机选择一个图标
                org.bukkit.util.CachedServerIcon icon = serverIcons.get(random.nextInt(serverIcons.size()));
                event.setServerIcon(icon);
            } catch (Exception e) {
                plugin.getLogger().warning("设置服务器图标时出错: " + e.getMessage());
            }
        }
    }

    // 新增辅助方法清除玩家样本
    private void clearPlayerSamples(ServerListPingEvent event) throws Exception {
        // 获取ServerPing对象
        Object serverPing = getServerPingObject(event);
        if (serverPing == null) return;
        
        // 获取players对象
        Object players = getPlayersObject(serverPing);
        if (players == null) return;
        
        // 获取sample字段
        Field sampleField = findFieldByName(players.getClass(), "sample", "c");
        if (sampleField != null) {
            sampleField.setAccessible(true);
            // 创建空数组
            Object emptyArray = java.lang.reflect.Array.newInstance(
                    sampleField.getType().getComponentType(), 0);
            // 设置为空数组
            sampleField.set(players, emptyArray);
        }
    }

    // 新增辅助方法设置自定义玩家样本
    private void setCustomPlayerSample(ServerListPingEvent event, String message) throws Exception {
        // 获取ServerPing对象
        Object serverPing = getServerPingObject(event);
        if (serverPing == null) return;
        
        // 获取players对象
        Object players = getPlayersObject(serverPing);
        if (players == null) return;
        
        // 创建GameProfile
        Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
        Constructor<?> constructor = gameProfileClass.getConstructor(UUID.class, String.class);
        Object profile = constructor.newInstance(UUID.randomUUID(), message);
        
        // 获取sample字段
        Field sampleField = findFieldByName(players.getClass(), "sample", "c");
        if (sampleField != null) {
            sampleField.setAccessible(true);
            
            // 创建数组并设置
            Object array = java.lang.reflect.Array.newInstance(
                    sampleField.getType().getComponentType(), 1);
            java.lang.reflect.Array.set(array, 0, profile);
            sampleField.set(players, array);
        }
    }

    // 获取ServerPing对象
    private Object getServerPingObject(ServerListPingEvent event) throws Exception {
        // 直接找ping字段
        Field pingField = findFieldByName(event.getClass(), "ping", "a");
        if (pingField != null) {
            pingField.setAccessible(true);
            return pingField.get(event);
        }
        
        // 尝试getData方法
        try {
            Method getDataMethod = event.getClass().getMethod("getServerData");
            return getDataMethod.invoke(event);
        } catch (NoSuchMethodException e) {
            // 尝试所有字段
            for (Field field : event.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(event);
                if (value != null && value.getClass().getName().contains("ServerPing")) {
                    return value;
                }
            }
        }
        return null;
    }

    // 获取Players对象
    private Object getPlayersObject(Object serverPing) throws Exception {
        // 直接尝试players字段
        Field playersField = findFieldByName(serverPing.getClass(), "players", "b");
        if (playersField != null) {
            playersField.setAccessible(true);
            return playersField.get(serverPing);
        }
        
        // 尝试所有字段
        for (Field field : serverPing.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(serverPing);
            if (value != null && (
                field.getName().toLowerCase().contains("player") ||
                (value.getClass().getName().contains("Player") && 
                 !value.getClass().getName().contains("Count")))) {
                return value;
            }
        }
        return null;
    }

    // 通过名称查找字段 (尝试多个可能的名称)
    private Field findFieldByName(Class<?> clazz, String... possibleNames) {
        for (String name : possibleNames) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    // 检测是否为旧版本
    private boolean isLegacyVersion() {
        String version = plugin.getServer().getBukkitVersion();
        return version.contains("1.8") || version.contains("1.9") || 
               version.contains("1.10") || version.contains("1.11") || 
               version.contains("1.12") || version.contains("1.13") || 
               version.contains("1.14") || version.contains("1.15");
    }

    public void reloadServerIcons() {
        // 清空当前图标列表
        serverIcons.clear();
        
        // 检查是否启用了图标功能
        if (!plugin.getConfig().getBoolean("enable_server_icon", true)) {
            return;
        }
        
        // 获取icons文件夹
        File iconsFolder = new File(plugin.getDataFolder(), "icons");
        if (!iconsFolder.exists() || !iconsFolder.isDirectory()) {
            plugin.getLogger().warning("图标文件夹不存在或不是文件夹");
            return;
        }
        
        // 加载所有png文件
        File[] iconFiles = iconsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (iconFiles == null || iconFiles.length == 0) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("没有找到图标文件");
            }
            return;
        }
        
        for (File iconFile : iconFiles) {
            try {
                org.bukkit.util.CachedServerIcon icon = plugin.getServer().loadServerIcon(iconFile);
                serverIcons.add(icon);
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("已加载图标: " + iconFile.getName());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("加载图标时出错 " + iconFile.getName() + ": " + e.getMessage());
            }
        }
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("成功加载了 " + serverIcons.size() + " 个服务器图标");
        }
    }
} 