# Scaling Health for Minecraft 1.21.1 (NeoForge and Fabric)

An unofficial port of [Scaling Health](https://github.com/SilentChaos512/ScalingHealth) 8.0.2 by SilentChaos512
(Minecraft 1.20.1, Forge) to Minecraft 1.21.1, for NeoForge and for Fabric. License: LGPL-3.0-only (see [License](#license)).

Extra health and damage for players and mobs, with a highly customizable difficulty system:

- **Difficulty** grows with time played and distance from spawn (five area modes), changes with kills, deaths and
  sleep, and with the moon phase, biome and dimension. A meter shows it (key Z).
- **Mobs** get more health, damage and potion effects as the difficulty rises; some become **blights** (purple flames,
  much stronger, better loot).
- **Players** raise their max health with **heart crystals** (and XP levels) and their attack damage with **power
  crystals**; bandages and medkits heal over time; cursed / enchanted / chance hearts change the difficulty.
- A compact **heart HUD**: colored rows instead of a wall of hearts, heart tanks, absorption shields.
- Everything is tunable: client / server configs (in game: the mod list on NeoForge, Mod Menu on Fabric) and the
  `sh_mechanics` data pack files (expressions included).

The behaviour is that of 8.0.2; every difference is listed in [PORTLOG.md](PORTLOG.md).

## Requirements

- **NeoForge** 21.1.197 or newer for Minecraft 1.21.1.
- **Fabric**: Fabric Loader 0.16.14+, Fabric API, and Forge Config API Port (the configs use NeoForge's config API);
  Mod Menu for the config screen (optional).

Install on the server and on the clients.

## Building

JDK 21 for the mod, JDK 25 to run Gradle (Loom):

```
./gradlew build
```

The jars are in `neoforge/build/libs` and `fabric/build/libs`.

- `./gradlew :neoforge:runGameTestServer`, `./gradlew :fabric:runGametest`: in-game tests.
- `./gradlew :neoforge:runScene` / `:fabric:runScene`: screenshots of the HUD (world `run/saves/sh_scene`).
- `python3 tools/gen_resources.py`: regenerates the resources from the 1.20.1 ones.

## Credits

Scaling Health is by SilentChaos512 (and contributors); this port only updates it. SilentLib / silent-utils helpers
(MIT, SilentChaos512) and EvalEx (MIT, Udo Klimaschewski) are included.

## License

**LGPL-3.0-only** ([LICENSE](LICENSE), GPL-3.0 text in [COPYING](licenses/COPYING)), Copyright (c) 2026 Vladeezyy. The original Scaling Health, SilentLib / silent-utils and EvalEx are under the MIT License; the original license text and copyright notice are kept in [LICENSE-MIT](licenses/LICENSE-MIT). Forks and modified versions must stay open under the same license and credit the authors. Please download the mod only from the official release pages.
