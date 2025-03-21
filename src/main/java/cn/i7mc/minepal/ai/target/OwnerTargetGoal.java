package cn.i7mc.minepal.ai.target;

import cn.i7mc.minepal.MinePal;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.mobs.ai.Pathfinder;
import io.lumine.mythic.core.mobs.ai.PathfindingTarget;
import io.lumine.mythic.core.utils.annotations.MythicAIGoal;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 自定义的攻击主人目标的AI行为，适用于所有类型的实体
 * 不要求实体必须是Tameable
 */
@MythicAIGoal(name="ownerTarget", aliases={"ownertarget", "attackownertarget"}, description="攻击宠物主人的目标")
public class OwnerTargetGoal extends Pathfinder implements PathfindingTarget {
    private final MinePal plugin;
    private transient WeakReference<Player> owner = null;
    private boolean revenge;
    private double radiusSq;
    private int updateInterval;
    private int currentTick;
    
    // 支持的实体类型列表（确保包含HUSK）
    private static final List<EntityType> SUPPORTED_TYPES = Arrays.asList(
        EntityType.ZOMBIE, EntityType.SKELETON, EntityType.HUSK, EntityType.DROWNED, EntityType.STRAY,
        EntityType.WITHER_SKELETON, EntityType.ZOMBIE_VILLAGER, EntityType.ZOMBIFIED_PIGLIN,
        EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.SPIDER, EntityType.CAVE_SPIDER,
        EntityType.CREEPER, EntityType.BLAZE, EntityType.GHAST, EntityType.SILVERFISH,
        EntityType.ENDERMAN, EntityType.SLIME, EntityType.MAGMA_CUBE, EntityType.WITCH,
        EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN, EntityType.SHULKER, EntityType.ENDERMITE,
        EntityType.VEX, EntityType.VINDICATOR, EntityType.EVOKER, EntityType.RAVAGER,
        EntityType.PILLAGER, EntityType.PHANTOM, EntityType.ILLUSIONER, EntityType.WOLF,
        EntityType.WARDEN, EntityType.IRON_GOLEM, EntityType.SNOW_GOLEM
    );

    public OwnerTargetGoal(AbstractEntity entity, String line, MythicLineConfig mlc, MinePal plugin) {
        super(entity, line, mlc);
        this.plugin = plugin;
        this.goalType = Pathfinder.GoalType.TARGET;
        
        // 是否攻击针对主人的实体（复仇机制）
        this.revenge = mlc.getBoolean(new String[]{"revenge", "r"}, true);
        
        // 检测半径
        double radius = mlc.getDouble(new String[]{"radius", "r"}, 20.0);
        this.radiusSq = Math.pow(radius, 2.0);
        
        // 更新间隔（避免频繁检测）
        this.updateInterval = mlc.getInteger(new String[]{"interval", "i"}, 5);
        this.currentTick = 0;
        
        // 检查实体类型是否支持
        Entity bukkitEntity = BukkitAdapter.adapt(entity);
        if (!SUPPORTED_TYPES.contains(bukkitEntity.getType())) {
            // 删除调试日志
        }
    }

    @Override
    public boolean shouldStart() {
        // 每隔几个tick才检查一次，避免频繁检测
        if (++this.currentTick < this.updateInterval) {
            return false;
        }
        this.currentTick = 0;
        
        // 检查是否有主人
        if (this.owner == null || this.owner.get() == null) {
            if (!this.activeMob.getOwner().isPresent()) {
                return false;
            }
            Player player = Bukkit.getPlayer(this.activeMob.getOwner().get());
            if (player == null) {
                return false;
            }
            this.owner = new WeakReference<>(player);
        }
        
        Player ownerPlayer = this.owner.get();
        if (ownerPlayer == null || !ownerPlayer.isOnline()) return false;
        
        // 获取主人的目标
        LivingEntity ownerTarget = getPlayerTarget(ownerPlayer);
        if (ownerTarget == null) {
            return false;
        }
        
        // 检查是否自己
        Entity bukkitEntity = BukkitAdapter.adapt(this.entity);
        if (ownerTarget.equals(bukkitEntity)) {
            return false;
        }
        
        // 检查目标是否在范围内
        double distanceSq = this.entity.getLocation().distanceSquared(BukkitAdapter.adapt(ownerTarget.getLocation()));
        boolean inRange = distanceSq <= this.radiusSq;
        
        return inRange;
    }

    @Override
    public void start() {
        Player ownerPlayer = this.owner.get();
        if (ownerPlayer == null) return;
        
        LivingEntity ownerTarget = getPlayerTarget(ownerPlayer);
        
        if (ownerTarget != null) {
            Entity bukkitEntity = BukkitAdapter.adapt(this.entity);
            if (bukkitEntity instanceof Mob) {
                ((Mob) bukkitEntity).setTarget(ownerTarget);
            }
        }
    }

    @Override
    public boolean shouldEnd() {
        // 如果主人不存在，结束AI
        if (this.owner == null || this.owner.get() == null) {
            return true;
        }
        
        // 尝试获取当前目标
        Entity bukkitEntity = BukkitAdapter.adapt(this.entity);
        if (!(bukkitEntity instanceof Mob)) {
            return true;
        }
        
        Mob mob = (Mob) bukkitEntity;
        LivingEntity currentTarget = mob.getTarget();
        
        // 如果当前没有目标，结束AI
        if (currentTarget == null) {
            return true;
        }
        
        // 如果目标已经死亡，结束AI
        if (currentTarget.isDead()) {
            return true;
        }
        
        // 检查目标是否在范围内
        double distanceSq = this.entity.getLocation().distanceSquared(BukkitAdapter.adapt(currentTarget.getLocation()));
        return distanceSq > this.radiusSq;
    }

    @Override
    public void tick() {
        // 每隔几个tick才更新一次目标
        if (++this.currentTick < this.updateInterval) {
            return;
        }
        this.currentTick = 0;
        
        Player ownerPlayer = this.owner.get();
        if (ownerPlayer == null) return;
        
        LivingEntity ownerTarget = getPlayerTarget(ownerPlayer);
        if (ownerTarget != null) {
            Entity bukkitEntity = BukkitAdapter.adapt(this.entity);
            if (bukkitEntity instanceof Mob) {
                Mob mob = (Mob) bukkitEntity;
                LivingEntity currentTarget = mob.getTarget();
                
                // 如果当前目标不是主人的目标，更新目标
                if (currentTarget == null || !currentTarget.equals(ownerTarget)) {
                    // 删除调试日志
                    mob.setTarget(ownerTarget);
                }
            }
        }
    }

    @Override
    public void end() {
        // AI结束时的清理工作
    }
    
    /**
     * 获取玩家当前的目标
     * @param player 玩家
     * @return 玩家的目标实体，如果没有则返回null
     */
    private LivingEntity getPlayerTarget(Player player) {
        if (player == null) return null;
        
        try {
            // 从战斗监听器获取目标
            UUID targetUUID = plugin.getCombatListener().getPlayerTarget(player.getUniqueId());
            if (targetUUID != null) {
                Entity targetEntity = Bukkit.getEntity(targetUUID);
                if (targetEntity instanceof LivingEntity && !(targetEntity instanceof Player)) {
                    // 删除调试日志
                    return (LivingEntity) targetEntity;
                }
            }
            
            // 尝试从攻击者获取
            if (this.revenge) {
                UUID attackerUUID = plugin.getCombatListener().getPlayerAttacker(player.getUniqueId());
                if (attackerUUID != null) {
                    Entity attackerEntity = Bukkit.getEntity(attackerUUID);
                    if (attackerEntity instanceof LivingEntity && !(attackerEntity instanceof Player)) {
                        // 删除调试日志
                        return (LivingEntity) attackerEntity;
                    }
                }
            }
        } catch(Exception e) {
            // 删除调试日志
        }
        
        // 如果没有找到目标,返回null
        return null;
    }
} 