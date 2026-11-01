# Cute Mob Models Fix
A mod for Minecraft `1.20.1`, Minecraft Forge `47.4.10`.

Tries to fix some issues regarding [Cute Mob Models: Resource Pack](https://www.curseforge.com/minecraft/texture-packs/cute-mob-models-resource-pack).

⚠️This mod modifies mobs' hitboxes. Adding it to an existing world may cause mobs to take suffocation damage. It is recommended to use this in a new world.

## Issues Resolved
- Certain models were missing from version `1.0`, which is released for Minecraft 1.20.1.
    - Models for the Camel, Ender Dragon, Frog, Hoglin, Zoglin, Parrot, Polar Bear, Pufferfish, Salmon, Sniffer, Tadpole, and Turtle have been backported from `1.2 v5`.
- Mismatches between visual appearance and hitboxes.
    - Hitboxes have been adjusted to align with the models' visual appearance.
    - Axolotls, which showed significant interference with their swimming behavior, are currently excluded from the adjustments.
- Custom models were being incorrectly applied to mobs from other mods, causing visual glitches.
    - A filter has been added. By default, custom models are applied only to vanilla mobs (excluding the color variants from newer Minecraft versions).
    - This filter can be adjusted via config.

## Requirements
- Java `17`
- Minecraft `1.20.1`
- Minecraft Forge `47.4.10` or later
- [Entity Texture Features](https://www.curseforge.com/minecraft/mc-mods/entity-texture-features-fabric) `7.2.6` or later
- [Entity Model Features](https://www.curseforge.com/minecraft/mc-mods/entity-model-features) `3.3.12` or later

## How to Use
Download this mod from Releases, place it in your `mods/` folder, and launch Minecraft; the mod automatically completes the setup. If you wish to resolve issues related to hitboxes, the mod must be installed on both server and client.

You do not need to install the original resource pack manually. Mod will automatically retrieves CMMRP `1.0` and `1.2 v5` from CurseForge and merges them locally. The download process includes HTTPS and SHA256 integrity checks.

If you are building the JAR file from the source code, please run the `jar` task. As this project was tested in a production environment during development, the `runClient` task is not intended for use.

### Note: Regarding Resource Pack Downloads
This program retrieves files directly from CurseForge's CDN (`mediafilez.forgecdn.net`).

According to [CurseForge's description](https://blog.curseforge.com/introducing-api-key-authentication-for-curseforge-file-downloads/), direct downloads from the CDN should require API authentication; however, as of this writing (October 31, 2026), no such authentication is currently enforced, which is why this method is used.

If the download fails, please manually download versions `1.0` and `1.2 v5` of "Cute Mob Models" from CurseForge.

## License
In accordance with the terms of Cute Mob Models: Resource Pack, this program is released under `GPL-3.0-only` license. Although this program does not include any models, textures, or other copyrighted materials from the Pack.

Required resources are retrieved from the CurseForge CDN on the user's machine and processed locally to ensure compatibility. The redistribution of any generated resource packs is a separate action from the redistribution of this mod itself and must comply with the usage terms of the original Pack.

Additionally, [Phenol-Docs](https://github.com/KonoseYouka/Phenol-Docs) is not applied to this program.

## Disclaimer
To the extent permitted by applicable law, this program is provided without any warranties.

Unless otherwise specified in writing, Konose Youka provides this program "as is", without warranties of any kind, either express or implied-including, but not limited to, implied warranties of merchantability or fitness for a particular purpose.

The user assumes all risks regarding the quality and performance of this program. Should any defects be discovered, the user shall bear the full cost of any necessary servicing, repairs, or corrections.

## Credits
- Minecraft by Mojang, Microsoft
- Entity Texture Features, Entity Model Features by Traben
- Cute Mob Models by _jx (original), Yarrmateys (remake), Emmie_4
