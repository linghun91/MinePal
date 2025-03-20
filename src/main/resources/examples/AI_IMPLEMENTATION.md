# MinePal - AI实现状态说明

## 当前AI实现状态

在示例配置`PetHusk.yml`中，我们使用了MythicMobs内置的AI系统来控制宠物行为。目前的实现主要依赖MythicMobs提供的原生AI行为和目标选择器，而不是我们自己实现的自定义AI。

### MythicMobs内置AI (已使用)

配置文件中使用的AI选择器都是MythicMobs内置的，包括：
- `followowner` - 跟随主人的行为选择器
- `ownertarget` - 攻击主人目标的目标选择器
- `damageowner` - 攻击伤害主人实体的目标选择器

这些选择器由MythicMobs内部实现，我们只是通过配置使用它们。

### 我们的AI集成代码

目前，我们的代码主要实现了与MythicMobs的集成，而不是自定义AI行为：

1. **PetAIManager接口** - 定义了AI管理的方法
2. **MythicMobsPetAIManager实现** - 提供了与MythicMobs集成的具体实现

在`MythicMobsPetAIManager.java`中：
- `applyAI`方法 - 设置宠物的主人，使MythicMobs的内置AI能够正确识别主人
- `registerPathfindingGoal`和`registerPathfinderAdapter`方法 - 为自定义AI行为预留的接口，但目前尚未实现具体的自定义AI

## 待实现的功能

1. **自定义AI行为** - 目前我们没有自己实现专门的AI行为类
   - 需要创建`cn.i7mc.minepal.ai.behavior`包下的自定义行为类
   - 需要在`registerBehaviors`方法中注册这些行为

2. **自定义AI目标** - 目前我们没有自己实现专门的AI目标选择器
   - 需要创建`cn.i7mc.minepal.ai.target`包下的自定义目标选择器类
   - 需要在`registerTargets`方法中注册这些目标选择器

## 兼容性说明

虽然我们还没有实现自己的AI行为，但当前的实现方式已经足够满足基本需求：
1. 利用MythicMobs成熟的AI系统
2. 通过配置文件定义宠物行为
3. 通过`/mp`命令召唤符合要求的宠物

对于大多数用户场景，这种实现已经足够使用。后续可以根据需要扩展自定义AI行为，以支持更复杂的宠物行为模式。 