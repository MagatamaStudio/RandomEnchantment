package me.github.randomenchantment;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

public final class RandomEnchantment extends JavaPlugin implements Listener, org.bukkit.command.CommandExecutor {
    
    private static RandomEnchantment instance;
    private Logger logger;
    
    // 管理器
    private EnchantmentManager enchantmentManager;
    private ConfigManager configManager;
    
    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        
        // 保存默认配置
        saveDefaultConfig();
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // 初始化附魔管理器
        enchantmentManager = new EnchantmentManager(this);
        
        // 注册事件监听器
        Bukkit.getPluginManager().registerEvents(this, this);
        
        logger.info("RandomEnchantment插件已启用！");
    }
    
    @Override
    public void onDisable() {
        logger.info("RandomEnchantment插件已禁用！");
    }
    
    /**
     * 获取配置管理器
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * 获取附魔管理器
     */
    public EnchantmentManager getEnchantmentManager() {
        return enchantmentManager;
    }
    
    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        ItemStack originalItem = event.getItem();
        
        // 记录附魔前物品信息
        debug("玩家 " + player.getName() + " 尝试附魔物品: " + originalItem.getType() + 
              "，等级消耗: " + event.getExpLevelCost());
        
        // 检查是否强制使用随机附魔
        if (!configManager.isForceRandomEnchantment()) {
            // 如果不强制，则检查玩家是否有特殊权限可以使用原版附魔
            if (player.hasPermission("randomenchant.bypass")) {
                debug("玩家 " + player.getName() + " 拥有绕过权限，使用原版附魔");
                return;
            }
        }
        
        // 取消原版附魔
        event.setCancelled(true);
        
        // 创建物品的副本以避免修改原版物品
        ItemStack item = originalItem.clone();
        
        // 物品兼容性交给 EnchantmentManager 内部判断，
        // 这里不再按 allow-all-items-enchantment 直接拒绝所有物品，
        // 以免默认配置（false）下附魔台永远不给任何附魔。

        // 应用随机附魔
        boolean success = enchantmentManager.applyRandomEnchantments(item);
        
        if (success) {
            // 记录成功的附魔
            debug("为玩家 " + player.getName() + " 的物品 " + item.getType() + " 成功应用随机附魔");
            
            // 替换物品槽中的物品
            event.getInventory().setItem(0, item);
            
            // 消耗经验值
            int cost = event.getExpLevelCost();
            if (player.getLevel() >= cost) {
                player.setLevel(player.getLevel() - cost);
            } else {
                player.setLevel(0);
            }
            
            // 发送消息给玩家
            player.sendMessage("§a你的物品获得了随机附魔！");
        } else {
            // 记录失败原因
            debug("为物品 " + item.getType() + " 应用随机附魔失败");
            // 撤销取消，让原版附魔继续，避免"取消原版又什么都不给"
            event.setCancelled(false);
            player.sendMessage("§c无法应用随机附魔到该物品。可能该物品已拥有所有可用的附魔。");
        }
    }
    
    /**
     * 公开的方法，允许其他插件调用
     */
    public boolean applyRandomEnchantments(ItemStack item) {
        if (enchantmentManager != null) {
            return enchantmentManager.applyRandomEnchantments(item);
        }
        return false;
    }
    
    /**
     * 调试日志
     */
    private void debug(String message) {
        configManager.debug(message);
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfiguration() {
        reloadConfig();
        
        // 重新加载配置管理器
        if (configManager != null) {
            configManager.loadConfig();
        }
        
        // 清除附魔缓存
        if (enchantmentManager != null) {
            enchantmentManager.clearCache();
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // 处理命令
        if (cmd.getName().equalsIgnoreCase("randomenchant")) {
            if (args.length == 0) {
                // 显示帮助信息
                showHelpMessage(sender);
                return true;
            } else if (args[0].equalsIgnoreCase("reload")) {
                // 重新加载配置
                if (sender.hasPermission("randomenchant.reload")) {
                    reloadConfiguration();
                    sender.sendMessage("§aRandomEnchantment 配置已重新加载！");
                } else {
                    sender.sendMessage("§c你没有权限执行此命令！");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("help")) {
                // 显示帮助信息
                showHelpMessage(sender);
                return true;
            } else if (args[0].equalsIgnoreCase("info")) {
                // 显示插件信息
                sender.sendMessage("\n§6===== RandomEnchantment 插件信息 =====");
                sender.sendMessage("§a版本: " + getDescription().getVersion());
                sender.sendMessage("§a作者: " + getDescription().getAuthors().get(0));
                sender.sendMessage("§a描述: " + getDescription().getDescription());
                sender.sendMessage("\n§7随机附魔插件，允许在附魔台使用时获取随机属性。");
                return true;
            }
        }
        return false;
    }
    
    /**
     * 显示帮助信息
     */
    private void showHelpMessage(CommandSender sender) {
        sender.sendMessage("\n§6===== RandomEnchantment 插件 =====");
        sender.sendMessage("§a/randomenchant reload §7- 重新加载配置");
        sender.sendMessage("§a/randomenchant help §7- 显示帮助信息");
        sender.sendMessage("§a/randomenchant info §7- 显示插件信息");
        sender.sendMessage("\n§6由 RandomEnchantment 插件提供支持");
    }
    

    
    public static RandomEnchantment getInstance() {
        return instance;
    }
}
