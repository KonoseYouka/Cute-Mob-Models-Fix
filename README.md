# Cute Mob Models Fix
English: see [README_EN.md](README_EN.md).

Minecraft `1.20.1`、Minecraft Forge `47.4.10`向けプログラム。

[Cute Mob Models: Resource Pack](https://www.curseforge.com/minecraft/texture-packs/cute-mob-models-resource-pack)に関する問題を可能な限り修正します。

⚠️本Modはモブの当たり判定を変更します。既存のワールドに導入すると、変更によってモブが窒息ダメージを受ける可能性があります。新規ワールドへの導入を推奨します。

## 解決される問題
- Minecraft 1.20.1向けにリリースされているバージョン`1.0`には含まれていないモデルがある。
    - ラクダ、エンダードラゴン、カエル、ホグリン、ゾグリン、オウム、シロクマ、フグ、サケ、スニッファー、オタマジャクシ、カメが`1.2 v5`から移植されます。
- 見た目と当たり判定が一致しない。
    - 当たり判定をモデルの外観に基づき、必要に応じて補正します。
    - 遊泳行動との干渉が強く見受けられたウーパールーパーは、現時点では変更の対象外としています。
- 他のModのモブにカスタムモデルが適用されてしまい、見た目が崩壊する。
    - フィルターが追加されます。デフォルトではバニラのモブ (かつ、より新しいMinecraftにある色違いバージョンでないもの) のみモデルが適用されます。
    - フィルターはConfigから変更できます。

## 必要なもの
- Java `17`
- Minecraft `1.20.1`
- Minecraft Forge `47.4.10` 以降
- [Entity Texture Features](https://www.curseforge.com/minecraft/mc-mods/entity-texture-features-fabric) `7.2.6` 以降
- [Entity Model Features](https://www.curseforge.com/minecraft/mc-mods/entity-model-features) `3.3.12` 以降

## 使い方
Releasesから本Modをダウンロードし、`mods/`に配置してMinecraftを起動すると、必要な動作を実行します。当たり判定に関する問題を解決したい場合は、サーバー・クライアント両方に導入が必要です。

本来のリソースパックはインストール不要です。`1.0`と`1.2 v5`をCurseForgeから取得したのち結合します。ダウンロードにはHTTPSとSHA256のチェックを入れています。

ソースコードからJarファイルを作る場合、`jar`を実行してください。製作中、製品環境でテストしていたため、`runClient`の実行は想定していません。

### 注: リソースパックのダウンロードについて
本プログラムは、CurseForgeのCDNである`mediafilez.forgecdn.net`から直接ファイルを取得します。

[CurseForgeの説明](https://blog.curseforge.com/introducing-api-key-authentication-for-curseforge-file-downloads/)によれば、CDNからの直接ダウンロードにはAPI認証が必要であるそうですが、現時点 (2026年10月31日) では特に認証がなされない状態であるため、そのようにしています。

ダウンロードに失敗した場合は、お手数ですがCurseForgeからCute Mob Models `1.0`と`1.2 v5`を手動でダウンロードしてください。

## ライセンス
Cute Mob Models: Resource Packの表示に基づき、本プログラムも`GPL-3.0-only`で公開されます。なお、本プログラムにはCute Mob Models: Resource Packのモデル、テクスチャ、その他の著作物は一切同梱されていません。

必要なリソースは、利用者の環境でCurseForgeのCDNから取得され、互換性のためにローカル上で加工されます。生成されたリソースパックの再配布は、本Modの再配布とは別の行為であり、元リソースパックの利用条件に従う必要があります。

また、本プログラムは[Phenol-Docs](https://github.com/KonoseYouka/Phenol-Docs)の適用対象外です。

## 免責事項
適用法で認められる範囲において、本プログラムにはいかなる保証もありません。

書面で別途明記される場合を除き、此瀬榕花は、明示的か黙示的かを問わず、いかなる種類の保証もなく「現状のまま」本プログラムを提供します。これには、商品性および特定目的への適合性に関する黙示の保証が含まれますが、これらに限定されません。

本プログラムの品質および性能に関する一切のリスクは、利用者が負うものとします。本プログラムに欠陥が判明した場合、利用者は必要なすべてのサービス、修理、または修正にかかる費用を負担するものとします。

## 出典
- Minecraft by Mojang, Microsoft
- Entity Texture Features, Entity Model Features by Traben
- Cute Mob Models by _jx (original), Yarrmateys (remake), Emmie_4
