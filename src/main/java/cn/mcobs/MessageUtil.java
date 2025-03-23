package cn.mcobs;

import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;

import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class MessageUtil {
    
    private static final boolean HAS_ADVENTURE_API;
    
    static {
        boolean hasApi = false;
        try {
            Class.forName("net.kyori.adventure.text.Component");
            hasApi = true;
        } catch (ClassNotFoundException e) {
            hasApi = false;
        }
        HAS_ADVENTURE_API = hasApi;
    }
    
    /**
     * 发送消息，自动适配服务器版本
     */
    public static void sendMessage(CommandSender sender, String message, String colorCode) {
        if (HAS_ADVENTURE_API) {
            // 尝试使用Adventure API
            try {
                Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
                Method textMethod = componentClass.getMethod("text", String.class);
                Object component = textMethod.invoke(null, message);
                
                // 添加颜色
                if (colorCode != null) {
                    Class<?> namedTextColorClass = Class.forName("net.kyori.adventure.text.format.NamedTextColor");
                    Field colorField = namedTextColorClass.getField(colorCode.toUpperCase());
                    Object color = colorField.get(null);
                    
                    Method colorMethod = component.getClass().getMethod("color", Class.forName("net.kyori.adventure.text.format.TextColor"));
                    component = colorMethod.invoke(component, color);
                }
                
                // 发送消息
                Method sendMessageMethod = sender.getClass().getMethod("sendMessage", componentClass);
                sendMessageMethod.invoke(sender, component);
                return;
            } catch (Exception e) {
                // 如果反射调用失败，回退到传统方式
            }
        }
        
        // 回退到传统的ChatColor
        if (colorCode != null) {
            ChatColor color = getChatColorByName(colorCode);
            message = color + message;
        }
        sender.sendMessage(message);
    }
    
    /**
     * 根据名称获取ChatColor
     */
    private static ChatColor getChatColorByName(String name) {
        try {
            return ChatColor.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ChatColor.WHITE;
        }
    }
    
    /**
     * 检查是否支持Adventure API
     */
    public static boolean hasAdventureApi() {
        return HAS_ADVENTURE_API;
    }
} 