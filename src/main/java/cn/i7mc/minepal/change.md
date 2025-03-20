# MinePal 改进计划

为了使MinePal项目符合开发规范、提高代码质量和可维护性，需要进行以下改进。

## 规则遵守情况

| 规则 | 描述 | 已完成 | 优先级 |
| --- | --- | --- | --- |
| 1 | 使用中文回答，避免冗余操作 | ✅ | 低 |
| 2 | 避免硬编码处理，使用全局自动动态适配方法 | ✅ | 高 |
| 3 | 将项目输出的字符串放在message.yml中 | ✅ | 高 |
| 4 | 调试信息输出放在debugmessage.yml中 | ✅ | 高 |
| 5 | 为调试信息添加配置开关 | ✅ | 中 |
| 6 | 遵循统一方法调用原则，避免重复造轮子 | ✅ | 高 |
| 7 | 遵循统一优先原则处理相似方法 | ✅ | 高 |
| 8 | 创建新类前检查是否有相似类 | ✅ | 中 |
| 9 | 确保代码优美、可维护、简约、兼容性高 | ✅ | 高 |
| 10 | 深度理解后全局遵循以上规则 | ✅ | 高 |

## 详细改进计划

### 一、消息国际化

1. ✅ 硬编码消息迁移
   - 将所有硬编码的消息字符串提取到`message.yml`中
   - 添加变量替换功能
   - 提供默认消息

2. ✅ 调试信息标准化
   - 将所有调试信息提取到`debugmessage.yml`中
   - 添加`debug`配置开关
   - 标准化调试信息格式

### 二、代码结构优化

1. ✅ 创建统一工具类
   - ✅ 创建`EntityUtils`静态工具类，提供实体操作的统一入口
   - ✅ 创建`DamageUtils`工具类，统一处理伤害逻辑
   - ✅ 优化`PetUtils`类，增加双向查找（宠物->主人，主人->宠物）

2. ✅ 重构事件监听器结构
   - ✅ 重构`PetProtectionListener`，使用新的工具类
   - ✅ 优化`EntityDamageListener`，减少重复代码
   - ✅ 将所有监听器统一到`listeners`包中

3. ✅ AI行为系统优化
   - ✅ 增加`PetAIManager`接口的`setTarget`方法，统一目标设置
   - ✅ 修复`MythicMobsPetAIManager`实现，提高稳定性
   - ✅ 减少目标选择和行为判断中的冗余代码
   - ✅ 整合`cn.i7mc.minepal.manager.PetManager`到`cn.i7mc.minepal.pet.control.PetManager`

4. ⬜ 命令处理系统优化
   - 优化命令注册和处理逻辑
   - 移除重复的命令处理代码
   - 添加命令权限控制

### 三、API使用规范化

1. ✅ MythicMobs API使用检查
   - 确保正确使用MythicMobs API
   - 增强错误处理
   - 使用接口而非具体实现

2. ✅ Bukkit API改进
   - 正确使用Bukkit的实体接口，如`Tameable`、`Sittable`等
   - 优化事件处理
   - 改进权限检查

### 四、配置系统增强

1. ✅ 配置加载优化
   - ✅ 增加配置验证
   - ✅ 添加默认配置选项
   - ✅ 优化配置重载逻辑

2. ✅ 宠物配置系统
   - ✅ 修复宠物显示名称颜色问题
   - ✅ 保留MythicMobs配置中宠物原始颜色代码
   - ✅ 优化宠物名称变量替换
   - ✅ 确保配置与模板颜色代码相互兼容
   - ⬜ 提供更多配置示例
   - ⬜ 添加详细配置文档

### 五、功能扩展框架

1. ⬜ 宠物行为扩展
   - 添加更多宠物行为选项
   - 提供API以供扩展
   - 支持自定义脚本控制

2. ⬜ 宠物GUI系统
   - 优化GUI交互逻辑
   - 添加更多GUI定制选项
   - 提高GUI响应速度

## 进度跟踪

### 第一阶段：消息国际化和代码结构初步优化
- ✅ 完成硬编码消息迁移
- ✅ 完成调试信息标准化
- ✅ 创建EntityUtils和DamageUtils工具类
- ✅ 重构PetProtectionListener和EntityDamageListener
- ✅ API使用规范化检查

### 第二阶段：代码结构深度优化
- ✅ 优化AI行为系统，减少重复代码
- ✅ 增强PetUtils功能，提供双向查找
- ✅ 添加PetAIManager接口的setTarget方法
- ✅ 统一监听器位置，整合listener和listeners包
- ✅ 整合重复的PetManager类，消除冗余
- ⬜ 命令处理系统优化
- ✅ 配置系统增强

### 第三阶段：功能扩展和完善
- ⬜ 宠物行为扩展框架
- ⬜ 宠物GUI系统优化
- ⬜ 性能优化和测试
- ⬜ 文档完善

## 优化说明

1. `EntityUtils`类与`DamageUtils`类
   - 目前已完成这两个工具类的基本结构和主要方法
   - `EntityUtils`提供了统一的实体操作，支持不同类型实体的处理
   - `DamageUtils`集中处理伤害计算和防护逻辑
   - 这些改进大大减少了代码重复，提高了可维护性

2. 事件监听器重构
   - 已优化`PetProtectionListener`，使用新的工具类方法
   - 已优化`EntityDamageListener`，简化了处理逻辑
   - 所有监听器已统一到`listeners`包中
   - 这些改进提高了代码的清晰度和可维护性

3. AI系统优化
   - 已实现`setTarget`方法，确保AI目标设置的一致性
   - 修复了目标选择逻辑中的bug
   - 整合了多个PetManager类的功能，消除了重复代码
   - 这些改进增强了AI行为的稳定性和代码的一致性

4. 配置系统增强
   - 修复了宠物显示名称配置的路径问题，保持一致性
   - 增加了对`pets.name-variables-enabled`配置的检查
   - 支持`config.yml`中定义的变量替换系统
   - 提供了更灵活的变量替换机制
   - 修复了宠物显示名称颜色代码应用顺序问题
   - 优化了处理方式，确保配置中的颜色代码不会覆盖MythicMobs模板中的颜色

5. 下一步工作
   - 命令处理系统优化
   - 宠物行为扩展框架
   - 宠物GUI系统优化
   
## 最近变更记录

### 2025-04-07
- 统一监听器结构，将`EntityDamageListener`从`listener`包移动到`listeners`包
- 整合重复的`PetManager`类，将`cn.i7mc.minepal.manager.PetManager`功能合并到`cn.i7mc.minepal.pet.control.PetManager`
- 添加`updatePetTargets`和`getOwnerPets`方法到主要PetManager类

### 2025-04-08
- 修复`PetManager`类中对`EntityUtils`的错误引用方式，将实例引用改为静态工具类引用
- 移除了`PetManager`类中不必要的`entityUtils`实例变量
- 标准化`PetManager`类中所有`EntityUtils`方法的调用方式，确保使用静态方法调用
- 成功解决编译错误，项目现可正常编译和打包

### 2025-04-09
- 修复宠物显示名称配置路径不一致问题：将`settings.pet-name-format`改为`pets.display-name-format`
- 修复变量格式不一致问题：从`%owner%`改为`{owner_name}`格式
- 添加对`pets.name-variables-enabled`配置项的支持，根据配置决定是否启用变量替换
- 增强`updatePetDisplayName`方法，支持从配置中读取额外变量映射
- 添加`getVariableValue`辅助方法，根据变量名称提供对应的值
- 完善`debugmessage.yml`，添加与宠物显示名称相关的调试信息

### 2025-04-10
- 修复宠物显示名称颜色显示问题：确保配置文件中的颜色代码能正确应用
- 优化`updatePetDisplayName`方法，在变量替换完成后再应用颜色代码
- 添加对宠物原始名称颜色代码的清除，避免与配置中的颜色代码冲突
- 为未启用变量替换的情况添加默认颜色代码(&a)，保持风格一致性
- 更新调试信息，正确显示应用颜色代码后的最终名称

### 2025-04-11
- 修复名称颜色兼容性问题：允许MythicMobs模板中的颜色代码与配置中的颜色代码共存
- 重构`updatePetDisplayName`方法，分别处理主人部分和宠物名称部分
- 移除对宠物原始名称颜色代码的清除，保留MythicMobs配置中的原始颜色设置
- 只对主人部分(如"xxx的")应用配置文件中的颜色代码，不影响宠物名称原有颜色
- 为未启用变量替换的情况保留原始名称及其颜色代码 