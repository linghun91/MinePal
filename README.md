# MinePal 宠物插件开发文档

## 项目概述
MinePal是一款基于Spigot 1.21和MythicMobs的宠物插件，通过整合MythicMobs的AI系统并添加自定义扩展来实现宠物行为控制。

## API文档链接
- Spigot API文档: https://hub.spigotmc.org/javadocs/spigot/
- MythicMobs本地API路径: D:\aicore\MinePal\libs\MythicMobsAPI\io\lumine\mythic

## 核心功能规划

### 1. AI系统扩展
- ✅ 注册自定义AI行为管理器
- ✅ 注册自定义AI目标管理器
- ✅ 与MythicMobs的AI系统集成（主要使用MythicMobs内置AI，添加部分自定义扩展）
- ✅ 自定义视线追踪系统

### 2. 宠物控制
- ✅ 基于MythicMobs的怪物配置系统
- ✅ 宠物行为逻辑（主要通过MythicMobs配置实现）
- ✅ 宠物基础状态管理
- ✅ 宠物保护系统
- ✅ 宠物生命周期管理
- ⬜ 宠物数据持久化

### 3. 命令系统
- ✅ `/mp <宠物模板名>` - 召唤指定配置的宠物
- ✅ 命令权限管理
- ✅ 命令参数验证


## 开发注意事项

### 1. API使用规范
- 严格遵循Spigot API文档规范
- 正确使用MythicMobs本地API
- 避免直接修改MythicMobs核心功能

### 2. 性能优化
- 使用事件监听器优化互动逻辑
- 优化AI行为计算
- 合理使用缓存机制

### 3. 兼容性
- 确保与MythicMobs最新版本兼容
- 支持多种服务器版本
- 预留扩展接口

### 4. 安全性
- 权限系统完善
- 数据验证严格
- 防止恶意输入

## 开发计划

### 第一阶段: 基础框架(已完成)
- ✅ 搭建项目结构
- ✅ 实现配置系统
- ✅ 注册AI管理器

### 第二阶段: 核心功能(已完成)
- ✅ 实现宠物召唤
- ✅ 开发AI行为系统（整合MythicMobs内置AI和部分自定义扩展）
- ✅ 实现宠物与主人互动保护

### 第三阶段: 优化完善(进行中)
- ✅ 性能优化
- ✅ 宠物保护系统
- ⬜ 宠物数据持久化（计划使用YAML或SQLite存储宠物UUID和属性数据）
- ⬜ 功能测试（计划测试多种环境下的稳定性和性能）
- ⬜ 文档完善

## 项目进度

### 当前状态
- ✅ 基础框架已完成
- ✅ 与MythicMobs API集成
- ✅ 宠物命令系统实现
- ✅ 宠物管理器实现
- ✅ 示例宠物配置文件
- ✅ 自定义AI行为部分实现（但主要使用MythicMobs内置AI）
- ✅ 调试信息优化
- ✅ 主人-宠物保护系统实现
- ✅ 宠物目标选择优化（主要通过MythicMobs配置实现）
- ✅ 主人下线宠物自动移除
- ✅ 配置文件重载及宠物清理
- ✅ 宠物保护系统完善
  - ✅ 防止主人直接攻击宠物
  - ✅ 防止宠物攻击主人
  - ✅ 防止主人AOE技能伤害宠物
  - ✅ 防止主人药水效果影响宠物
  - ✅ 防止环境伤害影响宠物
  - ✅ 防止特殊伤害影响宠物
- ✅ 宠物AI系统优化
  - ✅ 修复主人战斗状态误判问题
  - ✅ 优化目标选择逻辑
  - ✅ 增加目标有效性检查
  - ✅ 完善调试信息输出
  - ✅ 修复战斗状态记录机制
  - ✅ 增加实际伤害判断
  - ✅ 优化战斗状态清理机制
- ✅ 完善示例配置文件注释说明
  - ✅ 补充meleeattack(近战攻击)所有参数详细注释
  - ✅ 补充gotoowner(跟随主人)所有参数详细注释
  - ✅ 补充playertarget(玩家目标)所有参数详细注释
- ✅ 代码结构优化
  - ✅ 创建EntityUtils静态工具类，提供实体操作的统一入口
  - ✅ 创建DamageUtils工具类，统一处理伤害逻辑
  - ✅ 优化PetUtils类，增加双向查找（宠物->主人，主人->宠物）
  - ✅ 增加PetAIManager接口的setTarget方法，统一目标设置
  - ✅ 重构事件监听器，减少重复代码
- ✅ API使用规范化
  - ✅ 确保正确使用MythicMobs API
  - ✅ 正确使用Bukkit的实体接口，如Tameable、Sittable等
  - ✅ 优化事件处理和错误捕获机制
- ⬜ 命令处理系统优化
  - ⬜ 优化命令注册和处理逻辑
  - ⬜ 移除重复的命令处理代码
  - ⬜ 添加命令权限控制
- ⬜ 配置系统增强
  - ⬜ 增加配置验证
  - ⬜ 添加默认配置选项
  - ⬜ 优化配置重载逻辑
- ⬜ 宠物持久化数据未实现
  - ⬜ 计划使用YAML文件存储基础数据
  - ⬜ 考虑添加SQLite支持大型服务器
  - ⬜ 数据模型将包含宠物UUID、类型、等级和自定义属性

### 最新变更
- [2025-03-05] 完成代码结构优化和API使用规范化
  - 完成EntityUtils和DamageUtils工具类
  - 重构PetProtectionListener和EntityDamageListener
  - 优化PetUtils类，增加双向查找功能
  - 增加PetAIManager接口的setTarget方法
  - 确保正确使用MythicMobs和Bukkit API
- [2025-03-05] 完善宠物配置示例文档
  - 为meleeattack(近战攻击)路径寻路器添加全部参数注释说明
  - 为gotoowner(跟随主人)路径寻路器添加全部参数注释说明
  - 为playertarget(玩家目标)目标选择器添加全部参数注释说明
  - 优化示例配置文件结构，提升可读性
- [2025-03-05] 优化宠物AI系统
  - 修复主人战斗状态误判问题
  - 优化目标选择逻辑,避免误判目标
  - 增加目标有效性检查,确保目标合法
  - 完善调试信息输出,增加UUID追踪
  - 优化主人战斗状态存储机制
  - 增加实际伤害判断,避免空攻击记录
  - 优化战斗状态清理机制,及时清理无效数据
- [2025-03-05] 完善宠物保护系统
  - 增加对主人AOE技能伤害的保护
  - 增加对主人药水效果的保护
  - 增加对更多环境伤害的保护
  - 优化调试信息输出
- [2025-03-05] 修复PotionEffectType常量名称错误
- [2025-03-05] 更新配置文件结构
  - 添加新的调试消息
  - 添加用户提示消息

### 文件结构
```
MinePal/
├── src/
│   └── main/
│       ├── java/
│       │   └── cn/
│       │       └── i7mc/
│       │           └── minepal/
│       │               ├── ai/
│       │               │   ├── behavior/
│       │               │   │   └── FollowOwnerGoal.java        # 实现宠物跟随主人的AI行为
│       │               │   ├── target/
│       │               │   │   ├── DamageOwnerGoal.java        # 实现宠物攻击伤害主人实体的AI行为
│       │               │   │   └── OwnerTargetGoal.java        # 实现宠物攻击主人目标的AI行为
│       │               │   └── manager/
│       │               │       ├── PetAIManager.java           # AI管理接口，定义AI操作方法
│       │               │       └── MythicMobsPetAIManager.java # MythicMobs AI管理器实现
│       │               ├── command/
│       │               │   └── handler/
│       │               │       └── CommandHandler.java         # 命令处理器，处理插件命令
│       │               ├── listeners/
│       │               │   ├── EntityDamageListener.java       # 实体伤害监听器，处理宠物伤害事件
│       │               │   ├── OwnerCombatListener.java        # 主人战斗状态监听器，记录战斗信息
│       │               │   ├── PetLifecycleListener.java       # 宠物生命周期监听器，处理玩家离线等事件
│       │               │   └── PetProtectionListener.java      # 宠物保护监听器，防止宠物与主人互相伤害
│       │               ├── pet/
│       │               │   └── control/
│       │               │       └── PetManager.java             # 宠物管理器，处理宠物生命周期和状态
│       │               ├── utils/
│       │               │   ├── ConfigManager.java              # 配置管理工具，处理配置文件加载和保存
│       │               │   ├── DamageUtils.java                # 伤害处理工具类，处理各种伤害类型
│       │               │   ├── EntityUtils.java                # 实体工具类，提供实体操作的通用方法
│       │               │   ├── MessageManager.java             # 消息管理工具，处理消息展示和国际化
│       │               │   └── PetUtils.java                   # 宠物相关工具方法
│       │               ├── change.md                           # 项目改进计划文档
│       │               └── MinePal.java                        # 插件主类，负责初始化和管理插件生命周期
│       └── resources/
│           ├── config.yml                                      # 插件主配置文件
│           ├── debugmessage.yml                                # 调试消息配置
│           ├── message.yml                                     # 插件消息配置
│           ├── plugin.yml                                      # 插件描述文件
│           ├── README_PET_CONFIG.md                            # 宠物配置说明文档
│           └── examples/
│               ├── AI_IMPLEMENTATION.md                        # AI实现说明文档
│               └── PetHusk.yml                                 # 示例尸壳宠物配置
├── libs/                                                      # 依赖库目录
│   └── MythicMobsAPI/                                          # MythicMobs API文档
├── pom.xml                                                     # Maven项目配置文件
└── README.md                                                   # 项目说明文档
```

## 版本更新计划

### v1.0.0 (基础版本)
- ✅ 基础宠物系统
- ✅ 宠物AI行为控制（主要使用MythicMobs内置AI和少量自定义扩展）
- ✅ 宠物保护机制
- ✅ 宠物生命周期管理

### v1.1.0 (增强版本)
- ⬜ 宠物数据持久化
- ⬜ 宠物等级系统
- ⬜ 宠物技能系统

### v1.2.0 (完整版本)
- ⬜ 宠物GUI管理界面
- ⬜ 宠物商店系统
- ⬜ 宠物皮肤系统

### 功能测试
- ⬜ 基础命令测试
- ⬜ 宠物生命周期测试
- ⬜ 宠物保护系统测试
- ⬜ 宠物AI行为测试

### 性能测试
- ⬜ 多玩家多宠物场景性能测试
- ⬜ 高频命令调用测试
- ⬜ 内存占用监控
