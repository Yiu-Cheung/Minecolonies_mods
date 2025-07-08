# MineColonies Integration Notes

Based on analysis of the MineColonies source code, here are the key findings for proper integration:

## Key Classes and Methods

### Colony Manager Access
```java
// Correct way to get colony manager
Class<?> apiClass = Class.forName("com.minecolonies.api.IMinecoloniesAPI");
Method getInstance = apiClass.getMethod("getInstance");
Object apiInstance = getInstance.invoke(null);
Method getColonyManager = apiInstance.getClass().getMethod("getColonyManager");
Object colonyManager = getColonyManager.invoke(apiInstance);
```

### Getting Colonies
```java
// Get all colonies
Method getAllColonies = colonyManager.getClass().getMethod("getAllColonies");
Object result = getAllColonies.invoke(colonyManager);
// Returns Set<IColony> or List<IColony>
```

### Getting Buildings from Colony
```java
// Correct approach based on source code:
// 1. Get building manager from colony
Method getBuildingManager = colony.getClass().getMethod("getBuildingManager");
Object buildingManager = getBuildingManager.invoke(colony);

// 2. Get buildings from building manager
Method getBuildings = buildingManager.getClass().getMethod("getBuildings");
Object result = getBuildings.invoke(buildingManager);
// Returns Map<BlockPos, IBuilding>
```

## Important Findings

1. **RegisteredStructureManager**: The building manager is actually a `RegisteredStructureManager` that implements `IRegisteredStructureManager`
2. **getBuildings()**: Returns `Map<BlockPos, IBuilding>` - this is the correct method to use
3. **Building Storage**: Buildings are stored as `ImmutableMap<BlockPos, IBuilding>` in the manager
4. **API Structure**: Use `IMinecoloniesAPI.getInstance().getColonyManager()` for proper access

## Common Mistakes to Avoid

1. **Direct colony.getBuildings()**: This doesn't exist - need to go through building manager
2. **Wrong return type casting**: `getBuildings()` returns `Map<BlockPos, IBuilding>`, not `Collection`
3. **Missing building manager step**: Always get building manager first, then buildings

## Request System Integration

For the free resources feature, the request system is located at:
- `com.minecolonies.api.colony.requestsystem.request.IRequest`
- `com.minecolonies.api.colony.requestsystem.manager.IRequestManager`

## Building Types

Common building classes:
- `BuildingTownHall`
- `BuildingWareHouse` 
- `BuildingMysticalSite`
- Various worker buildings in `workerbuildings` package

## Recipe System

Recipe crafting types are defined in the API and can be accessed through:
- `com.minecolonies.api.crafting.ICraftingType`
- `StandardFactoryController` for recipe management

## Best Practices

1. **Always use reflection with proper error handling**
2. **Log method names and return types for debugging**
3. **Check for null returns and handle exceptions gracefully**
4. **Use the API classes when possible instead of direct implementation classes**
5. **Test with different MineColonies versions as API may change**

## Debugging Tips

1. Log the class names of returned objects
2. Check method parameter counts and return types
3. Use try-catch blocks for each reflection call
4. Verify MineColonies is loaded before attempting integration
5. Test with a simple colony first before scaling up 

## NeoForge Command Registration Example (Working Pattern)

To register a working command in a NeoForge mod (as with `/mcmod`):

1. **Do NOT call `NeoForge.EVENT_BUS.register(this)` unless you have `@SubscribeEvent` methods.**
2. **Register your command handler using `NeoForge.EVENT_BUS.addListener`.**

Example:
```java
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class MyMod {
    public MyMod() {
        // Register the command handler
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("mcmod")
                .requires(source -> source.hasPermission(0))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("[mc_mod] /mcmod command executed!"), false);
                    return 1;
                })
        );
    }
}
```

**Key points:**
- Only use `NeoForge.EVENT_BUS.register(this)` if you have methods annotated with `@SubscribeEvent`.
- For command registration, always use `NeoForge.EVENT_BUS.addListener(this::onRegisterCommands)`.
- The command handler should use the event's dispatcher to register the command.
- The command can send a message to the player using `ctx.getSource().sendSuccess`.

This pattern ensures your command is registered and works in-game, as demonstrated by the working `/mcmod` command in this mod.

## Detecting When the Minecraft Server is Available (NeoForge)

To reliably detect when the Minecraft server is available (for scheduling pollers, sending messages, or MineColonies integration), use the following approach:

### 1. Use ServerLifecycleHooks.getCurrentServer()
```java
import net.neoforged.neoforge.server.ServerLifecycleHooks;

MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
if (server != null) {
    // Server is available, safe to interact
}
```
- This method returns `null` until the server is fully started.
- Poll for the server instance in a background thread if you need to schedule logic as soon as the server is ready.

### 2. Polling Example
```java
new Thread(() -> {
    MinecraftServer server = null;
    while (server == null) {
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        server = ServerLifecycleHooks.getCurrentServer();
    }
    // Now safe to schedule tasks on the server
    server.execute(() -> {
        // Your logic here (e.g., start poller, send messages)
    });
}, "mod-server-poller").start();
```
- Always schedule game logic on the main server thread using `server.execute(...)` for thread safety.
- Only send in-game messages or interact with players from the main server thread.

### 3. Best Practices
- Add debug logging to confirm when the server is found.
- If sending messages, check for online players and log if none are present.
- Avoid running game logic from background threads—always use `server.execute`.

### 4. Troubleshooting
- If `ServerLifecycleHooks.getCurrentServer()` never returns non-null, ensure your mod is loaded and MineColonies is present.
- Use log statements to trace the polling and server detection process. 