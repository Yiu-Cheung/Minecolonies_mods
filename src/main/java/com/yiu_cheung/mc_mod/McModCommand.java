package com.yiu_cheung.mc_mod;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

public class McModCommand {
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("mcmod")
            .executes(ctx -> {
                ServerPlayer sp;
                try {
                    sp = ctx.getSource().getPlayerOrException();
                } catch (Exception e) {
                    return 0;
                }
                try {
                    for (Object colony : getAllColonies()) {
                        sp.sendSystemMessage(Component.literal("[mc_mod] Colony: " + colony));
                        // Buildings
                        Object buildingManager = colony.getClass().getMethod("getBuildingManager").invoke(colony);
                        Map<?, ?> buildingsMap = (Map<?, ?>) buildingManager.getClass().getMethod("getBuildings").invoke(buildingManager);
                        for (Object building : buildingsMap.values()) {
                            Object requestManager = getRequestManager(colony);
                            Object requestHandler = requestManager.getClass().getMethod("getRequestHandler").invoke(requestManager);
                            Class<?> iRequesterClass = Class.forName("com.minecolonies.api.colony.requestsystem.requester.IRequester");
                            Iterable<Object> requests = (Iterable<Object>) requestHandler.getClass().getMethod("getRequestsMadeByRequester", iRequesterClass).invoke(requestHandler, building);
                            for (Object request : requests) {
                                sp.sendSystemMessage(Component.literal("[mc_mod] Building request: " + request));
                            }
                        }
                        // Citizens
                        Object citizenManager = colony.getClass().getMethod("getCitizenManager").invoke(colony);
                        Iterable<?> citizens = (Iterable<?>) citizenManager.getClass().getMethod("getCitizens").invoke(citizenManager);
                        for (Object citizen : citizens) {
                            Object requestManager = getRequestManager(colony);
                            Object requestHandler = requestManager.getClass().getMethod("getRequestHandler").invoke(requestManager);
                            Class<?> iRequesterClass = Class.forName("com.minecolonies.api.colony.requestsystem.requester.IRequester");
                            Iterable<Object> requests = (Iterable<Object>) requestHandler.getClass().getMethod("getRequestsMadeByRequester", iRequesterClass).invoke(requestHandler, citizen);
                            for (Object request : requests) {
                                sp.sendSystemMessage(Component.literal("[mc_mod] Citizen request: " + request));
                            }
                        }
                    }
                } catch (Exception e) {
                    sp.sendSystemMessage(Component.literal("[mc_mod] Error: " + e));
                }
                return 1;
            })
            .then(Commands.literal("debug")
                .executes(ctx -> {
                    ServerPlayer sp;
                    try {
                        sp = ctx.getSource().getPlayerOrException();
                    } catch (Exception e) {
                        return 0;
                    }
                    sp.sendSystemMessage(Component.literal("[mc_mod] === Request Capture Debug Info ==="));
                    sp.sendSystemMessage(Component.literal("[mc_mod] Active Request States:"));
                    sp.sendSystemMessage(Component.literal("  - PENDING"));
                    sp.sendSystemMessage(Component.literal("  - IN_PROGRESS"));
                    sp.sendSystemMessage(Component.literal("  - FOLLOWUP_IN_PROGRESS"));
                    sp.sendSystemMessage(Component.literal("  - IN_PROGRESS_DELIVERY"));
                    sp.sendSystemMessage(Component.literal("  - FOLLOWUP_IN_PROGRESS_DELIVERY"));
                    sp.sendSystemMessage(Component.literal("  - IN_PROGRESS_PICKUP"));
                    sp.sendSystemMessage(Component.literal("  - FOLLOWUP_IN_PROGRESS_PICKUP"));
                    sp.sendSystemMessage(Component.literal("[mc_mod] Allowed Resolver Types:"));
                    sp.sendSystemMessage(Component.literal("  - StandardPlayerRequestResolver"));
                    sp.sendSystemMessage(Component.literal("  - StandardRetryingRequestResolver"));
                    sp.sendSystemMessage(Component.literal("  - StandardDeliveryRequestResolver"));
                    sp.sendSystemMessage(Component.literal("  - StandardPickupRequestResolver"));
                    sp.sendSystemMessage(Component.literal("  - StandardCraftingRequestResolver"));
                    sp.sendSystemMessage(Component.literal("  - StandardBuildingRequestResolver"));
                    sp.sendSystemMessage(Component.literal("[mc_mod] ================================="));
                    return 1;
                }))
        );
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onServerStarted(ServerStartedEvent event) {
        // Announce mod enabled to all online players
        try {
            Object server = event.getServer();
            Iterable<?> playerList = (Iterable<?>) server.getClass().getMethod("getPlayerList").invoke(server);
            for (Object player : playerList) {
                if (player instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal("[mc_mod] Auto-fulfill mod ENABLED!"));
                }
            }
        } catch (Exception e) {
            // log error if needed
        }
    }

    // Utility to get all colonies from the server
    private static Iterable<?> getAllColonies() {
        try {
            Class<?> apiProxyClass = Class.forName("com.minecolonies.api.MinecoloniesAPIProxy");
            Object apiProxy = apiProxyClass.getMethod("getInstance").invoke(null);
            Object colonyManager = apiProxyClass.getMethod("getColonyManager").invoke(apiProxy);
            if (colonyManager == null) return Collections.emptyList();
            return (Iterable<?>) colonyManager.getClass().getMethod("getColonies").invoke(colonyManager);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static Object getRequestManager(Object colony) {
        try {
            return colony.getClass().getMethod("getRequestManager").invoke(colony);
        } catch (Exception e) {
            return null;
        }
    }
} 