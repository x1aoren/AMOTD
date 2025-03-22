package cn.mcobs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class MessageFormatter {
    
    private static final boolean PAPER_SERVER;
    
    static {
        // 检查服务器是否是Paper
        boolean isPaper = false;
        try {
            Class.forName("io.papermc.paper.text.PaperComponents");
            isPaper = true;
        } catch (ClassNotFoundException e) {
            // 不是Paper服务器
        }
        PAPER_SERVER = isPaper;
    }
    
    /**
     * 格式化消息，根据配置使用传统格式或MiniMessage格式
     * @param plugin 插件实例
     * @param message 消息内容
     * @param useMinimessage 是否尝试使用MiniMessage格式
     * @return 格式化后的消息
     */
    public static String formatMessage(AMOTD plugin, String message, boolean useMinimessage) {
        if (useMinimessage) {
            try {
                // 使用简易MiniMessage解析器处理消息
                return SimpleMiniMessage.parseMiniMessage(message);
            } catch (Exception e) {
                plugin.getLogger().warning("MiniMessage解析失败: " + e.getMessage());
                // 如果解析失败，返回纯文本（移除所有标签）
                return removeAllTags(message);
            }
        } else {
            // 使用传统的颜色代码
            return ChatColor.translateAlternateColorCodes('&', message);
        }
    }
    
    /**
     * 移除所有尖括号标签
     * @param message 原始消息
     * @return 移除标签后的消息
     */
    private static String removeAllTags(String message) {
        // 去除所有<tag>内容</tag>格式的标签
        String result = message.replaceAll("<[^<>]+>([^<>]*)</[^<>]+>", "$1");
        // 去除所有<tag>格式的标签
        result = result.replaceAll("<[^<>]+>", "");
        return result;
    }
} 