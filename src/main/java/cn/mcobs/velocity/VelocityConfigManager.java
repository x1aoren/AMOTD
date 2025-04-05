package cn.mcobs.velocity;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VelocityConfigManager {
    
    private final AMOTDVelocity plugin;
    private ConfigurationNode config;
    private Path configPath;
    
    public VelocityConfigManager(AMOTDVelocity plugin) {
        this.plugin = plugin;
        this.configPath = plugin.getDataDirectory().resolve("config.yml");
    }
    
    public void loadConfig() {
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
        // 先更新内存中的配置
        String[] keys = path.split("\\.");
        ConfigurationNode node = config;
        
        for (int i = 0; i < keys.length - 1; i++) {
            node = node.node(keys[i]);
        }
        
        try {
            node.node(keys[keys.length - 1]).set(value);
        } catch (SerializationException e) {
            plugin.getLogger().error("设置配置值时出错", e);
            throw new RuntimeException("设置配置值失败: " + e.getMessage(), e);
        }
        
        // 然后使用正则表达式更新文件中的特定行，保留注释和格式
        updateConfigFile(path, value);
    }
    
    public void saveConfig() {
        // 只重新加载配置，不重写文件
        loadConfig();
    }
    
    /**
     * 更新配置文件中的特定值，保留注释和格式
     * 
     * @param path 配置路径，如 "message_format"
     * @param value 新值
     */
    private void updateConfigFile(String path, String value) {
        try {
            // 读取整个配置文件
            List<String> lines = Files.readAllLines(configPath);
            List<String> updatedLines = new ArrayList<>();
            
            // YAML的多层路径转换为正则表达式模式
            String pathPattern = createPathRegex(path);
            Pattern pattern = Pattern.compile(pathPattern);
            boolean updated = false;
            
            for (String line : lines) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    // 替换值，保留缩进和键名
                    String indent = matcher.group(1);
                    String key = matcher.group(2);
                    
                    // 处理字符串值，确保正确添加引号
                    String formattedValue = formatValue(value);
                    updatedLines.add(indent + key + ": " + formattedValue);
                    updated = true;
                } else {
                    updatedLines.add(line);
                }
            }
            
            // 如果未找到并更新，则记录警告
            if (!updated) {
                plugin.getLogger().warn("无法在配置文件中找到路径: " + path);
                return;
            }
            
            // 写回文件
            Files.write(configPath, updatedLines);
            plugin.getLogger().info("已更新配置项: " + path);
            
        } catch (IOException e) {
            plugin.getLogger().error("更新配置文件时出错", e);
        }
    }
    
    /**
     * 将配置路径转换为匹配YAML的正则表达式
     */
    private String createPathRegex(String path) {
        String[] parts = path.split("\\.");
        if (parts.length == 1) {
            // 顶级键
            return "(\\s*)(" + Pattern.quote(parts[0]) + ")\\s*:.*";
        } else {
            // 嵌套键 - 只匹配最后一个键
            return "(\\s*)(" + Pattern.quote(parts[parts.length - 1]) + ")\\s*:.*";
        }
    }
    
    /**
     * 根据值的类型格式化YAML值
     */
    private String formatValue(String value) {
        // 如果是字符串且包含特殊字符，添加引号
        if (value.contains(" ") || value.contains(":") || value.isEmpty() ||
            value.contains("'") || value.contains("\"") || value.contains("\n")) {
            
            // 处理包含引号的字符串
            if (value.contains("\"")) {
                return "'" + value + "'";
            } else {
                return "\"" + value + "\"";
            }
        }
        return value;
    }
} 