package cn.mcobs;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;

public class MOTDManager {
    private final AMOTD plugin;
    
    public MOTDManager(AMOTD plugin) {
        this.plugin = plugin;
    }
    
    public String processMOTD(String motd, boolean useMinimessage) {
        if (useMinimessage) {
            return processMinimessageMOTD(motd);
        } else {
            return processLegacyMOTD(motd);
        }
    }
    
    public String processLegacyMOTD(String motd) {
        // 使用新的API处理传统的颜色代码
        return LegacyComponentSerializer.legacyAmpersand().serialize(
               LegacyComponentSerializer.legacyAmpersand().deserialize(motd));
    }
    
    public String processMinimessageMOTD(String motd) {
        try {
            // 尝试使用MiniMessage
            Component component = MiniMessage.miniMessage().deserialize(motd);
            return LegacyComponentSerializer.legacySection().serialize(component);
        } catch (Exception e) {
            plugin.getLogger().warning("处理MiniMessage格式时出错: " + e.getMessage());
            return motd;
        }
    }
} 