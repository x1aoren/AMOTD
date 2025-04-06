package cn.mcobs.velocity;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VelocityMiniMessageHandler {
    
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    
    /**
     * 解析MiniMessage格式文本为Adventure组件
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        try {
            // 预处理以确保标签格式正确
            String processed = ensureCorrectTags(text);
            return MINI_MESSAGE.deserialize(processed);
        } catch (Exception e) {
            return Component.text(text);
        }
    }
    
    /**
     * 解析传统颜色代码为Adventure组件
     */
    public static Component parseLegacy(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        try {
            return LEGACY_SERIALIZER.deserialize(text.replace('&', '§'));
        } catch (Exception e) {
            return Component.text(text);
        }
    }
    
    /**
     * 将MiniMessage文本转换为传统格式（用于不支持Adventure的系统）
     */
    public static String miniToLegacy(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            Component component = MINI_MESSAGE.deserialize(text);
            return LEGACY_SERIALIZER.serialize(component);
        } catch (Exception e) {
            return text;
        }
    }
    
    /**
     * 确保标签格式正确
     */
    private static String ensureCorrectTags(String text) {
        // 完全删除所有闭合标签，然后在后续处理中重新应用格式
        text = text.replaceAll("</[^>]+>", "");
        
        // 为每个颜色标签分别应用格式
        Pattern colorBoldPattern = Pattern.compile("<color:([^>]+)><bold>([^<]+)");
        Matcher matcher = colorBoldPattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String colorValue = matcher.group(1);
            String content = matcher.group(2);
            String replacement = "<bold><color:" + colorValue + ">" + content;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
} 