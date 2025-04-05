package cn.mcobs;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MessageUtil {

    public static void sendMessage(CommandSender sender, String message, String colorName) {
        if (message == null || message.isEmpty()) {
            return;
        }
        
        try {
            // 尝试使用Adventure API (Paper)
            Component component;
            
            if (colorName != null) {
                TextColor color = getColorByName(colorName);
                component = Component.text(message).color(color);
            } else {
                component = LegacyComponentSerializer.legacySection().deserialize(message);
            }
            
            if (sender instanceof Player) {
                ((Player) sender).sendMessage(component);
            } else {
                // 对于控制台，转换为普通字符串
                sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(component));
            }
        } catch (Exception e) {
            // 降级到旧的API或基本消息
            if (colorName != null) {
                String legacyColorCode = getLegacyColorCode(colorName);
                sender.sendMessage(legacyColorCode + message);
            } else {
                sender.sendMessage(message);
            }
        }
    }
    
    private static TextColor getColorByName(String colorName) {
        try {
            return NamedTextColor.NAMES.value(colorName.toLowerCase());
        } catch (Exception e) {
            return NamedTextColor.WHITE;
        }
    }
    
    private static String getLegacyColorCode(String colorName) {
        switch (colorName.toLowerCase()) {
            case "black": return "§0";
            case "dark_blue": return "§1";
            case "dark_green": return "§2";
            case "dark_aqua": return "§3";
            case "dark_red": return "§4";
            case "dark_purple": return "§5";
            case "gold": return "§6";
            case "gray": return "§7";
            case "dark_gray": return "§8";
            case "blue": return "§9";
            case "green": return "§a";
            case "aqua": return "§b";
            case "red": return "§c";
            case "light_purple": return "§d";
            case "yellow": return "§e";
            case "white": return "§f";
            default: return "§f";
        }
    }
    
    public static String translateColorCodes(String text) {
        if (text == null) return "";
        return LegacyComponentSerializer.legacyAmpersand().serialize(
               LegacyComponentSerializer.legacyAmpersand().deserialize(text));
    }
} 