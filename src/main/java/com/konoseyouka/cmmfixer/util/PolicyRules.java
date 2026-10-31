package com.konoseyouka.cmmfixer.util;

import java.util.Collection;
import java.util.Set;

public final class PolicyRules {
    private static final Set<String> VANILLA_CATS
            = Set.of("tabby",  "black",   "red",     "siamese", "british_shorthair",
                     "calico", "persian", "ragdoll", "white",   "jellie",
                     "all_black");

    public static boolean entityAllowed(String id, Collection<? extends String> additional) {
        return id.startsWith("minecraft:") || additional.contains(id);
    }

    public static boolean originalVariant(String entityId, String variantId) {
        if (variantId == null || !variantId.startsWith("minecraft:")) return false;
        String variant = variantId.substring("minecraft:".length());

        return switch(entityId) {
            case "minecraft:pig", "minecraft:cow", "minecraft:chicken" -> variant.equals("temperate");
            case "minecraft:wolf" -> variant.equals("pale");
            case "minecraft:cat" -> VANILLA_CATS.contains(variant);
            case "minecraft:frog" -> Set.of("temperate", "warm", "cold").contains(variant);
            default -> false;
        };
    }
    private PolicyRules() {}
}
