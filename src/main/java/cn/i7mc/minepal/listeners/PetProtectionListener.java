package cn.i7mc.minepal.listeners;

import cn.i7mc.minepal.MinePal;
import cn.i7mc.minepal.utils.EntityUtils;
import cn.i7mc.minepal.utils.PetUtils;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.skills.SkillCaster;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.skills.SkillMetadataImpl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Optional;
import java.util.UUID;

/**
 * 宠物保护监听器，负责处理宠物与主人互相伤害的防护机制
 */
public class PetProtectionListener implements Listener {
    private final MinePal plugin;
    private final PetUtils petUtils;

    public PetProtectionListener(MinePal plugin) {
        this.plugin = plugin;
        this.petUtils = plugin.getPetUtils();
    }

    /**
     * 处理实体伤害事件，防止宠物受到伤害
     * @param event 实体伤害事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        // 如果事件已经被取消，则不处理
        if (event.isCancelled()) {
            return;
        }

        Entity entity = event.getEntity();

        // 检查是否是宠物
        if (!EntityUtils.isPet(entity)) {
            return;
        }

        // 获取宠物的主人
        Player owner = EntityUtils.getPetOwner(entity);
        if (owner == null) {
            return;
        }

        // 检查伤害类型，根据配置决定是否防止宠物受伤
        if (shouldPreventDamage(event.getCause())) {
            event.setCancelled(true);
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("pet.protection.cancel-damage", 
                        "pet_type", entity.getType().name(),
                        "cause", event.getCause().name());
            }
        }

        // 如果是实体攻击实体的伤害
        if (event instanceof EntityDamageByEntityEvent) {
            handleEntityDamageByEntity((EntityDamageByEntityEvent) event, owner);
        }
    }

    /**
     * 处理实体被其他实体伤害的事件
     * @param event 实体伤害事件
     * @param owner 宠物的主人
     */
    private void handleEntityDamageByEntity(EntityDamageByEntityEvent event, Player owner) {
        Entity damager = event.getDamager();

        // 如果攻击者是宠物的主人，取消伤害
        if (damager.equals(owner)) {
            event.setCancelled(true);
            owner.sendMessage(plugin.getMessageManager().getMessage("damage.prevent-owner-attack"));
            return;
        }

        // 如果攻击者是主人的另一个宠物，取消伤害
        if (EntityUtils.isPet(damager) && EntityUtils.isPlayerPet(damager, owner)) {
            event.setCancelled(true);
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("pet.protection.cancel-pet-attack", 
                        "pet1", event.getEntity().getType().name(),
                        "pet2", damager.getType().name());
            }
        }
    }

    /**
     * 处理实体目标选择事件，防止宠物攻击主人或主人的其他宠物
     * @param event 实体目标选择事件
     */
    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        // 如果事件已经被取消，则不处理
        if (event.isCancelled()) {
            return;
        }

        Entity entity = event.getEntity();
        Entity target = event.getTarget();

        // 如果目标为空，则不处理
        if (target == null) {
            return;
        }

        // 检查是否是宠物
        if (!EntityUtils.isPet(entity)) {
            return;
        }

        // 获取宠物的主人
        Player owner = EntityUtils.getPetOwner(entity);
        if (owner == null) {
            return;
        }

        // 如果目标是宠物的主人，取消目标选择
        if (target.equals(owner)) {
            event.setCancelled(true);
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("pet.protection.cancel-target-owner", 
                        "pet_type", entity.getType().name());
            }
            return;
        }

        // 如果目标是主人的另一个宠物，取消目标选择
        if (EntityUtils.isPet(target) && EntityUtils.isPlayerPet(target, owner)) {
            event.setCancelled(true);
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("pet.protection.cancel-target-other-pet", 
                        "pet1", entity.getType().name(),
                        "pet2", target.getType().name());
            }
        }
    }

    /**
     * 根据伤害类型决定是否应该防止宠物受到伤害
     * @param cause 伤害原因
     * @return 是否应该防止伤害
     */
    private boolean shouldPreventDamage(EntityDamageEvent.DamageCause cause) {
        // 根据配置决定哪些伤害类型应该被防止
        switch (cause) {
            case FALL:
            case DROWNING:
            case FIRE:
            case FIRE_TICK:
            case LAVA:
            case LIGHTNING:
            case POISON:
            case STARVATION:
            case WITHER:
            case THORNS:
            case DRAGON_BREATH:
            case FLY_INTO_WALL:
            case HOT_FLOOR:
            case CRAMMING:
            case DRYOUT:
                return true;
            default:
                return false;
        }
    }

    /**
     * 处理MythicMobs技能伤害
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMythicMobSkillDamage(EntityDamageByEntityEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK &&
            event.getCause() != EntityDamageEvent.DamageCause.CUSTOM &&
            event.getCause() != EntityDamageEvent.DamageCause.MAGIC) {
            return;
        }
        
        Entity damaged = event.getEntity();
        Entity damager = event.getDamager();
        
        // 检查是否是玩家的宠物
        Player petOwner = EntityUtils.getPetOwner(damaged);
        if (petOwner == null) return;
        
        // 检查是否是主人的技能伤害
        if (damager instanceof Player && damager.getUniqueId().equals(petOwner.getUniqueId())) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("pet.prevent-owner-mythic-skill", 
                    "owner", petOwner.getName(),
                    "pet_type", damaged.getType().toString(),
                    "damage_cause", event.getCause().toString());
            }
            event.setCancelled(true);
            return;
        }
        
        // 检查AOE伤害距离
        if (damager instanceof Player && petOwner.getWorld().equals(damager.getWorld())) {
            double distance = petOwner.getLocation().distance(damager.getLocation());
            if (distance < 15.0) { // 15格内视为主人的AOE影响范围
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getMessageManager().debug("pet.prevent-owner-aoe", 
                        "owner", petOwner.getName(),
                        "pet_type", damaged.getType().toString(),
                        "damage_source", damager.getName(),
                        "distance", String.format("%.2f", distance));
                }
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * 处理一般伤害事件，保护宠物免受环境伤害
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageGeneral(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        
        // 使用EntityUtils检查是否是宠物
        Player owner = EntityUtils.getPetOwner(entity);
        if (owner == null) return;
        
        // 检查伤害类型，保护宠物免受特定类型的伤害
        if (isSpecialDamage(event.getCause())) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("pet.prevent-special-damage", 
                    "damage_type", event.getCause().toString(),
                    "pet_type", entity.getType().toString(),
                    "owner", owner.getName());
            }
            event.setCancelled(true);
            return;
        }
        
        // 如果主人与宠物在同一个世界，并且距离很近，也防止环境伤害
        if (owner.getWorld().equals(entity.getWorld()) && 
            owner.getLocation().distance(entity.getLocation()) < 20) {
            
            if (isEnvironmentalDamage(event.getCause())) {
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getMessageManager().debug("pet.prevent-environment-damage", 
                        "damage_type", event.getCause().toString(),
                        "pet_type", entity.getType().toString(),
                        "owner", owner.getName());
                }
                event.setCancelled(true);
                return;
            }
        }
    }
    
    /**
     * 处理药水效果事件，防止主人的伤害性药水效果影响宠物
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        Entity entity = event.getEntity();
        
        // 检查是否为宠物
        Player owner = EntityUtils.getPetOwner(entity);
        if (owner == null) return;
        
        // 检查是否是伤害性药水效果
        PotionEffect effect = event.getNewEffect();
        if (effect != null && isHarmfulEffect(effect.getType())) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("pet.prevent-potion-effect", 
                    "effect_type", effect.getType().getName(),
                    "pet_type", entity.getType().toString(),
                    "owner", owner.getName());
            }
            event.setCancelled(true);
        }
    }
    
    /**
     * 判断伤害类型是否属于特殊伤害
     */
    private boolean isSpecialDamage(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.VOID ||
               cause == EntityDamageEvent.DamageCause.CUSTOM ||
               cause == EntityDamageEvent.DamageCause.MAGIC ||
               cause == EntityDamageEvent.DamageCause.POISON ||
               cause == EntityDamageEvent.DamageCause.WITHER ||
               cause == EntityDamageEvent.DamageCause.DRAGON_BREATH ||
               cause == EntityDamageEvent.DamageCause.MELTING;
    }
    
    /**
     * 判断伤害类型是否属于环境伤害
     */
    private boolean isEnvironmentalDamage(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.SUFFOCATION ||
               cause == EntityDamageEvent.DamageCause.FALL ||
               cause == EntityDamageEvent.DamageCause.FIRE ||
               cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
               cause == EntityDamageEvent.DamageCause.LAVA ||
               cause == EntityDamageEvent.DamageCause.DROWNING ||
               cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
               cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
               cause == EntityDamageEvent.DamageCause.CONTACT ||
               cause == EntityDamageEvent.DamageCause.THORNS;
    }
    
    /**
     * 判断药水效果是否有害
     */
    private boolean isHarmfulEffect(PotionEffectType type) {
        return type == PotionEffectType.POISON ||
               type == PotionEffectType.WITHER ||
               type == PotionEffectType.INSTANT_DAMAGE ||
               type == PotionEffectType.SLOWNESS ||
               type == PotionEffectType.WEAKNESS ||
               type == PotionEffectType.NAUSEA ||
               type == PotionEffectType.BLINDNESS ||
               type == PotionEffectType.HUNGER ||
               type == PotionEffectType.MINING_FATIGUE ||
               type == PotionEffectType.UNLUCK;
    }
} 