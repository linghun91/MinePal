package cn.i7mc.minepal.ai.manager;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.mobs.ai.PathfinderAdapter;
import io.lumine.mythic.core.mobs.ai.PathfindingGoal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 宠物AI管理器接口，用于注册和管理宠物AI行为和目标，与MythicMobs API集成
 */
public interface PetAIManager {
    
    /**
     * 初始化AI管理器
     */
    void initialize();
    
    /**
     * 注册自定义AI行为
     */
    void registerBehaviors();
    
    /**
     * 注册自定义AI目标
     */
    void registerTargets();
    
    /**
     * 为特定宠物应用AI设置
     * @param mythicMob MythicMobs的实体
     * @param owner 宠物主人
     */
    void applyAI(ActiveMob mythicMob, Player owner);
    
    /**
     * 移除宠物的AI设置
     * @param mythicMob MythicMobs的实体
     */
    void removeAI(ActiveMob mythicMob);
    
    /**
     * 更新宠物AI状态
     * @param mythicMob MythicMobs的实体
     */
    void updateAI(ActiveMob mythicMob);
    
    /**
     * 注册自定义寻路目标到MythicMobs系统
     * @param name 目标名称
     * @param goalClass 实现PathfindingGoal的类
     */
    void registerPathfindingGoal(String name, Class<? extends PathfindingGoal> goalClass);
    
    /**
     * 注册自定义寻路适配器到MythicMobs系统
     * @param name 适配器名称
     * @param adapterClass 实现PathfinderAdapter的类
     */
    void registerPathfinderAdapter(String name, Class<? extends PathfinderAdapter> adapterClass);
    
    /**
     * 获取MythicMobs中的ActiveMob对象
     * @param entity Bukkit实体
     * @return MythicMobs的ActiveMob对象，如果不存在则返回null
     */
    default ActiveMob getMythicMob(Entity entity) {
        AbstractEntity abstractEntity = BukkitAdapter.adapt(entity);
        return getMythicMob(abstractEntity);
    }
    
    /**
     * 获取MythicMobs中的ActiveMob对象
     * @param abstractEntity MythicMobs的抽象实体
     * @return MythicMobs的ActiveMob对象，如果不存在则返回null
     */
    ActiveMob getMythicMob(AbstractEntity abstractEntity);
    
    /**
     * 设置宠物的目标实体
     * @param petUuid 宠物的UUID
     * @param target 目标实体
     * @return 是否成功设置
     */
    boolean setTarget(UUID petUuid, Entity target);
} 