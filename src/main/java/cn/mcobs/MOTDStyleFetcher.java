package cn.mcobs;

import org.bukkit.plugin.java.JavaPlugin;
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
    
    private final JavaPlugin plugin;
    
    public MOTDStyleFetcher(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 获取MOTD样式
     * @param styleCode 样式码
     * @return 是否成功
     */
    public boolean fetchStyle(String styleCode, Callback callback) {
        // 在异步线程中执行网络操作
        // 检测是否为Folia服务端
        boolean isFolia = isFoliaServer();
        
        if (isFolia) {
            try {
                // 使用反射获取异步调度器
                try {
                    Class<?> serverClass = plugin.getServer().getClass();
                    Object asyncScheduler = serverClass.getMethod("getAsyncScheduler").invoke(plugin.getServer());
                    asyncScheduler.getClass().getMethod("runNow", JavaPlugin.class, java.util.function.Consumer.class)
                            .invoke(asyncScheduler, plugin, (java.util.function.Consumer<Object>) task -> {
                    fetchStyleInternal(styleCode, callback);
                            });
                    } catch (Exception e) {
                        plugin.getLogger().warning("无法使用Folia API执行异步任务: " + e.getMessage());
                        // 回退到传统方法
                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                            fetchStyleInternal(styleCode, callback);
                        });
                    }
                    return true;
            } catch (Exception e) {
                plugin.getLogger().warning("无法使用Folia API执行异步任务: " + e.getMessage());
                // 回退到传统方法
            }
        }
        
        // 传统方法
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            fetchStyleInternal(styleCode, callback);
        });
        
        return true;
    }
    
    /**
     * 内部方法，实际执行获取MOTD样式的逻辑
     */
    private void fetchStyleInternal(String styleCode, Callback callback) {
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
                runOnMainThread(() -> callback.onFailure("API请求失败，响应码: " + responseCode));
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
            runOnMainThread(() -> {
                if (configSuccess) {
                    callback.onSuccess(line1, line2, finalIconSuccess, finalFormatType);
                } else {
                    callback.onFailure("配置更新失败");
                }
            });
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "获取MOTD样式时出错", e);
            runOnMainThread(() -> callback.onFailure("错误: " + e.getMessage()));
        }
    }
    
    /**
     * 在主线程上执行任务
     */
    private void runOnMainThread(Runnable task) {
        boolean isFolia = isFoliaServer();
        
        if (isFolia) {
            try {
                // 使用反射调用Folia API
                try {
                    Class<?> serverClass = plugin.getServer().getClass();
                    Object regionScheduler = serverClass.getMethod("getRegionScheduler").invoke(plugin.getServer());
                    regionScheduler.getClass().getMethod("execute", JavaPlugin.class, Runnable.class)
                            .invoke(regionScheduler, plugin, task);
                } catch (Exception e) {
                    plugin.getLogger().warning("无法使用Folia API执行主线程任务: " + e.getMessage());
                    // 回退到传统方法
                    plugin.getServer().getScheduler().runTask(plugin, task);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("无法使用Folia API执行主线程任务: " + e.getMessage());
                // 回退到传统方法
                plugin.getServer().getScheduler().runTask(plugin, task);
            }
        } else {
            // 传统方法
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }
    
    /**
     * 检测是否为Folia服务端
     */
    private boolean isFoliaServer() {
        try {
            // 尝试加载Folia特有的类
            Class<?> foliaClass = Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return foliaClass != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
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
                
                // 保留其他行
                newLines.add(line);
            }
            
            // 如果没有找到需要更新的行，添加到相应部分
            if (!updatedFormat) {
                int index = findIndexStartingWith(newLines, "legacy:");
                if (index >= 0) {
                    newLines.add(index, "message_format: \"" + formatType + "\"");
                } else {
                    newLines.add("message_format: \"" + formatType + "\"");
                }
            }
            
            if ("minecraft".equals(formatType) && (!updatedLegacy1 || !updatedLegacy2)) {
                int index = findIndexStartingWith(newLines, "legacy:");
                if (index < 0) {
                    newLines.add("legacy:");
                    if (!updatedLegacy1) newLines.add("  line1: " + escapeYaml(line1));
                    if (!updatedLegacy2) newLines.add("  line2: " + escapeYaml(line2));
                } else {
                    if (!updatedLegacy1) newLines.add(index + 1, "  line1: " + escapeYaml(line1));
                    if (!updatedLegacy2) newLines.add(index + (updatedLegacy1 ? 2 : 1), "  line2: " + escapeYaml(line2));
                }
            } else if ("minimessage".equals(formatType) && (!updatedMini1 || !updatedMini2)) {
                int index = findIndexStartingWith(newLines, "minimessage:");
                if (index < 0) {
                    newLines.add("minimessage:");
                    if (!updatedMini1) newLines.add("  line1: " + escapeYaml(line1));
                    if (!updatedMini2) newLines.add("  line2: " + escapeYaml(line2));
                } else {
                    if (!updatedMini1) newLines.add(index + 1, "  line1: " + escapeYaml(line1));
                    if (!updatedMini2) newLines.add(index + (updatedMini1 ? 2 : 1), "  line2: " + escapeYaml(line2));
                }
            }
            
            // 写回文件
            Files.write(configFile.toPath(), newLines, StandardCharsets.UTF_8);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "更新配置文件时出错", e);
            return false;
        }
    }
    
    private int findIndexStartingWith(List<String> lines, String prefix) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().startsWith(prefix)) {
                return i;
            }
        }
        return -1;
    }
    
    private String escapeYaml(String str) {
        if (str == null) return "\"\"";
        
        // 如果是数字、布尔值或特殊字符，需要加引号
        boolean needQuotes = str.isEmpty() || 
                             str.matches("^[0-9]+$") || // 纯数字
                             "true".equalsIgnoreCase(str) || 
                             "false".equalsIgnoreCase(str) ||
                             "yes".equalsIgnoreCase(str) ||
                             "no".equalsIgnoreCase(str) ||
                             "on".equalsIgnoreCase(str) ||
                             "off".equalsIgnoreCase(str) ||
                             str.contains(":") ||
                             str.contains("#") ||
                             str.contains("'") ||
                             str.contains("\"") ||
                             str.contains("{") ||
                             str.contains("}") ||
                             str.contains("[") ||
                             str.contains("]") ||
                             str.contains(",") ||
                             str.contains("&") ||
                             str.contains("*") ||
                             str.contains("?") ||
                             str.contains("|") ||
                             str.contains(">") ||
                             str.contains("<") ||
                             str.contains("=") ||
                             str.contains("!") ||
                             str.contains("%") ||
                             str.contains("@") ||
                             str.contains("`") ||
                             str.startsWith(" ") ||
                             str.endsWith(" ");
        
        if (needQuotes) {
            // 转义双引号
            String escaped = str.replace("\"", "\\\"");
            return "\"" + escaped + "\"";
        } else {
            return str;
        }
    }
    
    public interface Callback {
        void onSuccess(String line1, String line2, boolean iconSuccess, String formatType);
        void onFailure(String errorMessage);
    }
} 