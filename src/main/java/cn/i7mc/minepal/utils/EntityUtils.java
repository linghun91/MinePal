package cn.i7mc.minepal.utils;

import cn.i7mc.minepal.MinePal;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 实体工具类，统一处理不同实体类型的操作
 * 遵循接口优先原则，优先使用实体接口而非具体实现类
 */
public class EntityUtils {
    private static MinePal plugin;
    private static MessageManager messageManager;
    private static ConfigManager configManager;

    /**
     * 初始化工具类
     * @param plugin 插件实例
     */
    public static void init(MinePal plugin) {
        EntityUtils.plugin = plugin;
        if (plugin.getConfigManager().isDebug()) {
            plugin.getMessageManager().debug("entity.utils.init");
        }
    }

    /**
     * 初始化工具类
     * @param plugin 插件实例
     * @param messageManager 消息管理器
     * @param configManager 配置管理器
     */
    public static void init(Plugin plugin, MessageManager messageManager, ConfigManager configManager) {
        EntityUtils.messageManager = messageManager;
        EntityUtils.configManager = configManager;
    }

    /**
     * 检查实体是否为宠物
     * @param entity 要检查的实体
     * @return 是否为宠物
     */
    public static boolean isPet(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        // 对于可驯服的实体，检查是否已被驯服
        if (entity instanceof Tameable) {
            return ((Tameable) entity).isTamed();
        }
        
        // 对于其他类型的实体，需要根据具体情况判断
        return false;
    }
    
    /**
     * 获取宠物的主人
     * @param entity 宠物实体
     * @return 主人实体，如果没有主人则返回null
     */
    public static Player getPetOwner(Entity entity) {
        if (entity == null) {
            return null;
        }
        
        // 对于可驯服的实体，直接获取主人
        if (entity instanceof Tameable) {
            AnimalTamer owner = ((Tameable) entity).getOwner();
            if (owner instanceof Player) {
                return (Player) owner;
            }
        }
        
        // 自定义宠物逻辑（如果有）
        return null;
    }
    
    /**
     * 获取宠物的主人，返回Optional<AnimalTamer>
     * @param entity 宠物实体
     * @return 可选的主人对象
     */
    public static Optional<AnimalTamer> getPetOwnerOptional(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        
        // 对于可驯服的实体，直接获取主人
        if (entity instanceof Tameable) {
            Tameable tameable = (Tameable) entity;
            if (tameable.isTamed() && tameable.getOwner() != null) {
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getMessageManager().debug("entity.get-owner", 
                            "entity_type", entity.getType().toString());
                }
                return Optional.of(tameable.getOwner());
            }
        }
        
        // 自定义宠物逻辑（如果有）
        return Optional.empty();
    }
    
    /**
     * 检查实体是否是指定玩家的宠物
     * 
     * @param entity 要检查的实体
     * @param player 可能的主人
     * @return 是否是该玩家的宠物
     */
    public static boolean isPlayerPet(Entity entity, Player player) {
        if (entity == null || player == null) return false;
        
        // 获取实体的主人
        Optional<AnimalTamer> owner = getPetOwnerOptional(entity);
        if (!owner.isPresent()) return false;
        
        // 检查主人是否匹配
        return owner.get().getUniqueId().equals(player.getUniqueId());
    }
    
    /**
     * 设置宠物坐下状态
     * 
     * @param entity 宠物实体
     * @param sitting 是否坐下
     * @return 是否成功设置
     */
    public static boolean setPetSitting(Entity entity, boolean sitting) {
        if (entity == null) return false;
        
        if (entity instanceof Sittable) {
            Sittable sittable = (Sittable) entity;
            sittable.setSitting(sitting);
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("entity.set-sitting", String.valueOf(sitting));
            }
            return true;
        }
        
        return false;
    }
    
    /**
     * 设置实体的目标
     * @param entity 实体
     * @param target 目标实体
     * @return 是否成功设置
     */
    public static boolean setTarget(Entity entity, LivingEntity target) {
        if (!(entity instanceof Mob)) {
            return false;
        }
        
        Mob mob = (Mob) entity;
        mob.setTarget(target);
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getMessageManager().debug("entity.set-target", 
                    "pet_type", entity.getType().toString(),
                    "target_type", target != null ? target.getType().toString() : "null");
        }
        
        return true;
    }
    
    /**
     * 设置MythicMobs实体的目标
     * @param pet MythicMobs实体
     * @param target 目标实体
     * @return 是否成功设置
     */
    public static boolean setMythicTarget(Entity pet, Entity target) {
        try {
            ActiveMob am = MythicBukkit.inst().getMobManager().getActiveMob(pet.getUniqueId()).orElse(null);
            if (am == null) {
                return false;
            }
            
            if (target != null) {
                AbstractEntity abstractTarget = BukkitAdapter.adapt(target);
                am.setTarget(abstractTarget);
            } else {
                am.resetTarget();
            }
            
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("entity.set-mythic-target", 
                        "pet_type", pet.getType().toString(),
                        "target_type", target != null ? target.getType().toString() : "null");
            }
            
            return true;
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("设置MythicMobs目标时出错: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * 设置实体是否可受伤害
     * 
     * @param entity 实体
     * @param invulnerable 是否无敌
     */
    public static void setInvulnerable(Entity entity, boolean invulnerable) {
        if (entity == null) return;
        entity.setInvulnerable(invulnerable);
    }
    
    /**
     * 检查实体是否在战斗状态
     * 
     * @param entity 实体
     * @return 是否在战斗
     */
    public static boolean isInCombat(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        // 这里应该根据插件的战斗状态管理逻辑来判断
        if (entity instanceof Player) {
            return plugin.getOwnerCombatListener().isInCombat((Player) entity);
        }
        
        // 如果是宠物，检查其主人是否处于战斗状态
        Player owner = getPetOwner(entity);
        if (owner != null) {
            return plugin.getOwnerCombatListener().isInCombat(owner);
        }
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getMessageManager().debug("entity.check-combat");
        }
        
        // 返回基于战斗逻辑的结果
        return false;
    }
    
    /**
     * 传送宠物到主人位置
     * @param pet 宠物实体
     * @param owner 主人
     * @return 是否成功传送
     */
    public static boolean teleportPetToOwner(Entity pet, Player owner) {
        try {
            if (pet == null || owner == null) {
                return false;
            }
            
            return pet.teleport(owner.getLocation());
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("entity.teleport-error", 
                        "pet_type", pet.getType().toString(),
                        "owner", owner.getName(),
                        "error", e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * 检查实体是否是敌对生物
     * 
     * @param entity 要检查的实体
     * @return 是否为敌对生物
     */
    public static boolean isHostile(Entity entity) {
        if (entity == null) return false;
        
        return entity instanceof Monster || // 大多数敌对生物
               entity instanceof Slime || // 史莱姆
               entity instanceof Phantom || // 幻翼
               entity instanceof Hoglin || // 疣猪兽
               entity instanceof Shulker || // 潜影贝
               entity instanceof Boss; // Boss生物
    }
    
    /**
     * 计算两个实体间的安全攻击距离
     * 
     * @param attacker 攻击者
     * @param target 目标
     * @return 建议的攻击距离
     */
    public static double getAttackDistance(Entity attacker, Entity target) {
        if (attacker == null || target == null) return 3.0;
        
        // 基础攻击距离
        double baseDistance = 3.0;
        
        // 根据实体类型调整
        if (attacker instanceof Monster) {
            if (attacker.getType() == EntityType.CREEPER) {
                baseDistance = 2.0; // 苦力怕爆炸范围
            } else if (attacker.getType() == EntityType.SKELETON || 
                       attacker.getType() == EntityType.STRAY) {
                baseDistance = 8.0; // 骷髅远程攻击
            } else if (attacker.getType() == EntityType.BLAZE) {
                baseDistance = 6.0; // 烈焰人火球攻击
            }
        }
        
        return baseDistance;
    }
    
    /**
     * 查找实体周围最近的敌人
     * 
     * @param entity 中心实体
     * @param radius 搜索半径
     * @return 最近的敌人，如果没有找到返回null
     */
    public static LivingEntity findNearestEnemy(Entity entity, double radius) {
        if (entity == null) return null;
        
        LivingEntity nearestEnemy = null;
        double nearestDistanceSq = radius * radius;
        
        for (Entity nearby : entity.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof LivingEntity && isHostile(nearby)) {
                // 计算距离平方
                double distanceSq = entity.getLocation().distanceSquared(nearby.getLocation());
                if (distanceSq < nearestDistanceSq) {
                    nearestDistanceSq = distanceSq;
                    nearestEnemy = (LivingEntity) nearby;
                }
            }
        }
        
        return nearestEnemy;
    }
    
    /**
     * 查找主人周围的敌人并设置为宠物的目标
     * 
     * @param pet 宠物实体
     * @param owner 主人
     * @param radius 搜索半径
     * @return 是否找到并设置了目标
     */
    public static boolean setOwnerEnemyAsTarget(Entity pet, Player owner, double radius) {
        if (pet == null || owner == null) return false;
        
        // 获取主人最近的敌人
        LivingEntity nearestEnemy = null;
        double nearestDistanceSq = radius * radius;
        
        for (Entity nearby : owner.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof Monster) {
                LivingEntity monster = (LivingEntity) nearby;
                
                // 如果这个怪物正在攻击主人，优先考虑
                if (monster instanceof Mob && ((Mob) monster).getTarget() != null && 
                    ((Mob) monster).getTarget().equals(owner)) {
                    
                    // 计算距离平方
                    double distanceSq = owner.getLocation().distanceSquared(monster.getLocation());
                    if (distanceSq < nearestDistanceSq) {
                        nearestDistanceSq = distanceSq;
                        nearestEnemy = monster;
                    }
                }
            }
        }
        
        // 如果找到敌人，设置为宠物的目标
        if (nearestEnemy != null) {
            return setTarget(pet, nearestEnemy);
        }
        
        return false;
    }
    
    /**
     * 使宠物查看指定位置
     * 
     * @param pet 宠物实体
     * @param location 目标位置
     */
    public static void makePetLookAt(Entity pet, org.bukkit.Location location) {
        if (pet == null || location == null) return;
        
        // 获取方向向量
        org.bukkit.util.Vector direction = location.toVector().subtract(pet.getLocation().toVector()).normalize();
        
        // 设置宠物朝向
        pet.teleport(pet.getLocation().setDirection(direction));
    }
    
    /**
     * 通过MythicMobs获取宠物的显示名称
     * 
     * @param pet 宠物实体
     * @return 显示名称，如果获取失败则返回默认名称
     */
    public static String getPetDisplayName(Entity pet) {
        if (pet == null) return "宠物";
        
        // 检查自定义名称
        if (pet.getCustomName() != null && !pet.getCustomName().isEmpty()) {
            return pet.getCustomName();
        }
        
        // 检查是否为MythicMobs实体
        Optional<ActiveMob> activeMob = MythicBukkit.inst().getMobManager().getActiveMob(pet.getUniqueId());
        if (activeMob.isPresent()) {
            try {
                String displayName = activeMob.get().getDisplayName();
                if (displayName != null && !displayName.isEmpty()) {
                    return displayName;
                }
            } catch (Exception e) {
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().warning("获取宠物显示名称时出错: " + e.getMessage());
                }
            }
        }
        
        // 默认使用实体类型名称
        return pet.getType().toString();
    }

    /**
     * 检查实体是否属于指定玩家
     * @param entity 要检查的实体
     * @param playerUuid 玩家UUID
     * @return 是否属于该玩家
     */
    public static boolean isOwnedBy(Entity entity, UUID playerUuid) {
        return getPetOwnerOptional(entity)
                .map(AnimalTamer::getUniqueId)
                .filter(uuid -> uuid.equals(playerUuid))
                .isPresent();
    }

    /**
     * 检查实体是否可以坐下
     * @param entity 要检查的实体
     * @return 是否可以坐下
     */
    public static boolean canSit(Entity entity) {
        return entity instanceof Sittable;
    }

    /**
     * 传送宠物到指定位置
     * @param pet 宠物实体
     * @param location 目标位置
     * @return 是否成功传送
     */
    public static boolean teleportPet(Entity pet, Location location) {
        if (pet == null || location == null) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("entity.teleport-error", "pet_or_location_null");
            }
            return false;
        }
        
        try {
            return pet.teleport(location);
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("entity.teleport-error", e.getMessage());
            }
            return false;
        }
    }

    /**
     * 查找半径内的敌对实体
     * @param center 中心位置
     * @param radius 半径
     * @return 敌对实体集合
     */
    public static Collection<Entity> findNearbyEnemies(Location center, double radius) {
        Collection<Entity> nearbyEntities = center.getWorld().getNearbyEntities(center, radius, radius, radius);
        
        Collection<Entity> enemies = nearbyEntities.stream()
                .filter(entity -> entity instanceof Monster || entity instanceof Phantom || entity instanceof Hoglin)
                .collect(Collectors.toList());
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getMessageManager().debug("entity.find-enemy", 
                    "radius", String.valueOf(radius),
                    "found", String.valueOf(enemies.size()));
        }
        
        return enemies;
    }

    /**
     * 使宠物看向指定位置
     * @param pet 宠物实体
     * @param location 目标位置
     */
    public static void lookAt(Entity pet, Location location) {
        if (pet == null || location == null) {
            return;
        }
        
        Location petLoc = pet.getLocation();
        double dx = location.getX() - petLoc.getX();
        double dz = location.getZ() - petLoc.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        
        petLoc.setYaw(yaw);
        pet.teleport(petLoc);
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getMessageManager().debug("entity.look-at", 
                    "pet_type", pet.getType().toString(),
                    "location", location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        }
    }

    /**
     * 获取实体类型的显示名称
     * @param entity 实体
     * @return 显示名称
     */
    public static String getEntityDisplayName(Entity entity) {
        if (entity == null) {
            return "未知";
        }
        
        if (entity.getCustomName() != null && !entity.getCustomName().isEmpty()) {
            return entity.getCustomName();
        }
        
        if (entity instanceof Player) {
            return ((Player) entity).getName();
        }
        
        // 尝试获取MythicMobs实体的名称
        if (MythicBukkit.inst() != null) {
            try {
                ActiveMob am = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId()).orElse(null);
                if (am != null) {
                    String mobName = am.getDisplayName();
                    if (mobName != null && !mobName.isEmpty()) {
                        return mobName;
                    }
                }
            } catch (Exception e) {
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().warning("获取宠物显示名称时出错: " + e.getMessage());
                }
            }
        }
        
        return entity.getType().toString();
    }

    /**
     * 从UUID获取实体
     * @param uuid 实体的UUID
     * @return 对应的实体，如果找不到则返回null
     */
    public static Entity getEntityFromUUID(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(uuid)) {
                    return entity;
                }
            }
        }
        
        return null;
    }
} 