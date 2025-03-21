package cn.i7mc.minepal.listeners;

import cn.i7mc.minepal.MinePal;
import cn.i7mc.minepal.utils.MessageManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 监听玩家的战斗状态
 * 记录玩家攻击的目标和攻击玩家的敌人
 */
public class OwnerCombatListener implements Listener {
    private final MinePal plugin;
    private final MessageManager messageManager;
    // 记录玩家攻击的目标
    private final Map<UUID, UUID> playerTargets = new ConcurrentHashMap<>();
    // 记录玩家被攻击的来源
    private final Map<UUID, UUID> playerAttackers = new ConcurrentHashMap<>();
    // 记录最后一次战斗时间
    private final Map<UUID, Long> lastCombatTime = new ConcurrentHashMap<>();
    // 战斗状态持续时间(毫秒)
    private static final long COMBAT_DURATION = 10000;

    public OwnerCombatListener(MinePal plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    /**
     * 监听伤害事件,记录玩家的战斗状态
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();
        
        // 处理玩家攻击其他实体的情况
        if (damager instanceof Player && victim instanceof LivingEntity) {
            Player player = (Player) damager;
            UUID playerUUID = player.getUniqueId();
            UUID targetUUID = victim.getUniqueId();
            
            // 无条件更新战斗状态
            playerTargets.put(playerUUID, targetUUID);
            lastCombatTime.put(playerUUID, System.currentTimeMillis());
            
            // 重要：更新宠物目标，让宠物也攻击该目标
            plugin.getPetManager().updatePetTarget(playerUUID, targetUUID);
        }
        
        // 处理实体攻击玩家的情况
        if (victim instanceof Player) {
            Player player = (Player) victim;
            UUID playerUUID = player.getUniqueId();
            UUID attackerUUID;
            Entity actualAttacker = damager;
            
            // 处理投射物
            if (damager instanceof Projectile) {
                ProjectileSource shooter = ((Projectile) damager).getShooter();
                if (shooter instanceof Entity) {
                    actualAttacker = (Entity) shooter;
                } else {
                    return; // 如果射手不是实体，不处理
                }
            }
            
            // 只处理生物实体攻击
            if (!(actualAttacker instanceof LivingEntity)) {
                return;
            }
            
            attackerUUID = actualAttacker.getUniqueId();
            
            // 无条件更新战斗状态
            playerAttackers.put(playerUUID, attackerUUID);
            lastCombatTime.put(playerUUID, System.currentTimeMillis());
            
            // 总是更新宠物目标，让宠物攻击伤害主人的实体
            plugin.getPetManager().updatePetTarget(playerUUID, attackerUUID);
        }
    }

    /**
     * 监听箭矢伤害事件
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Projectile)) return;
        
        Projectile projectile = (Projectile) event.getEntity();
        Entity shooter = (Entity) projectile.getShooter();
        Entity hitEntity = event.getHitEntity();
        
        if (shooter instanceof Player && hitEntity instanceof LivingEntity) {
            Player player = (Player) shooter;
            UUID playerUUID = player.getUniqueId();
            UUID targetUUID = hitEntity.getUniqueId();
            
            // 无条件更新战斗状态
            playerTargets.put(playerUUID, targetUUID);
            lastCombatTime.put(playerUUID, System.currentTimeMillis());
            
            // 更新宠物目标
            plugin.getPetManager().updatePetTarget(playerUUID, targetUUID);
        }
    }

    /**
     * 监听环境伤害和其他非实体伤害事件
     * 注意：实体攻击已经在onEntityDamageByEntity方法中处理
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageEvent(EntityDamageEvent event) {
        // 如果是实体攻击事件，已经在onEntityDamageByEntity方法中处理过了
        if (event instanceof EntityDamageByEntityEvent) {
            return;
        }
        
        // 获取被伤害实体
        Entity victim = event.getEntity();
        if (!(victim instanceof Player)) {
            return;
        }
        
        Player player = (Player) victim;
        UUID playerUUID = player.getUniqueId();
        
        // 记录环境伤害
        lastCombatTime.put(playerUUID, System.currentTimeMillis());
    }

    /**
     * 检查玩家是否在战斗状态
     */
    public boolean isPlayerInCombat(UUID playerUUID) {
        if (!lastCombatTime.containsKey(playerUUID)) {
            return false;
        }
        
        long lastTime = lastCombatTime.get(playerUUID);
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastTime;
        
        return elapsedTime < COMBAT_DURATION;
    }
    
    /**
     * 获取玩家当前的目标
     * @param playerUUID 玩家的UUID
     * @return 目标的UUID，如果没有则返回null
     */
    public UUID getPlayerTarget(UUID playerUUID) {
        return playerTargets.get(playerUUID);
    }
    
    /**
     * 获取攻击玩家的实体
     * @param playerUUID 玩家的UUID
     * @return 攻击者的UUID，如果没有则返回null
     */
    public UUID getPlayerAttacker(UUID playerUUID) {
        return playerAttackers.get(playerUUID);
    }
    
    /**
     * 清除玩家的战斗记录
     * @param playerUUID 玩家的UUID
     */
    public void clearCombatRecords(UUID playerUUID) {
        playerTargets.remove(playerUUID);
        playerAttackers.remove(playerUUID);
        lastCombatTime.remove(playerUUID);
    }
    
    /**
     * 强制将玩家设为战斗状态
     * @param playerUUID 玩家UUID
     */
    public void forceCombatState(UUID playerUUID) {
        lastCombatTime.put(playerUUID, System.currentTimeMillis());
    }
    
    /**
     * 获取战斗持续时间
     * @return 战斗持续时间（毫秒）
     */
    public long getCombatDuration() {
        return COMBAT_DURATION;
    }
    
    /**
     * 获取距离上次战斗的时间
     * @param playerUUID 玩家UUID
     * @return 距离上次战斗的时间（毫秒），如果没有战斗记录则返回-1
     */
    public long getTimeSinceLastCombat(UUID playerUUID) {
        if (!lastCombatTime.containsKey(playerUUID)) {
            return -1;
        }
        
        long lastTime = lastCombatTime.get(playerUUID);
        long currentTime = System.currentTimeMillis();
        return currentTime - lastTime;
    }
} 