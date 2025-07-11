package com.yiu_cheung.mc_mod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.*;

public class RequestLogger {
    private static final Logger LOGGER = LogManager.getLogger("mc_mod");

    /**
     * Finds all assigned requests in all colonies and logs their details.
     */
    public static void logAllAssignedRequests() {
        try {
            Class<?> apiClass = Class.forName("com.minecolonies.api.IMinecoloniesAPI");
            Object apiInstance = apiClass.getMethod("getInstance").invoke(null);
            if (apiInstance == null) {
                LOGGER.warn("[RequestLogger] MineColonies API not available");
                return;
            }
            Object colonyManager = apiInstance.getClass().getMethod("getColonyManager").invoke(apiInstance);
            if (colonyManager == null) {
                LOGGER.warn("[RequestLogger] Colony manager not available");
                return;
            }
            Object colonies = colonyManager.getClass().getMethod("getAllColonies").invoke(colonyManager);
            if (colonies == null) {
                LOGGER.warn("[RequestLogger] No colonies available");
                return;
            }
            Collection<?> colonyCollection = (Collection<?>) colonies;
            int totalRequests = 0;
            for (Object colony : colonyCollection) {
                Object requestManager = colony.getClass().getMethod("getRequestManager").invoke(colony);
                if (requestManager == null) continue;
                // Get resolvers
                Object playerResolver = requestManager.getClass().getMethod("getPlayerResolver").invoke(requestManager);
                Object retryingResolver = requestManager.getClass().getMethod("getRetryingRequestResolver").invoke(requestManager);
                // Get all assigned tokens
                Collection<?> playerTokens = (Collection<?>) playerResolver.getClass().getMethod("getAllAssignedRequests").invoke(playerResolver);
                Collection<?> retryingTokens = (Collection<?>) retryingResolver.getClass().getMethod("getAllAssignedRequests").invoke(retryingResolver);
                Set<Object> allTokens = new HashSet<>();
                if (playerTokens != null) allTokens.addAll(playerTokens);
                if (retryingTokens != null) allTokens.addAll(retryingTokens);
                // Log each request
                for (Object token : allTokens) {
                    Object request = requestManager.getClass().getMethod("getRequestForToken", token.getClass()).invoke(requestManager, token);
                    if (request == null) continue;
                    // Extract details
                    String id = "?";
                    String state = "?";
                    String type = request.getClass().getSimpleName();
                    String requesterType = "?";
                    String resolverName = "?";
                    String itemName = "?";
                    String requesterName = "?";
                    try {
                        Object requestId = request.getClass().getMethod("getId").invoke(request);
                        id = String.valueOf(requestId);
                    } catch (Exception ignored) {}
                    try {
                        Object stateObj = request.getClass().getMethod("getState").invoke(request);
                        state = String.valueOf(stateObj);
                    } catch (Exception ignored) {}
                    try {
                        Object requester = request.getClass().getMethod("getRequester").invoke(request);
                        if (requester != null) {
                            requesterType = requester.getClass().getSimpleName();
                            // Try to get name
                            try {
                                Object nameObj = requester.getClass().getMethod("getName").invoke(requester);
                                if (nameObj != null) requesterName = String.valueOf(nameObj);
                            } catch (Exception ignored2) {}
                        }
                    } catch (Exception ignored) {}
                    try {
                        // Get resolver
                        Class<?> iTokenClass = Class.forName("com.minecolonies.api.colony.requestsystem.token.IToken");
                        Object resolver = requestManager.getClass().getMethod("getResolverForRequest", iTokenClass).invoke(requestManager, token);
                        if (resolver != null) resolverName = resolver.getClass().getSimpleName();
                    } catch (Exception ignored) {}
                    try {
                        // Get item name from display stacks
                        List<?> displayStacks = (List<?>) request.getClass().getMethod("getDisplayStacks").invoke(request);
                        if (displayStacks != null && !displayStacks.isEmpty()) {
                            Object itemStack = displayStacks.get(0);
                            Object stackCopy = itemStack.getClass().getMethod("copy").invoke(itemStack);
                            Object item = stackCopy.getClass().getMethod("getItem").invoke(stackCopy);
                            String descId = (String) item.getClass().getMethod("getDescriptionId").invoke(item);
                            if (descId != null) {
                                itemName = descId.replace("item.", "").replace("block.", "").replace("minecraft.", "").replace("minecolonies.", "");
                            }
                        }
                    } catch (Exception ignored) {}
                    LOGGER.info("[RequestLogger] Colony: {} | Request ID: {} | State: {} | Resolver: {} | Item: {} | Requester: {} | RequesterType: {} | Type: {}", colony, id, state, resolverName, itemName, requesterName, requesterType, type);
                    totalRequests++;
                }
            }
            LOGGER.info("[RequestLogger] Total assigned requests found: {}", totalRequests);
        } catch (Exception e) {
            LOGGER.error("[RequestLogger] Error logging assigned requests: {}", e.getMessage(), e);
        }
    }
} 