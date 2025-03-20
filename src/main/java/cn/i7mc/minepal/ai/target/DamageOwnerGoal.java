package cn.i7mc.minepal.ai.target;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.mobs.ai.Pathfinder;
import io.lumine.mythic.core.mobs.ai.PathfindingTarget;
import io.lumine.mythic.core.utils.annotations.MythicAIGoal;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 自定义的攻击伤害主人的实体的AI行为
 * 不要求实体必须是Tameable
 */
@MythicAIGoal(name="damageOwner", aliases={"damageowner", "ownerHurt"}, description="攻击伤害宠物主人的实体")
public class DamageOwnerGoal extends Pathfinder implements PathfindingTarget {
    private transient WeakReference<Player> owner = null;
    private double radiusSq;
    private int updateInterval;
    private int currentTick;
    
    // 记录伤害主人的实体
    private static final Map<UUID, Entity> ownerAttackers = new HashMap<>();

    public DamageOwnerGoal(AbstractEntity entity, String line, MythicLineConfig mlc) {
        super(entity, line, mlc);
        this.goalType = Pathfinder.GoalType.TARGET;
        
        // 检测半径
        double radius = mlc.getDouble(new String[]{"radius", "r"}, 20.0);
        this.radiusSq = Math.pow(radius, 2.0);
        
        // 更新间隔（避免频繁检测）
        this.updateInterval = mlc.getInteger(new String[]{"interval", "i"}, 5);
        this.currentTick = 0;
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
        
        // 获取伤害主人的实体
        Entity attacker = getOwnerAttacker(ownerPlayer);
        if (attacker == null || !(attacker instanceof LivingEntity)) {
            return false;
        }
        
        // 检查目标是否在范围内
        double distanceSq = this.entity.getLocation().distanceSquared(BukkitAdapter.adapt(attacker.getLocation()));
        return distanceSq <= this.radiusSq;
    }

    @Override
    public void start() {
        Player ownerPlayer = this.owner.get();
        Entity attacker = getOwnerAttacker(ownerPlayer);
        
        if (attacker != null && attacker instanceof LivingEntity) {
            Entity bukkitEntity = BukkitAdapter.adapt(this.entity);
            if (bukkitEntity instanceof Mob) {
                ((Mob) bukkitEntity).setTarget((LivingEntity) attacker);
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
        
        Entity attacker = getOwnerAttacker(ownerPlayer);
        if (attacker != null && attacker instanceof LivingEntity) {
            Entity bukkitEntity = BukkitAdapter.adapt(this.entity);
            if (bukkitEntity instanceof Mob) {
                Mob mob = (Mob) bukkitEntity;
                LivingEntity currentTarget = mob.getTarget();
                
                // 如果当前目标不是主人的攻击者，更新目标
                if (currentTarget == null || !currentTarget.equals(attacker)) {
                    mob.setTarget((LivingEntity) attacker);
                }
            }
        }
    }

    @Override
    public void end() {
        // AI结束时的清理工作
    }
    
    /**
     * 获取伤害主人的实体
     * @param player 玩家
     * @return 伤害玩家的实体，如果没有则返回null
     */
    private Entity getOwnerAttacker(Player player) {
        if (player == null) return null;
        
        // 检查记录中是否有伤害主人的实体
        Entity attacker = ownerAttackers.get(player.getUniqueId());
        
        // 如果有记录的攻击者
        if (attacker != null && !attacker.isDead()) {
            // 检查距离
            if (attacker.getLocation().distanceSquared(player.getLocation()) <= this.radiusSq) {
                return attacker;
            }
        }
        
        // 检查当前是否受到伤害
        if (player.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) player.getLastDamageCause();
            Entity damager = event.getDamager();
            
            // 更新攻击者记录
            ownerAttackers.put(player.getUniqueId(), damager);
            
            return damager;
        }
        
        return null;
    }
    
    /**
     * 注册玩家受到伤害的事件
     * @param player 被伤害的玩家
     * @param attacker 攻击者
     */
    public static void registerAttacker(Player player, Entity attacker) {
        if (player != null && attacker != null) {
            ownerAttackers.put(player.getUniqueId(), attacker);
        }
    }
    
    /**
     * 清除玩家的攻击者记录
     * @param player 玩家
     */
    public static void clearAttacker(Player player) {
        if (player != null) {
            ownerAttackers.remove(player.getUniqueId());
        }
    }
} 