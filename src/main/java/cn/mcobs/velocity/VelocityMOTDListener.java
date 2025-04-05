package cn.mcobs.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class VelocityMOTDListener {
    
    private final AMOTDVelocity plugin;
    private final List<Favicon> serverIcons = new ArrayList<>();
    private final Random random = new Random();
    
    public VelocityMOTDListener(AMOTDVelocity plugin) {
        this.plugin = plugin;
        loadServerIcons();
    }
    
    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing.Builder pingBuilder = event.getPing().asBuilder();
        
        // 获取消息格式类型
        String formatType = plugin.getConfigManager().getString("message_format", "legacy");
        boolean useMinimessage = "minimessage".equalsIgnoreCase(formatType);
        
        // 获取MOTD文本
        String line1, line2;
        
        if (useMinimessage) {
            line1 = plugin.getConfigManager().getString("minimessage.line1", "<green>默认的第一行MOTD</green>");
            line2 = plugin.getConfigManager().getString("minimessage.line2", "<yellow>默认的第二行MOTD</yellow>");
        } else {
            line1 = plugin.getConfigManager().getString("legacy.line1", "&a默认的第一行MOTD");
            line2 = plugin.getConfigManager().getString("legacy.line2", "&e默认的第二行MOTD");
        }
        
        // 处理MOTD文本
        Component motdComponent;
        if (useMinimessage) {
            String combinedText = line1 + "\n" + line2;
            motdComponent = MiniMessage.miniMessage().deserialize(combinedText);
        } else {
            String combinedText = line1 + "\n" + line2;
            String coloredText = combinedText.replace('&', '§');
            motdComponent = LegacyComponentSerializer.legacySection().deserialize(coloredText);
        }
        
        // 设置MOTD
        pingBuilder.description(motdComponent);
        
        // 处理人数显示
        if (plugin.getConfigManager().getBoolean("player_count.enabled", false)) {
            int maxPlayers = plugin.getConfigManager().getInt("player_count.max_players", 100);
            pingBuilder.maximumPlayers(maxPlayers);
        }
        
        // 处理玩家列表悬停文本
        boolean enableHoverText = plugin.getConfigManager().getBoolean("hover_player_list.enabled", true);
        if (!enableHoverText) {
            // 隐藏玩家列表
            pingBuilder.clearSamplePlayers();
        } else {
            // 检查是否有玩家在线
            int onlinePlayers = plugin.getServer().getPlayerCount();
            if (onlinePlayers == 0) {
                // 没有玩家在线时显示自定义消息
                String emptyMessage = plugin.getConfigManager().getString("hover_player_list.empty_message", "目前没有玩家在线");
                List<ServerPing.SamplePlayer> samplePlayers = new ArrayList<>();
                samplePlayers.add(new ServerPing.SamplePlayer(emptyMessage, UUID.randomUUID()));
                pingBuilder.samplePlayers(samplePlayers.toArray(new ServerPing.SamplePlayer[0]));
            }
        }
        
        // 处理服务器图标
        if (plugin.getConfigManager().getBoolean("enable_server_icon", true) && !serverIcons.isEmpty()) {
            // 随机选择一个图标
            Favicon icon = serverIcons.get(random.nextInt(serverIcons.size()));
            pingBuilder.favicon(icon);
        }
        
        // 应用更改
        event.setPing(pingBuilder.build());
    }
    
    private void loadServerIcons() {
        // 清空当前图标列表
        serverIcons.clear();
        
        // 检查是否启用了图标功能
        if (!plugin.getConfigManager().getBoolean("enable_server_icon", true)) {
            return;
        }
        
        // 获取icons文件夹
        Path iconsFolder = plugin.getDataDirectory().resolve("icons");
        
        // 加载所有png文件
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(iconsFolder, "*.png")) {
            for (Path iconPath : stream) {
                try {
                    BufferedImage image = ImageIO.read(iconPath.toFile());
                    if (image != null) {
                        Favicon favicon = Favicon.create(image);
                        serverIcons.add(favicon);
                        plugin.getLogger().info("已加载图标: " + iconPath.getFileName());
                    }
                } catch (IOException e) {
                    plugin.getLogger().error("加载图标时出错 " + iconPath.getFileName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            plugin.getLogger().error("读取图标目录时出错: " + e.getMessage());
        }
        
        plugin.getLogger().info("成功加载了 " + serverIcons.size() + " 个服务器图标");
    }
    
    public void reloadServerIcons() {
        loadServerIcons();
    }
} 