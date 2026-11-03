package me.github.randomenchantment;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;

import java.util.*;
import java.util.logging.Logger;

public class ConfigManager {
    
    private final RandomEnchantment plugin;
    private final Logger logger;
    private FileConfiguration config;
    
    // 配置缓存
    private boolean debugMode;
    private boolean forceRandomEnchantment;
    private boolean allowDuplicateEnchantments;
    private boolean allowAllItemsEnchantment;
    
    // 附魔数量范围缓存
    private final Map<String, EnchantmentRange> enchantmentRanges;
    
    // 物品类型对应的附魔列表缓存
    private final Map<String, List<String>> itemEnchantmentsMap;
    
    public ConfigManager(RandomEnchantment plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.enchantmentRanges = new HashMap<>();
        this.itemEnchantmentsMap = new HashMap<>();
    }
    
    /**
     * 加载配置文件
     */
    public void loadConfig() {
        // 保存默认配置（如果不存在）
        plugin.saveDefaultConfig();
        
        // 重新加载配置
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        
        // 清空缓存
        enchantmentRanges.clear();
        itemEnchantmentsMap.clear();
        
        // 加载基本配置
        loadBasicSettings();
        
        // 加载附魔数量范围配置
        loadEnchantmentRanges();
        
        // 加载物品类型与附魔映射
        loadItemEnchantmentsMap();
        
        // 验证配置有效性
        validateConfig();
        
        if (debugMode) {
            logger.info("配置加载完成，验证通过。");
        }
    }
    
    /**
     * 加载基本设置
     */
    private void loadBasicSettings() {
        debugMode = config.getBoolean("debug-mode", false);
        forceRandomEnchantment = config.getBoolean("force-random-enchantment", true);
        allowDuplicateEnchantments = config.getBoolean("allow-duplicate-enchantments", false);
        allowAllItemsEnchantment = config.getBoolean("allow-all-items-enchantment", false);
    }
    
    /**
     * 加载附魔数量范围配置
     */
    private void loadEnchantmentRanges() {
        ConfigurationSection countSection = config.getConfigurationSection("enchantment-count");
        if (countSection == null) {
            logger.warning("配置文件中未找到 enchantment-count 部分，使用默认值。");
            // 添加默认配置
            enchantmentRanges.put("default", new EnchantmentRange(1, 3));
            return;
        }
        
        for (String key : countSection.getKeys(false)) {
            String rangeStr = countSection.getString(key);
            if (rangeStr != null) {
                try {
                    EnchantmentRange range = parseRange(rangeStr);
                    if (range != null) {
                        enchantmentRanges.put(key, range);
                        if (debugMode) {
                            logger.info("加载附魔范围: " + key + " = " + range.min + "-" + range.max);
                        }
                    }
                } catch (Exception e) {
                    logger.warning("解析附魔范围 '" + key + "' 时出错: " + e.getMessage());
                }
            }
        }
        
        // 确保默认范围存在
        if (!enchantmentRanges.containsKey("default")) {
            enchantmentRanges.put("default", new EnchantmentRange(1, 3));
            logger.info("添加默认附魔范围: 1-3");
        }
    }
    
    /**
     * 解析范围字符串
     */
    private EnchantmentRange parseRange(String rangeStr) {
        if (rangeStr.contains("-")) {
            String[] parts = rangeStr.split("-");
            if (parts.length == 2) {
                try {
                    int min = Integer.parseInt(parts[0].trim());
                    int max = Integer.parseInt(parts[1].trim());
                    
                    if (min < 0 || max < min) {
                        throw new IllegalArgumentException("无效的范围值: " + min + "-" + max);
                    }
                    
                    return new EnchantmentRange(min, max);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("无法解析数字: " + rangeStr, e);
                }
            }
        } else {
            // 尝试作为固定值
            try {
                int value = Integer.parseInt(rangeStr.trim());
                if (value < 0) {
                    throw new IllegalArgumentException("无效的范围值: " + value);
                }
                return new EnchantmentRange(value, value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("无法解析数字: " + rangeStr, e);
            }
        }
        
        return null;
    }
    
    /**
     * 加载物品类型与附魔映射
     */
    private void loadItemEnchantmentsMap() {
        // 不再硬编码物品类型列表：遍历所有以 -enchantments 结尾的键，
        // 这样 config.yml 里的 fishing-rod-enchantments（连字符）也能被正确读取
        for (String key : config.getKeys(false)) {
            if (key == null || !key.endsWith("-enchantments")) {
                continue;
            }
            String itemType = normalizeItemType(key.substring(0, key.length() - "-enchantments".length()));
            if (itemType == null || itemType.isEmpty()) {
                continue;
            }
            if (config.isList(key)) {
                List<String> enchantments = config.getStringList(key);
                if (!enchantments.isEmpty()) {
                    itemEnchantmentsMap.put(itemType, new ArrayList<>(enchantments));
                    if (debugMode) {
                        logger.info("加载物品类型附魔: " + itemType + " (" + enchantments.size() + " 个附魔)");
                    }
                }
            }
        }
        
        // 检查是否有任何物品类型配置
        if (itemEnchantmentsMap.isEmpty()) {
            logger.warning("未找到任何物品类型的附魔配置！请检查配置文件。");
        }
    }
    
    /**
     * 统一物品类型键名：去掉首尾空白、转小写、把连字符换成下划线
     */
    private static String normalizeItemType(String itemType) {
        if (itemType == null) {
            return null;
        }
        return itemType.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
    
    /**
     * 验证配置有效性
     */
    private void validateConfig() {
        boolean hasErrors = false;
        
        // 验证附魔范围
        for (Map.Entry<String, EnchantmentRange> entry : enchantmentRanges.entrySet()) {
            EnchantmentRange range = entry.getValue();
            if (range.min < 0 || range.max < range.min) {
                logger.warning("无效的附魔范围配置: " + entry.getKey() + " = " + range.min + "-" + range.max);
                hasErrors = true;
            }
        }
        
        // 验证附魔名称
        for (Map.Entry<String, List<String>> entry : itemEnchantmentsMap.entrySet()) {
            String itemType = entry.getKey();
            for (String enchantName : entry.getValue()) {
                // 先做旧版附魔 ID → 现代 key 的映射，避免误报"未知的附魔类型"
                enchantName = EnchantmentManager.legacyToKey(enchantName);
                if (enchantName == null) {
                    continue;
                }
                Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchantName.toLowerCase()));
                if (enchantment == null) {
                    // 尝试通过其他方式查找
                    boolean found = false;
                    for (Enchantment e : Enchantment.values()) {
                        if (e.getName().equalsIgnoreCase(enchantName) || 
                            e.getKey().getKey().equalsIgnoreCase(enchantName)) {
                            found = true;
                            break;
                        }
                    }
                    
                    if (!found) {
                        logger.warning("未知的附魔类型: " + enchantName + " (在 " + itemType + " 配置中)");
                    }
                }
            }
        }
        
        if (hasErrors && debugMode) {
            logger.info("配置验证完成，但发现一些警告。");
        }
    }
    
    /**
     * 获取指定物品类型的附魔数量范围
     */
    public EnchantmentRange getEnchantmentRange(String itemType) {
        EnchantmentRange range = enchantmentRanges.get(itemType);
        if (range == null) {
            // config.yml 的 enchantment-count 使用分组键名（swords/armor/tools），做别名回退
            for (String alias : getRangeAliases(itemType)) {
                range = enchantmentRanges.get(alias);
                if (range != null) {
                    break;
                }
            }
        }
        if (range == null) {
            return enchantmentRanges.get("default");
        }
        return range;
    }
    
    /**
     * 单个物品类型对应的分组键名别名
     */
    private static List<String> getRangeAliases(String itemType) {
        List<String> aliases = new ArrayList<>();
        if (itemType == null) {
            return aliases;
        }
        switch (itemType) {
            case "sword":
                aliases.add("swords");
                break;
            case "pickaxe":
            case "axe":
            case "shovel":
            case "tool":
                aliases.add("tools");
                break;
            case "helmet":
            case "chestplate":
            case "leggings":
            case "boots":
                aliases.add("armor");
                break;
            default:
                break;
        }
        return aliases;
    }
    
    /**
     * 获取指定物品类型的可用附魔列表
     */
    public List<String> getEnchantmentsForItemType(String itemType) {
        List<String> enchantments = itemEnchantmentsMap.get(normalizeItemType(itemType));
        if (enchantments == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(enchantments);
    }
    
    /**
     * 检查配置中是否包含指定的物品类型
     */
    public boolean hasItemType(String itemType) {
        return itemEnchantmentsMap.containsKey(normalizeItemType(itemType));
    }
    
    // Getters
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public boolean isForceRandomEnchantment() {
        return forceRandomEnchantment;
    }
    
    public boolean isAllowDuplicateEnchantments() {
        return allowDuplicateEnchantments;
    }
    
    /**
     * 是否允许为所有物品附魔
     */
    public boolean isAllowAllItemsEnchantment() {
        return allowAllItemsEnchantment;
    }
    
    /**
     * 附魔范围类
     */
    public static class EnchantmentRange {
        public final int min;
        public final int max;
        
        public EnchantmentRange(int min, int max) {
            this.min = min;
            this.max = max;
        }
        
        /**
         * 生成范围内的随机数
         */
        public int getRandom(Random random) {
            return min + random.nextInt(max - min + 1);
        }
        
        @Override
        public String toString() {
            return min + "-" + max;
        }
    }
    
    /**
     * 获取所有已配置的物品类型
     */
    public Set<String> getAllItemTypes() {
        return new HashSet<>(itemEnchantmentsMap.keySet());
    }
    
    /**
     * 获取配置文件实例
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * 记录调试信息
     */
    public void debug(String message) {
        if (debugMode) {
            logger.info("[调试] " + message);
        }
    }
}
