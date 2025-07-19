package cn.mcobs.platform;

import org.bukkit.plugin.java.JavaPlugin;
import cn.mcobs.platform.PlatformDetector.PlatformType;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 平台管理器类，提供统一的API调用
 */
public class PlatformManager {
    
    private final JavaPlugin plugin;
    private final PlatformType platformType;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public PlatformManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.platformType = PlatformDetector.detectPlatform();
    }
    
    /**
     * 获取平台类型
     * @return 平台类型
     */
    public PlatformType getPlatformType() {
        return platformType;
    }
    
    /**
     * 在主线程执行任务
     * @param task 要执行的任务
     */
    public void runTask(Runnable task) {
        if (platformType == PlatformType.FOLIA) {
            // Folia平台使用全局区域调度器
            try {
                Class<?> serverClass = plugin.getServer().getClass();
                Object scheduler = serverClass.getMethod("getGlobalRegionScheduler").invoke(plugin.getServer());
                scheduler.getClass().getMethod("execute", JavaPlugin.class, Runnable.class)
                        .invoke(scheduler, plugin, task);
            } catch (Exception e) {
                plugin.getLogger().severe("无法在Folia平台执行任务: " + e.getMessage());
                // 回退到普通Bukkit调度器
                plugin.getServer().getScheduler().runTask(plugin, task);
            }
        } else {
            // 其他平台使用普通Bukkit调度器
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }
    
    /**
     * 异步执行任务
     * @param task 要执行的任务
     */
    public void runTaskAsync(Runnable task) {
        if (platformType == PlatformType.FOLIA) {
            // Folia平台使用异步调度器
            try {
                Class<?> serverClass = plugin.getServer().getClass();
                Object scheduler = serverClass.getMethod("getAsyncScheduler").invoke(plugin.getServer());
                scheduler.getClass().getMethod("runNow", JavaPlugin.class, Consumer.class)
                        .invoke(scheduler, plugin, (Consumer<Object>)(obj) -> task.run());
            } catch (Exception e) {
                plugin.getLogger().severe("无法在Folia平台异步执行任务: " + e.getMessage());
                // 回退到普通Bukkit调度器
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
            }
        } else {
            // 其他平台使用普通Bukkit调度器
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
        }
    }
    
    /**
     * 在主线程延迟执行任务
     * @param task 要执行的任务
     * @param delay 延迟时间（ticks）
     */
    public void runTaskLater(Runnable task, long delay) {
        if (platformType == PlatformType.FOLIA) {
            // Folia平台使用全局区域调度器
            try {
                Class<?> serverClass = plugin.getServer().getClass();
                Object scheduler = serverClass.getMethod("getGlobalRegionScheduler").invoke(plugin.getServer());
                scheduler.getClass().getMethod("runDelayed", JavaPlugin.class, Runnable.class, long.class)
                        .invoke(scheduler, plugin, task, delay);
            } catch (Exception e) {
                plugin.getLogger().severe("无法在Folia平台延迟执行任务: " + e.getMessage());
                // 回退到普通Bukkit调度器
                plugin.getServer().getScheduler().runTaskLater(plugin, task, delay);
            }
        } else {
            // 其他平台使用普通Bukkit调度器
            plugin.getServer().getScheduler().runTaskLater(plugin, task, delay);
        }
    }
    
    /**
     * 在主线程重复执行任务
     * @param task 要执行的任务
     * @param delay 初始延迟（ticks）
     * @param period 周期（ticks）
     */
    public void runTaskTimer(Runnable task, long delay, long period) {
        if (platformType == PlatformType.FOLIA) {
            // Folia平台使用全局区域调度器
            try {
                Class<?> serverClass = plugin.getServer().getClass();
                Object scheduler = serverClass.getMethod("getGlobalRegionScheduler").invoke(plugin.getServer());
                scheduler.getClass().getMethod("runAtFixedRate", JavaPlugin.class, Runnable.class, long.class, long.class)
                        .invoke(scheduler, plugin, task, delay, period);
            } catch (Exception e) {
                plugin.getLogger().severe("无法在Folia平台重复执行任务: " + e.getMessage());
                // 回退到普通Bukkit调度器
                plugin.getServer().getScheduler().runTaskTimer(plugin, task, delay, period);
            }
        } else {
            // 其他平台使用普通Bukkit调度器
            plugin.getServer().getScheduler().runTaskTimer(plugin, task, delay, period);
        }
    }
    
    /**
     * 获取插件实例
     * @return 插件实例
     */
    public JavaPlugin getPlugin() {
        return plugin;
    }
} 