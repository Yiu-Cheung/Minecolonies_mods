# MC Mod - Auto-Fulfill Builder Requests

A Minecraft mod for NeoForge 1.21.1 that automatically fulfills builder requests in MineColonies, making colony management more convenient.

## Features

- **Auto-Fulfill Builder Requests**: Automatically provides resources to builders when they request them
- **In-Game Notifications**: Sends messages to all players when requests are auto-fulfilled
- **Configurable**: Enable/disable the auto-fulfill feature
- **Safe Integration**: Gracefully handles cases where MineColonies is not installed
- **Command System**: Easy control via in-game commands

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.186 or later
- MineColonies mod (required for functionality)

## Installation

1. Download the mod JAR file
2. Place it in your `mods` folder
3. Start your Minecraft server/client
4. The mod will automatically detect if MineColonies is installed

## Configuration

The mod includes a simple configuration option:

- **Enable Auto-Fulfill Builder Requests**: Master toggle for the auto-fulfill feature (enabled by default)

## How It Works

When the mod is loaded and MineColonies is detected:

1. The mod monitors builder requests from MineColonies
2. When a builder requests resources, the mod automatically provides them
3. Builders can continue their work without waiting for manual resource delivery
4. All actions are logged for transparency

## Commands

The mod provides commands to control the auto-fulfill feature:

```
/mc_mod auto_fulfill status    # Check if auto-fulfill is enabled
/mc_mod auto_fulfill enable    # Enable auto-fulfill
/mc_mod auto_fulfill disable   # Disable auto-fulfill
```

## Troubleshooting

### MineColonies Not Detected
If you see a message that MineColonies is not detected:
1. Ensure MineColonies mod is installed
2. Check that both mods are compatible versions
3. Restart your server/client

### Auto-Fulfill Not Working
1. Check the configuration settings with `/mc_mod auto_fulfill status`
2. Ensure the feature is enabled with `/mc_mod auto_fulfill enable`
3. Check the server logs for any error messages

## Development

This mod is built using:
- NeoForge MDK for Minecraft 1.21.1
- Java 21
- Gradle build system

### Building from Source

1. Clone the repository
2. Run `./gradlew build`
3. Find the JAR file in `build/libs/`

## License

All Rights Reserved

## Support

For issues or questions, please check the mod's issue tracker or contact the author.

## Version History

- **1.0.0**: Initial release with auto-fulfill builder requests functionality

Installation information
=======

This template repository can be directly cloned to get you started with a new
mod. Simply create a new repository cloned from this one, by following the
instructions provided by [GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template).

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/
