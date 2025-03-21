package cn.i7mc.minepal.utils;

import cn.i7mc.minepal.MinePal;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 宠物相关工具类
 */
public class PetUtils {
    private static final Map<UUID, UUID> playerPets = new HashMap<>();
    private final MinePal plugin;
    
    public PetUtils(MinePal plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 获取玩家的宠物
     * @param player 玩家
     * @return 宠物实体UUID，如果没有则返回null
     */
    public UUID getPetUUID(Player player) {
        return playerPets.get(player.getUniqueId());
    }
    
    /**
     * 注册玩家的宠物
     * @param player 玩家
     * @param pet 宠物实体
     */
    public void registerPet(Player player, Entity pet) {
        // 检查玩家的宠物数量是否已达上限
        if (hasPet(player) && getMaxPetsPerPlayer() <= 1) {
            // 如果已有宠物且限制为1，先移除现有宠物
            removePetIfExists(player);
        }
        
        playerPets.put(player.getUniqueId(), pet.getUniqueId());
    }
    
    /**
     * 如果玩家已有宠物，尝试移除它
     * @param player 玩家
     */
    private void removePetIfExists(Player player) {
        if (plugin.getPetManager() != null) {
            plugin.getPetManager().removePet(player);
        }
    }
    
    /**
     * 注销玩家的宠物
     * @param player 玩家
     */
    public void unregisterPet(Player player) {
        playerPets.remove(player.getUniqueId());
    }
    
    /**
     * 计算安全的宠物召唤位置
     * @param player 玩家
     * @return 安全的召唤位置
     */
    public Location getSafeSpawnLocation(Player player) {
        Location playerLoc = player.getLocation();
        // 在玩家位置前方1格的位置召唤宠物
        return playerLoc.clone().add(playerLoc.getDirection().normalize().multiply(1.5));
    }
    
    /**
     * 检查玩家是否有宠物
     * @param player 玩家
     * @return 是否有宠物
     */
    public boolean hasPet(Player player) {
        return playerPets.containsKey(player.getUniqueId());
    }
    
    /**
     * 获取宠物数量上限
     * @return 宠物数量上限
     */
    public int getMaxPetsPerPlayer() {
        return plugin.getConfigManager().getConfig("config.yml").getInt("settings.max-pets", 1);
    }
    
    /**
     * 获取所有已注册的宠物映射
     * @return 玩家UUID到宠物UUID的映射
     */
    public Map<UUID, UUID> getAllPets() {
        // 返回一个副本，防止外部修改
        return new HashMap<>(playerPets);
    }
    
    /**
     * 清空所有宠物注册信息
     */
    public void clearAllPets() {
        playerPets.clear();
    }
    
    /**
     * 根据宠物实体查找主人
     * 
     * @param pet 宠物实体
     * @return 主人玩家，如果不是宠物或未找到则返回null
     */
    public Player getOwnerByPet(Entity pet) {
        if (pet == null) return null;
        
        // 遍历所有记录查找匹配的宠物UUID
        for (Map.Entry<UUID, UUID> entry : playerPets.entrySet()) {
            if (entry.getValue().equals(pet.getUniqueId())) {
                // 找到匹配的玩家UUID
                return plugin.getServer().getPlayer(entry.getKey());
            }
        }
        
        return null;
    }
    
    /**
     * 根据主人查找宠物实体
     * 
     * @param owner 主人玩家
     * @return 宠物实体，如果不存在则返回null
     */
    public Entity getPetByOwner(Player owner) {
        if (owner == null) return null;
        
        // 获取宠物UUID
        UUID petUUID = getPetUUID(owner);
        if (petUUID == null) return null;
        
        // 在所有世界中查找实体
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(petUUID)) {
                    return entity;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 检查一个实体是否为宠物实体
     * 
     * @param entity 要检查的实体
     * @return 是否为宠物实体
     */
    public boolean isPetEntity(Entity entity) {
        if (entity == null) return false;
        
        // 检查是否在宠物注册表中
        for (UUID petUUID : playerPets.values()) {
            if (entity.getUniqueId().equals(petUUID)) {
                return true;
            }
        }
        
        return false;
    }
} 