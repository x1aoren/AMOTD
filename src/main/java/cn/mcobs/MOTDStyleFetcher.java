package cn.mcobs;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class MOTDStyleFetcher {
    
    private final AMOTD plugin;
    
    public MOTDStyleFetcher(AMOTD plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 获取MOTD样式
     * @param styleCode 样式码
     * @return 是否成功
     */
    public boolean fetchStyle(String styleCode, Callback callback) {
        // 在异步线程中执行网络操作
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 构建API URL
                String apiUrl = "https://motd.mcobs.cn/api/motd/" + styleCode;
                
                // 发送HTTP请求
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");
                
                // 检查响应码
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        callback.onFailure("API请求失败，响应码: " + responseCode));
                    return;
                }
                
                // 读取响应
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // 解析JSON
                JSONObject jsonObject = new JSONObject(response.toString());
                
                // 获取格式类型，默认为minecraft
                String formatType = jsonObject.optString("type", "minecraft");
                
                // 获取图标URL
                String iconUrl = jsonObject.optString("icon", "");
                
                // 从content子对象获取MOTD内容
                JSONObject content = jsonObject.optJSONObject("content");
                String line1, line2;
                
                if (content != null) {
                    // 新格式：从content对象获取
                    line1 = content.optString("line1", "");
                    line2 = content.optString("line2", "");
                } else {
                    // 兼容旧格式：直接从主对象获取
                    line1 = jsonObject.optString("line1", "");
                    line2 = jsonObject.optString("line2", "");
                }
                
                // 下载图标
                boolean iconSuccess = false;
                if (iconUrl != null && !iconUrl.isEmpty() && !iconUrl.equals("")) {
                    iconSuccess = downloadIcon(iconUrl);
                }
                
                // 根据格式类型更新配置
                boolean configSuccess = updateConfig(line1, line2, formatType);
                
                // 返回结果
                final boolean finalIconSuccess = iconSuccess;
                final String finalFormatType = formatType;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (configSuccess) {
                        callback.onSuccess(line1, line2, finalIconSuccess, finalFormatType);
                    } else {
                        callback.onFailure("配置更新失败");
                    }
                });
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "获取MOTD样式时出错", e);
                Bukkit.getScheduler().runTask(plugin, () -> 
                    callback.onFailure("错误: " + e.getMessage()));
            }
        });
        
        return true;
    }
    
    /**
     * 下载图标
     * @param iconUrl 图标URL
     * @return 是否成功
     */
    private boolean downloadIcon(String iconUrl) {
        try {
            // 创建icons文件夹（如果不存在）
            File iconsFolder = new File(plugin.getDataFolder(), "icons");
            if (!iconsFolder.exists()) {
                iconsFolder.mkdirs();
            }
            
            // 下载图片
            URL url = new URL(iconUrl);
            BufferedImage image = ImageIO.read(url);
            
            // 检查图片尺寸
            if (image.getWidth() != 64 || image.getHeight() != 64) {
                plugin.getLogger().warning("图标不是64x64像素，将调整大小");
            }
            
            // 保存图片，使用唯一文件名
            String filename = "motd_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
            File outputFile = new File(iconsFolder, filename);
            ImageIO.write(image, "png", outputFile);
            
            plugin.getLogger().info("成功下载图标到 " + outputFile.getPath());
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "下载图标时出错", e);
            return false;
        }
    }
    
    /**
     * 更新配置文件
     * @param line1 第一行MOTD
     * @param line2 第二行MOTD
     * @param formatType 格式类型 (minecraft 或 minimessage)
     * @return 是否成功
     */
    private boolean updateConfig(String line1, String line2, String formatType) {
        try {
            // 获取配置文件
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            
            // 读取当前配置文件的所有行
            List<String> lines = Files.readAllLines(configFile.toPath(), StandardCharsets.UTF_8);
            List<String> newLines = new ArrayList<>();
            
            boolean inLegacySection = false;
            boolean inMinimessageSection = false;
            boolean updatedLegacy1 = false;
            boolean updatedLegacy2 = false;
            boolean updatedMini1 = false;
            boolean updatedMini2 = false;
            boolean updatedFormat = false;
            
            // 处理每一行
            for (String line : lines) {
                // 检测所在节
                if (line.trim().startsWith("legacy:")) {
                    inLegacySection = true;
                    inMinimessageSection = false;
                } else if (line.trim().startsWith("minimessage:")) {
                    inLegacySection = false;
                    inMinimessageSection = true;
                } else if (line.trim().startsWith("message_format:")) {
                    // 更新消息格式类型
                    newLines.add("message_format: \"" + formatType + "\"");
                    updatedFormat = true;
                    continue;
                }
                
                // 更新对应部分
                if (inLegacySection && line.trim().startsWith("line1:") && "minecraft".equals(formatType)) {
                    newLines.add("  line1: " + escapeYaml(line1));
                    updatedLegacy1 = true;
                    continue;
                } else if (inLegacySection && line.trim().startsWith("line2:") && "minecraft".equals(formatType)) {
                    newLines.add("  line2: " + escapeYaml(line2));
                    updatedLegacy2 = true;
                    continue;
                } else if (inMinimessageSection && line.trim().startsWith("line1:") && "minimessage".equals(formatType)) {
                    newLines.add("  line1: " + escapeYaml(line1));
                    updatedMini1 = true;
                    continue;
                } else if (inMinimessageSection && line.trim().startsWith("line2:") && "minimessage".equals(formatType)) {
                    newLines.add("  line2: " + escapeYaml(line2));
                    updatedMini2 = true;
                    continue;
                }
                
                // 保留原行
                newLines.add(line);
            }
            
            // 如果没有找到对应的行，添加到合适的位置
            if ("minecraft".equals(formatType) && (!updatedLegacy1 || !updatedLegacy2)) {
                int legacyIndex = findIndexStartingWith(newLines, "legacy:");
                if (legacyIndex >= 0) {
                    if (!updatedLegacy1) {
                        newLines.add(legacyIndex + 1, "  line1: " + escapeYaml(line1));
                    }
                    if (!updatedLegacy2) {
                        newLines.add(legacyIndex + 2, "  line2: " + escapeYaml(line2));
                    }
                }
            }
            
            if ("minimessage".equals(formatType) && (!updatedMini1 || !updatedMini2)) {
                int miniIndex = findIndexStartingWith(newLines, "minimessage:");
                if (miniIndex >= 0) {
                    if (!updatedMini1) {
                        newLines.add(miniIndex + 1, "  line1: " + escapeYaml(line1));
                    }
                    if (!updatedMini2) {
                        newLines.add(miniIndex + 2, "  line2: " + escapeYaml(line2));
                    }
                }
            }
            
            if (!updatedFormat) {
                newLines.add("message_format: \"" + formatType + "\"");
            }
            
            // 写回配置文件
            Files.write(configFile.toPath(), newLines, StandardCharsets.UTF_8);
            
            // 重新加载配置
            plugin.reloadConfig();
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("更新配置文件时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 在列表中查找以指定前缀开头的行的索引
     */
    private int findIndexStartingWith(List<String> lines, String prefix) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().startsWith(prefix)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 转义YAML特殊字符，确保字符串保持引号
     */
    private String escapeYaml(String str) {
        if (str == null) {
            return "";
        }
        
        // 转义特殊字符
        String escaped = str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
        
        // 如果字符串包含特殊字符或为空，强制使用引号
        if (escaped.isEmpty() || 
            escaped.contains(":") || 
            escaped.contains("#") || 
            escaped.contains("[") || 
            escaped.contains("]") || 
            escaped.contains("{") || 
            escaped.contains("}") || 
            escaped.contains(",") || 
            escaped.contains("&") || 
            escaped.contains("*") || 
            escaped.contains("!") || 
            escaped.contains("|") || 
            escaped.contains(">") || 
            escaped.contains("'") || 
            escaped.contains("\"") || 
            escaped.contains("%") || 
            escaped.contains("@") || 
            escaped.contains("`") ||
            escaped.matches("^[0-9]+$") || // 纯数字
            escaped.matches("^(true|false|yes|no|on|off|null)$") || // 布尔值
            escaped.startsWith(" ") || 
            escaped.endsWith(" ") ||
            escaped.contains("\n") ||
            escaped.contains("\r") ||
            escaped.contains("\t")) {
            return "\"" + escaped + "\"";
        }
        
        return escaped;
    }
    
    /**
     * 回调接口
     */
    public interface Callback {
        void onSuccess(String line1, String line2, boolean iconSuccess, String formatType);
        void onFailure(String errorMessage);
    }
} 