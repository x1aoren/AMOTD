package cn.mcobs.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cn.mcobs.velocity.AMOTDVelocity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class VelocityLanguageManager {
    private final AMOTDVelocity plugin;
    private String currentLanguage;
    private Map<String, String> messages;
    private static final Gson gson = new Gson();
    
    public VelocityLanguageManager(AMOTDVelocity plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        this.currentLanguage = plugin.getConfigManager().getString("language", "en");
        loadLanguage();
    }
    
    public void setLanguage(String language) {
        this.currentLanguage = language;
        loadLanguage();
    }
    
    public String getMessage(String key) {
        return messages.getOrDefault(key, "Missing message: " + key);
    }
    
    public String getMessage(String key, Object... args) {
        String message = getMessage(key);
        try {
            return String.format(message, args);
        } catch (Exception e) {
            plugin.getLogger().error("Error formatting message '" + key + "': " + e.getMessage());
            return message;
        }
    }
    
    private void loadLanguage() {
        // 确保语言文件夹存在
        Path languagesFolder = plugin.getDataDirectory().resolve("languages");
        if (!Files.exists(languagesFolder)) {
            try {
                Files.createDirectories(languagesFolder);
                saveDefaultLanguageFiles();
            } catch (IOException e) {
                plugin.getLogger().error("Failed to create languages directory", e);
                return;
            }
        }
        
        // 加载指定语言文件
        Path languageFile = languagesFolder.resolve(currentLanguage + ".json");
        if (!Files.exists(languageFile)) {
            plugin.getLogger().warn("Language file " + currentLanguage + ".json not found, falling back to English");
            saveResource("languages/en.json", languagesFolder.resolve("en.json"));
            languageFile = languagesFolder.resolve("en.json");
            currentLanguage = "en";
        }
        
        try (Reader reader = Files.newBufferedReader(languageFile, StandardCharsets.UTF_8)) {
            messages = gson.fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
            if (plugin.getConfigManager().getBoolean("debug", false)) {
                plugin.getLogger().info("Loaded language: " + currentLanguage);
            }
        } catch (Exception e) {
            plugin.getLogger().error("Failed to load language file: " + languageFile.getFileName(), e);
            messages = new HashMap<>();
        }
    }
    
    private void saveDefaultLanguageFiles() {
        saveResource("languages/en.json", plugin.getDataDirectory().resolve("languages/en.json"));
        saveResource("languages/zh_cn.json", plugin.getDataDirectory().resolve("languages/zh_cn.json"));
    }
    
    private void saveResource(String resourcePath, Path destination) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                plugin.getLogger().error("Could not find resource: " + resourcePath);
                return;
            }
            
            if (!Files.exists(destination.getParent())) {
                Files.createDirectories(destination.getParent());
            }
            
            if (!Files.exists(destination)) {
                Files.copy(in, destination);
            }
        } catch (IOException e) {
            plugin.getLogger().error("Could not save " + resourcePath, e);
        }
    }
} 