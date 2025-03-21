package cn.i7mc.minepal.listeners;

import cn.i7mc.minepal.MinePal;
import cn.i7mc.minepal.utils.EntityUtils;
import cn.i7mc.minepal.utils.PetUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * 实体伤害监听器，用于处理与实体伤害相关的事件
 */
public class EntityDamageListener implements Listener {
    private final MinePal plugin;
    private final PetUtils petUtils;

    public EntityDamageListener(MinePal plugin) {
        this.plugin = plugin;
        this.petUtils = plugin.getPetUtils();
    }

    /**
     * 处理实体伤害事件，主要用于宠物保护和战斗处理
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        Entity damager = event.getDamager();

        // 处理投射物的情况
        if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Entity) {
                damager = (Entity) shooter;
            }
        }

        // 获取受伤实体的主人（如果是宠物）
        Player damagedOwner = null;
        if (EntityUtils.isPet(damaged)) {
            damagedOwner = EntityUtils.getPetOwner(damaged);
        }

        // 获取攻击者的主人（如果是宠物）
        Player damagerOwner = null;
        if (EntityUtils.isPet(damager)) {
            damagerOwner = EntityUtils.getPetOwner(damager);
        }

        // 处理宠物与主人之间的伤害
        handlePetOwnerDamage(event, damaged, damager, damagedOwner, damagerOwner);

        // 处理同一主人的宠物之间的伤害
        handleSameOwnerPetDamage(event, damagedOwner, damagerOwner);
    }

    /**
     * 处理宠物与主人之间的伤害
     */
    private void handlePetOwnerDamage(EntityDamageByEntityEvent event, Entity damaged, Entity damager, Player damagedOwner, Player damagerOwner) {
        // 情况1：宠物被主人攻击
        if (damagedOwner != null && damager instanceof Player && damager.equals(damagedOwner)) {
            event.setCancelled(true);
            return;
        }

        // 情况2：宠物攻击主人
        if (damagerOwner != null && damaged instanceof Player && damaged.equals(damagerOwner)) {
            event.setCancelled(true);
            
            // 尝试重置宠物的目标
            EntityUtils.setTarget(damager, null);
            return;
        }
    }

    /**
     * 处理同一主人的宠物之间的伤害
     */
    private void handleSameOwnerPetDamage(EntityDamageByEntityEvent event, Player damagedOwner, Player damagerOwner) {
        // 如果攻击者和受伤者都是宠物，且拥有同一个主人
        if (damagedOwner != null && damagerOwner != null && damagedOwner.equals(damagerOwner)) {
            event.setCancelled(true);
        }
    }
} 