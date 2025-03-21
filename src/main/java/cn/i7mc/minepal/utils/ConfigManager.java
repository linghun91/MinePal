package cn.i7mc.minepal.utils;

import cn.i7mc.minepal.MinePal;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final MinePal plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();

    public ConfigManager(MinePal plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化所有配置文件
     */
    public void loadAllConfigs() {
        loadConfig("config.yml");
        loadConfig("message.yml");
    }

    /**
     * 加载指定配置文件
     * @param filename 配置文件名
     */
    public void loadConfig(String filename) {
        // 跳过debugmessage.yml文件
        if (filename.equals("debugmessage.yml")) {
            return;
        }
        
        File file = new File(plugin.getDataFolder(), filename);
        if (!file.exists()) {
            plugin.saveResource(filename, false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(filename, config);
    }

    /**
     * 保存指定配置文件
     * @param filename 配置文件名
     */
    public void saveConfig(String filename) {
        if (!configs.containsKey(filename)) {
            return;
        }
        
        try {
            File file = new File(plugin.getDataFolder(), filename);
            configs.get(filename).save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存配置文件: " + filename);
        }
    }

    /**
     * 获取指定配置文件
     * @param filename 配置文件名
     * @return 配置文件对象
     */
    public FileConfiguration getConfig(String filename) {
        return configs.getOrDefault(filename, null);
    }
    
    /**
     * 检查是否启用调试模式
     * @return 是否启用调试模式
     */
    public boolean isDebug() {
        // 保留方法定义，但始终返回false
        return false;
    }

    /**
     * 重新加载所有配置文件
     */
    public void reloadAllConfigs() {
        configs.clear();
        loadAllConfigs();
    }
} 