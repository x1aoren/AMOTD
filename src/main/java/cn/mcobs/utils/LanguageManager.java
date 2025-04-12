package cn.mcobs.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class LanguageManager {
    private final JavaPlugin plugin;
    private String currentLanguage;
    private Map<String, String> messages;
    private static final Gson gson = new Gson();
    
    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        this.currentLanguage = "en";
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
            plugin.getLogger().warning("Error formatting message '" + key + "': " + e.getMessage());
            return message;
        }
    }
    
    private void loadLanguage() {
        // 确保语言文件夹存在
        File languagesFolder = new File(plugin.getDataFolder(), "languages");
        if (!languagesFolder.exists()) {
            languagesFolder.mkdirs();
            saveDefaultLanguageFiles();
        }
        
        // 加载指定语言文件
        File languageFile = new File(languagesFolder, currentLanguage + ".json");
        if (!languageFile.exists()) {
            plugin.getLogger().warning("Language file " + currentLanguage + ".json not found, falling back to English");
            saveResource("languages/en.json", false);
            languageFile = new File(languagesFolder, "en.json");
            currentLanguage = "en";
        }
        
        try (Reader reader = new InputStreamReader(new FileInputStream(languageFile), StandardCharsets.UTF_8)) {
            messages = gson.fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Loaded language: " + currentLanguage);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load language file: " + languageFile.getName(), e);
            messages = new HashMap<>();
        }
    }
    
    private void saveDefaultLanguageFiles() {
        // 保存所有支持的语言文件
        saveResource("languages/en.json", false);
        saveResource("languages/zh_cn.json", false);
    }
    
    private void saveResource(String resourcePath, boolean replace) {
        try {
            InputStream in = plugin.getResource(resourcePath);
            if (in == null) {
                plugin.getLogger().warning("Could not find resource: " + resourcePath);
                return;
            }
            
            File outFile = new File(plugin.getDataFolder(), resourcePath);
            if (!outFile.exists() || replace) {
                outFile.getParentFile().mkdirs();
                try (FileOutputStream out = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
            }
            in.close();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + resourcePath, e);
        }
    }
} 