package cn.i7mc.minepal.utils;

import cn.i7mc.minepal.MinePal;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 伤害工具类，用于处理伤害相关逻辑
 */
public class DamageUtils {
    private final MinePal plugin;
    private final MessageManager messageManager;
    private static final Set<EntityDamageEvent.DamageCause> PROTECTED_DAMAGE_CAUSES = new HashSet<>(Arrays.asList(
            EntityDamageEvent.DamageCause.FALL,
            EntityDamageEvent.DamageCause.DROWNING,
            EntityDamageEvent.DamageCause.FIRE,
            EntityDamageEvent.DamageCause.FIRE_TICK,
            EntityDamageEvent.DamageCause.LAVA,
            EntityDamageEvent.DamageCause.POISON,
            EntityDamageEvent.DamageCause.WITHER,
            EntityDamageEvent.DamageCause.THORNS,
            EntityDamageEvent.DamageCause.DRAGON_BREATH,
            EntityDamageEvent.DamageCause.FLY_INTO_WALL,
            EntityDamageEvent.DamageCause.HOT_FLOOR,
            EntityDamageEvent.DamageCause.CRAMMING,
            EntityDamageEvent.DamageCause.DRYOUT
    ));

    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public DamageUtils(MinePal plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    /**
     * 处理宠物伤害事件
     * @param event 伤害事件
     */
    public void processPetDamageEvent(EntityDamageEvent event) {
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
        
        // 检查伤害类型是否在保护列表中
        if (PROTECTED_DAMAGE_CAUSES.contains(event.getCause())) {
            event.setCancelled(true);
            
            // 输出调试信息
            if (plugin.getConfigManager().isDebug()) {
                messageManager.debug("damage.prevent", 
                        "pet_type", entity.getType().name(),
                        "cause", event.getCause().name(),
                        "owner", owner.getName());
            }
        }
        
        // 如果是实体攻击实体的情况，需要额外处理
        if (event instanceof EntityDamageByEntityEvent) {
            handlePetDamageByEntity((EntityDamageByEntityEvent) event, owner, entity);
        }
    }

    /**
     * 处理宠物被实体伤害的情况
     * @param event 实体伤害事件
     * @param owner 宠物的主人
     * @param pet 宠物实体
     */
    private void handlePetDamageByEntity(EntityDamageByEntityEvent event, Player owner, Entity pet) {
        Entity damager = getDamageSource(event);
        
        // 如果伤害来源是宠物的主人，取消伤害
        if (damager instanceof Player && damager.getUniqueId().equals(owner.getUniqueId())) {
            event.setCancelled(true);
            
            // 发送消息给主人
            owner.sendMessage(messageManager.getMessage("damage.prevent-owner-attack"));
            
            // 输出调试信息
            if (plugin.getConfigManager().isDebug()) {
                messageManager.debug("damage.prevent-owner-attack", 
                        "owner", owner.getName(),
                        "pet", pet.getType().name());
            }
            
            return;
        }
        
        // 检查伤害来源是否是同一主人的另一个宠物
        if (EntityUtils.isPet(damager)) {
            Player damagerOwner = EntityUtils.getPetOwner(damager);
            if (damagerOwner != null && damagerOwner.getUniqueId().equals(owner.getUniqueId())) {
                event.setCancelled(true);
                
                // 输出调试信息
                if (plugin.getConfigManager().isDebug()) {
                    messageManager.debug("damage.prevent-same-owner-pet-attack", 
                            "owner", owner.getName(),
                            "pet1", pet.getType().name(),
                            "pet2", damager.getType().name());
                }
            }
        }
    }

    /**
     * 获取伤害来源实体（处理投射物等特殊情况）
     * @param event 实体伤害事件
     * @return 真正的伤害来源实体
     */
    public Entity getDamageSource(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        
        // 处理投射物
        if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Entity) {
                return (Entity) shooter;
            }
        }
        
        return damager;
    }

    /**
     * 检查药水效果是否有害
     * @param effectType 药水效果类型
     * @return 如果是有害效果返回true，否则返回false
     */
    public boolean isHarmfulEffect(PotionEffectType effectType) {
        // 列出所有被认为是有害的药水效果
        return effectType == PotionEffectType.POISON || 
               effectType == PotionEffectType.WITHER || 
               effectType == PotionEffectType.WEAKNESS || 
               effectType == PotionEffectType.HUNGER || 
               effectType == PotionEffectType.BLINDNESS;
    }

    /**
     * 防止实体被特定的目标伤害
     * @param entity 要保护的实体
     * @param protectedFrom 保护免受的实体UUID列表
     * @return 如果实体需要被保护，返回true，否则返回false
     */
    public boolean shouldPreventDamage(Entity entity, Set<UUID> protectedFrom) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        
        // 检查当前实体状态
        LivingEntity livingEntity = (LivingEntity) entity;
        if (livingEntity.getHealth() <= 0 || livingEntity.isDead()) {
            return false;
        }
        
        // 如果保护列表为空，不进行保护
        if (protectedFrom == null || protectedFrom.isEmpty()) {
            return false;
        }
        
        // 检查当前实体的UUID是否在保护列表中
        return protectedFrom.contains(entity.getUniqueId());
    }

    /**
     * 判断是否应该防止宠物受到伤害
     * @param cause 伤害原因
     * @return 如果应该防止伤害返回true，否则返回false
     */
    public boolean shouldPreventPetDamage(EntityDamageEvent.DamageCause cause) {
        return PROTECTED_DAMAGE_CAUSES.contains(cause);
    }
} 