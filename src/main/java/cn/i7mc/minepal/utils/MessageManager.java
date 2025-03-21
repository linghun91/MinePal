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
        // 由于调试功能已移除，直接返回提示信息
        return "[调试已禁用]";
    }
    
    /**
     * 输出调试信息
     * @param path 消息路径
     */
    public void debug(String path) {
        // 方法保留，但不执行任何操作
    }
    
    /**
     * 调试消息输出
     * @param key 调试信息的键
     * @param args 参数替换内容（可选）
     */
    public void debug(String key, String... args) {
        // 方法保留，但不执行任何操作
    }

    /**
     * 刷新消息缓存
     * 用于插件重载时更新消息
     */
    public void refreshMessages() {
        // 重新加载消息配置文件
        configManager.loadConfig("message.yml");
    }

    /**
     * 获取调试消息并替换变量
     * @param path 消息路径
     * @param placeholders 变量替换映射表
     * @return 格式化后的调试消息
     */
    public String getDebugMessage(String path, Map<String, String> placeholders) {
        // 由于调试功能已移除，直接返回提示信息
        return "[调试已禁用]";
    }

    /**
     * 带有复杂替换的调试消息输出
     * @param key 调试信息的键
     * @param placeholderKeys 待替换的占位符数组
     * @param placeholderValues 对应的值数组
     */
    public void debug(String key, String[] placeholderKeys, String[] placeholderValues) {
        // 方法保留，但不执行任何操作
    }
} 