package cn.i7mc.minepal.listeners;

import cn.i7mc.minepal.MinePal;
import cn.i7mc.minepal.utils.MessageManager;
import org.bukkit.Bukkit;
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

import java.util.HashMap;
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
        // 输出初始化信息
        Bukkit.getLogger().info("[MinePal] 战斗监听器初始化完成");
    }

    /**
     * 监听伤害事件,记录玩家的战斗状态
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 检查debug配置
        boolean debug = plugin.getConfig().getBoolean("debug", false);
        
        // 输出事件基本信息，只在debug模式下输出
        if (debug) {
            Bukkit.getLogger().info("[MinePal] 收到伤害事件: " + 
                event.getDamager().getType() + "[" + event.getDamager().getUniqueId() + "] -> " + 
                event.getEntity().getType() + "[" + event.getEntity().getUniqueId() + "]" + 
                ", 伤害: " + event.getDamage() +
                ", 是否取消: " + event.isCancelled());
        }
            
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();
        
        // 处理玩家攻击其他实体的情况
        if (damager instanceof Player && victim instanceof LivingEntity) {
            Player player = (Player) damager;
            UUID playerUUID = player.getUniqueId();
            UUID targetUUID = victim.getUniqueId();
            
            // 无条件更新战斗状态，删除isCommandSpawnedEntity检查
            playerTargets.put(playerUUID, targetUUID);
            lastCombatTime.put(playerUUID, System.currentTimeMillis());
            
            // 输出基本日志，只在debug模式下输出
            if (debug) {
                Bukkit.getLogger().info("[MinePal] 记录主人攻击事件: " + player.getName() + 
                    " -> " + victim.getType() + "[" + targetUUID + "], 伤害: " + 
                    String.format("%.2f", event.getDamage()));
                Bukkit.getLogger().info("[MinePal] Debug模式: " + debug);
                
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%player_name%", player.getName());
                placeholders.put("%player_uuid%", playerUUID.toString());
                placeholders.put("%target_type%", victim.getType().toString());
                placeholders.put("%target_uuid%", targetUUID.toString());
                placeholders.put("%damage%", String.format("%.2f", event.getDamage()));
                placeholders.put("%event_type%", "主人攻击实体");
                placeholders.put("%victim_health%", String.format("%.2f", ((LivingEntity)victim).getHealth()));
                placeholders.put("%victim_max_health%", String.format("%.2f", ((LivingEntity)victim).getMaxHealth()));
                
                String debugMessage = messageManager.getDebugMessage(
                    "owner-combat.player-attack-entity",
                    placeholders
                );
                Bukkit.getLogger().info(debugMessage);
            }
            
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
            
            // 输出基本日志，只在debug模式下输出
            if (debug) {
                Bukkit.getLogger().info("[MinePal] 记录实体攻击主人事件: " + actualAttacker.getType() + 
                    "[" + attackerUUID + "] -> " + player.getName() + ", 伤害: " + 
                    String.format("%.2f", event.getDamage()));
                
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%player_name%", player.getName());
                placeholders.put("%player_uuid%", playerUUID.toString());
                placeholders.put("%attacker_type%", actualAttacker.getType().toString());
                placeholders.put("%attacker_uuid%", attackerUUID.toString());
                placeholders.put("%damage%", String.format("%.2f", event.getDamage()));
                placeholders.put("%event_type%", "实体攻击主人");
                placeholders.put("%player_health%", String.format("%.2f", player.getHealth()));
                placeholders.put("%player_max_health%", String.format("%.2f", player.getMaxHealth()));
                
                String debugMessage = messageManager.getDebugMessage(
                    "owner-combat.entity-attack-player",
                    placeholders
                );
                Bukkit.getLogger().info(debugMessage);
            }
            
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
        
        // 检查debug配置
        boolean debug = plugin.getConfig().getBoolean("debug", false);
        
        Projectile projectile = (Projectile) event.getEntity();
        Entity shooter = (Entity) projectile.getShooter();
        Entity hitEntity = event.getHitEntity();
        
        if (shooter instanceof Player && hitEntity instanceof LivingEntity) {
            Player player = (Player) shooter;
            UUID playerUUID = player.getUniqueId();
            UUID targetUUID = hitEntity.getUniqueId();
            
            // 无条件更新战斗状态，删除isCommandSpawnedEntity检查
            playerTargets.put(playerUUID, targetUUID);
            lastCombatTime.put(playerUUID, System.currentTimeMillis());
            
            // 只在debug模式下输出日志
            if (debug) {
                Bukkit.getLogger().info("[MinePal] 记录主人远程攻击事件: " + player.getName() + 
                    " -> " + hitEntity.getType() + "[" + targetUUID + "]");
                
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%player_name%", player.getName());
                placeholders.put("%player_uuid%", playerUUID.toString());
                placeholders.put("%target_type%", hitEntity.getType().toString());
                placeholders.put("%target_uuid%", targetUUID.toString());
                placeholders.put("%damage%", "远程攻击");
                placeholders.put("%event_type%", "主人远程攻击实体");
                placeholders.put("%victim_health%", String.format("%.2f", ((LivingEntity)hitEntity).getHealth()));
                placeholders.put("%victim_max_health%", String.format("%.2f", ((LivingEntity)hitEntity).getMaxHealth()));
                
                Bukkit.getLogger().info(messageManager.getDebugMessage(
                    "owner-combat.player-attack-entity",
                    placeholders
                ));
            }
            
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
        
        // 输出调试信息
        if (plugin.getConfig().getBoolean("debug", false)) {
            Bukkit.getLogger().info("[MinePal] [DEBUG] 玩家受到环境伤害: " + 
                "玩家: " + player.getName() + "[" + playerUUID + "], " +
                "伤害: " + event.getDamage() + ", " +
                "原因: " + event.getCause() + ", " +
                "取消: " + event.isCancelled());
        }
    }

    /**
     * 检查玩家是否在战斗状态
     */
    public boolean isPlayerInCombat(UUID playerUUID) {
        if (!lastCombatTime.containsKey(playerUUID)) {
            return false;
        }
        
        long lastTime = lastCombatTime.get(playerUUID);
        boolean inCombat = System.currentTimeMillis() - lastTime < COMBAT_DURATION;
        
        // 如果不在战斗状态，清除所有相关数据
        if (!inCombat) {
            clearPlayerCombatData(playerUUID);
        }
        
        return inCombat;
    }

    /**
     * 获取玩家当前的目标
     */
    public UUID getPlayerTarget(UUID playerUUID) {
        if (!isPlayerInCombat(playerUUID)) {
            return null;
        }
        
        // 优先返回攻击者
        UUID attackerUUID = playerAttackers.get(playerUUID);
        if (attackerUUID != null) {
            // 检查攻击者是否还存在
            Entity attacker = Bukkit.getEntity(attackerUUID);
            if (attacker != null && attacker.isValid() && !attacker.isDead()) {
                // 更新战斗时间,确保战斗状态持续
                lastCombatTime.put(playerUUID, System.currentTimeMillis());
                return attackerUUID;
            }
        }
        
        // 如果没有攻击者，返回玩家正在攻击的目标
        UUID targetUUID = playerTargets.get(playerUUID);
        if (targetUUID != null) {
            // 检查目标是否还存在
            Entity target = Bukkit.getEntity(targetUUID);
            if (target != null && target.isValid() && !target.isDead()) {
                // 更新战斗时间,确保战斗状态持续
                lastCombatTime.put(playerUUID, System.currentTimeMillis());
                return targetUUID;
            }
        }
        
        // 如果目标不存在，清除战斗数据
        clearPlayerCombatData(playerUUID);
        return null;
    }

    /**
     * 获取玩家当前的攻击者
     */
    public UUID getPlayerAttacker(UUID playerUUID) {
        if (!isPlayerInCombat(playerUUID)) {
            return null;
        }
        
        UUID attackerUUID = playerAttackers.get(playerUUID);
        if (attackerUUID != null) {
            // 检查攻击者是否还存在
            Entity attacker = Bukkit.getEntity(attackerUUID);
            if (attacker == null || !attacker.isValid() || attacker.isDead()) {
                clearPlayerCombatData(playerUUID);
                return null;
            }
        }
        
        return attackerUUID;
    }

    /**
     * 清除玩家的战斗记录
     */
    public void clearPlayerCombatData(UUID playerUUID) {
        playerTargets.remove(playerUUID);
        playerAttackers.remove(playerUUID);
        lastCombatTime.remove(playerUUID);
    }

    /**
     * 清理所有过期的战斗状态数据
     */
    public void cleanupCombatData() {
        long currentTime = System.currentTimeMillis();
        // 遍历所有战斗记录，清理过期的数据
        lastCombatTime.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() >= COMBAT_DURATION) {
                UUID playerUUID = entry.getKey();
                playerTargets.remove(playerUUID);
                playerAttackers.remove(playerUUID);
                return true;
            }
            return false;
        });
    }

    /**
     * 检查实体是否受到过实际伤害
     */
    public boolean hasEntityBeenDamaged(Entity entity) {
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            // 如果实体当前生命值小于最大生命值，说明受到过伤害
            return livingEntity.getHealth() < livingEntity.getMaxHealth();
        }
        return false;
    }

    /**
     * 获取玩家最后战斗时间
     * @param playerUUID 玩家UUID
     * @return 最后战斗时间的时间戳，如果不存在则返回当前时间
     */
    public long getLastCombatTime(UUID playerUUID) {
        if (lastCombatTime.containsKey(playerUUID)) {
            return lastCombatTime.get(playerUUID);
        }
        return System.currentTimeMillis(); // 如果不存在，返回当前时间
    }

    /**
     * 更新玩家的战斗状态，将当前时间设置为最后的战斗时间
     * @param player 玩家
     */
    public void updateCombatStatus(Player player) {
        if (player == null) {
            return;
        }
        
        lastCombatTime.put(player.getUniqueId(), System.currentTimeMillis());
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            Bukkit.getLogger().info("[MinePal] 玩家 " + player.getName() + " 进入战斗状态");
        }
    }

    /**
     * 检查玩家是否处于战斗状态
     * @param player 玩家
     * @return 如果玩家处于战斗状态则返回true，否则返回false
     */
    public boolean isInCombat(Player player) {
        if (player == null) {
            return false;
        }
        
        Long lastCombatTime = this.lastCombatTime.get(player.getUniqueId());
        if (lastCombatTime == null) {
            return false;
        }
        
        // 检查战斗状态是否过期
        boolean inCombat = (System.currentTimeMillis() - lastCombatTime) < COMBAT_DURATION;
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            Bukkit.getLogger().info("[MinePal] 玩家 " + player.getName() + " 当前战斗状态: " + inCombat);
        }
        
        return inCombat;
    }

    /**
     * 清除玩家的战斗状态
     * @param player 玩家
     */
    public void clearCombatStatus(Player player) {
        if (player == null) {
            return;
        }
        
        lastCombatTime.remove(player.getUniqueId());
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            Bukkit.getLogger().info("[MinePal] 玩家 " + player.getName() + " 战斗状态已清除");
        }
    }
} 