# MinePal 宠物配置说明

## 配置文件位置
本插件使用MythicMobs提供的生物系统来创建宠物。所有宠物模板配置应当放置在：
```
plugins/MythicMobs/Mobs/Pets.yml
```
或其他MythicMobs加载的怪物配置目录中。

## 示例配置
在`examples/PetHusk.yml`中提供了一个示例配置，展示了如何创建一个会跟随主人并攻击主人目标的尸壳宠物。

## AI行为说明

### 当前支持的AI目标
配置文件中的`AITargetSelectors`部分定义了宠物会攻击哪些目标：

- `ownertarget` - 攻击主人的目标
  - 参数: `revenge=true` - 是否攻击主人的仇敌
  - 参数: `radius=20` - 检测半径
  
- `damageowner` - 攻击伤害主人的实体
  - 参数: `radius=20` - 检测半径

### 当前支持的AI行为
配置文件中的`AIGoalSelectors`部分定义了宠物的基本行为：

- `followowner` - 跟随主人的行为
  - 参数: `distance=10` - 开始跟随的距离
  - 参数: `teleport=30` - 传送到主人身边的距离阈值
  - 参数: `speed=1.0` - 跟随速度倍率

## 使用方法
1. 将配置文件放置在正确的MythicMobs目录中
2. 重载MythicMobs配置：`/mm reload`
3. 使用MinePal命令召唤宠物：`/mp PetHusk`

## 注意事项
- 宠物名称必须与配置中定义的名称一致（区分大小写）
- 更改配置后需要重载MythicMobs配置才能生效
- 可以通过`Display`属性设置宠物显示名称，支持变量如`%owner_name%` 