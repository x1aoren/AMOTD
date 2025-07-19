package cn.mcobs;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// 替换ChatColor导入
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.TextComponent;

import java.lang.reflect.Field;

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
            MessageUtil.sendMessage(sender, "AMOTD 插件命令:", "yellow");
            MessageUtil.sendMessage(sender, "/amotd reload - 重新加载配置和图标", "yellow");
            MessageUtil.sendMessage(sender, "/amotd get <样式码> - 获取预设MOTD样式", "yellow");
            return true;
        }
        
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("amotd.command.reload")) {
                MessageUtil.sendMessage(sender, "你没有权限执行这个命令！", "red");
                return true;
            }
            
            // 检查配置文件是否存在
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                MessageUtil.sendMessage(sender, "未找到配置文件，正在重新生成默认配置...", "yellow");
                plugin.saveDefaultConfig();
            }
            
            // 重载配置
            plugin.reloadConfig();
            // 重载图标
            motdListener.reloadServerIcons();
            
            // 更新最大人数设置
            plugin.updateMaxPlayers();
            
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
                    MessageUtil.sendMessage(sender, "当前使用MiniMessage格式，但服务器不是Paper。将使用简易MiniMessage解析器，部分高级功能可能不可用。", "yellow");
                } else {
                    MessageUtil.sendMessage(sender, "检测到Paper服务器，完整MiniMessage格式可用。", "green");
                }
            }
            
            MessageUtil.sendMessage(sender, "AMOTD 配置和图标已重新加载！", "green");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("get")) {
            if (!sender.hasPermission("amotd.command.get")) {
                MessageUtil.sendMessage(sender, "你没有权限执行这个命令！", "red");
                return true;
            }
            
            if (args.length < 2) {
                MessageUtil.sendMessage(sender, "用法: /amotd get <样式码>", "red");
                return true;
            }
            
            // 获取样式码
            String styleCode = args[1];
            MessageUtil.sendMessage(sender, "正在获取MOTD样式，请稍候...", "yellow");
            
            // 获取样式
            styleFetcher.fetchStyle(styleCode, new MOTDStyleFetcher.Callback() {
                @Override
                public void onSuccess(String line1, String line2, boolean iconSuccess, String formatType) {
                    MessageUtil.sendMessage(sender, "MOTD样式获取成功！", "green");
                    
                    // 格式类型信息
                    TextComponent typeMsg = Component.text("格式类型: ").color(NamedTextColor.GREEN)
                            .append(Component.text(formatType).color(NamedTextColor.YELLOW));
                    MessageUtil.sendMessage(sender, typeMsg.toString(), null);
                    
                    // 处理MOTD格式
                    boolean isMinimessage = "minimessage".equalsIgnoreCase(formatType);
                    String processedLine1, processedLine2;
                    
                    if (isMinimessage) {
                        processedLine1 = SimpleMiniMessage.parseMiniMessage(line1);
                        processedLine2 = SimpleMiniMessage.parseMiniMessage(line2);
                    } else {
                        processedLine1 = org.bukkit.ChatColor.translateAlternateColorCodes('&', line1);
                        processedLine2 = org.bukkit.ChatColor.translateAlternateColorCodes('&', line2);
                    }
                    
                    // 第一行信息
                    TextComponent line1Msg = Component.text("第一行: ").color(NamedTextColor.GREEN)
                            .append(Component.text(processedLine1));
                    MessageUtil.sendMessage(sender, line1Msg.toString(), null);
                    
                    // 第二行信息
                    TextComponent line2Msg = Component.text("第二行: ").color(NamedTextColor.GREEN)
                            .append(Component.text(processedLine2));
                    MessageUtil.sendMessage(sender, line2Msg.toString(), null);
                    
                    if (iconSuccess) {
                        MessageUtil.sendMessage(sender, "服务器图标已成功下载", "green");
                    } else {
                        MessageUtil.sendMessage(sender, "服务器图标下载失败或未提供", "yellow");
                    }
                    
                    // 重载MOTD监听器以应用新图标
                    motdListener.reloadServerIcons();
                }
                
                @Override
                public void onFailure(String errorMessage) {
                    MessageUtil.sendMessage(sender, "获取MOTD样式失败: " + errorMessage, "red");
                }
            });
            
            return true;
        }
        
        // 未知命令，显示帮助
        MessageUtil.sendMessage(sender, "用法: /amotd reload|get <样式码>", "yellow");
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