package cn.i7mc.minepal.listeners;

import cn.i7mc.minepal.MinePal;
import cn.i7mc.minepal.pet.control.PetManager;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.utils.serialize.Optl;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * 宠物生命周期监听器，负责处理与宠物生命周期相关的事件
 * 例如玩家离线时自动移除宠物
 */
public class PetLifecycleListener implements Listener {
    private final MinePal plugin;
    private final PetManager petManager;

    public PetLifecycleListener(MinePal plugin) {
        this.plugin = plugin;
        this.petManager = plugin.getPetManager();
    }

    /**
     * 处理玩家离线事件，在玩家离线时自动移除宠物
     * @param event 玩家离线事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 检查玩家是否有宠物
        if (petManager != null && plugin.getPetUtils().hasPet(player)) {
            // 移除玩家的宠物
            petManager.removePet(player);
        }
    }
    
    /**
     * 监听宠物死亡事件
     * @param event 实体死亡事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        
        // 检查是否为MythicMobs实体
        Optional<ActiveMob> activeMob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId());
        if (!activeMob.isPresent()) return;
        
        // 检查该实体是否为玩家的宠物
        Optl<UUID> ownerUUID = activeMob.get().getOwner();
        if (!ownerUUID.isPresent()) return;
        
        Player owner = plugin.getServer().getPlayer(ownerUUID.get());
        if (owner == null) return;
        
        // 确认是宠物
        UUID petUUID = plugin.getPetUtils().getPetUUID(owner);
        if (petUUID == null || !petUUID.equals(entity.getUniqueId())) return;
        
        // 从注册表中移除宠物
        plugin.getPetUtils().unregisterPet(owner);
        
        // 告知玩家宠物已死亡
        owner.sendMessage(plugin.getMessageManager().getMessage("pet.death")
                .replace("%pet_name%", entity.getName()));
    }
    
    /**
     * 监听玩家传送事件，确保宠物能跟随传送
     * @param event 玩家传送事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // 检查玩家是否有宠物
        if (!plugin.getPetUtils().hasPet(player)) return;
        
        UUID petUUID = plugin.getPetUtils().getPetUUID(player);
        if (petUUID == null) return;
        
        // 找到宠物实体
        Entity petEntity = null;
        for (Entity entity : player.getWorld().getEntities()) {
            if (entity.getUniqueId().equals(petUUID)) {
                petEntity = entity;
                break;
            }
        }
        
        if (petEntity == null) return;
    }
    
    /**
     * 监听玩家更换世界事件，重新刷新宠物
     * @param event 玩家更换世界事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        
        // 检查玩家是否有宠物
        if (!plugin.getPetUtils().hasPet(player)) return;
        
        // 获取宠物信息，如果宠物在之前的世界，需要移除并在新世界重新召唤
        UUID petUUID = plugin.getPetUtils().getPetUUID(player);
        if (petUUID == null) return;
        
        // 查找宠物实体
        Entity petEntity = null;
        for (Entity entity : event.getFrom().getEntities()) {
            if (entity.getUniqueId().equals(petUUID)) {
                petEntity = entity;
                break;
            }
        }
        
        if (petEntity != null) {
            // 宠物在之前的世界，需要移除并在新世界重新召唤
            
            // 获取宠物信息
            ActiveMob activeMob = MythicBukkit.inst().getMobManager().getActiveMob(petUUID).orElse(null);
            if (activeMob == null) return;
            
            // 获取宠物类型
            String mobType = activeMob.getMobType();
            
            // 移除旧宠物
            petManager.removePet(player);
            
            // 延迟一点时间后在新世界召唤宠物
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                petManager.summonPet(player, mobType);
            }, 10L);
        }
    }
    
    /**
     * 监听玩家死亡事件，移除玩家的宠物
     * @param event 玩家死亡事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player == null) return;

        // 检查玩家是否有宠物
        if (!plugin.getPetUtils().hasPet(player)) return;
        
        // 移除玩家的宠物
        petManager.removePet(player);
    }
    
    /**
     * 监听世界卸载事件，确保世界卸载时清理该世界内的所有宠物
     * @param event 世界卸载事件
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldUnload(org.bukkit.event.world.WorldUnloadEvent event) {
        if (event.getWorld() == null) return;
        
        // 检查该世界中所有可能的宠物实体并清理
        for (Entity entity : event.getWorld().getEntities()) {
            // 检查是否为宠物
            if (isPotentialPet(entity)) {
                try {
                    // 获取宠物的主人
                    Player owner = plugin.getPetUtils().getOwnerByPet(entity);
                    if (owner != null) {
                        // 移除宠物的登记信息
                        plugin.getPetUtils().unregisterPet(owner);
                        
                        // 从世界中移除实体
                        entity.remove();
                    }
                } catch (Exception e) {
                    // 处理可能的错误
                }
            }
        }
    }
    
    /**
     * 检查实体是否为潜在的宠物
     * @param entity 要检查的实体
     * @return 是否可能是宠物
     */
    private boolean isPotentialPet(Entity entity) {
        // 检查是否为 MythicMob
        if (MythicBukkit.inst().getMobManager().isActiveMob(entity.getUniqueId())) {
            ActiveMob mob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId()).orElse(null);
            return mob != null && mob.getOwner().isPresent();
        }
        return false;
    }
} 