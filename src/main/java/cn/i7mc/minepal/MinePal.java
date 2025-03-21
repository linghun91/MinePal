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
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
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
    private ConsoleCommandSender console;

    @Override
    public void onEnable() {
        // 获取控制台发送者
        console = getServer().getConsoleSender();
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.loadAllConfigs();
        
        // 初始化消息管理器
        messageManager = new MessageManager(this, configManager);
        
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
            // 清理可能存在的无主宠物（服务器崩溃后残留的宠物）
            String cleanupMessage = messageManager.getMessage("plugin.pet-cleanup");
            console.sendMessage(ChatColor.translateAlternateColorCodes('&', cleanupMessage));
            
            petManager.removeAllPets();
            
            String completeMessage = messageManager.getMessage("plugin.pet-cleanup-complete");
            console.sendMessage(ChatColor.translateAlternateColorCodes('&', completeMessage));
            
            // 初始化AI系统
            aiManager.initialize();
            
            // 注册宠物生命周期监听器
            lifecycleListener = new PetLifecycleListener(this);
            Bukkit.getPluginManager().registerEvents(lifecycleListener, this);
            
            // 更新命令处理器中的宠物管理器引用
            updateCommandHandler();
        }, 40L); // 2秒后执行
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new EntityDamageListener(this), this);
        
        // 插件启动逻辑
        String enableMessage = messageManager.getMessage("plugin.enable");
        console.sendMessage(ChatColor.GREEN + ChatColor.translateAlternateColorCodes('&', enableMessage));
    }
    
    /**
     * 监听MythicMobs重载事件
     */
    @EventHandler
    public void onMythicReload(MythicReloadedEvent event) {
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
        try {
            // 检查messageManager是否为null
            if (messageManager == null) {
                // 如果messageManager为null，直接使用控制台发送固定信息
                console = getServer().getConsoleSender();
                console.sendMessage(ChatColor.YELLOW + "[MinePal] " + ChatColor.RED + "插件已禁用");
                return;
            }
            
            // 优先清理所有宠物实体（确保在服务器关闭时执行）
            if (petManager != null) {
                String cleanupMessage = messageManager.getMessage("plugin.pet-cleanup");
                console.sendMessage(ChatColor.translateAlternateColorCodes('&', cleanupMessage));
                
                petManager.removeAllPets();
                
                String completeMessage = messageManager.getMessage("plugin.pet-cleanup-complete");
                console.sendMessage(ChatColor.translateAlternateColorCodes('&', completeMessage));
            }
            
            // 插件关闭逻辑
            String disableMessage = messageManager.getMessage("plugin.disable");
            console.sendMessage(ChatColor.RED + ChatColor.translateAlternateColorCodes('&', disableMessage));
        } catch (Exception e) {
            // 捕获任何可能的异常，确保插件可以正常关闭
            getLogger().severe("插件禁用过程中发生错误: " + e.getMessage());
        }
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