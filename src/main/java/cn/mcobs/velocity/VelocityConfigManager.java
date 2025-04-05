package cn.mcobs.velocity;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class VelocityConfigManager {
    
    private final AMOTDVelocity plugin;
    private ConfigurationNode config;
    
    public VelocityConfigManager(AMOTDVelocity plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        Path configPath = plugin.getDataDirectory().resolve("config.yml");
        
        // 如果配置文件不存在，创建默认配置
        if (!Files.exists(configPath)) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configPath);
                } else {
                    plugin.getLogger().error("无法从资源中加载默认配置");
                    return;
                }
            } catch (IOException e) {
                plugin.getLogger().error("无法创建默认配置文件", e);
                return;
            }
        }
        
        // 加载配置
        try {
            config = YamlConfigurationLoader.builder()
                    .path(configPath)
                    .build()
                    .load();
            plugin.getLogger().info("配置文件已加载");
        } catch (IOException e) {
            plugin.getLogger().error("无法加载配置文件", e);
        }
    }
    
    public ConfigurationNode getConfig() {
        return config;
    }
    
    public String getString(String path, String defaultValue) {
        String[] keys = path.split("\\.");
        ConfigurationNode node = config;
        
        for (String key : keys) {
            node = node.node(key);
        }
        
        return node.getString(defaultValue);
    }
    
    public boolean getBoolean(String path, boolean defaultValue) {
        String[] keys = path.split("\\.");
        ConfigurationNode node = config;
        
        for (String key : keys) {
            node = node.node(key);
        }
        
        return node.getBoolean(defaultValue);
    }
    
    public int getInt(String path, int defaultValue) {
        String[] keys = path.split("\\.");
        ConfigurationNode node = config;
        
        for (String key : keys) {
            node = node.node(key);
        }
        
        return node.getInt(defaultValue);
    }
    
    public void setString(String path, String value) {
        String[] keys = path.split("\\.");
        ConfigurationNode node = config;
        
        for (int i = 0; i < keys.length - 1; i++) {
            node = node.node(keys[i]);
        }
        
        try {
            node.node(keys[keys.length - 1]).set(value);
            saveConfig();
        } catch (org.spongepowered.configurate.serialize.SerializationException e) {
            plugin.getLogger().error("设置配置值时出错", e);
            throw new RuntimeException("设置配置值失败: " + e.getMessage(), e);
        }
    }
    
    public void saveConfig() {
        try {
            YamlConfigurationLoader.builder()
                .path(plugin.getDataDirectory().resolve("config.yml"))
                .build()
                .save(config);
            plugin.getLogger().info("配置文件已保存");
        } catch (IOException e) {
            plugin.getLogger().error("保存配置文件时出错", e);
        }
    }
} 