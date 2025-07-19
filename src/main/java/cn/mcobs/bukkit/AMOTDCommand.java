package cn.mcobs.bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import cn.mcobs.utils.LanguageManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONObject;
import java.io.File;

public class AMOTDCommand implements CommandExecutor, TabCompleter {

    private final AMOTD plugin;
    private final LanguageManager lang;
    
    public AMOTDCommand(AMOTD plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6" + lang.getMessage("help_title"));
            sender.sendMessage("§e" + lang.getMessage("help_reload"));
            sender.sendMessage("§e" + lang.getMessage("help_get"));
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("amotd.command.reload")) {
                    sender.sendMessage("§c" + lang.getMessage("no_permission"));
                    return true;
                }
                
                // 检查配置文件是否存在，如果不存在则重新生成
                File configFile = new File(plugin.getDataFolder(), "config.yml");
                if (!configFile.exists()) {
                    sender.sendMessage("§e" + lang.getMessage("config_not_found"));
                    plugin.saveDefaultConfig();
                }
                
                plugin.reloadConfig();
                
                if (plugin.getMotdListener() != null) {
                    plugin.getMotdListener().reloadServerIcons();
                }
                
                plugin.updateMaxPlayers();
                sender.sendMessage("§a" + lang.getMessage("reload_success"));
                return true;
                
            case "get":
                if (!sender.hasPermission("amotd.command.get")) {
                    sender.sendMessage("§c" + lang.getMessage("no_permission"));
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage("§c" + lang.getMessage("command_usage", "/amotd get <style code>"));
                    return true;
                }
                
                String styleCode = args[1];
                sender.sendMessage("§e" + lang.getMessage("fetching_style", styleCode));
                
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
                            String iconUrl = json.optString("icon", "");
                            
                            // 从content子对象获取MOTD内容
                            JSONObject content = json.optJSONObject("content");
                            String line1, line2;
                            
                            if (content != null) {
                                // 新格式：从content对象获取
                                line1 = content.optString("line1", "");
                                line2 = content.optString("line2", "");
                            } else {
                                // 兼容旧格式：直接从主对象获取
                                line1 = json.optString("line1", "");
                                line2 = json.optString("line2", "");
                            }
                            
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
                                sender.sendMessage("§a" + lang.getMessage("style_fetch_success"));
                                sender.sendMessage("§b" + lang.getMessage("format_type", configType));
                                sender.sendMessage("§f" + lang.getMessage("line1", line1));
                                sender.sendMessage("§f" + lang.getMessage("line2", line2));
                                
                                if (iconUrl != null && !iconUrl.isEmpty()) {
                                    // 下载图标...实现略
                                    sender.sendMessage("§6图标URL: " + iconUrl);
                                }
                                
                                sender.sendMessage("§e" + lang.getMessage("config_updated"));
                            });
                        } else {
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                sender.sendMessage("§c" + lang.getMessage("fetch_failed", "HTTP " + responseCode));
                            });
                        }
                    } catch (Exception e) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            sender.sendMessage("§c" + lang.getMessage("fetch_failed", e.getMessage()));
                        });
                    }
                });
                
                return true;
                
            default:
                sender.sendMessage("§c" + lang.getMessage("unknown_command"));
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