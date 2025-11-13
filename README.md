# RandomEnchantment 插件

一个Minecraft Spigot插件，实现玩家使用附魔台附魔时随机属性的功能，且配置文件中可设置每个工具与装备可被附魔的属性与附魔数量。

# 这个插件未完成
这个插件有很多BUG,包括且不限于无法对三叉戟进行附魔(因为没有为三叉戟分配附魔)
可以自定义物品的可用附魔列表
且不能自定义附魔强度
欢迎Fork

## 功能特性

- 自定义附魔系统，替换原版附魔机制
- 支持为不同类型的物品配置不同的可用附魔列表
- 可自定义每种物品类型的附魔数量范围
- 支持设置是否允许重复附魔
- 支持玩家权限绕过，使用原版附魔系统
- 完整的命令系统，包括重载配置和查看帮助
- 详细的调试日志功能

## 安装方法

1. 确保您的服务器运行的是 Spigot 或 Paper 1.20.1 或更高版本
2. 下载 `RandomEnchantment.jar` 文件
3. 将文件放入服务器的 `plugins` 文件夹中
4. 重启服务器以加载插件
5. 编辑配置文件以自定义附魔设置

## 配置说明

插件启动后会在 `plugins/RandomEnchantment/` 目录下生成 `config.yml` 文件。以下是配置项的说明：

### 基本设置

```yaml
# 是否强制使用随机附魔机制（如果为false，则有权限的玩家可以使用原版附魔）
force-random-enchant: true

# 是否允许重复附魔（同一物品上多次添加相同的附魔）
allow-duplicate-enchantments: false

# 是否允许为所有物品附魔（包括原版不可附魔的物品）
allow-all-items-enchantment: false

# 是否启用调试模式（会输出详细的日志信息）
debug-mode: false
```

### 附魔数量配置

```yaml
# 各类型物品的附魔数量范围（格式：最小值-最大值）
enchantment-count:
  default: "1-3"       # 默认附魔数量
  sword: "1-4"         # 剑的附魔数量
  pickaxe: "1-4"       # 镐的附魔数量
  axe: "1-3"           # 斧的附魔数量
  shovel: "1-2"        # 铲子的附魔数量
  hoe: "1-3"           # 锄头的附魔数量
  bow: "1-3"           # 弓的附魔数量
  crossbow: "1-3"      # 弩的附魔数量
  fishing_rod: "1-3"   # 钓鱼竿的附魔数量
  helmet: "1-4"        # 头盔的附魔数量
  chestplate: "1-4"    # 胸甲的附魔数量
  leggings: "1-4"      # 护腿的附魔数量
  boots: "1-4"         # 靴子的附魔数量
  book: "1-1"          # 书的附魔数量
```

### 附魔配置

```yaml
# 各类型物品可获得的附魔列表（使用附魔的内部名称）
default-enchantments:
  - "DURABILITY"
  - "MENDING"

sword-enchantments:
  - "SHARPNESS"
  - "SMITE"
  - "BANE_OF_ARTHROPODS"
  - "KNOCKBACK"
  - "FIRE_ASPECT"
  - "LOOT_BONUS_MOBS"
  - "SWEEPING_EDGE"
  - "IMPALING"
  - "RIPTIDE"
  - "CHANNELING"
  - "LOYALTY"
  
pickaxe-enchantments:
  - "EFFICIENCY"
  - "SILK_TOUCH"
  - "FORTUNE"
  - "UNBREAKING"
  - "MENDING"
  
# 其他物品类型的附魔配置...
```

## 可用的附魔名称

以下是一些常用的附魔内部名称：

- **武器附魔**：SHARPNESS, SMITE, BANE_OF_ARTHROPODS, KNOCKBACK, FIRE_ASPECT, LOOT_BONUS_MOBS, SWEEPING_EDGE
- **工具附魔**：EFFICIENCY, SILK_TOUCH, FORTUNE, UNBREAKING, MENDING
- **防具附魔**：PROTECTION_ENVIRONMENTAL, PROTECTION_FIRE, PROTECTION_FALL, PROTECTION_EXPLOSIONS, PROTECTION_PROJECTILE, THORNS, DEPTH_STRIDER, FROST_WALKER, CURSE_OF_BINDING, BINDING_CURSE, VANISHING_CURSE, SOUL_SPEED
- **弓/弩附魔**：ARROW_DAMAGE, ARROW_FIRE, ARROW_KNOCKBACK, ARROW_INFINITE, QUICK_CHARGE, PIERCING, MULTISHOT
- **钓鱼竿附魔**：LUCK_OF_THE_SEA, LURE
- **三叉戟附魔**：LOYALTY, IMPALING, RIPTIDE, CHANNELING

## 命令说明

| 命令 | 描述 | 权限 |
|------|------|------|
| `/randomenchant` | 显示帮助信息 | 无需权限 |
| `/randomenchant help` | 显示详细帮助信息 | 无需权限 |
| `/randomenchant reload` | 重新加载插件配置 | `randomenchant.reload` |
| `/randomenchant info` | 显示插件信息 | 无需权限 |

## 权限说明

| 权限 | 描述 | 默认 |
|------|------|------|
| `randomenchant.bypass` | 允许玩家使用原版附魔系统 | op |
| `randomenchant.reload` | 允许重新加载插件配置 | op |

## 物品类型键

配置文件中使用的物品类型键如下：

- sword：所有剑类物品
- pickaxe：所有镐类物品
- axe：所有斧类物品
- shovel：所有铲子类物品
- hoe：所有锄头类物品
- bow：弓
- crossbow：弩
- fishing_rod：钓鱼竿
- trident：三叉戟
- helmet：所有头盔
- chestplate：所有胸甲
- leggings：所有护腿
- boots：所有靴子
- book：附魔书
- default：默认配置，应用于未指定类型的物品

## 常见问题

### 1. 如何添加新的附魔到配置中？

编辑 `config.yml` 文件，在相应的物品类型配置中添加附魔的内部名称即可。

### 2. 如何让特定玩家使用原版附魔系统？

设置 `force-random-enchant: false`，然后为该玩家分配 `randomenchant.bypass` 权限。

### 3. 插件不生效怎么办？

- 检查服务器版本是否兼容（1.20.1+）
- 检查配置文件格式是否正确
- 启用 `debug-mode: true` 查看详细日志
- 确保物品没有被禁止附魔（通过配置）

## 版本历史

### 1.0
- 初始版本发布
- 支持自定义附魔列表和数量
- 完整的权限和命令系统

## 作者

RandomEnchantment 插件由KujouShiori 开发。
由Trae CN的AI提供Debug支持。

## 许可证

本插件采用 MIT 许可证。详见 LICENSE 文件。
