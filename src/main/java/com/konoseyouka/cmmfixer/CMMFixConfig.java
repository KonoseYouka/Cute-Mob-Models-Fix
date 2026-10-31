package com.konoseyouka.cmmfixer;

import com.konoseyouka.cmmfixer.util.DimensionProfile;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public final class CMMFixConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PROFILES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SERVER_MODELS;
    public static final ForgeConfigSpec.BooleanValue HITBOXES;

    static {
        var b = new ForgeConfigSpec.Builder();

        ALLOWED = b.comment(
                "Cute Mob Modelsのモデルを使用するバニラ以外のモブのリストです。",
                "なお、新しいMinecraftバージョンから輸入された、動物の色違いなどは常に除外対象です。",
                "",
                "Additional entity IDs allowed to use Cute Mob Models' models.",
                "Backported variants (e.g. animals based on biomes) from newer Minecraft versions are always excluded."
                )
                .defineListAllowEmpty("allowedEntities",
                        List.of(),
                        v -> v instanceof String s && ResourceLocation.tryParse(s) != null);

        HITBOXES = b.comment(
                "サーバー全体で当たり判定を変更するかどうか。Modをサーバー・クライアント両方に導入する必要があります。",
                "",
                "Whether to enable authoritative dimensions across server. Mod must be installed on both server and client."
                )
                .define("hitboxes", true);

        SERVER_MODELS = b.comment(
                "マルチプレイ・LAN公開: 全プレイヤーに対してCMMモデルが有効化されるエンティティID。空の場合、マージ版パックに含まれるモデルがすべて使用されます。",
                "ソロプレイ: 選択されたCMMリソースを自動的に検出します。",
                "",
                "Multiplayer or LAN: entity IDs whose CMM models are enabled for every player. Empty uses all models in the merged pack.",
                "Single player: detect the selected CMM resources automatically."
                )
                .defineListAllowEmpty("serverModels",
                        List.of(),
                        v -> v instanceof String s && ResourceLocation.tryParse(s) != null);

        PROFILES = b.comment(
                "当たり判定の寸法: entity_id, width, height, babyScale[minX, minY, minZ, maxX, maxY, maxZ]。",
                "調整にはゲーム内[F3 + B]のデバッグ表示を使用してください。赤ちゃんモデルは「entity_id#baby」と書きます。",
                "ソロプレイでは、この設定がない場合、描画モデルに基づいて寸法が測定されます。マルチプレイ・LAN公開では、設定が書き出されるまで、概算された寸法が使用されます。",
                "",
                "Collision dimensions: entity_id, width, height, babyScale[minX, minY, minZ, maxX, maxY, maxZ].",
                "Tune using [F3 + B] in game. Use entity_id#baby for a separately measured baby.",
                "Integrated servers measure rendered models when no override is specified. Dedicated servers use coarse fallback dimensions unless exported profiles are supplied."
                )
                .defineListAllowEmpty("dimensions",
                        List.of(),
                        v -> v instanceof String s && DimensionProfile.parse(s) != null);

        SPEC = b.build();
    }

    private CMMFixConfig() {}
}
