# How to Create a Minecraft Mod for AI

This guide explains how to create a Minecraft mod (using NeoForge or Forge) that can interact with AI systems, and how to ensure your custom Minecraft commands work in-game.

---

## 1. Setting Up Your Modding Environment

1. **Install Java Development Kit (JDK)**
   - Use JDK 17 or the version required by your Minecraft/Forge/NeoForge version.
2. **Set Up Your IDE**
   - Recommended: IntelliJ IDEA or Eclipse.
3. **Download NeoForge/Forge MDK**
   - Get the correct version for your target Minecraft version.
4. **Extract and Open the Project**
   - Open the folder in your IDE as a Gradle project.
5. **Run `gradlew genIntellijRuns` or `gradlew eclipse`**
   - This sets up run configurations for development.

---

## 2. Creating Your Mod

1. **Rename the Example Mod**
   - Change package names and mod IDs to match your project.
2. **Edit `mods.toml` and `build.gradle`**
   - Set your mod name, version, and dependencies.
3. **Create Your Main Mod Class**
   - Annotate with `@Mod("yourmodid")`.
   - Register event listeners and commands here.
4. **Add Resources**
   - Place assets (lang, textures, etc.) in `src/main/resources`.

---

## 3. How to Make Minecraft Commands Work In-Game

### Step-by-Step Command Registration

1. **Create a Command Registration Class**
   - Example: `AutoLearnCommand.java`
2. **Register Your Command**
   - Use the appropriate event (e.g., `RegisterCommandsEvent` for Forge/NeoForge).
   - Example:
     ```java
     @SubscribeEvent
     public static void onRegisterCommands(RegisterCommandsEvent event) {
         CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
         dispatcher.register(Commands.literal("your_command")
             .executes(ctx -> { /* command logic */ return 1; })
         );
     }
     ```
3. **Register the Event Handler**
   - In your main mod class constructor:
     ```java
     MinecraftForge.EVENT_BUS.register(new YourCommandClass());
     ```
   - For NeoForge, use `NeoForge.EVENT_BUS.register(...)`.
4. **Rebuild and Run Minecraft**
   - Use `./gradlew build` and copy the JAR to your mods folder.
   - Start Minecraft with the correct mod loader.

### Troubleshooting Commands

- **Command Not Found?**
  - Make sure your command registration method is called (add log output).
  - Check that your mod is loaded (see logs or use `/mods`).
  - Ensure you are using the correct command syntax and mod ID.
- **Permissions Issues?**
  - By default, commands may require OP level. For testing, remove permission requirements.
- **Command Logic Not Running?**
  - Add debug log output in your command handler to verify execution.
- **Reloading Commands**
  - Restart the world/server after changing command code.

### Example: Registering a Simple Command

```java
public class ExampleCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("helloai")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("Hello, AI!"), false);
                    return 1;
                })
        );
    }
}
```

---

## 4. Tips for AI Integration

- Use reflection to interact with other mods (like MineColonies) if you want compatibility without direct dependencies.
- Add configuration options for AI features (e.g., auto-learning, free resources).
- Use extensive logging for debugging AI-driven features.

---

## 5. Useful Resources

- [NeoForge Documentation](https://docs.neoforged.net/)
- [Forge Documentation](https://docs.minecraftforge.net/)
- [Minecraft Modding Wiki](https://mcforge.readthedocs.io/)
- [MineColonies Source Code](https://github.com/ldtteam/MineColonies)

---

Happy modding! If your command still doesn't work, check your logs for errors and ensure your mod is loaded and event handlers are registered. 