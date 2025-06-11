# AntryDeathLoot

A comprehensive death chest plugin for Minecraft servers that creates chests containing a player's items when they die.

## ✨ Features

- **Death Chests**: Automatically creates chests containing player items on death
- **Falling Chest Animation**: Optional animated falling chest effect
- **Hologram Display**: Shows player name and countdown timer above chests
- **Auto-Break Timer**: Chests automatically break after a configurable time
- **Instant Break**: Optional ability for players to break chests immediately
- **Version Compatible**: Works on Minecraft 1.7.10 through 1.21.5

## 🔧 Version Compatibility

This plugin is designed to work seamlessly across a wide range of Minecraft versions:

- **Minecraft 1.7.10** - Basic support (no holograms, limited sounds)
- **Minecraft 1.8.x** - Full support with legacy materials and sounds
- **Minecraft 1.9-1.12.x** - Full support with updated sound system
- **Minecraft 1.13-1.21.5** - Full support with modern material and sound system

The plugin automatically detects your server version and adapts accordingly:
- **Legacy versions (1.8-1.12)**: Uses legacy sound names and falling block methods
- **Modern versions (1.13+)**: Uses updated material system and block data

### Version-Specific Features

| Feature | 1.7.10 | 1.8-1.12 | 1.13-1.21.5 |
|---------|--------|----------|-------------|
| Death Chests | ✅ | ✅ | ✅ |
| Falling Chest Animation | ✅ | ✅ | ✅ |
| Hologram Display | ❌ | ✅ | ✅ |
| Auto-Break Timer | ✅ | ✅ | ✅ |
| Sound Effects | Limited | Legacy sounds | Modern sounds |
| Block Data | Material + byte | Material + byte | BlockData |

**1.7.10 Limitations:**
- No hologram support (ArmorStands were added in 1.8)
- Very limited sound effects (fallback to basic sounds like CLICK)
- Basic falling chest animation support

## 📋 Requirements

- **Bukkit/Spigot/Paper** server
- **Java 8+** (for building and compatibility)
- **ProtocolLib** (dependency - version 4.8.0+ for 1.7.10 support)
- **Minecraft 1.7.10 - 1.21.5**

**Note**: The plugin is built with Java 8 for maximum compatibility with older Minecraft versions, including 1.7.10.

## 🚀 Installation

1. Download the latest release
2. Install **ProtocolLib** on your server
3. Place the plugin JAR in your `plugins` folder
4. Restart your server
5. Configure the plugin in `plugins/AntryDeathLoot/config.yml`

## ⚙️ Configuration

```yaml
# Plugin prefix used in all messages
prefix: "&f&l[&3&lAntryDeathLoot&f&l] "

# Time before chest breaks (seconds)
chest-break-time: 10

# Allow instant breaking
allow-instant-break: true

# Falling chest animation
falling-chest:
  enabled: true
  height: 20

# Hologram settings
hologram:
  enabled: true
  height: 1.0
  line-spacing: 0.3
  first-line: "&7%player%'s &fLoot"
  second-line: "&fTime remaining: &c%seconds%s"
```

## 🔧 Technical Details

### Version Detection
The plugin automatically detects your Minecraft version and adjusts:
- **Sound System**: Automatically uses appropriate sound names for your version
- **Material System**: Handles both legacy and modern material systems
- **Falling Blocks**: Uses version-appropriate spawning methods

### Compatibility Layer
- Uses reflection-free detection for maximum stability
- Graceful fallbacks for unsupported features
- Comprehensive error handling for edge cases

## 🏗️ Building

1. Clone the repository
2. Run `mvn clean package`
3. Find the compiled JAR in the `target` folder

### Version Compatibility Notes

The plugin is built with Java 8 and targets compatibility across the entire range:
- **Minecraft 1.7.10-1.16**: Fully supported with Java 8
- **Minecraft 1.17-1.20**: Supported (requires Java 17+ on server, but plugin works)
- **Minecraft 1.21-1.21.5**: Supported (requires Java 21+ on server, but plugin works)

## 📝 License

This project is licensed under the MIT License.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 🐛 Known Issues

- None currently known. Please report any issues on GitHub.

## 📞 Support

For support, please:
1. Check the configuration guide above
2. Review server logs for error messages
3. Open an issue on GitHub with detailed information 