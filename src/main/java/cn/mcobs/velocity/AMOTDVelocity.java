package cn.mcobs.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Plugin(id = "amotd", 
        name = "AMOTD", 
        version = "1.0",
        description = "高级MOTD插件，支持渐变颜色和MiniMessage格式",
        authors = {"Your Name"})
public class AMOTDVelocity {
    
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private File configFile;
    private VelocityConfigManager configManager;
    private VelocityMOTDListener motdListener;
    
    @Inject
    public AMOTDVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }
    
    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        // 创建配置目录
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                logger.error("无法创建配置目录", e);
                return;
            }
        }
        
        // 生成中文配置文件
        saveChineseConfig();
        
        // 创建icons文件夹
        Path iconsPath = dataDirectory.resolve("icons");
        if (!Files.exists(iconsPath)) {
            try {
                Files.createDirectories(iconsPath);
            } catch (IOException e) {
                logger.error("无法创建icons目录", e);
            }
        }
        
        // 加载配置
        configManager = new VelocityConfigManager(this);
        configManager.loadConfig();
        
        // 注册事件监听器
        motdListener = new VelocityMOTDListener(this);
        server.getEventManager().register(this, motdListener);
        
        // 注册命令处理器
        server.getCommandManager().register(
            server.getCommandManager().metaBuilder("amotd").build(),
            new VelocityCommandHandler(this)
        );
        
        logger.info("AMOTD 插件已启用! (Velocity版本)");
    }
    
    private void saveChineseConfig() {
        Path chineseConfigPath = dataDirectory.resolve("config_zh.yml");
        if (!Files.exists(chineseConfigPath)) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config_zh.yml")) {
                if (in != null) {
                    Files.copy(in, chineseConfigPath);
                    if (configManager != null && configManager.getBoolean("debug", false)) {
                        logger.info("已生成中文配置文件 config_zh.yml");
                    }
                }
            } catch (IOException e) {
                logger.error("无法创建中文配置文件: " + e.getMessage());
            }
        }
    }
    
    public ProxyServer getServer() {
        return server;
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    public Path getDataDirectory() {
        return dataDirectory;
    }
    
    public VelocityConfigManager getConfigManager() {
        return configManager;
    }
    
    // 修改图标重载方法
    public void reloadServerIcons() {
        // 只重新加载图标，不重新保存配置
        if (motdListener != null) {
            motdListener.reloadServerIcons();
            if (configManager.getBoolean("debug", false)) {
                logger.info("服务器图标已重新加载");
            }
        }
    }
} 