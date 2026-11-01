package com.konoseyouka.cmmfixer.util;

import java.util.*;

/** マージ版パック用の、だいたいの人型エンベロープ。
 *  明示的なサーバー設定が優先される。統合ホストでは、フォールバックを一度だけ調整。
 */
public final class BaselineDimensions {
    private static final Map<String,ModelSize> SIZES;

    static {
        Map<String,ModelSize> sizes = new TreeMap<>();

        put(sizes,0.6f,1.8f,"blaze cow creeper drowned evoker goat husk illusioner mooshroom piglin piglin_brute pillager sheep skeleton snow_golem spider stray villager vindicator wandering_trader witch zombie zombie_villager zombified_piglin");
        put(sizes,0.5f,1.4f,"chicken cod dolphin endermite fox frog parrot pufferfish rabbit salmon silverfish vex wolf cat ocelot axolotl bee");
        put(sizes,0.35f,0.9f,"allay bat tadpole");
        put(sizes,0.6f,1.6f,"pig strider turtle shulker");
        put(sizes,0.7f,2.0f,"donkey horse mule panda skeleton_horse zombie_horse hoglin zoglin guardian");
        put(sizes,0.8f,2.3f,"camel llama trader_llama polar_bear ravager elder_guardian wither");
        put(sizes,0.8f,2.7f,"enderman iron_golem warden wither_skeleton");
        put(sizes,1.0f,3.0f,"ghast");
        put(sizes,1.0f,2.0f,"squid glow_squid");
        put(sizes,0.8f,1.8f,"slime magma_cube");
        put(sizes,0.45f,1.35f,"cave_spider");
        put(sizes,1.2f,3.6f,"sniffer");
        put(sizes,3.6f,10.8f,"giant");

        // 大まかな水平方向のフォールバック。ソロプレイ等では実測値に置き換え。
        sizes.put("minecraft:phantom", new ModelSize(2.0f, 0.75f, -1, 0.5f, -1, 1, 1.25f, 1));
        sizes.put("minecraft:ender_dragon", new ModelSize(8, 4, -2, -1, -1, 2, 3, 7));

        SIZES = Map.copyOf(sizes);
    }

    private static void put(Map<String, ModelSize> map, float width, float height, String names) {
        for (String name : names.split(" "))
            map.put("minecraft:" + name, new ModelSize(width, height));
    }

    public static Set<String> ids() { return SIZES.keySet(); }

    public static ModelSize get(String id,boolean baby) {
        ModelSize adult = SIZES
                .getOrDefault(id, new ModelSize(0.6f, 1.8f))
                .compactFootprint();

        return baby
                ? adult.scale(0.5f)
                : adult;
    }

    private BaselineDimensions() {}
}
