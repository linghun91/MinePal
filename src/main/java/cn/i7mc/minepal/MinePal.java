package cn.i7mc.minepal;

import cn.i7mc.minepal.ai.manager.MythicMobsPetAIManager;
import cn.i7mc.minepal.ai.manager.PetAIManager;
import cn.i7mc.minepal.command.handler.CommandHandler;
import cn.i7mc.minepal.listeners.PetLifecycleListener;
import cn.i7mc.minepal.listeners.PetProtectionListener;
import cn.i7mc.minepal.listeners.OwnerCombatListener;
import cn.i7mc.minepal.listeners.EntityDamageListener;
import cn.i7mc.minepal.pet.control.PetManager;
import cn.i7mc.minepal.utils.ConfigManager;
import cn.i7mc.minepal.utils.MessageManager;
import cn.i7mc.minepal.utils.PetUtils;
import cn.i7mc.minepal.utils.EntityUtils;
import cn.i7mc.minepal.utils.DamageUtils;
import io.lumine.mythic.bukkit.events.MythicReloadedEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class MinePal extends JavaPlugin implements Listener {
    private ConfigManager configManager;
    private MessageManager messageManager;
    private PetUtils petUtils;
    private PetAIManager aiManager;
    private PetManager petManager;
    private CommandHandler commandHandler;
    private PetProtectionListener protectionListener;
    private PetLifecycleListener lifecycleListener;
    private OwnerCombatListener combatListener;
    private DamageUtils damageUtils;

    @Override
    public void onEnable() {
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.loadAllConfigs();
        
        // 初始化消息管理器
        messageManager = new MessageManager(this, configManager);
        
        // 输出调试信息
        messageManager.debug("plugin.enable");
        
        // 初始化宠物工具类
        petUtils = new PetUtils(this);
        
        // 初始化EntityUtils静态工具类
        EntityUtils.init(this);
        
        // 初始化DamageUtils
        damageUtils = new DamageUtils(this);
        
        // 初始化战斗监听器
        combatListener = new OwnerCombatListener(this);
        // 注册战斗监听器
        Bukkit.getPluginManager().registerEvents(combatListener, this);
        
        // 初始化宠物管理器
        petManager = new PetManager(this, null, petUtils);
        
        // 初始化AI管理器
        aiManager = new MythicMobsPetAIManager(this, petManager, combatListener);
        
        // 更新宠物管理器的AI管理器引用
        petManager.setAIManager(aiManager);
        
        // 注册事件监听器，监听MythicMobs重载事件
        Bukkit.getPluginManager().registerEvents(this, this);
        
        // 注册宠物保护监听器
        protectionListener = new PetProtectionListener(this);
        Bukkit.getPluginManager().registerEvents(protectionListener, this);
        
        // 注册命令
        commandHandler = new CommandHandler(this, null); // 暂时传入null，待petManager初始化后再设置
        getCommand("mp").setExecutor(commandHandler);
        getCommand("mp").setTabCompleter(commandHandler);
        
        // 使用延迟任务确保MythicMobs完全加载
        Bukkit.getScheduler().runTaskLater(this, () -> {
            getLogger().info("开始延迟初始化AI系统...");
            aiManager.initialize();
            getLogger().info("AI系统初始化完成！");
            
            // 注册宠物生命周期监听器
            lifecycleListener = new PetLifecycleListener(this);
            Bukkit.getPluginManager().registerEvents(lifecycleListener, this);
            
            // 更新命令处理器中的宠物管理器引用
            updateCommandHandler();
        }, 40L); // 2秒后执行
        
        // 注册定时任务，定期清理过期的战斗状态数据
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (combatListener != null) {
                combatListener.cleanupCombatData();
            }
        }, 100L, 100L); // 每5秒清理一次
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new EntityDamageListener(this), this);
        
        // 插件启动逻辑
        getLogger().info(messageManager.getMessage("plugin.enable"));
        
        messageManager.debug("plugin.enable-complete");
    }
    
    /**
     * 监听MythicMobs重载事件
     */
    @EventHandler
    public void onMythicReload(MythicReloadedEvent event) {
        // 检查是否开启调试模式
        if (configManager.isDebug()) {
            getLogger().info("[DEBUG] 检测到MythicMobs重载，重新初始化系统...");
        } else {
            getLogger().info("检测到MythicMobs重载，重新初始化系统...");
        }
        
        // 重载配置
        configManager.reloadAllConfigs();
        
        // 更新消息
        messageManager.refreshMessages();
        
        // 重新初始化AI系统
        if (aiManager != null) {
            aiManager.initialize();
        }
        
        // 清除所有宠物
        if (petManager != null) {
            petManager.removeAllPets();
        }
        
        if (configManager.isDebug()) {
            getLogger().info("[DEBUG] 系统重新初始化完成！");
        } else {
            getLogger().info("系统重新初始化完成！");
        }
    }
    
    /**
     * 更新命令处理器中的宠物管理器引用
     */
    public void updateCommandHandler() {
        if (commandHandler != null && petManager != null) {
            commandHandler.setPetManager(petManager);
        }
    }

    @Override
    public void onDisable() {
        // 输出调试信息
        messageManager.debug("plugin.disable");
        
        // 优先清理所有宠物实体（确保在服务器关闭时执行）
        if (petManager != null) {
            messageManager.debug("plugin.pet-cleanup-start");
            getLogger().info(messageManager.getMessage("plugin.pet-cleanup"));
            petManager.removeAllPets();
            getLogger().info(messageManager.getMessage("plugin.pet-cleanup-complete"));
            messageManager.debug("plugin.pet-cleanup-complete");
        }
        
        // 插件关闭逻辑
        getLogger().info(messageManager.getMessage("plugin.disable"));
        
        messageManager.debug("plugin.disable-complete");
    }
    
    /**
     * 获取配置管理器
     * @return 配置管理器实例
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * 获取消息管理器
     * @return 消息管理器实例
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    /**
     * 获取宠物工具类
     * @return 宠物工具类实例
     */
    public PetUtils getPetUtils() {
        return petUtils;
    }
    
    /**
     * 获取AI管理器
     * @return AI管理器实例
     */
    public PetAIManager getAIManager() {
        return aiManager;
    }
    
    /**
     * 获取宠物管理器
     * @return 宠物管理器实例
     */
    public PetManager getPetManager() {
        return petManager;
    }
    
    /**
     * 获取战斗状态监听器
     * @return 战斗状态监听器实例
     */
    public OwnerCombatListener getCombatListener() {
        return combatListener;
    }
    
    /**
     * 获取主人战斗监听器
     * @return 主人战斗监听器实例
     */
    public OwnerCombatListener getOwnerCombatListener() {
        return combatListener;
    }
    
    /**
     * 获取伤害工具类
     * @return 伤害工具类
     */
    public DamageUtils getDamageUtils() {
        return damageUtils;
    }
} 