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
    
    public AMOTDCommand(AMOTD plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("amotd.command.reload")) {
                sender.sendMessage(ChatColor.RED + "你没有权限执行这个命令！");
                return true;
            }
            
            // 重载配置
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "AMOTD 配置已重新加载！");
            return true;
        }
        
        // 如果没有参数或参数不是reload，显示使用方法
        sender.sendMessage(ChatColor.YELLOW + "用法: /amotd reload - 重新加载配置");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // 只有当输入第一个参数时才提供补全
        if (args.length == 1) {
            // 只有拥有reload权限的玩家才能看到reload补全选项
            if (sender.hasPermission("amotd.command.reload")) {
                // 如果玩家输入的开头匹配"reload"，则添加"reload"作为补全选项
                if ("reload".startsWith(args[0].toLowerCase())) {
                    completions.add("reload");
                }
            }
            return completions;
        }
        
        // 对于其他情况，返回空列表（不提供补全选项）
        return completions;
    }
} 