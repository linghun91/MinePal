package cn.i7mc.minepal.listeners;

import cn.i7mc.minepal.MinePal;
import cn.i7mc.minepal.pet.control.PetManager;
import io.lumine.mythic.bukkit.BukkitAdapter;
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
import org.bukkit.event.world.WorldUnloadEvent;

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
            // 输出调试信息
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("pet.player-quit", "player_name", player.getName());
            }
            
            // 移除玩家的宠物
            petManager.removePet(player);
            
            // 输出调试信息
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("pet.player-quit-complete", "player_name", player.getName());
            }
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
        
        // 输出调试信息
        if (plugin.getConfigManager().isDebug()) {
            plugin.getMessageManager().debug("pet.death-info", 
                "pet_type", entity.getType().toString(),
                "killer", event.getEntity().getKiller() != null ? event.getEntity().getKiller().getName() : "未知",
                "owner", owner.getName(),
                "pet_location", String.format("%.2f, %.2f, %.2f", 
                    entity.getLocation().getX(), 
                    entity.getLocation().getY(), 
                    entity.getLocation().getZ()),
                "owner_location", String.format("%.2f, %.2f, %.2f", 
                    owner.getLocation().getX(), 
                    owner.getLocation().getY(), 
                    owner.getLocation().getZ()));
        }
        
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
        
        // 输出调试信息
        if (plugin.getConfigManager().isDebug()) {
            plugin.getMessageManager().debug("pet.teleport-info",
                "player", player.getName(),
                "from_location", String.format("%.2f, %.2f, %.2f", 
                    event.getFrom().getX(), 
                    event.getFrom().getY(), 
                    event.getFrom().getZ()),
                "to_location", String.format("%.2f, %.2f, %.2f", 
                    event.getTo().getX(), 
                    event.getTo().getY(), 
                    event.getTo().getZ()),
                "pet_location", String.format("%.2f, %.2f, %.2f", 
                    petEntity.getLocation().getX(), 
                    petEntity.getLocation().getY(), 
                    petEntity.getLocation().getZ()));
        }
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
        
        // 输出调试信息
        if (plugin.getConfigManager().isDebug()) {
            plugin.getMessageManager().debug("pet.world-change",
                "player", player.getName(),
                "from_world", event.getFrom().getName(),
                "to_world", player.getWorld().getName());
        }
        
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
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("pet.world-change-remount");
            }
            
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
                
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getMessageManager().debug("pet.world-change-summon",
                        "mob_type", mobType);
                }
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
        
        // 输出调试信息
        if (plugin.getConfigManager().isDebug()) {
            plugin.getMessageManager().debug("pet.owner-death", 
                "owner", player.getName(),
                "pet_uuid", plugin.getPetUtils().getPetUUID(player).toString());
        }
        
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
        
        // 输出调试信息
        if (plugin.getConfigManager().isDebug()) {
            plugin.getMessageManager().debug("pet.world-unload", "world_name", event.getWorld().getName());
        }
        
        // 检查该世界中所有可能的宠物实体并清理
        for (Entity entity : event.getWorld().getEntities()) {
            // 检查是否为宠物
            if (isPotentialPet(entity)) {
                // 输出调试信息
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getMessageManager().debug("pet.world-unload-cleanup",
                            new String[]{"%entity_type%", "%entity_uuid%", "%world%"},
                            new String[]{entity.getType().toString(), entity.getUniqueId().toString(), event.getWorld().getName()});
                }
                
                try {
                    // 尝试通过MythicMobs清理
                    Optional<ActiveMob> activeMob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId());
                    if (activeMob.isPresent()) {
                        // 检查是否有主人
                        Optl<UUID> ownerUUID = activeMob.get().getOwner();
                        if (ownerUUID.isPresent()) {
                            Player owner = plugin.getServer().getPlayer(ownerUUID.get());
                            if (owner != null) {
                                // 如果主人在线，使用标准方法移除宠物
                                petManager.removePet(owner);
                                continue;
                            }
                        }
                        
                        // 如果未能通过标准方法移除，则直接移除实体
                        if (petManager.getAIManager() != null) {
                            petManager.getAIManager().removeAI(activeMob.get());
                        }
                        activeMob.get().remove();
                    } else {
                        // 直接移除实体
                        entity.remove();
                    }
                } catch (Exception e) {
                    // 出错时直接移除实体
                    entity.remove();
                    
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getLogger().warning("清理世界卸载时的宠物实体时出错: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * 判断一个实体是否可能是宠物
     * @param entity 要检查的实体
     * @return 是否可能是宠物
     */
    private boolean isPotentialPet(Entity entity) {
        if (entity == null) return false;
        
        // 检查实体的元数据
        if (entity.hasMetadata("owner") || entity.hasMetadata("pet")) {
            return true;
        }
        
        // 检查是否在PetUtils中注册过
        if (plugin.getPetUtils().isPetEntity(entity)) {
            return true;
        }
        
        // 检查实体类型是否通常作为宠物
        if (entity instanceof org.bukkit.entity.Tameable) {
            org.bukkit.entity.Tameable tameable = (org.bukkit.entity.Tameable) entity;
            if (tameable.isTamed()) {
                return true;
            }
        }
        
        // 检查MythicMobs标记
        try {
            ActiveMob activeMob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId()).orElse(null);
            if (activeMob != null) {
                // 检查MythicMobs标记或名称是否表明这是一个宠物
                String mobType = activeMob.getType().getInternalName();
                Optl<UUID> owner = activeMob.getOwner();
                
                if (owner.isPresent()) {
                    return true;
                }
                
                return mobType != null && (mobType.toLowerCase().contains("pet") || 
                        (entity.getCustomName() != null && entity.getCustomName().contains("的")));
            }
        } catch (Exception ignored) {
            // 如果获取MythicMobs实体失败，忽略
        }
        
        return false;
    }
} 