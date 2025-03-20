package cn.i7mc.minepal.command.handler;

import cn.i7mc.minepal.MinePal;
import cn.i7mc.minepal.pet.control.PetManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 插件主命令处理器
 */
public class CommandHandler implements CommandExecutor, TabCompleter {
    private final MinePal plugin;
    private PetManager petManager;
    
    public CommandHandler(MinePal plugin, PetManager petManager) {
        this.plugin = plugin;
        this.petManager = petManager;
    }
    
    /**
     * 更新宠物管理器引用
     * @param petManager 宠物管理器实例
     */
    public void setPetManager(PetManager petManager) {
        this.petManager = petManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        // 检查权限
        if (!player.hasPermission("minepal.use")) {
            player.sendMessage(plugin.getMessageManager().getMessage("command.no-permission"));
            return true;
        }
        
        // 处理命令
        if (args.length == 0) {
            // 显示帮助信息
            showHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "summon":
            case "spawn":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getMessage("command.summon-usage"));
                    return true;
                }
                
                String petName = args[1];
                petManager.summonPet(player, petName);
                break;
                
            case "remove":
            case "dismiss":
                petManager.removePet(player);
                break;
                
            case "reload":
                if (!player.hasPermission("minepal.reload")) {
                    player.sendMessage(plugin.getMessageManager().getMessage("command.no-permission"));
                    return true;
                }
                
                // 重载配置文件
                plugin.getConfigManager().reloadAllConfigs();
                
                // 刷新消息缓存
                plugin.getMessageManager().refreshMessages();
                
                // 清空所有现有宠物
                if (petManager != null) {
                    petManager.removeAllPets();
                }
                
                player.sendMessage(plugin.getMessageManager().getMessage("command.reload-success"));
                break;
                
            default:
                // 默认情况下尝试召唤指定名称的宠物
                petManager.summonPet(player, subCommand);
                break;
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("minepal.use")) {
            return new ArrayList<>();
        }
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 主命令补全
            completions.addAll(Arrays.asList("summon", "spawn", "remove", "dismiss"));
            
            // 如果有reload权限，添加reload选项
            if (player.hasPermission("minepal.reload")) {
                completions.add("reload");
            }
            
            // 只返回与输入匹配的命令
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("summon") || args[0].equalsIgnoreCase("spawn"))) {
            // 宠物名称补全（这里可以获取MythicMobs中的宠物列表）
            // 为简化示例，这里返回空列表
            // 实际应该根据MythicMobs API获取可用的宠物列表
            return new ArrayList<>();
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 显示帮助信息
     * @param player 玩家
     */
    private void showHelp(Player player) {
        for (String line : plugin.getMessageManager().getMessage("command.help").split("\n")) {
            player.sendMessage(line);
        }
    }
} 