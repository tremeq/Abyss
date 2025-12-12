# Abyss - Public Item Storage Plugin

A lightweight Spigot/Paper plugin that creates a shared public storage where players can dispose of unwanted items and take items left by others. Features automatic ground item collection with full support for custom items from popular plugins.

## Author
**tremeq**

## Requirements
- **Minecraft**: 1.20 - 1.21+
- **Server**: Paper / Spigot / Purpur (Paper recommended)
- **Java**: 21

## Features

### 🎯 Core Features
- ✅ **Public GUI Storage** - Shared vault accessible to all players
- ✅ **Command-based Opening** - Players open GUI using `/abyss` or `/otchlan` commands
- ✅ **Optional Auto-open** - Can be enabled in config for automatic periodic opening
- ✅ **Automatic Item Collection** - Collects items from ground and stores them in the Abyss
- ✅ **Multi-page GUI** - Automatic pagination for large quantities of items
- ✅ **Real-time Synchronization** - All players see changes instantly
- ✅ **Full Custom Item Support** - Works with ItemsAdder, Oraxen, MMOItems, and more
- ✅ **Multi-language System** - Polish and English included (easily extensible)
- ✅ **Highly Configurable** - Everything in YAML with HEX and legacy color support

### 🎨 GUI & Navigation
- Configurable GUI size (9-54 slots)
- Bottom navigation bar filled with glass panes
- Navigation buttons: previous/next page, info, close
- Each button can be individually enabled/disabled
- Custom materials, names, and positions for all buttons
- Glass filler with configurable material and name
- Default layout: arrows on sides, info and close in center

### 🌍 Item Collection
- Automatic ground item collection at configurable intervals
- **World Blacklist** - Exclude specific worlds from collection
- Player notifications when items are collected
- Full preservation of NBT and metadata for custom items
- Configurable collection interval

### 🎨 Colors & Formatting
- Legacy color codes (`&a`, `&c`, `&l`, etc.)
- HEX colors (`#FFFFFF`, `&#9B59B6`)
- Full formatting support (bold, italic, underline)
- All text fully configurable in language files

## Installation

1. Download `Abyss-1.0.0.jar` from the releases
2. Place the file in your server's `plugins/` folder
3. Start/restart your server
4. Configure settings in `plugins/Abyss/config.yml`
5. Customize messages in `plugins/Abyss/messages_pl.yml` or `messages_en.yml`

## Configuration

### Basic Configuration (config.yml)

```yaml
# Plugin language (pl / en)
language: pl

# GUI Settings
gui:
  title: '&8&lAbyss'
  size: 54  # GUI size (last row reserved for navigation)

# Auto-open Settings (disabled by default - GUI opens via commands)
auto-open:
  enabled: false  # Set to true to enable automatic opening
  interval: 300   # Interval in seconds between openings
  duration: 10    # Duration in seconds GUI stays open

# Item Collection Settings
item-collection:
  enabled: true
  interval: 60  # Interval in seconds between collection cycles
  notify-players: true
  # World blacklist - items from these worlds will NOT be collected
  world-blacklist:
    - world_nether
    - world_the_end
```

### Navigation Configuration (config.yml)

```yaml
navigation:
  # Glass pane filler for bottom bar
  glass-filler:
    enabled: true
    material: GRAY_STAINED_GLASS_PANE
    name: ' '

  # Previous page button
  previous-page:
    enabled: true
    slot: 0  # Left corner
    material: ARROW

  # Next page button
  next-page:
    enabled: true
    slot: 8  # Right corner
    material: ARROW

  # Close button
  close:
    enabled: true
    slot: 5  # Center-right
    material: BARRIER

  # Info button
  info:
    enabled: true
    slot: 3  # Center-left
    material: BOOK
```

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/abyss` | Opens the Abyss GUI | `devotchlan.use` |
| `/otchlan` | Opens the Abyss GUI (Polish) | `devotchlan.use` |
| `/abyssreload` | Reloads plugin configuration | `devotchlan.reload` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `devotchlan.use` | Allows using GUI commands | All players |
| `devotchlan.reload` | Allows reloading configuration | Operators |
| `devotchlan.*` | All permissions | Operators |

## Custom Items Support

The plugin **fully supports** all types of custom items through proper ItemStack cloning:

### ✅ Supported Plugins
- **ItemsAdder** - Full NBT, textures, models
- **Oraxen** - Weapons, armor, blocks
- **MMOItems** - Stats, abilities, upgrades
- **ExecutableItems** - Command bindings
- **Mythic Mobs** - Custom drops
- **Custom Enchants+** - All enchantments
- **EcoItems** - Full support
- **Any plugin using standard ItemStack NBT**

### ✅ Supported Item Types
- Custom items with NBT data
- Enchanted items (vanilla + custom)
- Shulker boxes with contents
- Signed books
- Player heads with textures
- Items with custom model data
- Potions with effects
- Tipped arrows
- Fireworks
- Banners with patterns
- Maps
- And more!

### How It Works
The plugin uses `ItemStack.clone()` everywhere to ensure:
- ✅ All NBT data is preserved
- ✅ All metadata is copied
- ✅ No reference issues
- ✅ Custom items retain full functionality

## Usage

### For Players

1. **Opening GUI**: Use the `/abyss` or `/otchlan` command
2. **Adding Items**:
   - Drag items from your inventory to the GUI (shift+click)
   - Click with an item on your cursor in an empty GUI slot
3. **Taking Items**: Click an item in the GUI to take it
4. **Navigation**: Use arrow buttons (on sides) to switch pages
5. **Closing**: Click the "Close" button or press ESC

**Note**: Admins can enable automatic GUI opening in the config (disabled by default)

### For Admins

1. **Change Language**: Set `language: pl` or `language: en` in config.yml
2. **Customize Messages**: Edit `messages_pl.yml` or `messages_en.yml`
3. **Change GUI Colors**: Use HEX colors (`&#FFFFFF`) or legacy codes (`&a`)
4. **World Blacklist**: Add world names to `world-blacklist` list
5. **Disable Buttons**: Set `enabled: false` for unwanted navigation buttons
6. **Reload**: Use `/abyssreload` after making changes

## HEX Color Examples

```yaml
# Purple gradient
title: '&#9B59B6&lA&#8E44AD&lb&#674EA7&ly&#6C3483&ls&#9B59B6&ls'

# Rainbow gradient
info:
  name: '&#FF0000A&#FF7F00b&#FFFF00y&#00FF00s&#0000FFs'

# Pink glass
glass-filler:
  material: PINK_STAINED_GLASS_PANE
  name: '&#FF69B4 '
```

## File Structure

```
plugins/Abyss/
├── config.yml           # Main configuration
├── messages_pl.yml      # Polish messages
└── messages_en.yml      # English messages
```

## FAQ

**Q: Do custom items keep their properties?**
A: Yes! The plugin uses `ItemStack.clone()` which preserves all NBT data and metadata.

**Q: Can I exclude specific worlds from item collection?**
A: Yes, add world names to the `world-blacklist` in config.yml.

**Q: Can I change the navigation button layout?**
A: Yes, change the `slot` value for each button (0-8 in the bottom row).

**Q: Can I disable automatic GUI opening?**
A: Yes, it's disabled by default. Set `auto-open.enabled: false` in config.yml.

**Q: How do I add a custom language?**
A: Copy `messages_pl.yml` as `messages_xx.yml`, translate it, and set `language: xx` in config.

**Q: Does it work with Minecraft 1.21?**
A: Yes, the plugin is compatible with Minecraft 1.20 - 1.21+.

**Q: What happens if a player's inventory is full?**
A: The item stays in the Abyss and the player receives a message that their inventory is full.

**Q: Are items synchronized between players in real-time?**
A: Yes, all players with the GUI open see changes instantly.

**Q: Is the plugin thread-safe?**
A: Yes, all storage operations use proper synchronization.

## Technical Details

### Thread Safety
- Uses `Collections.synchronizedList()` for storage
- All operations properly synchronized
- No race conditions or concurrent modification issues

### Performance
- Lightweight and optimized
- Minimal memory overhead
- Efficient pagination (only loads visible items)
- Configurable collection intervals to reduce lag

### Memory Management
- Proper task cleanup on disable
- No memory leaks
- Automatic cleanup of offline player data

### Security
- Input validation on all operations
- Permission checks on commands
- Event cancellation prevents exploits
- No item duplication possible

## Support

If you encounter a bug or have a suggestion:
1. Enable debug mode: `debug: true` in config.yml
2. Check server logs
3. Report the issue with detailed reproduction steps

## Changelog

### v1.0.0 (2025-12-13)
- Initial public release
- Full custom item support
- Multi-page GUI with navigation
- Multi-language system (PL/EN)
- World blacklist for item collection
- Configurable navigation bar
- HEX and legacy color support
- Automatic ground item collection
- Real-time synchronization between players

## Credits

**Author**: tremeq
**License**: All Rights Reserved

---

**Thank you for using Abyss!** 🌟

If you enjoy this plugin, consider leaving a review and sharing it with others.
