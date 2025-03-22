package cn.mcobs;

import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简易MiniMessage解析器，为非Paper服务器提供基本的MiniMessage支持
 */
public class SimpleMiniMessage {
    
    // 匹配标签的正则表达式
    private static final Pattern TAG_PATTERN = Pattern.compile("<([^<>]+)>(.*?)</([^<>]+)>|<([^<>]+)>");
    
    // 颜色映射表
    private static final Map<String, ChatColor> COLOR_MAP = new HashMap<>();
    
    static {
        COLOR_MAP.put("black", ChatColor.BLACK);
        COLOR_MAP.put("dark_blue", ChatColor.DARK_BLUE);
        COLOR_MAP.put("dark_green", ChatColor.DARK_GREEN);
        COLOR_MAP.put("dark_aqua", ChatColor.DARK_AQUA);
        COLOR_MAP.put("dark_red", ChatColor.DARK_RED);
        COLOR_MAP.put("dark_purple", ChatColor.DARK_PURPLE);
        COLOR_MAP.put("gold", ChatColor.GOLD);
        COLOR_MAP.put("gray", ChatColor.GRAY);
        COLOR_MAP.put("dark_gray", ChatColor.DARK_GRAY);
        COLOR_MAP.put("blue", ChatColor.BLUE);
        COLOR_MAP.put("green", ChatColor.GREEN);
        COLOR_MAP.put("aqua", ChatColor.AQUA);
        COLOR_MAP.put("red", ChatColor.RED);
        COLOR_MAP.put("light_purple", ChatColor.LIGHT_PURPLE);
        COLOR_MAP.put("yellow", ChatColor.YELLOW);
        COLOR_MAP.put("white", ChatColor.WHITE);
        
        // 特殊格式
        COLOR_MAP.put("bold", ChatColor.BOLD);
        COLOR_MAP.put("italic", ChatColor.ITALIC);
        COLOR_MAP.put("underlined", ChatColor.UNDERLINE);
        COLOR_MAP.put("strikethrough", ChatColor.STRIKETHROUGH);
        COLOR_MAP.put("obfuscated", ChatColor.MAGIC);
        COLOR_MAP.put("reset", ChatColor.RESET);
    }
    
    /**
     * 将MiniMessage格式转换为传统颜色代码
     * @param input MiniMessage格式文本
     * @return 传统颜色代码文本
     */
    public static String parseMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String result = input;
        
        // 首先处理十六进制渐变色，这是最复杂的格式
        result = handleHexGradient(result);
        
        // 然后处理命名颜色的渐变
        result = handleGradient(result);
        
        // 处理彩虹颜色
        result = handleRainbow(result);
        
        // 处理基本标签
        Matcher matcher = TAG_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String tag = matcher.group(1);
            String content = matcher.group(2);
            String closeTag = matcher.group(3);
            String singleTag = matcher.group(4);
            
            if (singleTag != null) {
                // 处理单标签 <tag>
                matcher.appendReplacement(sb, getColorCodeForTag(singleTag));
            } else if (tag != null && content != null && closeTag != null) {
                // 处理成对标签 <tag>content</tag>
                String replacement = getColorCodeForTag(tag) + content + ChatColor.RESET;
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * 获取标签对应的颜色代码
     * @param tag 标签名
     * @return 颜色代码字符串
     */
    private static String getColorCodeForTag(String tag) {
        // 分割属性，例如color:red
        String[] parts = tag.split(":", 2);
        String tagName = parts[0].toLowerCase();
        
        if (tagName.equals("color") && parts.length > 1) {
            // 处理 <color:red> 或 <color:#FF5555> 格式
            String colorName = parts[1].toLowerCase();
            
            // 检查是否是十六进制颜色
            if (colorName.startsWith("#")) {
                // 转换为最近的Minecraft颜色代码
                return getNearestColorFromHex(colorName);
            }
            
            ChatColor color = COLOR_MAP.get(colorName);
            return color != null ? color.toString() : "";
        } else if (COLOR_MAP.containsKey(tagName)) {
            // 处理 <red> 格式
            return COLOR_MAP.get(tagName).toString();
        }
        
        // 未知标签，返回空字符串
        return "";
    }
    
    /**
     * 从十六进制颜色获取最近的Minecraft颜色代码
     * @param hexColor 十六进制颜色，例如 #FF5555
     * @return 对应的ChatColor
     */
    private static String getNearestColorFromHex(String hexColor) {
        try {
            // 移除#号
            if (hexColor.startsWith("#")) {
                hexColor = hexColor.substring(1);
            }
            
            // 解析RGB值
            int r = Integer.parseInt(hexColor.substring(0, 2), 16);
            int g = Integer.parseInt(hexColor.substring(2, 4), 16);
            int b = Integer.parseInt(hexColor.substring(4, 6), 16);
            
            // 根据RGB值选择最近的颜色
            if (r > 200 && g < 60 && b < 60) return ChatColor.RED.toString();
            if (r > 200 && g > 200 && b < 60) return ChatColor.YELLOW.toString();
            if (r < 60 && g > 200 && b < 60) return ChatColor.GREEN.toString();
            if (r < 60 && g > 200 && b > 200) return ChatColor.AQUA.toString();
            if (r < 60 && g < 60 && b > 200) return ChatColor.BLUE.toString();
            if (r > 200 && g < 60 && b > 200) return ChatColor.LIGHT_PURPLE.toString();
            if (r > 200 && g > 100 && b < 60) return ChatColor.GOLD.toString();
            if (r > 220 && g > 220 && b > 220) return ChatColor.WHITE.toString();
            if (r > 130 && g > 130 && b > 130) return ChatColor.GRAY.toString();
            if (r < 100 && g < 100 && b < 100) return ChatColor.DARK_GRAY.toString();
            
            // 默认颜色
            return ChatColor.WHITE.toString();
        } catch (Exception e) {
            return ChatColor.WHITE.toString();
        }
    }
    
    /**
     * 简单处理渐变颜色，将其转换为近似颜色
     * @param input 输入文本
     * @return 处理后的文本
     */
    private static String handleGradient(String input) {
        // 只匹配命名颜色的渐变，如 <gradient:red:blue>
        Pattern gradientPattern = Pattern.compile("<gradient:([a-zA-Z_]+):([a-zA-Z_]+)>(.*?)</gradient>");
        Matcher matcher = gradientPattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String startColor = matcher.group(1).toLowerCase();
            String endColor = matcher.group(2).toLowerCase();
            String content = matcher.group(3);
            
            if (content.length() > 0) {
                StringBuilder gradientText = new StringBuilder();
                
                // 获取起始颜色和结束颜色
                ChatColor color1 = COLOR_MAP.getOrDefault(startColor, ChatColor.WHITE);
                ChatColor color2 = COLOR_MAP.getOrDefault(endColor, ChatColor.WHITE);
                
                // 创建渐变文本
                if (content.length() >= 3) {
                    int segments = Math.min(content.length(), 3); // 最多分3段
                    int charsPerSegment = content.length() / segments;
                    int remainingChars = content.length() % segments;
                    
                    int currentPos = 0;
                    ChatColor[] colors = {color1, getMidColor(startColor, endColor), color2};
                    
                    for (int i = 0; i < segments; i++) {
                        int charsToTake = charsPerSegment + (i < remainingChars ? 1 : 0);
                        gradientText.append(colors[i % colors.length])
                                  .append(content.substring(currentPos, currentPos + charsToTake));
                        currentPos += charsToTake;
                    }
                } else {
                    // 内容太短，直接使用起始颜色
                    gradientText.append(color1).append(content);
                }
                
                matcher.appendReplacement(sb, Matcher.quoteReplacement(gradientText.toString()));
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * 尝试获取两种颜色之间的中间颜色
     * 简单实现，仅支持一些常见颜色组合
     */
    private static ChatColor getMidColor(String color1, String color2) {
        // 简单的中间颜色映射
        if ((color1.equals("red") && color2.equals("blue")) || 
            (color1.equals("blue") && color2.equals("red"))) {
            return ChatColor.LIGHT_PURPLE; // 红蓝之间返回粉色
        }
        if ((color1.equals("red") && color2.equals("yellow")) || 
            (color1.equals("yellow") && color2.equals("red"))) {
            return ChatColor.GOLD; // 红黄之间返回金色
        }
        if ((color1.equals("green") && color2.equals("blue")) || 
            (color1.equals("blue") && color2.equals("green"))) {
            return ChatColor.AQUA; // 绿蓝之间返回天蓝色
        }
        if ((color1.equals("green") && color2.equals("yellow")) || 
            (color1.equals("yellow") && color2.equals("green"))) {
            return ChatColor.GREEN; // 绿黄之间返回亮绿色
        }
        
        // 如果没有特定的中间颜色，就返回默认颜色
        return ChatColor.WHITE;
    }
    
    /**
     * 更好地处理彩虹文本
     * @param input 输入文本
     * @return 处理后的文本
     */
    private static String handleRainbow(String input) {
        Pattern rainbowPattern = Pattern.compile("<rainbow>(.*?)</rainbow>");
        Matcher matcher = rainbowPattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        
        ChatColor[] rainbowColors = {
            ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW, 
            ChatColor.GREEN, ChatColor.AQUA, ChatColor.LIGHT_PURPLE
        };
        
        while (matcher.find()) {
            String content = matcher.group(1);
            StringBuilder rainbow = new StringBuilder();
            
            for (int i = 0; i < content.length(); i++) {
                ChatColor color = rainbowColors[i % rainbowColors.length];
                rainbow.append(color).append(content.charAt(i));
            }
            
            matcher.appendReplacement(sb, Matcher.quoteReplacement(rainbow.toString()));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }

    /**
     * 处理十六进制渐变色
     */
    private static String handleHexGradient(String input) {
        // 匹配形如 <gradient:#FF5555:#5555FF>text</gradient> 的格式
        Pattern hexGradientPattern = Pattern.compile("<gradient:#([0-9A-Fa-f]{6}):#([0-9A-Fa-f]{6})>(.*?)</gradient>");
        Matcher matcher = hexGradientPattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String hexStart = matcher.group(1);
            String hexEnd = matcher.group(2);
            String content = matcher.group(3);
            
            if (content.length() > 0) {
                // 解析十六进制颜色为RGB值
                int rStart = Integer.parseInt(hexStart.substring(0, 2), 16);
                int gStart = Integer.parseInt(hexStart.substring(2, 4), 16);
                int bStart = Integer.parseInt(hexStart.substring(4, 6), 16);
                
                int rEnd = Integer.parseInt(hexEnd.substring(0, 2), 16);
                int gEnd = Integer.parseInt(hexEnd.substring(2, 4), 16);
                int bEnd = Integer.parseInt(hexEnd.substring(4, 6), 16);
                
                // 创建渐变文本
                StringBuilder gradient = new StringBuilder();
                
                // 特殊处理短文本：增加颜色变化点
                if (content.length() <= 4) {
                    // 短文本的每个字符都使用不同颜色
                    for (int i = 0; i < content.length(); i++) {
                        // 计算当前位置的颜色比例
                        float ratio = (float) i / (content.length() - 1);
                        if (content.length() == 1) ratio = 0.5f; // 单字符居中
                        
                        // 插值颜色
                        int r = Math.round(rStart + (rEnd - rStart) * ratio);
                        int g = Math.round(gStart + (gEnd - gStart) * ratio);
                        int b = Math.round(bStart + (bEnd - bStart) * ratio);
                        
                        // 获取最接近的Minecraft颜色
                        ChatColor color = getOptimizedColor(r, g, b);
                        gradient.append(color).append(content.charAt(i));
                    }
                } else {
                    // 长文本处理：分段渐变
                    int segments = Math.min(content.length(), 8); // 最多8段颜色
                    
                    for (int i = 0; i < segments; i++) {
                        float ratio = (float) i / (segments - 1);
                        
                        // 插值颜色
                        int r = Math.round(rStart + (rEnd - rStart) * ratio);
                        int g = Math.round(gStart + (gEnd - gStart) * ratio);
                        int b = Math.round(bStart + (bEnd - bStart) * ratio);
                        
                        // 获取最接近的Minecraft颜色
                        ChatColor color = getOptimizedColor(r, g, b);
                        
                        // 计算这段使用的字符数
                        int startPos = content.length() * i / segments;
                        int endPos = content.length() * (i + 1) / segments;
                        if (i == segments - 1) endPos = content.length();
                        
                        // 添加这一段
                        gradient.append(color).append(content.substring(startPos, endPos));
                    }
                }
                
                matcher.appendReplacement(sb, Matcher.quoteReplacement(gradient.toString()));
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }

    /**
     * 优化的颜色选择算法，特别为MOTD渐变效果设计
     */
    private static ChatColor getOptimizedColor(int r, int g, int b) {
        // 定义MC颜色的RGB值和相应的ChatColor
        int[][] colorMap = {
            {0, 0, 0, ChatColor.BLACK.ordinal()},        // BLACK
            {0, 0, 170, ChatColor.DARK_BLUE.ordinal()},  // DARK_BLUE
            {0, 170, 0, ChatColor.DARK_GREEN.ordinal()}, // DARK_GREEN
            {0, 170, 170, ChatColor.DARK_AQUA.ordinal()},// DARK_AQUA
            {170, 0, 0, ChatColor.DARK_RED.ordinal()},   // DARK_RED
            {170, 0, 170, ChatColor.DARK_PURPLE.ordinal()}, // DARK_PURPLE
            {255, 170, 0, ChatColor.GOLD.ordinal()},     // GOLD
            {170, 170, 170, ChatColor.GRAY.ordinal()},   // GRAY
            {85, 85, 85, ChatColor.DARK_GRAY.ordinal()}, // DARK_GRAY 
            {85, 85, 255, ChatColor.BLUE.ordinal()},     // BLUE
            {85, 255, 85, ChatColor.GREEN.ordinal()},    // GREEN
            {85, 255, 255, ChatColor.AQUA.ordinal()},    // AQUA
            {255, 85, 85, ChatColor.RED.ordinal()},      // RED
            {255, 85, 255, ChatColor.LIGHT_PURPLE.ordinal()}, // LIGHT_PURPLE
            {255, 255, 85, ChatColor.YELLOW.ordinal()},  // YELLOW
            {255, 255, 255, ChatColor.WHITE.ordinal()}   // WHITE
        };
        
        // 红色-紫色-蓝色渐变特殊处理
        if (r > 200 && b > 150 && g < 100) {
            if (r > b) return ChatColor.RED;
            if (r < b) return ChatColor.BLUE;
            return ChatColor.LIGHT_PURPLE;
        }
        
        // 红色-橙色-黄色渐变特殊处理
        if (r > 200 && g > 100 && b < 100) {
            if (g < 150) return ChatColor.RED;
            if (g < 220) return ChatColor.GOLD;
            return ChatColor.YELLOW;
        }
        
        // 标准颜色距离计算
        ChatColor closestColor = ChatColor.WHITE;
        double closestDistance = Double.MAX_VALUE;
        
        for (int[] color : colorMap) {
            int cr = color[0];
            int cg = color[1];
            int cb = color[2];
            int ordinal = color[3];
            
            // 加权欧几里得距离 - 人眼对绿色更敏感
            double distance = Math.sqrt(
                Math.pow(cr - r, 2) * 0.30 + 
                Math.pow(cg - g, 2) * 0.59 + 
                Math.pow(cb - b, 2) * 0.11
            );
            
            if (distance < closestDistance) {
                closestDistance = distance;
                closestColor = ChatColor.values()[ordinal];
            }
        }
        
        return closestColor;
    }
} 