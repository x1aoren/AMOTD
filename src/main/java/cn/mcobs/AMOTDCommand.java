package cn.mcobs;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AMOTDCommand implements CommandExecutor, TabCompleter {
    
    private final AMOTD plugin;
    private final MOTDListener motdListener;
    private final MOTDStyleFetcher styleFetcher;
    
    public AMOTDCommand(AMOTD plugin) {
        this.plugin = plugin;
        this.motdListener = plugin.getMotdListener();
        this.styleFetcher = new MOTDStyleFetcher(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // 显示帮助信息
            sender.sendMessage(ChatColor.YELLOW + "AMOTD 插件命令:");
            sender.sendMessage(ChatColor.YELLOW + "/amotd reload - 重新加载配置和图标");
            sender.sendMessage(ChatColor.YELLOW + "/amotd get <样式码> - 获取预设MOTD样式");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("amotd.command.reload")) {
                sender.sendMessage(ChatColor.RED + "你没有权限执行这个命令！");
                return true;
            }
            
            // 重载配置
            plugin.reloadConfig();
            // 重载图标
            motdListener.reloadServerIcons();
            
            // 检查是否使用MiniMessage格式
            String formatType = plugin.getConfig().getString("message_format", "legacy");
            boolean useMinimessage = "minimessage".equalsIgnoreCase(formatType);
            
            if (useMinimessage) {
                boolean isPaper = false;
                try {
                    Class.forName("io.papermc.paper.text.PaperComponents");
                    isPaper = true;
                } catch (ClassNotFoundException e) {
                    // 不是Paper服务器
                }
                
                if (!isPaper) {
                    sender.sendMessage(ChatColor.YELLOW + "当前使用MiniMessage格式，但服务器不是Paper。" +
                            "将使用简易MiniMessage解析器，部分高级功能可能不可用。");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "检测到Paper服务器，完整MiniMessage格式可用。");
                }
            }
            
            sender.sendMessage(ChatColor.GREEN + "AMOTD 配置和图标已重新加载！");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("get")) {
            if (!sender.hasPermission("amotd.command.get")) {
                sender.sendMessage(ChatColor.RED + "你没有权限执行这个命令！");
                return true;
            }
            
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "用法: /amotd get <样式码>");
                return true;
            }
            
            // 获取样式码
            String styleCode = args[1];
            sender.sendMessage(ChatColor.YELLOW + "正在获取MOTD样式，请稍候...");
            
            // 获取样式
            styleFetcher.fetchStyle(styleCode, new MOTDStyleFetcher.Callback() {
                @Override
                public void onSuccess(String line1, String line2, boolean iconSuccess, String formatType) {
                    sender.sendMessage(ChatColor.GREEN + "MOTD样式获取成功！");
                    sender.sendMessage(ChatColor.GREEN + "格式类型: " + ChatColor.YELLOW + formatType);
                    sender.sendMessage(ChatColor.GREEN + "第一行: " + ChatColor.RESET + 
                            (formatType.equalsIgnoreCase("minimessage") ? 
                            SimpleMiniMessage.parseMiniMessage(line1) : 
                            ChatColor.translateAlternateColorCodes('&', line1)));
                    sender.sendMessage(ChatColor.GREEN + "第二行: " + ChatColor.RESET + 
                            (formatType.equalsIgnoreCase("minimessage") ? 
                            SimpleMiniMessage.parseMiniMessage(line2) : 
                            ChatColor.translateAlternateColorCodes('&', line2)));
                    
                    if (iconSuccess) {
                        sender.sendMessage(ChatColor.GREEN + "服务器图标已成功下载");
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "服务器图标下载失败或未提供");
                    }
                    
                    // 重载MOTD监听器以应用新图标
                    motdListener.reloadServerIcons();
                }
                
                @Override
                public void onFailure(String errorMessage) {
                    sender.sendMessage(ChatColor.RED + "获取MOTD样式失败: " + errorMessage);
                }
            });
            
            return true;
        }
        
        // 未知命令，显示帮助
        sender.sendMessage(ChatColor.YELLOW + "用法: /amotd reload|get <样式码>");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // 第一个参数的补全选项
        if (args.length == 1) {
            if (sender.hasPermission("amotd.command.reload") && "reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            if (sender.hasPermission("amotd.command.get") && "get".startsWith(args[0].toLowerCase())) {
                completions.add("get");
            }
            return completions;
        }
        
        // 其他情况不提供补全选项
        return completions;
    }
} 