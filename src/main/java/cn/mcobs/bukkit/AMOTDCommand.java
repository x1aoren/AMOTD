package cn.mcobs.bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONObject;

public class AMOTDCommand implements CommandExecutor, TabCompleter {

    private final AMOTD plugin;
    
    public AMOTDCommand(AMOTD plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6===== AMOTD 命令帮助 =====");
            sender.sendMessage("§e/amotd reload §f- 重新加载配置");
            sender.sendMessage("§e/amotd get <样式码> §f- 从网络获取MOTD样式");
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("amotd.command.reload")) {
                    sender.sendMessage("§c你没有权限执行此命令！");
                    return true;
                }
                
                plugin.reloadConfig();
                
                if (plugin.getMotdListener() != null) {
                    plugin.getMotdListener().reloadServerIcons();
                }
                
                plugin.updateMaxPlayers();
                sender.sendMessage("§aAMOTD 配置已重新加载！");
                return true;
                
            case "get":
                if (!sender.hasPermission("amotd.command.get")) {
                    sender.sendMessage("§c你没有权限执行此命令！");
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage("§c用法: /amotd get <样式码>");
                    return true;
                }
                
                String styleCode = args[1];
                sender.sendMessage("§e正在获取MOTD样式 " + styleCode + "...");
                
                // 在异步线程中获取MOTD
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
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
                            String type = json.optString("type", "minecraft");
                            String line1 = json.getString("line1");
                            String line2 = json.getString("line2");
                            String iconUrl = json.optString("icon", null);
                            
                            // 更新配置
                            String configType = "minecraft".equalsIgnoreCase(type) ? "legacy" : type;
                            
                            // 保存配置到插件
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                plugin.getConfig().set("message_format", configType);
                                
                                if ("minimessage".equalsIgnoreCase(configType)) {
                                    plugin.getConfig().set("minimessage.line1", line1);
                                    plugin.getConfig().set("minimessage.line2", line2);
                                } else {
                                    plugin.getConfig().set("legacy.line1", line1);
                                    plugin.getConfig().set("legacy.line2", line2);
                                }
                                
                                plugin.saveConfig();
                                
                                // 通知用户
                                sender.sendMessage("§a获取到MOTD样式:");
                                sender.sendMessage("§b类型: " + configType + " (已自动切换)");
                                sender.sendMessage("§f第一行: " + line1);
                                sender.sendMessage("§f第二行: " + line2);
                                
                                if (iconUrl != null && !iconUrl.isEmpty()) {
                                    // 下载图标...实现略
                                    sender.sendMessage("§6图标URL: " + iconUrl);
                                }
                                
                                sender.sendMessage("§e配置已更新，使用 /amotd reload 应用更改");
                            });
                        } else {
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                sender.sendMessage("§c获取MOTD失败，HTTP错误码: " + responseCode);
                            });
                        }
                    } catch (Exception e) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            sender.sendMessage("§c获取MOTD时出错: " + e.getMessage());
                        });
                    }
                });
                
                return true;
                
            default:
                sender.sendMessage("§c未知命令。使用 /amotd 查看帮助");
                return true;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = Arrays.asList("reload", "get");
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
} 