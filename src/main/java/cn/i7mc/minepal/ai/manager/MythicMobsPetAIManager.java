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
        // 延迟一点时间再注册，确保MythicMobs完全重载
        Bukkit.getScheduler().runTaskLater(plugin, this::initialize, 5L);
    }
    
    @Override
    public void initialize() {
        // 在MythicMobs完全加载后注册AI行为
        registerBehaviors();
        registerTargets();
    }
    
    @Override
    public void registerBehaviors() {
        // 注册行为AI
        // FollowOwnerGoal已使用@MythicAIGoal注解自动注册，这里只记录
        registeredGoals.put("followowner", FollowOwnerGoal.class);
    }
    
    @Override
    public void registerTargets() {
        // 注册目标AI
        // OwnerTargetGoal和DamageOwnerGoal已使用@MythicAIGoal注解自动注册，这里只记录
        registeredGoals.put("ownertarget", OwnerTargetGoal.class);
        registeredGoals.put("damageowner", DamageOwnerGoal.class);
    }
    
    @Override
    public void applyAI(ActiveMob mythicMob, Player owner) {
        if (mythicMob == null) return;
        
        try {
            // 设置宠物主人
            mythicMob.setOwner(owner.getUniqueId());
            
            // 使用MythicMobs的API设置宠物AI
            AbstractEntity entity = mythicMob.getEntity();
            
            // 强制刷新AI - 尝试重新触发宠物的AI
            try {
                org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
                if (bukkitEntity instanceof org.bukkit.entity.LivingEntity) {
                    // 先重置一次AI，然后再启用
                    org.bukkit.entity.LivingEntity livingEntity = (org.bukkit.entity.LivingEntity) bukkitEntity;
                    livingEntity.setAI(false);
                    
                    // 清除当前目标
                    if (livingEntity instanceof org.bukkit.entity.Mob) {
                        org.bukkit.entity.Mob mobEntity = (org.bukkit.entity.Mob) livingEntity;
                        mobEntity.setTarget(null);
                    }
                    
                    // 立即重新启用AI并设置目标
                    livingEntity.setAI(true);
                    
                    if (livingEntity instanceof org.bukkit.entity.Mob) {
                        org.bukkit.entity.Mob mobEntity = (org.bukkit.entity.Mob) livingEntity;
                        
                        // 检查主人的战斗状态
                        boolean isOwnerInCombat = plugin.getCombatListener().isPlayerInCombat(owner.getUniqueId());
                        org.bukkit.entity.LivingEntity ownerTarget = null;
                        
                        if (isOwnerInCombat) {
                            // 首先检查是否有攻击主人的实体
                            UUID attackerUUID = plugin.getCombatListener().getPlayerAttacker(owner.getUniqueId());
                            if (attackerUUID != null) {
                                Entity attackerEntity = Bukkit.getEntity(attackerUUID);
                                if (attackerEntity instanceof LivingEntity &&
                                    !attackerEntity.getUniqueId().equals(mobEntity.getUniqueId())) {
                                    // 优先设置攻击主人的实体为宠物目标
                                    mobEntity.setTarget((LivingEntity) attackerEntity);
                                    return; // 设置完攻击者为目标后直接返回
                                }
                            }
                            
                            // 如果没有攻击主人的实体，则检查主人攻击的目标
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
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 移除调试日志
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
                    petTaskMap.remove(petUUID).cancel();
                    return;
                }
                
                // 检查主人是否还在线
                if (!owner.isOnline()) {
                    petTaskMap.remove(petUUID).cancel();
                    return;
                }
                
                // 获取宠物实体
                org.bukkit.entity.Entity bukkitEntity = petMob.get().getEntity().getBukkitEntity();
                if (!(bukkitEntity instanceof org.bukkit.entity.Mob)) {
                    return;
                }
                
                org.bukkit.entity.Mob mobEntity = (org.bukkit.entity.Mob) bukkitEntity;
                
                // 检查主人的战斗状态
                boolean isOwnerInCombat = plugin.getCombatListener().isPlayerInCombat(owner.getUniqueId());
                org.bukkit.entity.LivingEntity ownerTarget = null;
                
                if (isOwnerInCombat) {
                    // 首先检查是否有攻击主人的实体
                    UUID attackerUUID = plugin.getCombatListener().getPlayerAttacker(owner.getUniqueId());
                    if (attackerUUID != null) {
                        Entity attackerEntity = Bukkit.getEntity(attackerUUID);
                        if (attackerEntity instanceof LivingEntity &&
                            !attackerEntity.getUniqueId().equals(mobEntity.getUniqueId())) {
                            // 优先设置攻击主人的实体为宠物目标
                            mobEntity.setTarget((LivingEntity) attackerEntity);
                            
                            // 记录宠物是否在战斗中和宠物当前目标
                            ownerCombatMap.put(petUUID, true);
                            ownerTargetMap.put(petUUID, (LivingEntity) attackerEntity);
                            return; // 直接返回，不执行后续代码
                        }
                    }
                    
                    // 如果没有攻击主人的实体，则检查主人攻击的目标
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
                    }
                } else {
                    // 如果主人不在战斗中，但宠物有目标，尝试寻找附近的目标
                    if (mobEntity.getTarget() != null) {
                        // 检查宠物当前目标是否有效
                        LivingEntity currentTarget = mobEntity.getTarget();
                        if (currentTarget.isDead() || !currentTarget.isValid()) {
                            mobEntity.setTarget(null);
                        } else {
                            // 检查宠物是否与目标相距太远
                            double distance = mobEntity.getLocation().distance(currentTarget.getLocation());
                            if (distance > 30.0) {  // 如果距离超过30格，清除目标
                                mobEntity.setTarget(null);
                            }
                        }
                    } else {
                        // 检查附近是否有潜在目标
                        // findNearbyTarget(owner, mobEntity); // 移除这个调用，让宠物不再主动攻击怪物
                    }
                }
                
                // 记录宠物是否在战斗中
                ownerCombatMap.put(petUUID, isOwnerInCombat);
                
                // 记录宠物当前目标
                if (mobEntity.getTarget() != null) {
                    ownerTargetMap.put(petUUID, mobEntity.getTarget());
                } else {
                    ownerTargetMap.remove(petUUID);
                }
                
            }, 40L, 40L); // 2秒检查一次
            
            // 将任务存储起来
            petTaskMap.put(petUUID, task);
            
        } catch (Exception e) {
            // 移除调试日志
        }
    }
    
    @Override
    public void removeAI(ActiveMob mythicMob) {
        if (mythicMob == null) return;
        
        try {
            // 获取宠物UUID
            UUID petUUID = mythicMob.getEntity().getUniqueId();
            
            // 取消定时任务
            if (petTaskMap.containsKey(petUUID)) {
                BukkitTask task = petTaskMap.remove(petUUID);
                if (task != null) {
                    task.cancel();
                }
            }
            
            // 清除战斗状态记录
            ownerCombatMap.remove(petUUID);
            ownerTargetMap.remove(petUUID);
            
            // 尝试清除宠物的目标
            org.bukkit.entity.Entity bukkitEntity = mythicMob.getEntity().getBukkitEntity();
            if (bukkitEntity instanceof org.bukkit.entity.Mob) {
                org.bukkit.entity.Mob mobEntity = (org.bukkit.entity.Mob) bukkitEntity;
                mobEntity.setTarget(null);
            }
            
        } catch (Exception e) {
            // 移除调试日志
        }
    }
    
    @Override
    public void updateAI(ActiveMob petMob) {
        if (petMob == null) return;
        
        // 获取宠物和主人信息
        AbstractEntity abstractEntity = petMob.getEntity();
        io.lumine.mythic.bukkit.utils.serialize.Optl<UUID> ownerUUID = petMob.getOwner();
        
        // 如果没有主人，不需要更新
        if (!ownerUUID.isPresent()) return;
        
        // 获取主人
        Player owner = Bukkit.getPlayer(ownerUUID.get());
        if (owner == null || !owner.isOnline()) return;
        
        // 更新宠物AI
        org.bukkit.entity.Entity bukkitEntity = abstractEntity.getBukkitEntity();
        if (!(bukkitEntity instanceof org.bukkit.entity.Mob)) return;
        
        org.bukkit.entity.Mob mobEntity = (org.bukkit.entity.Mob) bukkitEntity;
        
        // 检查主人的战斗状态
        boolean isOwnerInCombat = plugin.getCombatListener().isPlayerInCombat(owner.getUniqueId());
        
        // 如果主人在战斗中，检查是否需要更新宠物目标
        if (isOwnerInCombat) {
            // 首先检查是否有攻击主人的实体
            UUID attackerUUID = plugin.getCombatListener().getPlayerAttacker(owner.getUniqueId());
            if (attackerUUID != null) {
                Entity attackerEntity = Bukkit.getEntity(attackerUUID);
                if (attackerEntity instanceof LivingEntity) {
                    // 优先设置攻击主人的实体为宠物目标
                    mobEntity.setTarget((LivingEntity) attackerEntity);
                    return;
                }
            }
            
            // 如果没有找到攻击主人的实体，则检查主人攻击的目标
            UUID targetUUID = plugin.getCombatListener().getPlayerTarget(owner.getUniqueId());
            if (targetUUID != null) {
                Entity targetEntity = Bukkit.getEntity(targetUUID);
                if (targetEntity instanceof LivingEntity) {
                    // 设置宠物的目标
                    mobEntity.setTarget((LivingEntity) targetEntity);
                }
            }
        } else {
            // 如果主人不在战斗状态，可以考虑清除宠物目标
            // 这里选择保留目标，让宠物可以继续战斗
        }
    }
    
    @Override
    public void registerPathfindingGoal(String name, Class<? extends PathfindingGoal> goalClass) {
        if (name == null || goalClass == null) return;
        
        registeredGoals.put(name.toLowerCase(), goalClass);
        // 移除调试日志
    }
    
    @Override
    public void registerPathfinderAdapter(String name, Class<? extends PathfinderAdapter> adapterClass) {
        if (name == null || adapterClass == null) return;
        
        registeredAdapters.put(name.toLowerCase(), adapterClass);
        // 移除调试日志
    }
    
    @Override
    public ActiveMob getMythicMob(AbstractEntity abstractEntity) {
        if (abstractEntity == null) return null;
        
        // 使用MythicMobs API获取ActiveMob实例
        Optional<ActiveMob> activeMob = MythicBukkit.inst().getMobManager().getActiveMob(abstractEntity.getUniqueId());
        return activeMob.orElse(null);
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

            // 检查主人是否在战斗状态
            boolean ownerInCombat = plugin.getCombatListener().isPlayerInCombat(owner.getUniqueId());

            // 获取主人当前目标
            UUID targetUUID = plugin.getCombatListener().getPlayerTarget(owner.getUniqueId());
            if (targetUUID != null) {
                Entity target = Bukkit.getEntity(targetUUID);
                if (target != null && target.isValid() && !target.isDead()) {
                    // 设置宠物目标
                    if (pet instanceof Mob) {
                        Mob mob = (Mob) pet;
                        mob.setTarget((LivingEntity) target);
                    }
                }
            } else {
                // 如果主人没有目标，清除宠物目标
                if (pet instanceof Mob) {
                    Mob mob = (Mob) pet;
                    mob.setTarget(null);
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
            return false;
        }
        
        // 获取宠物实体
        org.bukkit.entity.Entity bukkitEntity = petMob.get().getEntity().getBukkitEntity();
        if (!(bukkitEntity instanceof org.bukkit.entity.Mob)) {
            return false;
        }
        
        // 设置目标
        org.bukkit.entity.Mob mobEntity = (org.bukkit.entity.Mob) bukkitEntity;
        if (target instanceof LivingEntity) {
            mobEntity.setTarget((LivingEntity) target);
            return true;
        } else {
            return false;
        }
    }
} 