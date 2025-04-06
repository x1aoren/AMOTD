package cn.mcobs;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * 简易MiniMessage解析器 - 重写版
 */
public class SimpleMiniMessage {
    
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
        
        COLOR_MAP.put("bold", ChatColor.BOLD);
        COLOR_MAP.put("italic", ChatColor.ITALIC);
        COLOR_MAP.put("underlined", ChatColor.UNDERLINE);
        COLOR_MAP.put("strikethrough", ChatColor.STRIKETHROUGH);
        COLOR_MAP.put("obfuscated", ChatColor.MAGIC);
        COLOR_MAP.put("reset", ChatColor.RESET);
    }
    
    /**
     * 将MiniMessage格式转换为传统颜色代码 - 完全重写
     */
    public static String parseMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // 预处理彩虹标签
        input = handleSpecialTags(input);
        
        // 字符转换
        CharacterConverter converter = new CharacterConverter(input);
        return converter.convert();
    }
    
    /**
     * 处理特殊标签，如彩虹和渐变
     */
    private static String handleSpecialTags(String input) {
        // 处理彩虹标签
        Pattern rainbowPattern = Pattern.compile("<rainbow>(.*?)</rainbow>");
        Matcher rainbowMatcher = rainbowPattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        
        while (rainbowMatcher.find()) {
            String content = rainbowMatcher.group(1);
            String rainbowText = createRainbowText(content);
            rainbowMatcher.appendReplacement(sb, Matcher.quoteReplacement(rainbowText));
        }
        rainbowMatcher.appendTail(sb);
        
        input = sb.toString();
        
        // 处理渐变标签
        Pattern gradientPattern = Pattern.compile("<gradient:([^:>]+):([^>]+)>(.*?)</gradient>");
        Matcher gradientMatcher = gradientPattern.matcher(input);
        sb = new StringBuffer();
        
        while (gradientMatcher.find()) {
            String startColor = gradientMatcher.group(1);
            String endColor = gradientMatcher.group(2);
            String content = gradientMatcher.group(3);
            String gradientText = createGradientText(content, startColor, endColor);
            gradientMatcher.appendReplacement(sb, Matcher.quoteReplacement(gradientText));
        }
        gradientMatcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * 创建彩虹文本
     */
    private static String createRainbowText(String text) {
        ChatColor[] rainbowColors = {
            ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW, 
            ChatColor.GREEN, ChatColor.AQUA, ChatColor.BLUE, 
            ChatColor.LIGHT_PURPLE
        };
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            int colorIndex = i % rainbowColors.length;
            sb.append(rainbowColors[colorIndex])
              .append(text.charAt(i));
        }
        
        return sb.toString();
    }
    
    /**
     * 创建渐变文本
     */
    private static String createGradientText(String text, String startColor, String endColor) {
        try {
            // 解析颜色
            int startRed, startGreen, startBlue;
            int endRed, endGreen, endBlue;
            
            if (startColor.startsWith("#")) {
                startColor = startColor.substring(1);
            }
            if (endColor.startsWith("#")) {
                endColor = endColor.substring(1);
            }
            
            startRed = Integer.parseInt(startColor.substring(0, 2), 16);
            startGreen = Integer.parseInt(startColor.substring(2, 4), 16);
            startBlue = Integer.parseInt(startColor.substring(4, 6), 16);
            
            endRed = Integer.parseInt(endColor.substring(0, 2), 16);
            endGreen = Integer.parseInt(endColor.substring(2, 4), 16);
            endBlue = Integer.parseInt(endColor.substring(4, 6), 16);
            
            // 生成渐变
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                float ratio = (float) i / Math.max(1, text.length() - 1);
                
                int red = Math.round(startRed + ratio * (endRed - startRed));
                int green = Math.round(startGreen + ratio * (endGreen - startGreen));
                int blue = Math.round(startBlue + ratio * (endBlue - startBlue));
                
                ChatColor nearestColor = findNearestColor(red, green, blue);
                sb.append(nearestColor).append(text.charAt(i));
            }
            
            return sb.toString();
        } catch (Exception e) {
            return text;
        }
    }
    
    /**
     * 查找最接近的Minecraft颜色
     */
    private static ChatColor findNearestColor(int red, int green, int blue) {
        ChatColor nearestColor = ChatColor.WHITE;
        double nearestDistance = Double.MAX_VALUE;
        
        int[][] colorValues = {
            {0, 0, 0},         // BLACK
            {0, 0, 170},       // DARK_BLUE
            {0, 170, 0},       // DARK_GREEN
            {0, 170, 170},     // DARK_AQUA
            {170, 0, 0},       // DARK_RED
            {170, 0, 170},     // DARK_PURPLE
            {255, 170, 0},     // GOLD
            {170, 170, 170},   // GRAY
            {85, 85, 85},      // DARK_GRAY
            {85, 85, 255},     // BLUE
            {85, 255, 85},     // GREEN
            {85, 255, 255},    // AQUA
            {255, 85, 85},     // RED
            {255, 85, 255},    // LIGHT_PURPLE
            {255, 255, 85},    // YELLOW
            {255, 255, 255}    // WHITE
        };
        
        ChatColor[] colors = {
            ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA,
            ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.GRAY,
            ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA,
            ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE
        };
        
        for (int i = 0; i < colorValues.length; i++) {
            double distance = Math.pow(colorValues[i][0] - red, 2) +
                             Math.pow(colorValues[i][1] - green, 2) +
                             Math.pow(colorValues[i][2] - blue, 2);
            
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestColor = colors[i];
            }
        }
        
        return nearestColor;
    }
    
    /**
     * 字符逐个转换器 - 核心解析引擎
     */
    private static class CharacterConverter {
        private final String input;
        private final StringBuilder output = new StringBuilder();
        private final Stack<String> formatStack = new Stack<>();
        private int pos = 0;
        
        public CharacterConverter(String input) {
            this.input = input;
        }
        
        public String convert() {
            while (pos < input.length()) {
                char c = input.charAt(pos);
                
                if (c == '<') {
                    handleTag();
                } else {
                    output.append(c);
                    pos++;
                }
            }
            
            return output.toString();
        }
        
        private void handleTag() {
            int tagStart = pos;
            pos++; // 跳过 <
            
            // 检查是否是闭合标签
            boolean isClosing = input.charAt(pos) == '/';
            if (isClosing) {
                // 完全跳过闭合标签，直接找到>
                while (pos < input.length() && input.charAt(pos) != '>') {
                    pos++;
                }
                if (pos < input.length()) pos++; // 跳过>
                return; // 忽略所有闭合标签，完全依赖我们自己的格式堆栈
            }
            
            // 读取标签名
            StringBuilder tagName = new StringBuilder();
            while (pos < input.length() && input.charAt(pos) != '>' && input.charAt(pos) != ':') {
                tagName.append(input.charAt(pos));
                pos++;
            }
            
            // 处理颜色标签的额外参数
            String colorParam = null;
            if (pos < input.length() && input.charAt(pos) == ':') {
                pos++; // 跳过 :
                StringBuilder param = new StringBuilder();
                while (pos < input.length() && input.charAt(pos) != '>') {
                    param.append(input.charAt(pos));
                    pos++;
                }
                colorParam = param.toString();
            }
            
            // 跳过 >
            if (pos < input.length() && input.charAt(pos) == '>') {
                pos++;
            }
            
            String tag = tagName.toString().toLowerCase();
            
            // 处理标签
            if (isClosing) {
                // 闭合标签，弹出堆栈并应用重置
                if (!formatStack.isEmpty() && formatStack.peek().equals(tag)) {
                    formatStack.pop();
                    output.append(ChatColor.RESET);
                    
                    // 重新应用堆栈中的所有格式
                    for (String format : formatStack) {
                        applyFormat(format, null);
                    }
                }
            } else {
                // 开始标签，解析并应用
                if (tag.equals("color") && colorParam != null) {
                    // 颜色标签带参数，如 <color:#FF5555>
                    formatStack.push(tag);
                    applyFormat(tag, colorParam);
                } else {
                    // 普通标签，如 <bold>
                    formatStack.push(tag);
                    applyFormat(tag, null);
                }
            }
        }
        
        private void applyFormat(String tag, String param) {
            if (tag.equals("color") && param != null) {
                // 处理十六进制颜色
                if (param.startsWith("#")) {
                    applyHexColor(param);
                } else {
                    // 尝试查找命名颜色
                    ChatColor color = COLOR_MAP.get(param.toLowerCase());
                    if (color != null) {
                        output.append(color);
                    }
                }
            } else {
                // 处理普通格式标签
                ChatColor color = COLOR_MAP.get(tag.toLowerCase());
                if (color != null) {
                    output.append(color);
                }
            }
        }
        
        private void applyHexColor(String hexColor) {
            try {
                // 移除#前缀
                if (hexColor.startsWith("#")) {
                    hexColor = hexColor.substring(1);
                }
                
                // 解析RGB值
                int red = Integer.parseInt(hexColor.substring(0, 2), 16);
                int green = Integer.parseInt(hexColor.substring(2, 4), 16);
                int blue = Integer.parseInt(hexColor.substring(4, 6), 16);
                
                // 找到最接近的Minecraft颜色
                ChatColor nearestColor = findNearestColor(red, green, blue);
                output.append(nearestColor);
                
            } catch (Exception e) {
                // 解析失败，使用默认颜色
                output.append(ChatColor.WHITE);
            }
        }
    }
} 