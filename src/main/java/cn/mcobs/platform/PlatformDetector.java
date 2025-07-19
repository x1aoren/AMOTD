package cn.mcobs.platform;

import org.bukkit.Bukkit;
import java.lang.reflect.Method;

/**
 * 平台检测工具类，用于检测当前运行环境
 */
public class PlatformDetector {
    
    /**
     * 平台类型枚举
     */
    public enum PlatformType {
        BUKKIT,     // 普通Bukkit/Spigot服务端
        PAPER,      // Paper服务端
        FOLIA,      // Folia服务端
        VELOCITY,   // Velocity代理服务端
        UNKNOWN     // 未知平台
    }
    
    /**
     * 检测当前运行的平台类型
     * @return 平台类型
     */
    public static PlatformType detectPlatform() {
        // 检查是否为Velocity平台
        try {
            Class.forName("com.velocitypowered.api.proxy.ProxyServer");
            return PlatformType.VELOCITY;
        } catch (ClassNotFoundException ignored) {
            // 不是Velocity
        }
        
        // 检查是否为Folia平台
        try {
            Class<?> foliaClass = Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            if (foliaClass != null) {
                return PlatformType.FOLIA;
            }
        } catch (ClassNotFoundException ignored) {
            // 不是Folia
        }
        
        // 检查是否为Paper平台
        try {
            Method isPrimaryThreadMethod = Bukkit.class.getMethod("isPrimaryThread");
            if (isPrimaryThreadMethod != null) {
                return PlatformType.PAPER;
            }
        } catch (NoSuchMethodException ignored) {
            // 不是Paper
        }
        
        // 默认为Bukkit/Spigot平台
        return PlatformType.BUKKIT;
    }
    
    /**
     * 检查是否为Folia服务端
     * @return 是否为Folia
     */
    public static boolean isFolia() {
        return detectPlatform() == PlatformType.FOLIA;
    }
    
    /**
     * 检查是否为Paper服务端
     * @return 是否为Paper
     */
    public static boolean isPaper() {
        PlatformType platform = detectPlatform();
        return platform == PlatformType.PAPER || platform == PlatformType.FOLIA;
    }
    
    /**
     * 检查是否为Bukkit/Spigot服务端
     * @return 是否为Bukkit/Spigot
     */
    public static boolean isBukkit() {
        PlatformType platform = detectPlatform();
        return platform == PlatformType.BUKKIT || platform == PlatformType.PAPER || platform == PlatformType.FOLIA;
    }
    
    /**
     * 检查是否为Velocity代理服务端
     * @return 是否为Velocity
     */
    public static boolean isVelocity() {
        return detectPlatform() == PlatformType.VELOCITY;
    }
} 