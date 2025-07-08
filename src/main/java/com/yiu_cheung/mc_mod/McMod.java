package com.yiu_cheung.mc_mod;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import java.lang.reflect.Method;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Collection;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import java.util.Map;
import net.minecraft.commands.Commands;
import java.util.HashSet;
import java.util.Set;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.core.BlockPos;
import java.util.Collections;
import java.util.HashMap;

@Mod("mc_mod")
public class McMod {
    public static final Logger LOGGER = LogManager.getLogger("mc_mod");
    public static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    public static McMod INSTANCE;

    // Message tracking and statistics
    private static final Map<String, Long> lastMessageTime = new HashMap<>();
    private static final Map<String, Integer> messageCount = new HashMap<>();
    private static final int MESSAGE_COOLDOWN_MS = 10000; // 10 seconds between similar messages
    private static final int MAX_MESSAGES_PER_TYPE = 3; // Max messages per type per session
    
    // Autofulfill statistics
    private static int totalRequestsProcessed = 0;
    private static int successfulFulfillments = 0;
    private static int failedFulfillments = 0;
    private static int skippedRequests = 0;
    private static long lastStatsReset = System.currentTimeMillis();
    private static final long STATS_RESET_INTERVAL = 300000; // 5 minutes

    public McMod() {
        LOGGER.info("MC Mod - Auto-Fulfill Builder Requests initialized!");
        INSTANCE = this;
        
        // Register command handler
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        
        // Start a background thread to poll for the server instance
        new Thread(() -> {
            LOGGER.info("[mc_mod] Polling for MinecraftServer instance...");
            sendServerMessage("[mc_mod] Polling for MinecraftServer instance...");
            MinecraftServer server = null;
            while (server == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
                server = getServerInstance();
            }
            LOGGER.info("[mc_mod] MinecraftServer instance found, scheduling auto-fulfill poller");
            sendServerMessage("[mc_mod] MinecraftServer instance found, scheduling auto-fulfill poller");
            MinecraftServer finalServer = server;
            finalServer.execute(() -> {
                LOGGER.info("[mc_mod] Scheduling auto-fulfill poller on main server thread");
                sendServerMessage("[mc_mod] Scheduling auto-fulfill poller on main server thread");
                executor.scheduleAtFixedRate(() -> INSTANCE.autoFulfillBuilderRequests(), 5, 5, java.util.concurrent.TimeUnit.SECONDS);
            });
        }, "mc_mod-server-poller").start();
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[mc_mod] FMLCommonSetupEvent received, scheduling auto-fulfill poller");
        sendServerMessage("[mc_mod] FMLCommonSetupEvent received, scheduling auto-fulfill poller");
        executor.scheduleAtFixedRate(() -> INSTANCE.autoFulfillBuilderRequests(), 5, 5, TimeUnit.SECONDS);
    }

    public static void sendServerMessage(String msg) {
        sendServerMessage(msg, "INFO");
    }
    
    public static void sendServerMessage(String msg, String type) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            LOGGER.debug("[mc_mod] sendServerMessage called but server is null: {}", msg);
            return;
        }
        if (!server.isSameThread()) {
            LOGGER.debug("[mc_mod] sendServerMessage not on main thread, scheduling on server: {}", msg);
            server.execute(() -> sendServerMessage(msg, type));
            return;
        }
        
        // Check message cooldown and limits
        if (!shouldSendMessage(msg, type)) {
            return;
        }
        
        LOGGER.info("[mc_mod] sendServerMessage [{}]: {}", type, msg);
        var playerList = server.getPlayerList().getPlayers();
        if (playerList.isEmpty()) {
            LOGGER.debug("[mc_mod] sendServerMessage: no players online to receive: {}", msg);
            return;
        }
        
        // Format message based on type
        String formattedMsg = formatMessage(msg, type);
        
        for (var player : playerList) {
            player.sendSystemMessage(Component.literal(formattedMsg));
            LOGGER.debug("[mc_mod] Sent message to player: {}", player.getName().getString());
        }
    }
    
    private static boolean shouldSendMessage(String msg, String type) {
        String messageKey = type + ":" + msg.hashCode();
        long currentTime = System.currentTimeMillis();
        
        // Check cooldown
        if (lastMessageTime.containsKey(messageKey)) {
            if (currentTime - lastMessageTime.get(messageKey) < MESSAGE_COOLDOWN_MS) {
                return false;
            }
        }
        
        // Check message limit
        int count = messageCount.getOrDefault(messageKey, 0);
        if (count >= MAX_MESSAGES_PER_TYPE) {
            return false;
        }
        
        // Update tracking
        lastMessageTime.put(messageKey, currentTime);
        messageCount.put(messageKey, count + 1);
        return true;
    }
    
    private static String formatMessage(String msg, String type) {
        String prefix = "";
        switch (type.toUpperCase()) {
            case "SUCCESS":
                prefix = "§a[✓] "; // Green checkmark
                break;
            case "ERROR":
                prefix = "§c[✗] "; // Red X
                break;
            case "WARNING":
                prefix = "§e[⚠] "; // Yellow warning
                break;
            case "INFO":
                prefix = "§b[ℹ] "; // Blue info
                break;
            case "PROGRESS":
                prefix = "§6[→] "; // Gold arrow
                break;
            case "STATS":
                prefix = "§d[📊] "; // Purple stats
                break;
            default:
                prefix = "§7[?] "; // Gray question mark
        }
        return prefix + msg;
    }
    
    private static void resetStats() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastStatsReset > STATS_RESET_INTERVAL) {
            totalRequestsProcessed = 0;
            successfulFulfillments = 0;
            failedFulfillments = 0;
            skippedRequests = 0;
            lastStatsReset = currentTime;
            lastMessageTime.clear();
            messageCount.clear();
        }
    }
    
    private static void sendStatsMessage() {
        if (totalRequestsProcessed > 0) {
            double successRate = (double) successfulFulfillments / totalRequestsProcessed * 100;
            String statsMsg = String.format("Autofulfill Stats: %d processed, %d successful (%.1f%%), %d failed, %d skipped", 
                totalRequestsProcessed, successfulFulfillments, successRate, failedFulfillments, skippedRequests);
            sendServerMessage(statsMsg, "STATS");
        }
    }
    
    public void autoFulfillBuilderRequests() {
        try {
            // Reset stats periodically
            resetStats();
            
            // Step 1: Get IMinecoloniesAPI instance
            Class<?> apiClass = Class.forName("com.minecolonies.api.IMinecoloniesAPI");
            Object apiInstance = apiClass.getMethod("getInstance").invoke(null);
            LOGGER.debug("[mc_mod] IMinecoloniesAPI instance: {}", apiInstance);

            // Step 2: Get ColonyManager
            Object colonyManager = apiInstance.getClass().getMethod("getColonyManager").invoke(apiInstance);
            LOGGER.debug("[mc_mod] ColonyManager: {}", colonyManager);

            // Step 3: Get all colonies
            Object colonies = colonyManager.getClass().getMethod("getAllColonies").invoke(colonyManager);
            LOGGER.debug("[mc_mod] Colonies: {}", colonies);

            int colonyCount = 0;
            for (Object colony : (Iterable<?>) colonies) {
                colonyCount++;
                LOGGER.debug("[mc_mod] Processing colony: {}", colony);
                processColonyRequests(colony);
            }
            
            // Send progress message
            if (colonyCount > 0) {
                sendServerMessage("Processed " + colonyCount + " colonies for autofulfill", "PROGRESS");
            }
            
            // Send stats every 10 cycles (about 50 seconds)
            if (totalRequestsProcessed > 0 && totalRequestsProcessed % 10 == 0) {
                sendStatsMessage();
            }
            
        } catch (Exception e) {
            LOGGER.error("[mc_mod] Error in autoFulfillBuilderRequests: {}", e.getMessage(), e);
            sendServerMessage("Autofulfill error: " + e.getMessage(), "ERROR");
        }
    }
    
    private void processColonyRequests(Object colony) {
        try {
            // Get the request manager
            Object requestManager = colony.getClass().getMethod("getRequestManager").invoke(colony);
            
            // Get all requests by iterating through all buildings and getting their requests
            Object buildingManager = colony.getClass().getMethod("getBuildingManager").invoke(colony);
            Map<?, ?> buildingsMap = (Map<?, ?>) buildingManager.getClass().getMethod("getBuildings").invoke(buildingManager);
            
            // Get all requesters (buildings) and their requests
            for (Object building : buildingsMap.values()) {
                processRequestsForBuilding(building, colony, requestManager);
            }
        } catch (Exception e) {
            LOGGER.error("[mc_mod] Error processing colony requests: {}", e.getMessage(), e);
        }
    }
    
    private void processRequestsForBuilding(Object building, Object colony, Object requestManager) {
        try {
            // Check if building is a requester
            Class<?> iRequesterClass = Class.forName("com.minecolonies.api.colony.requestsystem.requester.IRequester");
            if (!iRequesterClass.isInstance(building)) {
                return;
            }
            
            // Get requests made by this building
            Object requestHandler = requestManager.getClass().getMethod("getRequestHandler").invoke(requestManager);
            Collection<?> requests = (Collection<?>) requestHandler.getClass()
                .getMethod("getRequestsMadeByRequester", iRequesterClass)
                .invoke(requestHandler, building);
            
            if (requests != null) {
                for (Object request : requests) {
                    processRequest(request, colony);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[mc_mod] Error processing requests for building: {}", e.getMessage());
        }
    }
    
    private void processRequest(Object request, Object colony) {
        try {
            totalRequestsProcessed++;
            
            // Get the requester and building
            Object requester = null;
            try {
                requester = request.getClass().getMethod("getRequester").invoke(request);
            } catch (Exception e) {
                log("[mc_mod] Exception getting requester: " + e + " for request " + request.getClass().getName());
            }
            if (requester == null) {
                log("[mc_mod] Skipping request: requester is null for request " + request.getClass().getName());
                skippedRequests++;
                return;
            }
            
            // Check if requester is BuildingBasedRequester (like official logic)
            Class<?> buildingBasedRequesterClass = Class.forName("com.minecolonies.core.colony.requestsystem.requesters.BuildingBasedRequester");
            if (!buildingBasedRequesterClass.isInstance(requester)) {
                log("[mc_mod] Skipping request: requester is not BuildingBasedRequester (" + requester.getClass().getName() + ")");
                skippedRequests++;
                return;
            }
            
            // Try to get the building for this request using official method
            Object building = getBuildingOfficial(requester, colony, request);
            if (building == null) {
                log("[mc_mod] Skipping request: building is null for requester " + requester.getClass().getName());
                skippedRequests++;
                return;
            }
            
            String buildingClassName = building.getClass().getSimpleName();
            String requestClassName = request.getClass().getName();
            
            // Process all buildings, not just builder buildings (for testing)
            log("[mc_mod] Found request for building: " + buildingClassName + " - " + requestClassName);
            
            // Get the request state for logging
            Object state = request.getClass().getMethod("getState").invoke(request);
            String stateName = state.toString();
            
            // Process all requests regardless of state (like official creative resolve)
            log("[mc_mod] Processing request in state: " + stateName);
            
            // Check if the request is deliverable (like official creative resolve)
            Object requestable = request.getClass().getMethod("getRequest").invoke(request);
            if (!isDeliverable(requestable)) {
                log("[mc_mod] Skipping request: not deliverable");
                skippedRequests++;
                return;
            }
            
            // Use the creative resolve system to fulfill the request
            fulfillRequestWithCreativeResolve(request, colony, building, requestable);
            
        } catch (Exception e) {
            log("[mc_mod] Exception in processRequest: " + e);
            failedFulfillments++;
        }
    }
    
    private boolean isDeliverable(Object requestable) {
        try {
            // Check if the requestable implements IDeliverable
            Class<?> deliverableClass = Class.forName("com.minecolonies.api.colony.requestsystem.requestable.IDeliverable");
            return deliverableClass.isInstance(requestable);
        } catch (Exception e) {
            log("[mc_mod] Exception checking if requestable is deliverable: " + e);
            return false;
        }
    }
    
    private void fulfillRequestWithCreativeResolve(Object request, Object colony, Object building, Object requestable) {
        try {
            // Get the request ID
            Object requestId = request.getClass().getMethod("getId").invoke(request);
            
            // Get the citizen for this request
            Object citizen = getCitizenForRequest(building, request);
            
            if (citizen == null) {
                log("[mc_mod] No citizen found for request, cannot fulfill");
                sendServerMessage("No citizen assigned for request", "WARNING");
                failedFulfillments++;
                return;
            }
            
            // Get the display stacks to know what items to give (like official logic)
            List<Object> displayStacks = getDisplayStacks(request);
            if (displayStacks == null || displayStacks.isEmpty()) {
                log("[mc_mod] No display stacks found for request");
                sendServerMessage("No items found for request", "WARNING");
                failedFulfillments++;
                return;
            }
            
            // Get the first display stack and set the correct count (like official logic)
            Object itemStack = displayStacks.get(0);
            Object stackCopy = itemStack.getClass().getMethod("copy").invoke(itemStack);
            
            // Calculate count like official logic: Math.min(getCount(), getMaxStackSize())
            int count = getRequestCount(requestable);
            int maxStackSize = (int) stackCopy.getClass().getMethod("getMaxStackSize").invoke(stackCopy);
            int finalCount = Math.min(count, maxStackSize);
            stackCopy.getClass().getMethod("setCount", int.class).invoke(stackCopy, finalCount);
            
            // Get item name for better messaging
            String itemName = "Unknown Item";
            try {
                Object item = stackCopy.getClass().getMethod("getItem").invoke(stackCopy);
                itemName = item.getClass().getMethod("getDescriptionId").invoke(item).toString();
                // Clean up the item name
                if (itemName.startsWith("item.")) {
                    itemName = itemName.substring(5);
                }
                if (itemName.startsWith("block.")) {
                    itemName = itemName.substring(6);
                }
                itemName = itemName.replace("minecraft.", "").replace("minecolonies.", "");
            } catch (Exception e) {
                log("[mc_mod] Could not get item name: " + e.getMessage());
            }
            
            // Add the items to the citizen's inventory using official method
            Object remainingItemStack = addItemToCitizenInventoryOfficial(citizen, stackCopy);
            
            // Log details about the remaining item stack
            log("[mc_mod] Remaining item stack: " + (remainingItemStack != null ? remainingItemStack.getClass().getName() : "null"));
            if (remainingItemStack != null) {
                boolean isEmpty = isItemStackEmpty(remainingItemStack);
                log("[mc_mod] Remaining item stack is empty: " + isEmpty);
            }
            
            // Only resolve if all items were added successfully (like official logic)
            if (remainingItemStack != null && isItemStackEmpty(remainingItemStack)) {
                // Mark the request as resolved
                Object requestManager = colony.getClass().getMethod("getRequestManager").invoke(colony);
                Class<?> iTokenClass = Class.forName("com.minecolonies.api.colony.requestsystem.token.IToken");
                Class<?> requestStateClass = Class.forName("com.minecolonies.api.colony.requestsystem.request.RequestState");
                requestManager.getClass().getMethod("updateRequestState", iTokenClass, requestStateClass)
                    .invoke(requestManager, requestId, getRequestState("RESOLVED"));
                
                log("[mc_mod] Successfully fulfilled request with creative resolve");
                String buildingName = building.getClass().getSimpleName().replace("Building", "");
                sendServerMessage("Fulfilled " + finalCount + "x " + itemName + " for " + buildingName, "SUCCESS");
                successfulFulfillments++;
            } else {
                log("[mc_mod] Could not add all items to inventory, request not resolved");
                log("[mc_mod] Remaining item stack is null: " + (remainingItemStack == null));
                if (remainingItemStack != null) {
                    log("[mc_mod] Remaining item stack is empty: " + isItemStackEmpty(remainingItemStack));
                }
                sendServerMessage("Could not add " + finalCount + "x " + itemName + " to inventory", "ERROR");
                failedFulfillments++;
            }
            
        } catch (Exception e) {
            log("[mc_mod] Exception in fulfillRequestWithCreativeResolve: " + e);
            sendServerMessage("Error fulfilling request: " + e.getMessage(), "ERROR");
            failedFulfillments++;
        }
    }
    
    private Object addItemToCitizenInventoryOfficial(Object citizen, Object itemStack) {
        try {
            // Use official method: citizen.getInventory() (like official creative resolve logic)
            Object inventory = citizen.getClass().getMethod("getInventory").invoke(citizen);
            
            // Log inventory details for debugging
            log("[mc_mod] Inventory type: " + (inventory != null ? inventory.getClass().getName() : "null"));
            log("[mc_mod] ItemStack type: " + (itemStack != null ? itemStack.getClass().getName() : "null"));
            
            if (inventory == null) {
                log("[mc_mod] Inventory is null, cannot add item");
                return null;
            }
            
            if (itemStack == null) {
                log("[mc_mod] ItemStack is null, cannot add item");
                return null;
            }
            
            // Add the item to the inventory using InventoryUtils.addItemStackToItemHandlerWithResult
            // Use getDeclaredMethod to find the method with exact parameter types
            Class<?> inventoryUtilsClass = Class.forName("com.minecolonies.api.util.InventoryUtils");
            Class<?> itemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
            
            log("[mc_mod] Calling InventoryUtils.addItemStackToItemHandlerWithResult");
            // Get all declared methods and find the one with the correct signature
            java.lang.reflect.Method targetMethod = null;
            for (java.lang.reflect.Method method : inventoryUtilsClass.getDeclaredMethods()) {
                if (method.getName().equals("addItemStackToItemHandlerWithResult") && 
                    method.getParameterCount() == 2 &&
                    method.getParameterTypes()[1] == itemStackClass) {
                    targetMethod = method;
                    break;
                }
            }
            
            if (targetMethod == null) {
                log("[mc_mod] Could not find addItemStackToItemHandlerWithResult method");
                return null;
            }
            
            Object result = targetMethod.invoke(null, inventory, itemStack);
            
            log("[mc_mod] InventoryUtils.addItemStackToItemHandlerWithResult returned: " + (result != null ? result.getClass().getName() : "null"));
            return result;
        } catch (Exception e) {
            log("[mc_mod] Exception adding item to citizen inventory: " + e.getMessage());
            log("[mc_mod] Exception stack trace: " + e.toString());
            return null;
        }
    }
    
    private boolean isItemStackEmpty(Object itemStack) {
        try {
            // Use official ItemStackUtils.isEmpty method (like official creative resolve logic)
            Class<?> itemStackUtilsClass = Class.forName("com.minecolonies.api.util.ItemStackUtils");
            return (boolean) itemStackUtilsClass.getMethod("isEmpty", Class.forName("net.minecraft.world.item.ItemStack")).invoke(null, itemStack);
        } catch (Exception e) {
            log("[mc_mod] Exception checking if item stack is empty: " + e.getMessage());
            return false;
        }
    }
    
    private int getRequestCount(Object requestable) {
        try {
            // Try to get count from IDeliverable
            Class<?> deliverableClass = Class.forName("com.minecolonies.api.colony.requestsystem.requestable.IDeliverable");
            if (deliverableClass.isInstance(requestable)) {
                return (int) requestable.getClass().getMethod("getCount").invoke(requestable);
            }
            return 1; // Default to 1 if we can't determine
        } catch (Exception e) {
            log("[mc_mod] Exception getting request count: " + e);
            return 1;
        }
    }
    
    private Object getRequestState(String stateName) {
        try {
            Class<?> requestStateClass = Class.forName("com.minecolonies.api.colony.requestsystem.request.RequestState");
            for (Object state : requestStateClass.getEnumConstants()) {
                if (state.toString().equals(stateName)) {
                    return state;
                }
            }
            return null;
        } catch (Exception e) {
            log("[mc_mod] Exception getting request state: " + e);
            return null;
        }
    }
    
    private MinecraftServer getServerInstance() {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                LOGGER.info("[mc_mod] ServerLifecycleHooks.getCurrentServer() returned null");
            }
            return server;
        } catch (Exception e) {
            LOGGER.error("[mc_mod] Error calling ServerLifecycleHooks.getCurrentServer(): {}", e.getMessage());
            return null;
        }
    }
    
    private List<Object> getDisplayStacks(Object request) {
        try {
            // getDisplayStacks returns List<ItemStack> according to source code
            List<Object> displayStacks = (List<Object>) request.getClass().getMethod("getDisplayStacks").invoke(request);
            return displayStacks;
        } catch (Exception e) {
            LOGGER.debug("[AutoFulfill] Could not get display stacks: {}", e.getMessage());
            return null;
        }
    }
    
    private Object getBuildingOfficial(Object requester, Object colony, Object request) {
        try {
            // Get the request manager and request ID
            Object requestManager = colony.getClass().getMethod("getRequestManager").invoke(colony);
            Object requestId = request.getClass().getMethod("getId").invoke(request);
            
            // Get the building using the official method (like creative resolve logic)
            Object building = null;
            try {
                // Cast to the correct interface types
                Class<?> iRequestManagerClass = Class.forName("com.minecolonies.api.colony.requestsystem.manager.IRequestManager");
                Class<?> iTokenClass = Class.forName("com.minecolonies.api.colony.requestsystem.token.IToken");
                
                Optional<Object> buildingOptional = (Optional<Object>) requester.getClass()
                    .getMethod("getBuilding", iRequestManagerClass, iTokenClass)
                    .invoke(requester, requestManager, requestId);
                
                if (buildingOptional != null && buildingOptional.isPresent()) {
                    building = buildingOptional.get();
                }
            } catch (Exception e) {
                log("[mc_mod] Exception getting building using official method: " + e);
            }
            
            if (building != null) {
                log("[mc_mod] Found building using official method: " + building.getClass().getSimpleName());
                return building;
            }
        } catch (Exception e) {
            log("[mc_mod] Exception in getBuildingOfficial: " + e);
        }
        return null;
    }
    
    private Object getCitizenForRequest(Object building, Object request) {
        try {
            // Get the request ID
            Object requestId = request.getClass().getMethod("getId").invoke(request);
            
            // Log the building type for debugging
            log("[mc_mod] Building type: " + building.getClass().getName());
            log("[mc_mod] Building superclass: " + building.getClass().getSuperclass().getName());
            
            // Check if building implements IBuilding interface
            Class<?> iBuildingClass = Class.forName("com.minecolonies.api.colony.buildings.IBuilding");
            boolean isIBuilding = iBuildingClass.isInstance(building);
            log("[mc_mod] Is IBuilding: " + isIBuilding);
            
            // Check if building is AbstractBuilding
            Class<?> abstractBuildingClass = Class.forName("com.minecolonies.core.colony.buildings.AbstractBuilding");
            boolean isAbstractBuilding = abstractBuildingClass.isInstance(building);
            log("[mc_mod] Is AbstractBuilding: " + isAbstractBuilding);
            
            if (!isIBuilding && !isAbstractBuilding) {
                log("[mc_mod] Building is not an IBuilding or AbstractBuilding: " + building.getClass().getName());
                return null;
            }
            
            // First try to get citizen for the specific request
            try {
                Optional<Object> citizenOptional = (Optional<Object>) building.getClass().getMethod("getCitizenForRequest", requestId.getClass()).invoke(building, requestId);
                
                if (citizenOptional != null && citizenOptional.isPresent()) {
                    log("[mc_mod] Found citizen for specific request");
                    return citizenOptional.get();
                }
            } catch (Exception e) {
                log("[mc_mod] Exception getting citizen for specific request: " + e.getMessage());
            }
            
            // If no citizen for specific request, try to get assigned citizens from building
            log("[mc_mod] No citizen for specific request, trying to get assigned citizens from building");
            try {
                Set<Object> assignedCitizens = (Set<Object>) building.getClass().getMethod("getAllAssignedCitizen").invoke(building);
                
                if (assignedCitizens != null && !assignedCitizens.isEmpty()) {
                    log("[mc_mod] Found " + assignedCitizens.size() + " assigned citizens, using first one");
                    return assignedCitizens.iterator().next();
                }
            } catch (Exception e) {
                log("[mc_mod] Exception getting assigned citizens: " + e.getMessage());
            }
            
            log("[mc_mod] No citizen found for request, cannot fulfill");
            return null;
        } catch (Exception e) {
            log("[mc_mod] Exception getting citizen for request: " + e.getMessage());
            return null;
        }
    }

    // Register the /mcmod command
    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("mcmod")
                .requires(source -> source.hasPermission(0))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§b[ℹ] mc_mod commands:"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§6/autofulfill §7- Toggle autofulfill"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§6/stats §7- Show autofulfill statistics"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§6/trigger §7- Manually trigger autofulfill"), false);
                    return 1;
                })
                .then(Commands.literal("autofulfill")
                    .executes(ctx -> {
                        // Toggle autofulfill (placeholder for future implementation)
                        ctx.getSource().sendSuccess(() -> Component.literal("§a[✓] Autofulfill is currently running"), false);
                        return 1;
                    }))
                .then(Commands.literal("stats")
                    .executes(ctx -> {
                        if (totalRequestsProcessed > 0) {
                            double successRate = (double) successfulFulfillments / totalRequestsProcessed * 100;
                            ctx.getSource().sendSuccess(() -> Component.literal("§d[📊] Autofulfill Statistics:"), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(String.format("§7Processed: §f%d", totalRequestsProcessed)), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(String.format("§aSuccessful: §f%d (%.1f%%)", successfulFulfillments, successRate)), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(String.format("§cFailed: §f%d", failedFulfillments)), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(String.format("§eSkipped: §f%d", skippedRequests)), false);
                        } else {
                            ctx.getSource().sendSuccess(() -> Component.literal("§e[⚠] No autofulfill statistics available yet"), false);
                        }
                        return 1;
                    }))
                .then(Commands.literal("trigger")
                    .executes(ctx -> {
                        ctx.getSource().sendSuccess(() -> Component.literal("§6[→] Manually triggering autofulfill..."), false);
                        // Execute autofulfill on the server thread
                        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                        if (server != null) {
                            server.execute(() -> {
                                autoFulfillBuilderRequests();
                                ctx.getSource().sendSuccess(() -> Component.literal("§a[✓] Autofulfill completed"), false);
                            });
                        }
                        return 1;
                    }))
        );
    }

    // Remove @SubscribeEvent from onServerStarted, and register via event bus instead
    public static void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("[mc_mod] ServerStartedEvent received, scheduling auto-fulfill poller");
        sendServerMessage("[mc_mod] ServerStartedEvent received, scheduling auto-fulfill poller");
        executor.scheduleAtFixedRate(() -> INSTANCE.autoFulfillBuilderRequests(), 5, 5, TimeUnit.SECONDS);
    }

    // Replace log() to use debug for routine messages
    private static void log(String msg) {
        LOGGER.debug(msg);
    }
} 