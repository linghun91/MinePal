package cn.i7mc.minepal.ai.manager;

import cn.i7mc.minepal.MinePal;
import cn.i7mc.minepal.ai.behavior.FollowOwnerGoal;
import cn.i7mc.minepal.ai.target.DamageOwnerGoal;
import cn.i7mc.minepal.ai.target.OwnerTargetGoal;
import cn.i7mc.minepal.listeners.OwnerCombatListener;
import cn.i7mc.minepal.pet.control.PetManager;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.events.MythicReloadedEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.mobs.ai.PathfinderAdapter;
import io.lumine.mythic.core.mobs.ai.PathfindingGoal;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * MythicMobs的AI管理器实现
 */
public class MythicMobsPetAIManager implements PetAIManager, Listener {
    private final MinePal plugin;
    private final Map<String, Class<?>> registeredGoals = new HashMap<>();
    private final Map<String, Class<?>> registeredAdapters = new HashMap<>();
    private final Map<UUID, BukkitTask> petTaskMap = new HashMap<>();
    private final Map<UUID, Boolean> ownerCombatMap = new HashMap<>();
    private final Map<UUID, org.bukkit.entity.LivingEntity> ownerTargetMap = new HashMap<>();
    private final PetManager petManager;
    private final OwnerCombatListener ownerCombatListener;
    
    public MythicMobsPetAIManager(MinePal plugin, PetManager petManager, OwnerCombatListener ownerCombatListener) {
        this.plugin = plugin;
        this.petManager = petManager;
        this.ownerCombatListener = ownerCombatListener;
        // 注册自身作为事件监听器
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * 监听MythicMobs重载事件
     */
    @EventHandler
    public void onMythicReloaded(MythicReloadedEvent event) {
        plugin.getLogger().info("MythicMobs已重载，准备重新注册自定义AI");
        // 延迟一点时间再注册，确保MythicMobs完全重载
        Bukkit.getScheduler().runTaskLater(plugin, this::initialize, 5L);
    }
    
    @Override
    public void initialize() {
        plugin.getMessageManager().debug("ai.initialize");
        
        // 在MythicMobs完全加载后注册AI行为
        registerBehaviors();
        registerTargets();
        
        plugin.getLogger().info("MinePal自定义AI注册完成");
        plugin.getMessageManager().debug("ai.initialized");
    }
    
    @Override
    public void registerBehaviors() {
        plugin.getMessageManager().debug("ai.register-behaviors");
        
        // 注册行为AI
        // FollowOwnerGoal已使用@MythicAIGoal注解自动注册，这里只记录
        registeredGoals.put("followowner", FollowOwnerGoal.class);
        plugin.getLogger().info("已注册followowner行为: " + FollowOwnerGoal.class.getName());
        plugin.getMessageManager().debug("ai.register-goal-success", "name", "followowner");
    }
    
    @Override
    public void registerTargets() {
        plugin.getMessageManager().debug("ai.register-targets");
        
        // 注册目标AI
        // OwnerTargetGoal和DamageOwnerGoal已使用@MythicAIGoal注解自动注册，这里只记录
        registeredGoals.put("ownertarget", OwnerTargetGoal.class);
        plugin.getLogger().info("已注册ownertarget行为: " + OwnerTargetGoal.class.getName());
        plugin.getMessageManager().debug("ai.register-goal-success", "name", "ownertarget");
        
        registeredGoals.put("damageowner", DamageOwnerGoal.class);
        plugin.getLogger().info("已注册damageowner行为: " + DamageOwnerGoal.class.getName());
        plugin.getMessageManager().debug("ai.register-goal-success", "name", "damageowner");
    }
    
    @Override
    public void applyAI(ActiveMob mythicMob, Player owner) {
        if (mythicMob == null) return;
        
        plugin.getMessageManager().debug("ai.apply");
        
        try {
            // 设置宠物主人
            mythicMob.setOwner(owner.getUniqueId());
            
            // 使用MythicMobs的API设置宠物AI
            AbstractEntity entity = mythicMob.getEntity();
            
            // 输出调试信息
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("ai.apply-info", 
                    "pet_type", entity.getBukkitEntity().getType().toString(), 
                    "owner_name", owner.getName(), 
                    "owner_uuid", owner.getUniqueId().toString());
                plugin.getMessageManager().debug("ai.pet-owner-info", 
                    "pet_uuid", entity.getUniqueId().toString(), 
                    "has_owner", String.valueOf(mythicMob.getOwner().isPresent()), 
                    "owner_uuid", (mythicMob.getOwner().isPresent() ? mythicMob.getOwner().get().toString() : "无"));
            }
            
            // 强制刷新AI - 尝试重新触发宠物的AI
            try {
                org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
                if (bukkitEntity instanceof org.bukkit.entity.LivingEntity) {
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getMessageManager().debug("ai.recalculate");
                    }
                    
                    // 先重置一次AI，然后再启用
                    org.bukkit.entity.LivingEntity livingEntity = (org.bukkit.entity.LivingEntity) bukkitEntity;
                    livingEntity.setAI(false);
                    
                    // 清除当前目标
                    if (livingEntity instanceof org.bukkit.entity.Mob) {
                        org.bukkit.entity.Mob mobEntity = (org.bukkit.entity.Mob) livingEntity;
                        if (plugin.getConfigManager().isDebug()) {
                            plugin.getMessageManager().debug("ai.clear-target", 
                                "pet_uuid", mobEntity.getUniqueId().toString(),
                                "previous_target", mobEntity.getTarget() != null ? mobEntity.getTarget().getType().toString() : "无");
                        }
                        mobEntity.setTarget(null);
                    }
                    
                    // 立即重新启用AI并设置目标
                    livingEntity.setAI(true);
                    
                    if (livingEntity instanceof org.bukkit.entity.Mob) {
                        org.bukkit.entity.Mob mobEntity = (org.bukkit.entity.Mob) livingEntity;
                        if (plugin.getConfigManager().isDebug()) {
                            plugin.getMessageManager().debug("ai.enable", 
                                "pet_uuid", mobEntity.getUniqueId().toString(),
                                "has_ai", String.valueOf(mobEntity.hasAI()),
                                "current_target", mobEntity.getTarget() != null ? mobEntity.getTarget().getType().toString() : "无");
                        }
                        
                        // 检查主人的战斗状态
                        boolean isOwnerInCombat = plugin.getCombatListener().isPlayerInCombat(owner.getUniqueId());
                        org.bukkit.entity.LivingEntity ownerTarget = null;
                        
                        if (isOwnerInCombat) {
                            UUID targetUUID = plugin.getCombatListener().getPlayerTarget(owner.getUniqueId());
                            if (targetUUID != null) {
                                Entity targetEntity = Bukkit.getEntity(targetUUID);
                                if (targetEntity instanceof LivingEntity && 
                                    !targetEntity.getUniqueId().equals(mobEntity.getUniqueId())) {
                                    ownerTarget = (LivingEntity) targetEntity;
                                }
                            }
                        }
                        
                        // 如果主人正在战斗且有目标，设置宠物的目标
                        if (isOwnerInCombat && ownerTarget != null) {
                            // 检查宠物当前的目标与主人目标是否一致
                            if (mobEntity.getTarget() == null || 
                                !mobEntity.getTarget().getUniqueId().equals(ownerTarget.getUniqueId())) {
                                // 目标不同，设置宠物的目标
                                mobEntity.setTarget(ownerTarget);
                                if (plugin.getConfigManager().isDebug()) {
                                    plugin.getLogger().info("宠物["+entity.getUniqueId()+"]目标更新: "+ownerTarget.getType());
                                }
                            }
                            
                            // 获取上次战斗时间
                            long lastCombatTime = System.currentTimeMillis() - plugin.getOwnerCombatListener().getLastCombatTime(owner.getUniqueId());
                            
                            plugin.getMessageManager().debug("ai.owner-combat", 
                                "target", ownerTarget.getType().toString(),
                                "target_uuid", ownerTarget.getUniqueId().toString(),
                                "damage_time", String.valueOf(lastCombatTime));
                        } else {
                            plugin.getMessageManager().debug("ai.owner-not-combat");
                        }
                        
                        // 记录宠物当前目标
                        if (mobEntity.getTarget() != null) {
                            plugin.getMessageManager().debug("ai.pet-target", 
                                "target", mobEntity.getTarget().getType().toString(),
                                "target_uuid", mobEntity.getTarget().getUniqueId().toString(),
                                "target_distance", String.format("%.2f", mobEntity.getTarget().getLocation().distance(mobEntity.getLocation())),
                                "has_line_of_sight", String.valueOf(mobEntity.hasLineOfSight(mobEntity.getTarget())));
                        } else {
                            plugin.getMessageManager().debug("ai.pet-no-target");
                        }
                    }
                    
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getMessageManager().debug("ai.re-enabled");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("刷新AI时出错: " + e.getMessage());
            }
            
            // 注册周期性任务，每2秒检查一次宠物目标
            final UUID petUUID = entity.getUniqueId();
            
            // 如果已经有任务，先取消
            if (petTaskMap.containsKey(petUUID)) {
                petTaskMap.get(petUUID).cancel();
            }
            
            // 创建新任务
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                // 检查宠物是否还存在
                Optional<ActiveMob> petMob = MythicBukkit.inst().getMobManager().getActiveMob(petUUID);
                if (!petMob.isPresent()) {
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getMessageManager().debug("ai.pet-task-cancel", 
                            "pet_uuid", petUUID.toString(), 
                            "reason", "宠物不存在");
                    }
                    petTaskMap.remove(petUUID).cancel();
                    return;
                }
                
                // 检查主人是否还在线
                if (!owner.isOnline()) {
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getMessageManager().debug("ai.pet-task-cancel", 
                            "pet_uuid", petUUID.toString(), 
                            "reason", "主人已离线");
                    }
                    petTaskMap.remove(petUUID).cancel();
                    return;
                }
                
                // 获取宠物实体
                org.bukkit.entity.Entity bukkitEntity = petMob.get().getEntity().getBukkitEntity();
                if (!(bukkitEntity instanceof org.bukkit.entity.Mob)) {
                    return;
                }
                
                org.bukkit.entity.Mob mobEntity = (org.bukkit.entity.Mob) bukkitEntity;
                
                // 检查宠物与主人的距离
                double distance = mobEntity.getLocation().distance(owner.getLocation());
                if (plugin.getConfigManager().isDebug()) {
                    try {
                        plugin.getMessageManager().debug("ai.pet-distance", 
                            "distance", String.format("%.2f", distance),
                            "owner_uuid", owner.getUniqueId().toString());
                    } catch (Exception e) {
                        // 直接输出日志而不依赖于消息键
                        plugin.getLogger().info("[MinePal] [DEBUG] 宠物与主人距离: " + 
                            String.format("%.2f", distance) + "米, 主人UUID: " + 
                            owner.getUniqueId());
                    }
                }
                
                // 检查主人是否在战斗状态
                boolean isOwnerInCombat = plugin.getCombatListener().isPlayerInCombat(owner.getUniqueId());
                org.bukkit.entity.LivingEntity ownerTarget = null;
                
                if (isOwnerInCombat) {
                    UUID targetUUID = plugin.getCombatListener().getPlayerTarget(owner.getUniqueId());
                    if (targetUUID != null) {
                        Entity targetEntity = Bukkit.getEntity(targetUUID);
                        if (targetEntity instanceof LivingEntity && 
                            !targetEntity.getUniqueId().equals(mobEntity.getUniqueId())) {
                            ownerTarget = (LivingEntity) targetEntity;
                        }
                    }
                }
                
                // 如果主人正在战斗且有目标，设置宠物的目标
                if (isOwnerInCombat && ownerTarget != null) {
                    // 检查宠物当前的目标与主人目标是否一致
                    if (mobEntity.getTarget() == null || 
                        !mobEntity.getTarget().getUniqueId().equals(ownerTarget.getUniqueId())) {
                        // 目标不同，设置宠物的目标
                        mobEntity.setTarget(ownerTarget);
                        if (plugin.getConfigManager().isDebug()) {
                            plugin.getLogger().info("宠物["+petUUID+"]目标更新: "+ownerTarget.getType());
                        }
                    }
                    
                    // 获取上次战斗时间
                    long lastCombatTime = System.currentTimeMillis() - plugin.getOwnerCombatListener().getLastCombatTime(owner.getUniqueId());
                    
                    plugin.getMessageManager().debug("ai.owner-combat", 
                        "target", ownerTarget.getType().toString(),
                        "target_uuid", ownerTarget.getUniqueId().toString(),
                        "damage_time", String.valueOf(lastCombatTime));
                } else {
                    plugin.getMessageManager().debug("ai.owner-not-combat");
                }
                
                // 记录宠物当前目标
                if (mobEntity.getTarget() != null) {
                    plugin.getMessageManager().debug("ai.pet-target", 
                        "target", mobEntity.getTarget().getType().toString(),
                        "target_uuid", mobEntity.getTarget().getUniqueId().toString(),
                        "target_distance", String.format("%.2f", mobEntity.getTarget().getLocation().distance(mobEntity.getLocation())),
                        "has_line_of_sight", String.valueOf(mobEntity.hasLineOfSight(mobEntity.getTarget())));
                } else {
                    plugin.getMessageManager().debug("ai.pet-no-target");
                    
                    // 尝试找到一个附近的目标
                    findNearbyTarget(owner, mobEntity);
                }
            }, 40L, 40L); // 初次延迟2秒，之后每2秒执行一次
            
            // 保存任务
            petTaskMap.put(petUUID, task);
            
            plugin.getMessageManager().debug("ai.applied", "owner", owner.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("应用AI时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 寻找附近的敌对生物作为宠物目标
     * @param owner 宠物主人
     * @param mobEntity 宠物实体
     */
    private void findNearbyTarget(Player owner, org.bukkit.entity.Mob mobEntity) {
        try {
            if (plugin.getConfigManager().isDebug()) {
                try {
                    plugin.getMessageManager().debug("ai.pet-check", 
                        "pet_uuid", mobEntity.getUniqueId().toString());
                } catch (Exception e) {
                    // 直接输出日志而不依赖于消息键
                    plugin.getLogger().info("[MinePal] [DEBUG] 周期检查宠物[" + 
                        mobEntity.getUniqueId() + "]状态");
                }
            }
            
            // 检查主人的战斗状态
            boolean isOwnerInCombat = plugin.getCombatListener().isPlayerInCombat(owner.getUniqueId());
            UUID targetUUID = plugin.getCombatListener().getPlayerTarget(owner.getUniqueId());
            
            if (isOwnerInCombat && targetUUID != null) {
                Entity target = Bukkit.getEntity(targetUUID);
                if (target instanceof LivingEntity && 
                    !target.getUniqueId().equals(owner.getUniqueId()) && 
                    !target.getUniqueId().equals(mobEntity.getUniqueId()) &&
                    !(target instanceof Player)) {
                    
                    LivingEntity livingTarget = (LivingEntity) target;
                    // 检查目标是否真的有效
                    if (livingTarget.isValid() && !livingTarget.isDead() && 
                        mobEntity.hasLineOfSight(livingTarget) &&
                        livingTarget.getLocation().distance(mobEntity.getLocation()) <= 20) {
                        
                        if (plugin.getConfigManager().isDebug()) {
                            plugin.getMessageManager().debug("ai.pet-target", 
                                "target", livingTarget.getType().toString(),
                                "target_uuid", livingTarget.getUniqueId().toString(),
                                "target_distance", String.format("%.2f", livingTarget.getLocation().distance(mobEntity.getLocation())),
                                "has_line_of_sight", String.valueOf(mobEntity.hasLineOfSight(livingTarget)));
                        }
                        mobEntity.setTarget(livingTarget);
                    } else {
                        if (plugin.getConfigManager().isDebug()) {
                            plugin.getMessageManager().debug("ai.target-invalid", 
                                "reason", !livingTarget.isValid() ? "目标无效" :
                                    livingTarget.isDead() ? "目标已死亡" :
                                    !mobEntity.hasLineOfSight(livingTarget) ? "目标不可见" :
                                    "目标距离过远",
                                "target_uuid", livingTarget.getUniqueId().toString(),
                                "owner_uuid", owner.getUniqueId().toString(),
                                "pet_uuid", mobEntity.getUniqueId().toString());
                        }
                        mobEntity.setTarget(null);
                    }
                } else {
                    mobEntity.setTarget(null);
                }
            } else {
                // 如果主人不在战斗,清除目标
                mobEntity.setTarget(null);
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getMessageManager().debug("ai.pet-no-target");
                }
            }
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("ai.ai-error", "error", e.getMessage());
            }
            plugin.getLogger().warning("寻找目标时出错: " + e.getMessage());
        }
    }
    
    @Override
    public void removeAI(ActiveMob mythicMob) {
        if (mythicMob == null) return;
        
        plugin.getMessageManager().debug("ai.remove");
        
        // 移除宠物主人
        mythicMob.removeOwner();
        
        // 取消周期性任务
        UUID petUUID = mythicMob.getEntity().getUniqueId();
        if (petTaskMap.containsKey(petUUID)) {
            petTaskMap.get(petUUID).cancel();
            petTaskMap.remove(petUUID);
        }
        
        // 清理主人的战斗状态
        if (mythicMob.getOwner().isPresent()) {
            ownerCombatMap.remove(mythicMob.getOwner().get());
            ownerTargetMap.remove(mythicMob.getOwner().get());
        }
        
        plugin.getMessageManager().debug("ai.removed");
    }
    
    @Override
    public void updateAI(ActiveMob petMob) {
        if (petMob == null) return;
        
        plugin.getMessageManager().debug("ai.update");
        
        try {
            // 检查宠物是否有主人
            if (!petMob.getOwner().isPresent()) {
                return;
            }
            
            UUID ownerUUID = petMob.getOwner().get();
            Player owner = Bukkit.getPlayer(ownerUUID);
            
            // 检查主人是否在线
            if (owner == null || !owner.isOnline()) {
                return;
            }
            
            // 获取宠物实体
            AbstractEntity abstractEntity = petMob.getEntity();
            Entity bukkitEntity = abstractEntity.getBukkitEntity();
            if (!(bukkitEntity instanceof Mob)) {
                return;
            }
            
            Mob mobEntity = (Mob) bukkitEntity;
            UUID petUUID = abstractEntity.getUniqueId();
            
            // 检查主人的战斗状态
            boolean isOwnerInCombat = plugin.getCombatListener().isPlayerInCombat(ownerUUID);
            UUID targetUUID = plugin.getCombatListener().getPlayerTarget(ownerUUID);
            
            if (isOwnerInCombat && targetUUID != null) {
                Entity target = Bukkit.getEntity(targetUUID);
                if (target instanceof LivingEntity && 
                    !target.getUniqueId().equals(mobEntity.getUniqueId()) && 
                    target.isValid() && !target.isDead()) {
                    
                    LivingEntity livingTarget = (LivingEntity) target;
                    
                    // 保持对MythicMobs API的使用
                    petMob.setTarget(BukkitAdapter.adapt(livingTarget));
                    
                    // 设置Bukkit实体的目标
                    mobEntity.setTarget(livingTarget);
                    
                    if (plugin.getConfigManager().isDebug()) {
                        double distanceToTarget = mobEntity.getLocation().distance(livingTarget.getLocation());
                        plugin.getMessageManager().debug("ai.pet-target", 
                            "target", livingTarget.getType().toString(),
                            "target_uuid", livingTarget.getUniqueId().toString(),
                            "target_distance", String.format("%.2f", distanceToTarget));
                    }
                    
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getLogger().info("宠物[" + petUUID + "]目标更新: " + livingTarget.getType());
                    }
                }
            } else {
                // 清除目标 - 同时清除MythicMobs和Bukkit目标
                petMob.setTarget(null);
                
                if (mobEntity.getTarget() != null) {
                    mobEntity.setTarget(null);
                }
                
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getMessageManager().debug("ai.pet-no-target");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("更新AI时出错: " + e.getMessage());
            if (plugin.getConfigManager().isDebug()) {
                e.printStackTrace();
            }
        }
        
        plugin.getMessageManager().debug("ai.updated");
    }
    
    @Override
    public void registerPathfindingGoal(String name, Class<? extends PathfindingGoal> goalClass) {
        plugin.getMessageManager().debug("ai.register-goal", "name", name);
        
        // 记录已注册的AI目标
        registeredGoals.put(name, goalClass);
        plugin.getLogger().info("已记录自定义AI目标: " + name);
        plugin.getMessageManager().debug("ai.register-goal-success", "name", name);
    }
    
    @Override
    public void registerPathfinderAdapter(String name, Class<? extends PathfinderAdapter> adapterClass) {
        plugin.getMessageManager().debug("ai.register-adapter", "name", name);
        
        // 记录已注册的AI适配器
        registeredAdapters.put(name, adapterClass);
        plugin.getLogger().info("已记录自定义AI适配器: " + name);
        plugin.getMessageManager().debug("ai.register-adapter-success", "name", name);
    }
    
    @Override
    public ActiveMob getMythicMob(AbstractEntity abstractEntity) {
        return MythicBukkit.inst().getMobManager().getActiveMob(abstractEntity.getUniqueId()).orElse(null);
    }
    
    /**
     * 获取玩家正在看的实体
     * @param player 玩家
     * @param range 最大检测范围
     * @return 目标实体，如果没有则返回null
     */
    private org.bukkit.entity.LivingEntity getPlayerTargetEntity(Player player, int range) {
        try {
            // 使用Bukkit的raytrace功能获取玩家视线方向的实体
            java.util.List<Entity> entities = player.getNearbyEntities(range, range, range);
            org.bukkit.util.RayTraceResult rayResult = player.getWorld().rayTraceEntities(
                player.getEyeLocation(), 
                player.getLocation().getDirection(), 
                range, 
                entity -> entity != player && entities.contains(entity)
            );
            
            if (rayResult != null && rayResult.getHitEntity() != null && 
                rayResult.getHitEntity() instanceof org.bukkit.entity.LivingEntity) {
                return (org.bukkit.entity.LivingEntity) rayResult.getHitEntity();
            }
            return null;
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("ai.ai-error", "error", "获取玩家目标实体失败: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * 周期性检查宠物状态
     */
    private void checkPetStatus() {
        for (Map.Entry<UUID, LivingEntity> entry : petManager.getPets().entrySet()) {
            LivingEntity pet = entry.getValue();
            if (pet == null || !pet.isValid() || pet.isDead()) {
                continue;
            }

            // 获取宠物主人
            Player owner = petManager.getPetOwner(pet);
            if (owner == null || !owner.isOnline()) {
                continue;
            }

            // 计算与主人的距离
            double distanceToOwner = pet.getLocation().distance(owner.getLocation());
            if (plugin.getConfig().getBoolean("settings.debug", false)) {
                Bukkit.getLogger().info("[MinePal] [DEBUG] 宠物与主人距离: " + 
                    String.format("%.2f", distanceToOwner) + "米, 主人UUID: " + owner.getUniqueId());
            }

            // 检查主人是否在战斗状态
            boolean ownerInCombat = plugin.getCombatListener().isPlayerInCombat(owner.getUniqueId());
            if (plugin.getConfig().getBoolean("settings.debug", false)) {
                Bukkit.getLogger().info("[MinePal] [DEBUG] 主人当前" + (ownerInCombat ? "正在战斗" : "未战斗"));
            }

            // 获取主人当前目标
            UUID targetUUID = plugin.getCombatListener().getPlayerTarget(owner.getUniqueId());
            if (targetUUID != null) {
                Entity target = Bukkit.getEntity(targetUUID);
                if (target != null && target.isValid() && !target.isDead()) {
                    // 检查目标是否受到过实际伤害
                    if (plugin.getCombatListener().hasEntityBeenDamaged(target)) {
                        // 设置宠物目标
                        if (pet instanceof Mob) {
                            Mob mob = (Mob) pet;
                            mob.setTarget((LivingEntity) target);
                            
                            if (plugin.getConfig().getBoolean("settings.debug", false)) {
                                double distanceToTarget = pet.getLocation().distance(target.getLocation());
                                boolean hasLineOfSight = mob.hasLineOfSight(target);
                                Bukkit.getLogger().info("[MinePal] [DEBUG] 宠物当前目标: " + target.getType() + 
                                    ", UUID: " + targetUUID + ", 距离: " + 
                                    String.format("%.2f", distanceToTarget) + "米, 视线: " + hasLineOfSight);
                            }
                        }
                    }
                }
            } else {
                // 如果主人没有目标，清除宠物目标
                if (pet instanceof Mob) {
                    Mob mob = (Mob) pet;
                    mob.setTarget(null);
                    if (plugin.getConfig().getBoolean("settings.debug", false)) {
                        Bukkit.getLogger().info("[MinePal] [DEBUG] 宠物当前无目标，尝试寻找新目标");
                    }
                }
            }
        }
    }

    /**
     * 监听玩家死亡事件
     */
    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player == null) return;

        // 检查玩家是否有宠物
        UUID petUUID = plugin.getPetUtils().getPetUUID(player);
        if (petUUID == null) return;

        // 获取宠物实体
        Entity pet = null;
        for (Entity entity : player.getWorld().getEntities()) {
            if (entity.getUniqueId().equals(petUUID)) {
                pet = entity;
                break;
            }
        }

        if (pet != null) {
            // 获取MythicMobs实体
            AbstractEntity abstractEntity = BukkitAdapter.adapt(pet);
            ActiveMob activeMob = getMythicMob(abstractEntity);

            if (activeMob != null) {
                // 移除AI设置
                removeAI(activeMob);
                
                // 使用MythicMobs API移除实体
                activeMob.remove();
            } else {
                // 如果不是MythicMobs实体，直接移除
                pet.remove();
            }

            // 注销宠物
            plugin.getPetUtils().unregisterPet(player);

            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("pet.owner-death", 
                    "owner", player.getName(),
                    "pet_uuid", petUUID.toString());
            }
        }
    }

    /**
     * 根据宠物UUID设置其目标实体
     * 
     * @param petUuid 宠物的UUID
     * @param target 目标实体
     * @return 是否成功设置
     */
    @Override
    public boolean setTarget(UUID petUuid, Entity target) {
        if (petUuid == null || target == null) return false;
        
        // 获取宠物ActiveMob对象
        Optional<ActiveMob> petMob = MythicBukkit.inst().getMobManager().getActiveMob(petUuid);
        if (!petMob.isPresent()) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("ai.target-set-failed", 
                    "pet_uuid", petUuid.toString(), 
                    "reason", "宠物不存在");
            }
            return false;
        }
        
        // 获取宠物实体
        org.bukkit.entity.Entity bukkitEntity = petMob.get().getEntity().getBukkitEntity();
        if (!(bukkitEntity instanceof org.bukkit.entity.Mob)) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("ai.target-set-failed", 
                    "pet_uuid", petUuid.toString(), 
                    "reason", "宠物不是Mob类型");
            }
            return false;
        }
        
        // 设置目标
        org.bukkit.entity.Mob mobEntity = (org.bukkit.entity.Mob) bukkitEntity;
        if (target instanceof LivingEntity) {
            mobEntity.setTarget((LivingEntity) target);
            
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("ai.target-set-success", 
                    "pet_uuid", petUuid.toString(), 
                    "target_type", target.getType().toString(),
                    "target_uuid", target.getUniqueId().toString());
            }
            return true;
        } else {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("ai.target-set-failed", 
                    "pet_uuid", petUuid.toString(), 
                    "reason", "目标不是LivingEntity类型");
            }
            return false;
        }
    }
} 