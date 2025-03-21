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
                String iconUrl = jsonObject.getString("icon");
                JSONObject content = jsonObject.getJSONObject("content");
                String line1 = content.getString("line1");
                String line2 = content.getString("line2");
                
                // 下载图标
                boolean iconSuccess = false;
                if (iconUrl != null && !iconUrl.isEmpty()) {
                    iconSuccess = downloadIcon(iconUrl);
                }
                
                // 更新配置
                boolean configSuccess = updateConfig(line1, line2);
                
                // 返回结果
                final boolean finalIconSuccess = iconSuccess;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (configSuccess) {
                        callback.onSuccess(line1, line2, finalIconSuccess);
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
     * @return 是否成功
     */
    private boolean updateConfig(String line1, String line2) {
        try {
            FileConfiguration config = plugin.getConfig();
            config.set("line1", line1);
            config.set("line2", line2);
            plugin.saveConfig();
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "更新配置文件时出错", e);
            return false;
        }
    }
    
    /**
     * 回调接口
     */
    public interface Callback {
        void onSuccess(String line1, String line2, boolean iconSuccess);
        void onFailure(String errorMessage);
    }
} 