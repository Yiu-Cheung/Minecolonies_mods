# MC Mod - Auto-Fulfill Builder Requests Implementation Guide

## Overview

This mod automatically fulfills builder requests in MineColonies, but only when the resolver is a player. This prevents interference with automated systems while providing convenience for player-managed requests.

## Core Implementation

### 1. Key Classes and Interfaces

**MineColonies Classes to Import:**
```java
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.colony.requestsystem.resolver.player.IPlayerRequestResolver;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
```

### 2. Core Logic Implementation

**BuilderRequestHandler.processBuilderRequest() - Complete Implementation:**

```java
public static void processBuilderRequest(IRequest<?> request, IColony colony) {
    if (!Config.isAutoFulfillBuilderRequestsEnabled()) {
        return;
    }
    
    if (request == null || colony == null) {
        return;
    }
    
    // Check if this is a deliverable request (builder requesting items)
    if (!(request.getRequest() instanceof IDeliverable)) {
        return;
    }
    
    // Get the resolver for this request
    IRequestManager requestManager = colony.getRequestManager();
    IRequestResolver<?> resolver = requestManager.getResolverForRequest(request.getId());
    
    // Only auto-fulfill if the resolver is a player
    if (!(resolver instanceof IPlayerRequestResolver)) {
        McMod.LOGGER.debug("Skipping auto-fulfill - resolver is not a player: {}", 
            resolver.getClass().getSimpleName());
        return;
    }
    
    McMod.LOGGER.info("Processing player-resolved builder request: {}", request.getId());
    
    // Get the requested items
    IDeliverable deliverable = (IDeliverable) request.getRequest();
    List<ItemStack> requestedStacks = request.getDisplayStacks();
    
    if (requestedStacks.isEmpty()) {
        McMod.LOGGER.warn("No display stacks found for request: {}", request.getId());
        return;
    }
    
    // Try to fulfill the request by providing the items
    ItemStack requestedStack = requestedStacks.get(0).copy();
    requestedStack.setCount(Math.min(deliverable.getCount(), requestedStack.getMaxStackSize()));
    
    // Find a citizen to give the items to
    colony.getCitizenManager().getCitizens().stream()
        .filter(citizen -> citizen.getWorkBuilding() != null)
        .findFirst()
        .ifPresent(citizen -> {
            // Add items to citizen's inventory
            ItemStack remainingStack = InventoryUtils.addItemStackToItemHandlerWithResult(
                citizen.getInventory(),
                requestedStack
            );
            
            if (ItemStackUtils.isEmpty(remainingStack)) {
                // Successfully fulfilled the request
                requestManager.updateRequestState(request.getId(), RequestState.RESOLVED);
                McMod.LOGGER.info("Auto-fulfilled builder request {} for citizen {}", 
                    request.getId(), citizen.getName());
                
                // Send in-game message to all players
                String itemName = requestedStack.getDisplayName().getString();
                int amount = requestedStack.getCount();
                sendAutoFulfillMessage(request, colony, itemName, amount);
            } else {
                McMod.LOGGER.warn("Could not fully fulfill request {} - citizen inventory full", 
                    request.getId());
            }
        });
}
```

### 3. Event Integration

**Forge Event Registration:**
```java
@Mod(McMod.MOD_ID)
public class McMod {
    public McMod() {
        MinecraftForge.EVENT_BUS.register(this);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }
    
    private void setup(final FMLCommonSetupEvent event) {
        // Register MineColonies event listeners
    }
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        McModCommand.register(event.getDispatcher());
    }
}
```

**MineColonies Event Listening:**
```java
// Listen for request creation events
@SubscribeEvent
public void onRequestCreated(RequestCreatedEvent event) {
    IRequest<?> request = event.getRequest();
    IColony colony = event.getColony();
    
    if (BuilderRequestHandler.shouldAutoFulfill(request, colony)) {
        BuilderRequestHandler.processBuilderRequest(request, colony);
    }
}
```

### 4. Configuration Integration

**Config.java - Enhanced:**
```java
public class Config {
    public static boolean ENABLE_AUTO_FULFILL_BUILDER_REQUESTS = true;
    public static boolean LOG_AUTO_FULFILL_ACTIONS = true;
    public static int MAX_AUTO_FULFILL_PER_TICK = 5;
    
    public static void setEnableAutoFulfillBuilderRequests(boolean enabled) {
        ENABLE_AUTO_FULFILL_BUILDER_REQUESTS = enabled;
    }
    
    public static boolean isAutoFulfillBuilderRequestsEnabled() {
        return ENABLE_AUTO_FULFILL_BUILDER_REQUESTS;
    }
}
```

### 5. Command Integration

**McModCommand.java - Enhanced:**
```java
public class McModCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("mc_mod")
                .then(Commands.literal("auto_fulfill")
                    .then(Commands.literal("status")
                        .executes(McModCommand::status))
                    .then(Commands.literal("enable")
                        .executes(McModCommand::enable))
                    .then(Commands.literal("disable")
                        .executes(McModCommand::disable)))
        );
    }
}
```

## Integration Steps

### Step 1: Add Dependencies
Ensure your `build.gradle` includes MineColonies as a dependency:
```gradle
dependencies {
    compileOnly fg.deobf("com.minecolonies:minecolonies:${minecolonies_version}")
}
```

### Step 2: Import Required Classes
Add all the necessary MineColonies imports to your classes.

### Step 3: Register Event Listeners
Register your mod to listen for MineColonies events.

### Step 4: Test the Implementation
1. Start a server with MineColonies
2. Create a colony with builders
3. Have builders request resources
4. Use `/mc_mod auto_fulfill enable` to enable auto-fulfill
5. Verify that only player-resolved requests are auto-fulfilled

## Key Features

1. **Player-Only Resolution**: Only auto-fulfills when `resolver instanceof IPlayerRequestResolver`
2. **Deliverable Requests**: Only processes `IDeliverable` requests (builder resource requests)
3. **Citizen Integration**: Provides items directly to citizen inventories
4. **In-Game Messaging**: Sends messages to all players when requests are auto-fulfilled
5. **Logging**: Comprehensive logging for debugging and monitoring
6. **Configuration**: Easy enable/disable via commands and config
7. **Safety Checks**: Multiple validation steps to prevent errors

## Troubleshooting

- **Requests Not Auto-Fulfilling**: Check if resolver is `IPlayerRequestResolver`
- **Items Not Being Provided**: Verify citizen inventory space
- **Mod Not Loading**: Ensure MineColonies dependency is properly configured
- **Commands Not Working**: Check event registration in main mod class

This implementation provides a robust foundation for auto-fulfilling builder requests while respecting the MineColonies request system architecture. 