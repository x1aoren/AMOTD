package cn.mcobs.folia;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import cn.mcobs.utils.LanguageManager;
import cn.mcobs.MOTDStyleFetcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Consumer;

public class AMOTDCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final LanguageManager lang;
    private final MOTDStyleFetcher styleFetcher;
    
    public AMOTDCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        // 尝试获取语言管理器
        if (plugin instanceof AMOTD) {
            this.lang = ((AMOTD)plugin).getLanguageManager();
        } else {
            this.lang = null;
        }
        this.styleFetcher = new MOTDStyleFetcher(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6" + lang.getMessage("help_title"));
            sender.sendMessage("§e" + lang.getMessage("help_reload"));
            sender.sendMessage("§e" + lang.getMessage("help_get"));
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("amotd.command.reload")) {
                    sender.sendMessage("§c" + lang.getMessage("no_permission"));
                    return true;
                }
                
                // 检查配置文件是否存在，如果不存在则重新生成
                File configFile = new File(plugin.getDataFolder(), "config.yml");
                if (!configFile.exists()) {
                    sender.sendMessage("§e" + lang.getMessage("config_not_found"));
                    plugin.saveDefaultConfig();
                }
                
                // 使用Folia的区域调度器重载配置
                try {
                    // 使用反射调用Folia API
                    Class<?> serverClass = plugin.getServer().getClass();
                    Object regionScheduler = serverClass.getMethod("getRegionScheduler").invoke(plugin.getServer());
                    regionScheduler.getClass().getMethod("execute", JavaPlugin.class, Runnable.class)
                            .invoke(regionScheduler, plugin, (Runnable) () -> {
                    plugin.reloadConfig();
                    
                    // 重载服务器图标
                    if (plugin instanceof AMOTD) {
                        AMOTD foliaPlugin = (AMOTD) plugin;
                        if (foliaPlugin.getMotdListener() != null) {
                            foliaPlugin.getMotdListener().reloadServerIcons();
                        }
                        foliaPlugin.updateMaxPlayers();
                    }
                    sender.sendMessage("§a" + lang.getMessage("reload_success"));
                            });
                    } catch (Exception e) {
                        plugin.getLogger().warning("无法使用Folia API: " + e.getMessage());
                        // 回退到传统方法
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            plugin.reloadConfig();
                            
                            // 重载服务器图标
                            if (plugin instanceof AMOTD) {
                                AMOTD foliaPlugin = (AMOTD) plugin;
                                if (foliaPlugin.getMotdListener() != null) {
                                    foliaPlugin.getMotdListener().reloadServerIcons();
                                }
                                foliaPlugin.updateMaxPlayers();
                            }
                            sender.sendMessage("§a" + lang.getMessage("reload_success"));
                        });
                    }
                return true;
                
            case "get":
                if (!sender.hasPermission("amotd.command.get")) {
                    sender.sendMessage("§c" + lang.getMessage("no_permission"));
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage("§c" + lang.getMessage("command_usage", "/amotd get <style code>"));
                    return true;
                }
                
                String styleCode = args[1];
                sender.sendMessage("§e" + lang.getMessage("fetching_style", styleCode));
                
                // 使用Folia的异步调度器获取MOTD
                try {
                    // 使用反射调用Folia API
                    Class<?> serverClass = plugin.getServer().getClass();
                    Object asyncScheduler = serverClass.getMethod("getAsyncScheduler").invoke(plugin.getServer());
                    asyncScheduler.getClass().getMethod("runNow", JavaPlugin.class, java.util.function.Consumer.class)
                            .invoke(asyncScheduler, plugin, (java.util.function.Consumer<Object>) task -> {
                    styleFetcher.fetchStyle(styleCode, new MOTDStyleFetcher.Callback() {
                        @Override
                        public void onSuccess(String line1, String line2, boolean iconSuccess, String formatType) {
                            // 在主线程中处理结果
                            try {
                                // 使用反射调用Folia API
                                Class<?> serverClass = plugin.getServer().getClass();
                                Object regionScheduler = serverClass.getMethod("getRegionScheduler").invoke(plugin.getServer());
                                regionScheduler.getClass().getMethod("execute", JavaPlugin.class, Runnable.class)
                                        .invoke(regionScheduler, plugin, (Runnable) () -> {
                                sender.sendMessage("§a" + lang.getMessage("style_fetch_success"));
                                sender.sendMessage("§b" + lang.getMessage("format_type", formatType));
                                sender.sendMessage("§f" + lang.getMessage("line1", line1));
                                sender.sendMessage("§f" + lang.getMessage("line2", line2));
                                
                                if (iconSuccess) {
                                    sender.sendMessage("§a" + lang.getMessage("icon_download_success"));
                                } else {
                                    sender.sendMessage("§e" + lang.getMessage("icon_download_fail"));
                                }
                                
                                sender.sendMessage("§e" + lang.getMessage("config_updated"));
                                
                                // 重载MOTD监听器以应用新图标
                                if (plugin instanceof AMOTD) {
                                    AMOTD foliaPlugin = (AMOTD) plugin;
                                    if (foliaPlugin.getMotdListener() != null) {
                                        foliaPlugin.getMotdListener().reloadServerIcons();
                                    }
                                }
                                        });
                                } catch (Exception e) {
                                    plugin.getLogger().warning("无法使用Folia API: " + e.getMessage());
                                    // 回退到传统方法
                                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                                        sender.sendMessage("§a" + lang.getMessage("style_fetch_success"));
                                        sender.sendMessage("§b" + lang.getMessage("format_type", formatType));
                                        sender.sendMessage("§f" + lang.getMessage("line1", line1));
                                        sender.sendMessage("§f" + lang.getMessage("line2", line2));
                                        
                                        if (iconSuccess) {
                                            sender.sendMessage("§a" + lang.getMessage("icon_download_success"));
                                        } else {
                                            sender.sendMessage("§e" + lang.getMessage("icon_download_fail"));
                                        }
                                        
                                        sender.sendMessage("§e" + lang.getMessage("config_updated"));
                                        
                                        // 重载MOTD监听器以应用新图标
                                        if (plugin instanceof AMOTD) {
                                            AMOTD foliaPlugin = (AMOTD) plugin;
                                            if (foliaPlugin.getMotdListener() != null) {
                                                foliaPlugin.getMotdListener().reloadServerIcons();
                                            }
                                        }
                                    });
                                }
                        }
                        
                        @Override
                        public void onFailure(String errorMessage) {
                            // 在主线程中处理错误
                            try {
                                // 使用反射调用Folia API
                                Class<?> serverClass = plugin.getServer().getClass();
                                Object regionScheduler = serverClass.getMethod("getRegionScheduler").invoke(plugin.getServer());
                                regionScheduler.getClass().getMethod("execute", JavaPlugin.class, Runnable.class)
                                        .invoke(regionScheduler, plugin, (Runnable) () -> {
                                sender.sendMessage("§c" + lang.getMessage("fetch_failed", errorMessage));
                                        });
                                } catch (Exception e) {
                                    plugin.getLogger().warning("无法使用Folia API: " + e.getMessage());
                                    // 回退到传统方法
                                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                                        sender.sendMessage("§c" + lang.getMessage("fetch_failed", errorMessage));
                                    });
                                }
                        }
                    });
                            });
                    } catch (Exception e) {
                        plugin.getLogger().warning("无法使用Folia API: " + e.getMessage());
                        // 回退到传统方法
                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                            styleFetcher.fetchStyle(styleCode, new MOTDStyleFetcher.Callback() {
                                @Override
                                public void onSuccess(String line1, String line2, boolean iconSuccess, String formatType) {
                                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                                        sender.sendMessage("§a" + lang.getMessage("style_fetch_success"));
                                        sender.sendMessage("§b" + lang.getMessage("format_type", formatType));
                                        sender.sendMessage("§f" + lang.getMessage("line1", line1));
                                        sender.sendMessage("§f" + lang.getMessage("line2", line2));
                                        
                                        if (iconSuccess) {
                                            sender.sendMessage("§a" + lang.getMessage("icon_download_success"));
                                        } else {
                                            sender.sendMessage("§e" + lang.getMessage("icon_download_fail"));
                                        }
                                        
                                        sender.sendMessage("§e" + lang.getMessage("config_updated"));
                                        
                                        // 重载MOTD监听器以应用新图标
                                        if (plugin instanceof AMOTD) {
                                            AMOTD foliaPlugin = (AMOTD) plugin;
                                            if (foliaPlugin.getMotdListener() != null) {
                                                foliaPlugin.getMotdListener().reloadServerIcons();
                                            }
                                        }
                                    });
                                }
                                
                                @Override
                                public void onFailure(String errorMessage) {
                                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                                        sender.sendMessage("§c" + lang.getMessage("fetch_failed", errorMessage));
                                    });
                                }
                            });
                        });
                    }
                return true;
                
            default:
                sender.sendMessage("§c" + lang.getMessage("unknown_command"));
                return true;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = Arrays.asList("reload", "get");
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
} 