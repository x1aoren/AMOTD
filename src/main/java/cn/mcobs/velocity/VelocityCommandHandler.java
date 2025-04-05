package cn.mcobs.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class VelocityCommandHandler implements SimpleCommand {
    
    private final AMOTDVelocity plugin;
    
    public VelocityCommandHandler(AMOTDVelocity plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        
        if (args.length == 0) {
            sendHelp(source);
            return;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(source);
                break;
            case "get":
                handleGet(source, args);
                break;
            default:
                sendHelp(source);
                break;
        }
    }
    
    private void handleReload(CommandSource source) {
        // 检查权限
        if (!source.hasPermission("amotd.command.reload")) {
            source.sendMessage(Component.text("你没有权限执行此命令！").color(NamedTextColor.RED));
            return;
        }
        
        // 重新加载配置
        plugin.getConfigManager().loadConfig();
        
        // 重新加载图标
        plugin.reloadServerIcons();
        
        source.sendMessage(Component.text("AMOTD 配置已重新加载！").color(NamedTextColor.GREEN));
    }
    
    private void handleGet(CommandSource source, String[] args) {
        // 检查权限
        if (!source.hasPermission("amotd.command.get")) {
            source.sendMessage(Component.text("你没有权限执行此命令！").color(NamedTextColor.RED));
            return;
        }
        
        if (args.length < 2) {
            source.sendMessage(Component.text("用法: /amotd get <样式码>").color(NamedTextColor.RED));
            return;
        }
        
        String styleCode = args[1];
        source.sendMessage(Component.text("正在获取MOTD样式 " + styleCode + "...").color(NamedTextColor.YELLOW));
        
        // 异步获取MOTD
        CompletableFuture.runAsync(() -> {
            try {
                // 联网获取MOTD
                String apiUrl = "https://motd.mcobs.cn/api/motd/" + styleCode;
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // 解析JSON响应
                    JSONObject json = new JSONObject(response.toString());
                    String type = json.optString("type", "minecraft");  // 默认为minecraft类型
                    String line1 = json.getString("line1");
                    String line2 = json.getString("line2");
                    String iconUrl = json.optString("icon", null);
                    
                    // 根据API返回的类型设置message_format
                    // 注意: API返回的"minecraft"类型对应配置中的"legacy"格式
                    String configType = "minecraft".equalsIgnoreCase(type) ? "legacy" : type;
                    
                    try {
                        // 设置消息格式类型
                        plugin.getConfigManager().setString("message_format", configType);
                        
                        // 只更新对应类型的MOTD内容
                        if ("minimessage".equalsIgnoreCase(configType)) {
                            // 如果是minimessage格式，只更新minimessage配置
                            plugin.getConfigManager().setString("minimessage.line1", line1);
                            plugin.getConfigManager().setString("minimessage.line2", line2);
                            plugin.getLogger().info("已更新MiniMessage格式MOTD");
                        } else {
                            // 否则更新legacy(minecraft)配置
                            plugin.getConfigManager().setString("legacy.line1", line1);
                            plugin.getConfigManager().setString("legacy.line2", line2);
                            plugin.getLogger().info("已更新Legacy格式MOTD");
                        }
                    } catch (Exception e) {
                        plugin.getLogger().error("更新配置时出错", e);
                    }
                    
                    // 如果有图标URL，尝试下载图标
                    if (iconUrl != null && !iconUrl.isEmpty()) {
                        try {
                            // 创建icons目录
                            Path iconsPath = plugin.getDataDirectory().resolve("icons");
                            if (!Files.exists(iconsPath)) {
                                Files.createDirectories(iconsPath);
                            }
                            
                            // 从URL获取图标名称
                            String iconName = iconUrl.substring(iconUrl.lastIndexOf('/') + 1);
                            Path iconPath = iconsPath.resolve(iconName);
                            
                            // 下载图标
                            URL imageUrl = new URL(iconUrl);
                            try (InputStream in = imageUrl.openStream()) {
                                Files.copy(in, iconPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                            
                            final String savedIconName = iconName;
                            // 在主线程发送消息
                            plugin.getServer().getScheduler().buildTask(plugin, () -> {
                                source.sendMessage(Component.text("获取到MOTD样式:").color(NamedTextColor.GREEN));
                                source.sendMessage(Component.text("类型: " + configType + " (已自动切换)").color(NamedTextColor.AQUA));
                                source.sendMessage(Component.text("第一行: " + line1).color(NamedTextColor.WHITE));
                                source.sendMessage(Component.text("第二行: " + line2).color(NamedTextColor.WHITE));
                                
                                if (iconUrl != null && !iconUrl.isEmpty()) {
                                    source.sendMessage(Component.text("图标已下载到 icons/" + savedIconName).color(NamedTextColor.GOLD));
                                }
                                
                                source.sendMessage(Component.text("配置已自动更新，使用 /amotd reload 应用更改").color(NamedTextColor.YELLOW));
                            }).schedule();
                            
                        } catch (Exception e) {
                            // 如果下载图标失败，只发送文本消息
                            final String errorMsg = e.getMessage();
                            plugin.getServer().getScheduler().buildTask(plugin, () -> {
                                source.sendMessage(Component.text("获取到MOTD样式:").color(NamedTextColor.GREEN));
                                source.sendMessage(Component.text("类型: " + configType).color(NamedTextColor.AQUA));
                                source.sendMessage(Component.text("第一行: " + line1).color(NamedTextColor.WHITE));
                                source.sendMessage(Component.text("第二行: " + line2).color(NamedTextColor.WHITE));
                                source.sendMessage(Component.text("图标URL: " + iconUrl).color(NamedTextColor.GOLD));
                                source.sendMessage(Component.text("下载图标时出错: " + errorMsg).color(NamedTextColor.RED));
                                
                                // 提示用户如何应用此MOTD
                                source.sendMessage(Component.text("请在配置文件中设置这些值，然后使用 /amotd reload 重新加载").color(NamedTextColor.YELLOW));
                            }).schedule();
                        }
                    }
                } else {
                    // 在主线程发送消息
                    plugin.getServer().getScheduler().buildTask(plugin, () -> {
                        source.sendMessage(Component.text("获取MOTD失败，HTTP错误码: " + responseCode).color(NamedTextColor.RED));
                    }).schedule();
                }
            } catch (Exception e) {
                // 在主线程发送消息
                plugin.getServer().getScheduler().buildTask(plugin, () -> {
                    source.sendMessage(Component.text("获取MOTD时出错: " + e.getMessage()).color(NamedTextColor.RED));
                }).schedule();
            }
        });
    }
    
    private void sendHelp(CommandSource source) {
        source.sendMessage(Component.text("===== AMOTD 命令帮助 =====").color(NamedTextColor.GOLD));
        source.sendMessage(Component.text("/amotd reload").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 重新加载配置").color(NamedTextColor.WHITE)));
        source.sendMessage(Component.text("/amotd get <样式码>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 从网络获取MOTD样式").color(NamedTextColor.WHITE)));
    }
    
    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        
        if (args.length == 0 || args[0].isEmpty()) {
            return CompletableFuture.completedFuture(Arrays.asList("reload", "get"));
        }
        
        if (args.length == 1) {
            List<String> completions = Arrays.asList("reload", "get");
            String arg = args[0].toLowerCase();
            return CompletableFuture.completedFuture(completions.stream()
                    .filter(s -> s.startsWith(arg))
                    .collect(Collectors.toList()));
        }
        
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
} 