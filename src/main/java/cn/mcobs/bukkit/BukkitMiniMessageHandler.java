package cn.mcobs.bukkit;

import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Stack;

public class BukkitMiniMessageHandler {
    
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    
    /**
     * 解析MiniMessage格式为传统颜色代码
     */
    public static String parse(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            // 预处理标签
            String preprocessed = preprocessTags(text);
            
            // 使用官方MiniMessage解析
            Component component = MINI_MESSAGE.deserialize(preprocessed);
            return LEGACY_SERIALIZER.serialize(component);
        } catch (Exception e) {
            // 回退到基本解析器
            return cn.mcobs.SimpleMiniMessage.parseMiniMessage(text);
        }
    }
    
    /**
     * 预处理标签，修复嵌套顺序问题
     */
    private static String preprocessTags(String text) {
        // 首先，移除所有闭合标签，我们将自己管理格式
        text = text.replaceAll("</[^>]+>", "");
        
        // 然后，为每个格式标签和颜色组合重新构建格式
        StringBuilder result = new StringBuilder();
        boolean inTag = false;
        Stack<String> tagStack = new Stack<>();
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '<') {
                // 开始处理标签
                int tagEnd = text.indexOf('>', i);
                if (tagEnd == -1) {
                    // 无效标签
                    result.append(c);
                    continue;
                }
                
                String tag = text.substring(i + 1, tagEnd);
                if (!tag.startsWith("/")) {
                    // 开始标签
                    tagStack.push(tag);
                    result.append('<').append(tag).append('>');
                }
                
                i = tagEnd;
            } else {
                // 普通字符，附加所有活动的格式
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * 将复杂的嵌套标签转换为字符级的标签
     */
    private static String convertToCharacterLevel(String text) {
        // 查找所有类似 <color:#XXXXXX><bold>X</color></bold> 的模式
        Pattern pattern = Pattern.compile("<color:([^>]+)><([^>]+)>([^<])");
        Matcher matcher = pattern.matcher(text);
        
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String colorValue = matcher.group(1);
            String formatTag = matcher.group(2);
            char content = matcher.group(3).charAt(0);
            
            // 为每个字符创建单独的正确嵌套标签
            String replacement = "<" + formatTag + "><color:" + colorValue + ">" + content + 
                                 "</color></" + formatTag + ">";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * 解析传统颜色代码
     */
    public static String parseLegacy(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            return text.replace('&', '§');
        } catch (Exception e) {
            return text;
        }
    }
    
    /**
     * 将Adventure组件转换为Bukkit可用的字符串
     */
    public static String componentToString(Component component) {
        return BukkitComponentSerializer.legacy().serialize(component);
    }
} 