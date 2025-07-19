# AMOTD - 高级MOTD插件

[English](README.md)

## 简介

AMOTD (Advanced MOTD) 是一个功能丰富的Minecraft服务器MOTD自定义插件，支持传统颜色代码和现代MiniMessage格式。通过AMOTD，您可以创建具有渐变色、彩虹色和多种高级格式的服务器介绍文本，自定义玩家列表显示，以及设置自定义服务器图标。

主要特点：
- 支持传统颜色代码(&a, &b等)和MiniMessage格式
- 渐变色和彩虹色文本支持
- 自定义玩家数量显示
- 自定义玩家列表悬停文本
- 多服务器图标随机切换
- 支持从在线样式库获取预设样式

## 版本

- 支持的Minecraft版本：1.8.x - 1.21.x
- 当前插件版本：1.1.0

## 兼容的服务端

- Bukkit/Spigot/Paper - 完整支持，推荐使用Paper以获得最佳MiniMessage支持
- Velocity - 完整支持所有功能

## 使用说明

### 在线获取MOTD样式

插件支持从官方样式库获取预设MOTD样式：

1. 访问 [MOTD样式库](https://motd.mcobs.cn/) 浏览可用的MOTD样式
2. 每个样式都有一个唯一的样式码，显示在样式卡片上
3. 在服务器中运行命令 `/amotd get <样式码>` 来应用样式
4. 样式将自动下载并应用到您的服务器，包括：
   - MOTD文本内容
   - 格式类型（传统或MiniMessage）
   - 服务器图标（如果有）

例如：
```bash
/amotd get abc123
```

应用样式后，您可以使用 `/amotd reload` 命令立即刷新服务器MOTD。

您也可以在样式库网站上自定义创建样式，然后使用样式码在您的服务器上应用。

### 配置文件

插件配置文件位于 `plugins/AMOTD/config.yml`：

```yaml
# 消息格式: "legacy" 使用传统颜色代码, "minimessage" 使用现代格式
message_format: "minimessage"

# 传统格式配置
legacy:
  line1: "&a欢迎来到我的&6Minecraft&a服务器"
  line2: "&e享受游戏!"

# MiniMessage格式配置
minimessage:
  line1: "<gradient:green:gold>欢迎来到我的Minecraft服务器</gradient>"
  line2: "<yellow>享受游戏!</yellow>"

# 玩家数量设置
player_count:
  enabled: true
  max_players: 100
  apply_limit: false

# 玩家列表悬停文本
hover_player_list:
  enabled: true
  empty_message: "目前没有玩家在线"

# 是否启用服务器图标
enable_server_icon: true

# 调试模式
debug: false
```

### 命令

插件提供以下命令：

- `/amotd reload` - 重新加载配置文件和服务器图标
- `/amotd get <样式码>` - 从在线样式库获取预设MOTD样式

### 权限

- `amotd.command.reload` - 允许使用reload命令
- `amotd.command.get` - 允许使用get命令获取样式

### 服务器图标

将64x64像素的PNG图片放入 `plugins/AMOTD/icons/` 文件夹中。如果有多个图片，每次服务器列表刷新时会随机选择一个显示。

## 构建方法

要从源码构建AMOTD插件，请按照以下步骤操作：

1. 克隆仓库：
   ```bash
   git clone https://github.com/x1aoren/AMOTD.git
   cd AMOTD
   ```

2. 使用Maven构建：
   ```bash
   mvn clean package
   ```

3. 构建的JAR文件将位于 `target/` 目录中

## 持续集成

本项目使用GitHub Actions进行持续集成和部署：

- **构建工作流**：对每次推送到main分支和拉取请求，自动在多个Java版本(8, 11, 17)上构建项目。
- **发布工作流**：当推送标签（例如`v1.1.0`）时，自动创建新的发布版本。

### 构建状态
![Java CI with Maven](https://github.com/x1aoren/AMOTD/workflows/Java%20CI%20with%20Maven/badge.svg)

## 项目结构

```txt
src/main/java/cn/mcobs/
├── bukkit/ # Bukkit/Spigot实现
│ ├── AMOTD.java # Bukkit插件主类
│ ├── MOTDListener.java # 服务器列表请求监听器
│ ├── AMOTDCommand.java # 命令处理器
│ └── BukkitMiniMessageHandler.java # MiniMessage解析器
├── velocity/ # Velocity实现
│ ├── AMOTDVelocity.java # Velocity插件主类
│ ├── VelocityMOTDListener.java # Ping事件监听器
│ ├── VelocityMiniMessageHandler.java # MiniMessage解析器
│ ├── VelocityCommandHandler.java # 命令处理器
│ └── VelocityConfigManager.java # 配置管理器
├── SimpleMiniMessage.java # 跨平台MiniMessage解析器
├── MessageUtil.java # 消息工具类
├── MOTDManager.java # MOTD管理器
└── AdvancedMOTDManager.java # 高级MOTD管理功能
```


## 贡献指南

我们欢迎所有形式的贡献！如果您想要贡献代码，请遵循以下步骤：

1. Fork仓库并创建您的分支
2. 编写代码，添加新功能或修复问题
3. 确保代码风格一致
4. 提交Pull Request并描述您的更改

### 报告问题

如果您发现任何问题或有新功能建议，请使用GitHub Issues系统报告。请提供尽可能详细的信息，包括：

- 问题的清晰描述
- 重现步骤
- 服务器版本和插件版本
- 相关的错误日志或截图

## 许可证

本项目采用MIT许可证 - 详情请查看LICENSE文件

---

感谢您使用AMOTD插件！如有任何问题，请在GitHub上联系我们。
