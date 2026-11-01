package com.konoseyouka.cmmfixer.util;

import java.util.Set;
import java.util.Locale;

public final class HitboxRules {
    // 変更をかけない
    private static final Set<String> VANILLA = Set.of(
            "zombie", "husk", "drowned", "zombie_villager", "giant", "zombified_piglin",
            "skeleton", "stray", "wither_skeleton", "piglin", "piglin_brute", "enderman",
            "witch", "villager", "evoker", "vindicator", "pillager", "illusioner", "wandering_trader",
            "horse", "skeleton_horse", "zombie_horse", "donkey", "mule", "camel", "llama", "trader_llama");

    // 水生生物
    private static final Set<String> AQUATIC = Set.of("squid", "glow_squid", "cod", "salmon",
            "pufferfish", "tropical_fish", "tadpole", "axolotl", "dolphin", "guardian", "elder_guardian");

    // 標準的な人型
    private static final Set<String> HUMANOID = Set.of("cow", "mooshroom", "sheep", "pig", "panda",
            "goat", "spider", "cave_spider", "creeper", "hoglin", "zoglin", "snow_golem",
            "dolphin", "guardian", "squid", "glow_squid", "strider", "sniffer", "blaze", "silverfish", "turtle");

    private static String vanillaName(String id) {
        return id.startsWith("minecraft:")
                ? id.substring(10)
                : "";
    }

    public static boolean enabled(String id) { return !VANILLA.contains(vanillaName(id)); }
    public static boolean aquatic(String id) { return AQUATIC.contains(vanillaName(id)); }

    // 計算から除外するパーツ
    public static boolean excludePart(String entityId, String partId) {
        String mob = vanillaName(entityId), part = partId.toLowerCase(Locale.ROOT);
        return switch (mob) {
            case "fox", "guardian", "elder_guardian", "dolphin" -> part.contains("tail");
            case "parrot", "bee", "phantom", "vex", "allay", "bat" -> part.contains("wing") || part.contains("feather");
            case "chicken" -> part.contains("arm") || part.contains("wing");
            default -> false;
        };
    }

    public static ModelSize capToVanilla(ModelSize size, float width, float height) {
        return new ModelSize(Math.min(size.width(), width), Math.min(size.height(), height));
    }

    public static ModelSize limit(String id, ModelSize size, float vanillaWidth, float vanillaHeight,
                                  boolean inWater) {
        String mob = vanillaName(id);

        // カメは水中で寝そべり、陸上で直立
        if (mob.equals("turtle") && inWater)
            return new ModelSize(vanillaWidth, vanillaHeight);

        // 標準人型はプレイヤーと同一に
        if (HUMANOID.contains(mob))
            return capToVanilla(size, 0.6f, 1.8f);

        // エルダーガーディアンは高さを制限
        if (mob.equals("elder_guardian"))
            return capToVanilla(size, size.width(), vanillaHeight);

        // ウーパールーパーはバニラ以下の大きさに
        if (mob.equals("axolotl"))
            return capToVanilla(size, vanillaWidth, vanillaHeight);

        return size;
    }

    public static boolean restingSlime(boolean onGround, boolean passenger, float oldSquish, float squish) {
        return onGround
                && !passenger
                && Math.abs(oldSquish) <= 0.01f
                && Math.abs(squish) <= 0.01f;
    }

    private HitboxRules() {}
}
