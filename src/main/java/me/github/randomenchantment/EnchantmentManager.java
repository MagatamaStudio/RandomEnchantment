package me.github.randomenchantment;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.logging.Logger;

public class EnchantmentManager {
    
    private final RandomEnchantment plugin;
    private final Logger logger;
    private final Random random;
    private final ConfigManager configManager;
    
    // 缓存附魔映射，提高性能
    private final Map<String, List<Enchantment>> enchantmentCache;
    
    public EnchantmentManager(RandomEnchantment plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.random = new Random();
        this.configManager = plugin.getConfigManager();
        this.enchantmentCache = new HashMap<>();
    }
    
    /**
     * 为物品应用随机附魔
     * @param item 要附魔的物品
     * @return 是否成功应用附魔
     */
    public boolean applyRandomEnchantments(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            logDebug("尝试附魔无效物品");
            return false;
        }
        
        // 获取物品类型键
        String itemTypeKey = getItemTypeKey(item);
        logDebug("为物品类型应用附魔: " + itemTypeKey + " (材质: " + item.getType() + ")");
        
        // 检查是否有该物品类型的配置
        if (!configManager.hasItemType(itemTypeKey) && !configManager.hasItemType("default")) {
            logDebug("未找到物品类型 " + itemTypeKey + " 的配置，也没有默认配置");
            return false;
        }
        
        // 获取可应用的附魔列表
        List<Enchantment> possibleEnchantments = getPossibleEnchantments(itemTypeKey);
        if (possibleEnchantments.isEmpty()) {
            logDebug("没有为" + itemTypeKey + "找到可用的附魔");
            return false;
        }
        
        // 获取物品当前已有的附魔
        Set<Enchantment> existingEnchantments = new HashSet<>();
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasEnchants()) {
            existingEnchantments.addAll(meta.getEnchants().keySet());
        }
        
        // 过滤掉已有的附魔（如果不允许重复）
        if (!configManager.isAllowDuplicateEnchantments()) {
            possibleEnchantments.removeAll(existingEnchantments);
            if (possibleEnchantments.isEmpty()) {
                logDebug("物品已经拥有所有可用的附魔");
                return false;
            }
        }
        
        // 确定要应用的附魔数量
        int enchantmentCount = getRandomEnchantmentCount(itemTypeKey);
        logDebug("将应用 " + enchantmentCount + " 个随机附魔");
        
        // 随机选择附魔
        List<Enchantment> selectedEnchantments = selectRandomEnchantments(
                possibleEnchantments, enchantmentCount, 
                plugin.getConfig().getBoolean("allow-duplicate-enchantments", false));
        
        // 应用选定的附魔
        for (Enchantment enchantment : selectedEnchantments) {
            // 检查兼容性和有效性
            if (isEnchantmentCompatibleWithItem(enchantment, item)) {
                int level = getRandomEnchantmentLevel(enchantment);
                item.addUnsafeEnchantment(enchantment, level); // 使用unsafe避免一些兼容性检查
                logDebug("已应用附魔: " + enchantment.getKey() + " 等级: " + level);
            } else {
                logDebug("跳过不兼容的附魔: " + enchantment.getKey() + " 对于物品: " + item.getType());
            }
        }
        
        return !selectedEnchantments.isEmpty();
    }
    
    /**
     * 根据物品类型获取配置键
     */
    public String getItemTypeKey(ItemStack item) {
        Material material = item.getType();
        String materialName = material.name();
        
        // 武器类
        if (materialName.contains("SWORD")) return "sword";
        // 工具类
        else if (materialName.contains("PICKAXE")) return "pickaxe";
        else if (materialName.contains("AXE")) return "axe";
        else if (materialName.contains("SHOVEL")) return "shovel";
        else if (materialName.contains("HOE")) return "tool";
        // 防具类
        else if (materialName.contains("HELMET")) return "helmet";
        else if (materialName.contains("CHESTPLATE")) return "chestplate";
        else if (materialName.contains("LEGGINGS")) return "leggings";
        else if (materialName.contains("BOOTS")) return "boots";
        // 远程武器
        else if (materialName.contains("BOW")) return "bow";
        else if (materialName.contains("CROSSBOW")) return "crossbow";
        else if (materialName.contains("TRIDENT")) return "tool"; // 三叉戟作为工具处理
        // 其他
        else if (materialName.contains("FISHING_ROD")) return "fishing_rod";
        else return "default";
    }
    
    /**
     * 获取指定物品类型可用的附魔列表
     */
    public List<Enchantment> getPossibleEnchantments(String itemTypeKey) {
        // 检查缓存
        if (enchantmentCache.containsKey(itemTypeKey)) {
            return new ArrayList<>(enchantmentCache.get(itemTypeKey));
        }
        
        List<Enchantment> enchantments = new ArrayList<>();
        
        // 首先尝试获取指定物品类型的附魔
        List<String> enchantmentNames = configManager.getEnchantmentsForItemType(itemTypeKey);
        
        // 如果没有找到，尝试使用默认附魔
        if (enchantmentNames.isEmpty() && !"default".equals(itemTypeKey)) {
            logDebug("未找到物品类型 " + itemTypeKey + " 的特定附魔，尝试使用默认附魔");
            enchantmentNames = configManager.getEnchantmentsForItemType("default");
        }
        
        // 转换附魔名称为附魔对象
        for (String enchantName : enchantmentNames) {
            Enchantment enchantment = getEnchantmentByName(enchantName);
            if (enchantment != null) {
                enchantments.add(enchantment);
            } else {
                logDebug("未找到附魔: " + enchantName);
            }
        }
        
        // 缓存结果
        enchantmentCache.put(itemTypeKey, enchantments);
        return new ArrayList<>(enchantments);
    }
    
    /**
     * 通过名称获取附魔
     */
    private Enchantment getEnchantmentByName(String name) {
        // 尝试直接通过键获取（转换为小写）
        Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(name.toLowerCase()));
        if (enchantment != null) {
            return enchantment;
        }
        
        // 尝试通过名称匹配（更宽松的匹配方式）
        String lowerName = name.toLowerCase();
        for (Enchantment e : Enchantment.values()) {
            if (e.getName().equalsIgnoreCase(name) || 
                e.getKey().getKey().equalsIgnoreCase(name) ||
                e.getKey().getKey().equals(lowerName)) {
                return e;
            }
        }
        
        // 记录未知附魔类型警告
        logger.warning("未知的附魔类型: " + name);
        return null;
    }
    
    /**
     * 获取随机附魔数量
     */
    public int getRandomEnchantmentCount(String itemTypeKey) {
        ConfigManager.EnchantmentRange range = configManager.getEnchantmentRange(itemTypeKey);
        if (range != null) {
            int count = range.getRandom(random);
            logDebug("为物品类型 " + itemTypeKey + " 选择附魔数量: " + count + " (范围: " + range + ")");
            return count;
        }
        
        // 默认返回1个附魔
        logDebug("未找到物品类型 " + itemTypeKey + " 的范围配置，默认返回1个附魔");
        return 1;
    }
    

    
    /**
     * 随机选择附魔列表
     */
    private List<Enchantment> selectRandomEnchantments(List<Enchantment> possible, int count, boolean allowDuplicates) {
        List<Enchantment> selected = new ArrayList<>();
        List<Enchantment> tempList = new ArrayList<>(possible);
        
        if (tempList.isEmpty()) {
            return selected;
        }
        
        for (int i = 0; i < count; i++) {
            if (tempList.isEmpty()) {
                break;
            }
            
            int index = random.nextInt(tempList.size());
            selected.add(tempList.get(index));
            
            if (!allowDuplicates) {
                tempList.remove(index);
            }
        }
        
        return selected;
    }
    
    /**
     * 获取随机附魔等级
     */
    private int getRandomEnchantmentLevel(Enchantment enchantment) {
        int maxLevel = enchantment.getMaxLevel();
        // 基础概率分布：低级附魔概率更高
        // 1级: 50%, 2级: 30%, 3级: 15%, 4级: 5% (如果存在)
        
        if (maxLevel == 1) {
            return 1;
        }
        
        double randomValue = random.nextDouble();
        
        if (maxLevel == 2) {
            return randomValue < 0.7 ? 1 : 2;
        } else if (maxLevel == 3) {
            if (randomValue < 0.5) return 1;
            else if (randomValue < 0.8) return 2;
            else return 3;
        } else if (maxLevel >= 4) {
            if (randomValue < 0.5) return 1;
            else if (randomValue < 0.8) return 2;
            else if (randomValue < 0.95) return 3;
            else return 4;
        }
        
        return 1;
    }
    
    /**
     * 检查附魔是否与物品兼容
     */
    private boolean isEnchantmentCompatibleWithItem(Enchantment enchantment, ItemStack item) {
        try {
            // 如果允许为所有物品附魔，则跳过兼容性检查
            if (configManager.isAllowAllItemsEnchantment()) {
                logDebug("允许为所有物品附魔，跳过兼容性检查: " + enchantment.getKey() + " 对于物品: " + item.getType());
                return true;
            }
            
            // 首先检查原版兼容性
            if (enchantment.canEnchantItem(item)) {
                return true;
            }
            
            // 对于某些特殊情况，可以在这里添加自定义兼容性规则
            
            return false;
        } catch (Exception e) {
            logDebug("检查附魔兼容性时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 清除附魔缓存
     */
    public void clearCache() {
        enchantmentCache.clear();
        logDebug("附魔缓存已清除");
    }
    
    /**
     * 调试日志
     */
    private void logDebug(String message) {
        configManager.debug(message);
    }
}
