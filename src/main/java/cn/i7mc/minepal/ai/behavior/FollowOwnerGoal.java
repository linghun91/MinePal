package cn.i7mc.minepal.ai.behavior;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.mobs.ai.Pathfinder;
import io.lumine.mythic.core.mobs.ai.PathfindingGoal;
import io.lumine.mythic.core.utils.annotations.MythicAIGoal;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * 自定义的跟随主人AI行为，适用于所有类型的实体
 * 不要求实体必须是Tameable
 */
@MythicAIGoal(name="followOwner", aliases={"followowner"}, description="跟随宠物主人的AI行为")
public class FollowOwnerGoal extends Pathfinder implements PathfindingGoal {
    private transient WeakReference<Player> owner = null;
    private double followRangeSq;
    private double minRangeSq;
    private float speed;
    private boolean dropTarget;
    private double teleportDistanceSq;

    public FollowOwnerGoal(AbstractEntity entity, String line, MythicLineConfig mlc) {
        super(entity, line, mlc);
        this.goalType = Pathfinder.GoalType.MOVE_LOOK;
        
        // 读取配置参数
        // 开始跟随的距离
        double followRange = mlc.getDouble(new String[]{"distance", "d", "followrange", "fr"}, 10.0);
        this.followRangeSq = Math.pow(followRange, 2.0);
        
        // 最小跟随距离
        double minRange = mlc.getDouble(new String[]{"minrange", "mr"}, 2.0);
        this.minRangeSq = Math.pow(minRange, 2.0);
        
        // 移动速度
        this.speed = mlc.getFloat(new String[]{"speed", "s"}, 1.0f);
        
        // 是否丢弃目标
        this.dropTarget = mlc.getBoolean(new String[]{"droptarget", "dt"}, true);
        
        // 传送距离阈值
        double teleportDistance = mlc.getDouble(new String[]{"teleport", "tp"}, 30.0);
        this.teleportDistanceSq = Math.pow(teleportDistance, 2.0);
    }

    @Override
    public boolean shouldStart() {
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
        
        if (this.owner == null) {
            return false;
        }
        
        Player ownerPlayer = this.owner.get();
        AbstractLocation ownerLocation = BukkitAdapter.adapt(ownerPlayer.getLocation());
        
        // 如果主人在不同的世界，直接传送
        if (!ownerLocation.getWorld().getUniqueId().equals(this.entity.getLocation().getWorld().getUniqueId())) {
            this.entity.teleport(ownerLocation);
            return true;
        }
        
        // 计算与主人的距离
        double distanceSq = this.entity.getLocation().distanceSquared(ownerLocation);
        
        // 如果距离超过跟随阈值，启动AI
        if (distanceSq > this.followRangeSq) {
            if (this.dropTarget) {
                // 丢弃当前目标
                ai().setTarget((LivingEntity)BukkitAdapter.adapt(this.entity), null);
            }
            return true;
        }
        
        return false;
    }

    @Override
    public void start() {
        // 开始跟随时的初始化
    }

    @Override
    public void tick() {
        // 确保主人存在
        if (this.owner == null || this.owner.get() == null) {
            return;
        }
        
        Player ownerPlayer = this.owner.get();
        AbstractLocation ownerLocation = BukkitAdapter.adapt(ownerPlayer.getLocation());
        
        // 处理不同世界的情况
        if (!ownerLocation.getWorld().getUniqueId().equals(this.entity.getLocation().getWorld().getUniqueId())) {
            this.entity.teleport(ownerLocation);
            return;
        }
        
        double distanceSq = this.entity.getLocation().distanceSquared(ownerLocation);
        
        // 如果距离太远，直接传送
        if (distanceSq > this.teleportDistanceSq) {
            this.entity.teleport(ownerLocation);
            return;
        }
        
        // 如果距离大于最小距离，则向主人移动
        if (distanceSq > this.minRangeSq) {
            ai().navigateToLocation(this.entity, ownerLocation, this.speed);
        }
    }

    @Override
    public boolean shouldEnd() {
        // 如果主人不存在，结束AI
        if (this.owner == null || this.owner.get() == null) {
            return true;
        }
        
        Player ownerPlayer = this.owner.get();
        AbstractLocation ownerLocation = BukkitAdapter.adapt(ownerPlayer.getLocation());
        
        // 如果在不同世界，不结束AI
        if (!ownerLocation.getWorld().getUniqueId().equals(this.entity.getLocation().getWorld().getUniqueId())) {
            return false;
        }
        
        // 如果距离小于最小距离，结束AI
        double distanceSq = this.entity.getLocation().distanceSquared(ownerLocation);
        return distanceSq <= this.minRangeSq;
    }

    @Override
    public void end() {
        // AI结束时的清理工作
    }
} 