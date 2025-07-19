# AMOTD - Advanced MOTD Plugin

[中文文档](README_ZH.md)

## Introduction

AMOTD (Advanced MOTD) is a feature-rich Minecraft server MOTD customization plugin that supports both traditional color codes and modern MiniMessage format. With AMOTD, you can create server introduction texts with gradient colors, rainbow effects, and various advanced formatting options, customize player list display, and set custom server icons.

Key features:
- Support for traditional color codes (&a, &b, etc.) and MiniMessage format
- Gradient color and rainbow text support
- Custom player count display
- Custom player list hover text
- Multiple server icons with random rotation
- Support for obtaining preset styles from online style library

## Version

- Supported Minecraft versions: 1.8.x - 1.21.x
- Current plugin version: 1.1.0

## Compatible Servers

- Bukkit/Spigot/Paper - Full support, recommended to use Paper for best MiniMessage support
- Velocity - Full support for all features

## Usage Instructions

### Online MOTD Style Gallery

The plugin supports obtaining preset MOTD styles from the official style library:

1. Visit [MOTD Style Gallery](https://motd.mcobs.cn/) to browse available MOTD styles
2. Each style has a unique style code, displayed on the style card
3. Run the command `/amotd get <style code>` on your server to apply the style
4. The style will be automatically downloaded and applied to your server, including:
   - MOTD text content
   - Format type (traditional or MiniMessage)
   - Server icon (if available)

Example:
```bash
/amotd get abc123
```

After applying a style, you can use the `/amotd reload` command to immediately refresh your server's MOTD.

You can also create custom styles on the style gallery website, then apply them to your server using the style code.

### Configuration File

The plugin configuration file is located at `plugins/AMOTD/config.yml`:

```yaml
# Message format: "legacy" uses traditional color codes, "minimessage" uses modern format
message_format: "minimessage"

# Traditional format configuration
legacy:
  line1: "&aWelcome to my &6Minecraft &aserver"
  line2: "&eEnjoy your game!"

# MiniMessage format configuration
minimessage:
  line1: "<gradient:green:gold>Welcome to my Minecraft server</gradient>"
  line2: "<yellow>Enjoy your game!</yellow>"

# Player count settings
player_count:
  enabled: true
  max_players: 100
  apply_limit: false

# Player list hover text
hover_player_list:
  enabled: true
  empty_message: "No players online at the moment"

# Whether to enable server icon
enable_server_icon: true

# Debug mode
debug: false
```

### Commands

The plugin provides the following commands:

- `/amotd reload` - Reload the configuration file and server icons
- `/amotd get <style code>` - Get preset MOTD styles from the online style library

### Permissions

- `amotd.command.reload` - Allow using the reload command
- `amotd.command.get` - Allow using the get command to obtain styles

### Server Icons

Place 64x64 pixel PNG images in the `plugins/AMOTD/icons/` folder. If there are multiple images, one will be randomly selected each time the server list is refreshed.

## Build Method

To build the AMOTD plugin from source, follow these steps:

1. Clone the repository:
   ```bash
   git clone https://github.com/x1aoren/AMOTD.git
   cd AMOTD
   ```

2. Build using Maven:
   ```bash
   mvn clean package
   ```

3. The built JAR file will be in the `target/` directory

## Continuous Integration

This project uses GitHub Actions for continuous integration and deployment:

- **Build Workflow**: Automatically builds the project on multiple Java versions (8, 11, 17) for every push to main branch and pull requests.
- **Release Workflow**: Automatically creates a new release when a tag is pushed (e.g., `v1.1.0`).

### Build Status
![Java CI with Maven](https://github.com/x1aoren/AMOTD/workflows/Java%20CI%20with%20Maven/badge.svg)

## Project Structure

```txt
src/main/java/cn/mcobs/
├── bukkit/ # Bukkit/Spigot implementation
│ ├── AMOTD.java # Bukkit plugin main class
│ ├── MOTDListener.java # Server list request listener
│ ├── AMOTDCommand.java # Command processor
│ └── BukkitMiniMessageHandler.java # MiniMessage parser
├── velocity/ # Velocity implementation
│ ├── AMOTDVelocity.java # Velocity plugin main class
│ ├── VelocityMOTDListener.java # Ping event listener
│ ├── VelocityMiniMessageHandler.java # MiniMessage parser
│ ├── VelocityCommandHandler.java # Command processor
│ └── VelocityConfigManager.java # Configuration manager
├── SimpleMiniMessage.java # Cross-platform MiniMessage parser
├── MessageUtil.java # Message utility class
├── MOTDManager.java # MOTD manager
└── AdvancedMOTDManager.java # Advanced MOTD management features
```

## Contribution Guidelines

We welcome all forms of contribution! If you want to contribute code, please follow these steps:

1. Fork the repository and create your branch
2. Write code, add new features or fix issues
3. Ensure code style consistency
4. Submit a Pull Request and describe your changes

### Coding Standards

- Use standard Java code style
- Add JavaDoc comments for new methods
- All public APIs should be documented
- Use meaningful commit messages

### Reporting Issues

If you find any issues or have suggestions for new features, please use the GitHub Issues system to report them. Please provide as detailed information as possible, including:

- A clear description of the issue
- Steps to reproduce
- Server version and plugin version
- Relevant error logs or screenshots

## License

This project uses the MIT License - see the LICENSE file for details

---

Thank you for using the AMOTD plugin! If you have any questions, please contact us on GitHub.