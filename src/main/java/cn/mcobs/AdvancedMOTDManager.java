package cn.mcobs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

public class AdvancedMOTDManager {
    
    private final AMOTD plugin;
    private final boolean useNativeApi;
    private final boolean supportRgb;
    
    public AdvancedMOTDManager(AMOTD plugin) {
        this.plugin = plugin;
        
        // 检查服务器环境
        boolean hasAdventureApi = false;
        boolean hasRgbSupport = false;
        
        try {
            Class.forName("net.kyori.adventure.text.Component");
            Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            hasAdventureApi = true;
            
            // 检查RGB支持
            try {
                net.md_5.bungee.api.ChatColor.of("#FF5555");
                hasRgbSupport = true;
            } catch (Throwable t) {
                hasRgbSupport = false;
            }
        } catch (ClassNotFoundException e) {
            hasAdventureApi = false;
        }
        
        this.useNativeApi = hasAdventureApi;
        this.supportRgb = hasRgbSupport;
        
        plugin.getLogger().info("高级MOTD管理器初始化: Adventure API " + 
                                (useNativeApi ? "可用" : "不可用") + 
                                ", RGB颜色支持 " + (supportRgb ? "可用" : "不可用"));
    }
    
    /**
     * 处理MOTD文本，应用渐变色效果
     */
    public String processMOTD(String input, boolean useMinimessage) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("处理MOTD: " + input + ", 使用MiniMessage: " + useMinimessage);
        }
        
        if (!useMinimessage) {
            // 传统格式，直接处理颜色代码
            return ChatColor.translateAlternateColorCodes('&', input);
        }
        
        if (useNativeApi) {
            try {
                // 使用原生Adventure API处理
                String result = processWithNativeApi(input);
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("原生API处理结果: " + result);
                }
                return result;
            } catch (Exception e) {
                plugin.getLogger().warning("使用原生API处理MiniMessage失败: " + e.getMessage());
                // 回退到简易解析器
                return SimpleMiniMessage.parseMiniMessage(input);
            }
        } else {
            // 使用简易解析器
            return SimpleMiniMessage.parseMiniMessage(input);
        }
    }
    
    /**
     * 使用原生Adventure API处理MiniMessage
     */
    private String processWithNativeApi(String input) {
        // 解析MiniMessage
        Component component = MiniMessage.miniMessage().deserialize(input);
        
        // 转换为传统格式
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
    
    /**
     * 检查是否支持RGB颜色
     */
    public boolean supportsRgb() {
        return supportRgb;
    }
    
    /**
     * 检查是否支持原生Adventure API
     */
    public boolean supportsNativeApi() {
        return useNativeApi;
    }
} 