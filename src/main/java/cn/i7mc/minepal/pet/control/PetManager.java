package cn.i7mc.minepal.pet.control;

import cn.i7mc.minepal.MinePal;
import cn.i7mc.minepal.ai.manager.PetAIManager;
import cn.i7mc.minepal.utils.EntityUtils;
import cn.i7mc.minepal.utils.PetUtils;
import cn.i7mc.minepal.utils.DamageUtils;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * 宠物管理器，负责宠物的召唤、移除和管理
 */
public class PetManager {
    private final MinePal plugin;
    private PetAIManager aiManager;
    private final PetUtils petUtils;
    private final DamageUtils damageUtils;
    
    private final Map<UUID, LivingEntity> pets = new HashMap<>();
    private final Map<UUID, UUID> petOwnerMap = new HashMap<>();
    
    public PetManager(MinePal plugin, PetAIManager aiManager, PetUtils petUtils) {
        this.plugin = plugin;
        this.aiManager = aiManager;
        this.petUtils = petUtils;
        this.damageUtils = plugin.getDamageUtils();
    }
    
    /**
     * 设置AI管理器
     */
    public void setAIManager(PetAIManager aiManager) {
        this.aiManager = aiManager;
    }
    
    /**
     * 获取AI管理器
     * @return AI管理器实例
     */
    public PetAIManager getAIManager() {
        return this.aiManager;
    }
    
    /**
     * 召唤宠物
     * @param player 玩家
     * @param petName MythicMobs中配置的宠物模板名称
     * @return 是否成功召唤
     */
    public boolean summonPet(Player player, String petName) {
        // 检查MythicMobs是否存在该宠物模板
        Optional<MythicMob> mythicMob = MythicBukkit.inst().getMobManager().getMythicMob(petName);
        if (!mythicMob.isPresent()) {
            player.sendMessage(plugin.getMessageManager().getMessage("pet.summon-failed")
                    .replace("%reason%", "宠物模板不存在"));
            return false;
        }
        
        // 检查玩家是否已达到宠物数量上限
        if (petUtils.hasPet(player) && petUtils.getMaxPetsPerPlayer() <= 1) {
            // 如果配置了允许多个宠物，这里可以检查当前数量
            // 目前仅支持每个玩家一个宠物
            player.sendMessage(plugin.getMessageManager().getMessage("pet.limit-reached"));
            return false;
        }
        
        // 计算安全的召唤位置
        Location spawnLoc = petUtils.getSafeSpawnLocation(player);
        
        // 输出调试信息，添加宠物名称变量
        if (plugin.getConfigManager().isDebug()) {
            plugin.getMessageManager().debug("pet.summon-start", "pet_name", petName);
        }
        
        // 使用MythicMobs API召唤宠物
        try {
            // 使用MythicMobs API召唤生物
            ActiveMob activeMob = mythicMob.get().spawn(BukkitAdapter.adapt(spawnLoc), 1);
            
            // 获取Bukkit实体
            Entity pet = BukkitAdapter.adapt(activeMob.getEntity());
            
            // 确保宠物不会以主人为目标
            if (pet instanceof org.bukkit.entity.Mob) {
                ((org.bukkit.entity.Mob) pet).setTarget(null);
            }
            
            // 处理宠物显示名称替换变量
            updatePetDisplayName(activeMob, player, petName);
            
            // 设置宠物AI
            aiManager.applyAI(activeMob, player);
            
            // 尝试触发宠物寻找目标
            triggerPetTargeting(player);
            
            // 注册宠物到玩家
            petUtils.registerPet(player, pet);
            
            // 发送消息
            player.sendMessage(plugin.getMessageManager().getMessage("pet.summon-success")
                    .replace("%pet_name%", petName));
            
            // 输出调试信息，添加宠物名称变量
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("pet.summon-complete", "pet_name", petName);
            }
            return true;
        } catch (Exception e) {
            player.sendMessage(plugin.getMessageManager().getMessage("pet.summon-failed")
                    .replace("%reason%", e.getMessage()));
            plugin.getLogger().severe("召唤宠物失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 触发宠物目标行为
     * 寻找主人周围的敌对实体并设置为目标
     * @param owner 主人
     */
    public void triggerPetTargeting(Player owner) {
        if (owner == null || !petUtils.hasPet(owner)) {
            return;
        }
        
        UUID petUUID = petUtils.getPetUUID(owner);
        if (petUUID == null) {
            return;
        }
        
        Entity petEntity = null;
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(petUUID)) {
                    petEntity = entity;
                    break;
                }
            }
            if (petEntity != null) break;
        }
        
        if (petEntity == null) {
            return;
        }
        
        try {
            // 使用EntityUtils静态方法
            double radius = plugin.getConfigManager().getConfig("config.yml").getDouble("settings.pet-targeting-radius", 15.0);
            boolean found = EntityUtils.setOwnerEnemyAsTarget(petEntity, owner, radius);
            
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("pet.target-search", 
                    "owner", owner.getName(),
                    "pet_type", petEntity.getType().toString(),
                    "radius", String.valueOf(radius),
                    "found", String.valueOf(found));
            }
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("pet.target-selection-error", "error", e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 移除玩家的宠物
     * @param player 玩家
     * @return 是否成功移除
     */
    public boolean removePet(Player player) {
        UUID petUUID = petUtils.getPetUUID(player);
        if (petUUID == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("pet.remove-failed")
                    .replace("%reason%", "你没有宠物"));
            return false;
        }
        
        // 获取宠物实体
        Entity pet = null;
        for (Entity entity : player.getWorld().getEntities()) {
            if (entity.getUniqueId().equals(petUUID)) {
                pet = entity;
                break;
            }
        }
        
        if (pet == null) {
            // 宠物可能已经不存在，只清除记录
            petUtils.unregisterPet(player);
            player.sendMessage(plugin.getMessageManager().getMessage("pet.remove-success"));
            return true;
        }
        
        // 获取MythicMobs实体
        AbstractEntity abstractEntity = BukkitAdapter.adapt(pet);
        ActiveMob activeMob = aiManager.getMythicMob(abstractEntity);
        
        if (activeMob != null) {
            // 移除AI设置
            aiManager.removeAI(activeMob);
            
            // 使用MythicMobs API移除实体
            activeMob.remove();
        } else {
            // 如果不是MythicMobs实体，直接移除
            pet.remove();
        }
        
        // 注销宠物
        petUtils.unregisterPet(player);
        
        player.sendMessage(plugin.getMessageManager().getMessage("pet.remove-success"));
        return true;
    }
    
    /**
     * 更新玩家宠物的AI
     * @param player 玩家
     */
    public void updatePetAI(Player player) {
        UUID petUUID = petUtils.getPetUUID(player);
        if (petUUID == null) return;
        
        // 获取宠物实体
        for (Entity entity : player.getWorld().getEntities()) {
            if (entity.getUniqueId().equals(petUUID)) {
                AbstractEntity abstractEntity = BukkitAdapter.adapt(entity);
                ActiveMob activeMob = aiManager.getMythicMob(abstractEntity);
                
                if (activeMob != null) {
                    aiManager.updateAI(activeMob);
                }
                break;
            }
        }
    }
    
    /**
     * 移除所有玩家的宠物
     * 用于插件重载或服务器关闭时清空所有宠物实体
     */
    public void removeAllPets() {
        // 检查是否开启调试模式
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("[DEBUG] 开始清除所有宠物");
            // 使用标准调试消息
            plugin.getMessageManager().debug("pet.remove-all-start");
        }
        
        // 获取所有已注册的宠物UUID
        Map<UUID, UUID> allPets = petUtils.getAllPets();
        
        // 遍历所有宠物并移除
        for (Map.Entry<UUID, UUID> entry : allPets.entrySet()) {
            UUID playerUUID = entry.getKey();
            UUID petUUID = entry.getValue();
            
            // 尝试获取玩家对象
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                // 如果玩家在线，使用现有的removePet方法
                removePet(player);
            } else {
                // 如果玩家不在线，尝试直接移除宠物实体
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getMessageManager().debug("pet.remove-by-uuid", "%pet_uuid%", petUUID.toString());
                }
                removePetByUUID(petUUID);
            }
        }
        
        // 进行全局清理以确保没有残留的宠物实体
        forceCleanupAllPetEntities();
        
        // 清空宠物注册表
        petUtils.clearAllPets();
        pets.clear();
        petOwnerMap.clear();
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getMessageManager().debug("pet.clear-registry");
            plugin.getLogger().info("[DEBUG] 所有宠物已清除");
        }
    }
    
    /**
     * 通过UUID移除指定宠物，不依赖玩家对象
     * @param petUUID 宠物UUID
     */
    private void removePetByUUID(UUID petUUID) {
        if (petUUID == null) return;
        
        boolean removed = false;
        
        // 遍历所有世界查找宠物实体
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(petUUID)) {
                    // 获取MythicMobs实体
                    AbstractEntity abstractEntity = BukkitAdapter.adapt(entity);
                    ActiveMob activeMob = aiManager.getMythicMob(abstractEntity);
                    
                    if (activeMob != null) {
                        try {
                            // 移除AI设置
                            aiManager.removeAI(activeMob);
                            // 使用MythicMobs API移除实体
                            activeMob.remove();
                        } catch (Exception e) {
                            if (plugin.getConfigManager().isDebug()) {
                                plugin.getLogger().warning("移除MythicMobs宠物时出错: " + e.getMessage());
                            }
                            // 备用方案：直接移除实体
                            entity.remove();
                        }
                    } else {
                        // 如果不是MythicMobs实体，直接移除
                        entity.remove();
                    }
                    
                    removed = true;
                    break;
                }
            }
            if (removed) break;
        }
        
        // 从管理器中移除
        if (pets.containsKey(petUUID)) {
            pets.remove(petUUID);
            petOwnerMap.remove(petUUID);
        }
    }
    
    /**
     * 强制清理所有可能的宠物实体，用于服务器关闭时进行彻底清理
     * 这会检查所有实体，找出任何可能是宠物的实体并移除
     */
    private void forceCleanupAllPetEntities() {
        // 检查所有世界中的实体
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                // 检查是否为宠物的几种方式
                if (isPotentialPet(entity)) {
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getMessageManager().debug("pet.potential-pet-found", 
                                new String[]{"%entity_type%", "%entity_uuid%"}, 
                                new String[]{entity.getType().toString(), entity.getUniqueId().toString()});
                    }
                    
                    try {
                        // 尝试从MythicMobs移除
                        AbstractEntity abstractEntity = BukkitAdapter.adapt(entity);
                        ActiveMob activeMob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId()).orElse(null);
                        
                        if (activeMob != null) {
                            try {
                                // 移除AI
                                if (aiManager != null) {
                                    aiManager.removeAI(activeMob);
                                }
                                // 移除实体
                                activeMob.remove();
                            } catch (Exception e) {
                                // 直接移除实体
                                entity.remove();
                            }
                        } else {
                            // 直接移除实体
                            entity.remove();
                        }
                        
                        if (plugin.getConfigManager().isDebug()) {
                            plugin.getMessageManager().debug("pet.force-cleanup",
                                    new String[]{"%entity_type%", "%entity_uuid%"},
                                    new String[]{entity.getType().toString(), entity.getUniqueId().toString()});
                        }
                    } catch (Exception e) {
                        if (plugin.getConfigManager().isDebug()) {
                            plugin.getLogger().warning("清理宠物实体时出错: " + e.getMessage());
                        }
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
        // 检查实体的元数据
        if (entity.hasMetadata("owner") || entity.hasMetadata("pet")) {
            return true;
        }
        
        // 检查注册表中是否存在
        if (pets.containsKey(entity.getUniqueId()) || petOwnerMap.containsKey(entity.getUniqueId())) {
            return true;
        }
        
        // 检查是否在petUtils中注册过
        if (petUtils.isPetEntity(entity)) {
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
                return mobType != null && (mobType.toLowerCase().contains("pet") || 
                        (entity.getCustomName() != null && entity.getCustomName().contains("的")));
            }
        } catch (Exception ignored) {
            // 如果获取MythicMobs实体失败，忽略
        }
        
        return false;
    }
    
    /**
     * 更新宠物的显示名称
     * @param activeMob 宠物的ActiveMob实例
     * @param owner 宠物主人
     * @param petType 宠物类型名称
     */
    public void updatePetDisplayName(ActiveMob activeMob, Player owner, String petType) {
        if (activeMob == null || owner == null) return;
        
        try {
            Entity bukkitEntity = BukkitAdapter.adapt(activeMob.getEntity());
            if (!(bukkitEntity instanceof LivingEntity)) return;
            
            LivingEntity livingEntity = (LivingEntity) bukkitEntity;
            
            // 使用EntityUtils获取宠物的显示名称（基础名称）
            String petDisplayName = EntityUtils.getPetDisplayName(livingEntity);
            
            // 判断是否启用名称变量替换功能
            boolean nameVariablesEnabled = plugin.getConfigManager().getConfig("config.yml")
                    .getBoolean("pets.name-variables-enabled", true);
            
            if (nameVariablesEnabled) {
                // 从配置中获取显示名称格式
                String displayNameFormat = plugin.getConfigManager().getConfig("config.yml")
                        .getString("pets.display-name-format", "&a{owner_name}的");
                
                // 先处理变量替换
                String ownerPart = displayNameFormat;
                
                // 替换基本变量
                ownerPart = ownerPart.replace("{owner_name}", owner.getName());
                ownerPart = ownerPart.replace("{pet_type}", petType);
                
                // 从配置中获取额外的变量映射并替换
                if (plugin.getConfigManager().getConfig("config.yml").isConfigurationSection("pets.name-variables")) {
                    for (String key : plugin.getConfigManager().getConfig("config.yml").getConfigurationSection("pets.name-variables").getKeys(false)) {
                        String value = plugin.getConfigManager().getConfig("config.yml").getString("pets.name-variables." + key, "");
                        // 获取实际值
                        String actualValue = getVariableValue(key, value, owner, petType, livingEntity);
                        ownerPart = ownerPart.replace(key, actualValue);
                    }
                }
                
                // 应用颜色代码到所有者部分
                ownerPart = ChatColor.translateAlternateColorCodes('&', ownerPart);
                
                // 拼接最终结果 - 保留宠物原始名称中的颜色代码
                String finalDisplayName = ownerPart + petDisplayName;
                
                // 设置宠物显示名称
                livingEntity.setCustomName(finalDisplayName);
                livingEntity.setCustomNameVisible(true);
                
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getMessageManager().debug("pet.display-name-update", 
                        "original_name", petDisplayName,
                        "display_format", displayNameFormat,
                        "new_name", finalDisplayName,
                        "owner", owner.getName(),
                        "pet_type", petType);
                }
            } else {
                // 如果未启用变量替换，保留原始名称及其颜色代码
                livingEntity.setCustomName(petDisplayName);
                livingEntity.setCustomNameVisible(true);
                
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getMessageManager().debug("pet.display-name-update", 
                        "original_name", petDisplayName,
                        "new_name", petDisplayName,
                        "owner", owner.getName(),
                        "pet_type", petType);
                }
            }
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getMessageManager().debug("pet.display-name-error", "error", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取变量的实际值
     * @param key 变量键
     * @param defaultValue 默认值
     * @param owner 宠物主人
     * @param petType 宠物类型
     * @param pet 宠物实体
     * @return 变量的实际值
     */
    private String getVariableValue(String key, String defaultValue, Player owner, String petType, LivingEntity pet) {
        switch (key) {
            case "{owner_name}":
                return owner.getName();
            case "{pet_type}":
                return petType;
            case "{pet_health}":
                return String.format("%.1f", pet.getHealth());
            case "{pet_max_health}":
                return String.format("%.1f", pet.getMaxHealth());
            case "{owner_level}":
                return String.valueOf(owner.getLevel());
            default:
                return defaultValue;
        }
    }

    /**
     * 获取所有宠物
     */
    public Map<UUID, LivingEntity> getPets() {
        return pets;
    }

    /**
     * 获取宠物主人
     */
    public Player getPetOwner(LivingEntity pet) {
        if (pet == null) return null;
        UUID ownerUUID = petOwnerMap.get(pet.getUniqueId());
        if (ownerUUID == null) return null;
        return org.bukkit.Bukkit.getPlayer(ownerUUID);
    }

    /**
     * 添加宠物
     */
    public void addPet(LivingEntity pet, Player owner) {
        if (pet == null || owner == null) return;
        pets.put(pet.getUniqueId(), pet);
        petOwnerMap.put(pet.getUniqueId(), owner.getUniqueId());
    }

    /**
     * 移除宠物
     */
    public void removePet(LivingEntity pet) {
        if (pet == null) return;
        pets.remove(pet.getUniqueId());
        petOwnerMap.remove(pet.getUniqueId());
    }

    /**
     * 更新宠物的目标为攻击主人的实体
     * @param ownerUUID 主人UUID
     * @param targetUUID 目标UUID
     */
    public void updatePetTarget(UUID ownerUUID, UUID targetUUID) {
        if (ownerUUID == null || targetUUID == null) {
            return;
        }
        
        // 获取目标实体
        Entity target = Bukkit.getEntity(targetUUID);
        if (target == null || !target.isValid() || target.isDead()) {
            return;
        }
        
        if (!(target instanceof LivingEntity)) {
            return;
        }
        
        // 获取主人的宠物
        for (Map.Entry<UUID, LivingEntity> entry : pets.entrySet()) {
            LivingEntity pet = entry.getValue();
            
            // 检查宠物是否属于该主人
            UUID petOwnerUUID = petOwnerMap.get(pet.getUniqueId());
            if (petOwnerUUID == null || !petOwnerUUID.equals(ownerUUID)) {
                continue;
            }
            
            // 设置目标
            if (pet instanceof org.bukkit.entity.Mob) {
                org.bukkit.entity.Mob mobPet = (org.bukkit.entity.Mob) pet;
                mobPet.setTarget((LivingEntity) target);
                
                // 输出调试信息
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("[MinePal] 宠物[" + pet.getUniqueId() + "]目标更新: " + target.getType());
                }
            }
        }
    }

    /**
     * 检查玩家是否已召唤宠物
     *
     * @param player 玩家
     * @return 是否已召唤宠物
     */
    public boolean hasSummonedPet(Player player) {
        if (player == null) return false;
        return petUtils.hasPet(player);
    }
    
    /**
     * 触发宠物保护主人的行为
     *
     * @param owner 主人
     * @param attacker 攻击者
     */
    public void triggerPetProtection(Player owner, Entity attacker) {
        if (owner == null || attacker == null || !hasSummonedPet(owner)) return;
        
        // 获取玩家的宠物UUID
        UUID petUuid = petUtils.getPetUUID(owner);
        if (petUuid == null) return;
        
        // 获取宠物实体
        Entity pet = petUtils.getPetByOwner(owner);
        if (pet == null) return;
        
        // 设置攻击目标
        if (plugin.getConfigManager().getConfig("config.yml").getBoolean("pet.auto-protect-owner", true)) {
            // 设置攻击者为宠物的目标
            if (plugin.getAIManager() != null) {
                plugin.getAIManager().setTarget(petUuid, attacker);
                
                // 输出调试信息
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getMessageManager().debug("pet.set-target-protection", 
                        "owner", owner.getName(),
                        "pet_id", pet.getType().toString(),
                        "target", attacker.getType().toString());
                }
            }
        }
    }
    
    /**
     * 批量更新多个宠物的目标
     * @param ownerUUID 主人UUID
     * @param targetUUID 目标UUID
     */
    public void updatePetTargets(UUID ownerUUID, UUID targetUUID) {
        // 获取主人的所有宠物
        List<Entity> pets = getOwnerPets(ownerUUID);
        if (pets.isEmpty()) {
            return;
        }
        
        // 获取目标实体
        Entity target = Bukkit.getEntity(targetUUID);
        if (target == null || !target.isValid() || target.isDead()) {
            return;
        }
        
        // 更新每个宠物的目标
        for (Entity pet : pets) {
            if (pet instanceof LivingEntity && plugin.getAIManager() != null) {
                plugin.getAIManager().setTarget(pet.getUniqueId(), target);
                
                // 输出调试信息
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getMessageManager().debug("pet.update-pet-target", 
                        "pet_uuid", pet.getUniqueId().toString(),
                        "target_type", target.getType().toString());
                }
            }
        }
    }

    /**
     * 获取主人的所有宠物
     * @param ownerUUID 主人UUID
     * @return 宠物列表
     */
    public List<Entity> getOwnerPets(UUID ownerUUID) {
        List<Entity> pets = new ArrayList<>();
        // 首先检查通过PetUtils获取的主要宠物
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.hasMetadata("owner")) {
                    String ownerUUIDStr = entity.getMetadata("owner").get(0).asString();
                    if (ownerUUIDStr.equals(ownerUUID.toString())) {
                        pets.add(entity);
                    }
                }
            }
        }
        return pets;
    }
} 