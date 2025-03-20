package cn.i7mc.minepal.utils;

import cn.i7mc.minepal.MinePal;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.Map;

public class MessageManager {
    private final MinePal plugin;
    private final ConfigManager configManager;

    public MessageManager(MinePal plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * 获取消息
     * @param path 消息路径
     * @return 格式化后的消息
     */
    public String getMessage(String path) {
        FileConfiguration messageConfig = configManager.getConfig("message.yml");
        if (messageConfig == null) {
            return ChatColor.translateAlternateColorCodes('&', 
                getMessage("error.message-file-not-found").replace("%path%", path));
        }
        
        String message = messageConfig.getString("messages." + path);
        if (message == null) {
            return ChatColor.translateAlternateColorCodes('&', 
                getMessage("error.message-not-found").replace("%path%", path));
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * 获取调试消息
     * @param path 消息路径
     * @return 格式化后的调试消息
     */
    public String getDebugMessage(String path) {
        FileConfiguration debugConfig = configManager.getConfig("debugmessage.yml");
        if (debugConfig == null) {
            return getMessage("error.debug-file-not-found").replace("%path%", path);
        }
        
        String message = debugConfig.getString("debug-messages." + path);
        if (message == null) {
            return getMessage("error.debug-message-not-found").replace("%path%", path);
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * 输出调试信息
     * @param path 消息路径
     */
    public void debug(String path) {
        FileConfiguration config = configManager.getConfig("config.yml");
        if (config == null || !config.getBoolean("debug", false)) {
            return;
        }
        
        plugin.getLogger().info("[DEBUG] " + getDebugMessage(path));
    }
    
    /**
     * 调试消息输出
     * @param key 调试信息的键
     * @param args 参数替换内容（可选）
     */
    public void debug(String key, String... args) {
        if (!configManager.isDebug()) {
            return;
        }
        
        String message = getDebugMessage(key);
        if (message != null && !message.isEmpty()) {
            // 替换参数
            if (args != null && args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    message = message.replace("{" + i + "}", args[i]);
                }
            }
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }

    /**
     * 刷新消息缓存
     * 用于插件重载时更新消息
     */
    public void refreshMessages() {
        // 检查是否开启调试模式
        boolean isDebug = configManager.isDebug();
        
        if (isDebug) {
            debug("message.refresh-start");
        }
        
        // 重新加载消息配置文件
        configManager.loadConfig("message.yml");
        configManager.loadConfig("debugmessage.yml");
        
        if (isDebug) {
            debug("message.refresh-complete");
        }
    }

    /**
     * 获取调试消息并替换变量
     * @param path 消息路径
     * @param placeholders 变量替换映射表
     * @return 格式化后的调试消息
     */
    public String getDebugMessage(String path, Map<String, String> placeholders) {
        String message = getDebugMessage(path);
        
        // 替换变量
        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }
        
        return message;
    }

    /**
     * 带有复杂替换的调试消息输出
     * @param key 调试信息的键
     * @param placeholderKeys 待替换的占位符数组
     * @param placeholderValues 对应的值数组
     */
    public void debug(String key, String[] placeholderKeys, String[] placeholderValues) {
        if (!configManager.isDebug()) {
            return;
        }
        
        String message = getDebugMessage(key);
        if (message != null && !message.isEmpty() && placeholderKeys != null && placeholderValues != null) {
            // 确保数组长度一致，防止越界
            int minLength = Math.min(placeholderKeys.length, placeholderValues.length);
            
            // 替换参数
            for (int i = 0; i < minLength; i++) {
                message = message.replace(placeholderKeys[i], placeholderValues[i]);
            }
            
            plugin.getLogger().info("[DEBUG] " + message);
        } else {
            // 如果没有替换参数，直接输出原始消息
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
} 