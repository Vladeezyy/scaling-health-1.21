# Port log — Scaling Health 8.0.2 (1.20.1 Forge) → Minecraft 1.21.1 / NeoForge 21.1 and Fabric

Spec: SilentChaos512/ScalingHealth branch `1.20` (8.0.0, "final changes", 2023-06-19) plus what the released
8.0.1 / 8.0.2 jars changed (CurseForge file 5065403, compared with a decompile; the source of those two releases was
never pushed):
- 8.0.1: the vanilla heart row is drawn from vanilla's own icons ("hearts not rendering"); the deepslate heart crystal
  feature placed power crystal ore (fixed), and the deepslate features got more veins (heart 1 → 4, power 1 → 3).
- 8.0.2: after `/reload` every player gets the login packet (8.0.0 sent it to a null player and crashed dedicated
  servers; the same fix as commit c7c84c1 on branch 1.19).

SilentLib / silent-utils (MIT), which 1.20.1 needed as a separate mod, are inlined: Color, MathUtils, Anchor
(`lib/`), ClientTicks, DebugRenderOverlay; EntityHelper.heal and MCMathUtils.distanceSq where they were used.

## 2026-10-10 — first port

Everything of 8.0.2 is ported: the difficulty system (player, area and server difficulty, the five area modes,
lunar / location multipliers, group bonus, AFK, kill / death / sleep mutators), mob health / damage / potion scaling
and blights (with their flames), player heart and power crystals, XP hearts, bonus regeneration, damage scaling, pet
health, bandages and medkits, the difficulty hearts, ores and world generation, loot (bonus drops, chest additions),
the five commands, the heart HUD (colored rows, tanks, absorption styles, health text), the difficulty meter, the debug
overlay, the client / server configs and the `sh_mechanics` data pack files.

### How it is built
- One code base, two jars: `common` holds the game code, `neoforge` and `fabric` the loader glue (MultiLoader
  layout, like the Iron Backpacks port).
- Forge capabilities → fields added to every entity by a mixin (`data/`), attached the first time they are asked
  for with the 1.20.1 rules. They are saved like the capabilities were, under `ForgeCaps` /
  `scalinghealth:difficulty_affected` (`player_data`, `difficulty_source`, `pet_health`), so entity and player data of
  a 1.20.1 world reads back. The overworld's difficulty (a capability on the overworld level) is saved data
  (`data/scalinghealth.dat`); 1.20.1 kept it in Forge's `capabilities.dat`, which is not read.
- Events: NeoForge keeps most Forge events; where Fabric has none, a common mixin is used on both loaders so they
  behave the same: living tick (LivingTickEvent), damage (LivingAttackEvent / LivingHurtEvent), experience drops
  (LivingExperienceDropEvent, HIGHEST: the reward is changed before NeoForge's event), player tick, XP level change,
  sleeping and waking, spawner spawns (MobSpawnEvent.FinalizeSpawn).
- Config: the same `scalinghealth-client.toml` / `scalinghealth-server.toml`, NeoForge's config API (Forge's, renamed);
  on Fabric through Forge Config API Port (a required mod).
- EvalEx 2.1 (the expressions in the `sh_mechanics` files) is bundled in the jar, as 1.20.1 did.
- Packets: the four 1.20.1 messages as 1.21 payloads (`scalinghealth:client_sync`, `client_login`, `client_blight`,
  `mechanics`), same contents.
- Global loot modifiers: NeoForge's (`data/neoforge/loot_modifiers`). Fabric has none, so the Fabric jar reads the same
  files and applies them where NeoForge does (end of `LootTable.getRandomItems`).
- Ores: NeoForge biome modifier `data/scalinghealth/neoforge/biome_modifier/sh_ores.json`; Fabric adds the same four
  placed features to overworld biomes in code.
- Resources are generated from the 1.20.1 files by `tools/gen_resources.py` (never edited by hand): 1.21 folder
  names (`recipe`, `loot_table`, `advancement`, `tags/block`, `tags/entity_type`), recipe results (`id`),
  `forge:ingots/iron` → `c:ingots/iron`, `random_chance_with_looting` → `random_chance_with_enchanted_bonus` (as
  vanilla converted its own tables), the silk touch tool predicate, `forge:loot_table_id` → `neoforge:loot_table_id`,
  `forge:add_features` → `neoforge:add_features`.

### Differences from 8.0.2
- **Health text** ("2x" left of the hearts, `hearts.health.text.*`): 1.20.1 drew it in the Post event of the health
  overlay, which Forge skips when the Pre event is canceled — and Scaling Health cancels it whenever it draws the
  hearts. (The text code would also have crashed: it used the font of a never-opened Screen, null.) So 1.20.1 never
  showed it; the port draws it right after the hearts, as the config describes.
- Vanilla hearts are 1.21 GUI sprites (`hud/heart/...`); 8.0.2's icons.png cells map to them one to one.
- Attribute modifiers have ids: `scalinghealth:health_modifier`, `scalinghealth:damage_modifier`,
  `scalinghealth:effect.bandaged`. The 1.21 data fixer turns the 1.20.1 UUID modifiers into `minecraft:<uuid>`; those
  are removed when the new ones are set, so an upgraded player doesn't get the bonus twice.
- `sh_mechanics` files: the 1.20.1 operation names (ADDITION, MULTIPLY_BASE, MULTIPLY_TOTAL) are read and written;
  the 1.21 names (ADD_VALUE, ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL) are accepted too.
- Sparkle particles: 1.20.1 bound `textures/particle/generic0..3.png` while rendering (one texture for the whole
  particle batch); here the four frames are particle sprites, same frame order (0 1 2 3 2 1 0) and colors.
- Blight flames: 1.20.1 listened to RenderLivingEvent (Pre and Post) and drew them twice at the same place; drawn
  once.
- Bandages: milk still doesn't remove them (NeoForge: no milk cure; Fabric: kept by a mixin); a totem of undying
  does, as in 1.20.1.
- Spawner bonus (`spawnerModifier`): monster spawners, as 1.20.1; the trial spawners new in 1.21 are not spawners
  there.
- Item tooltips have no world in 1.21: the difficulty hearts show their amount also outside a world (1.20.1: 0).
- In-game config screen (1.20.1 had none): NeoForge's configuration screen, from the mod list (NeoForge) or Mod Menu
  (Fabric, through Forge Config API Port). Every option has an English label and a Russian label and tooltip
  (`scalinghealth.configuration.*`, from `tools/lang`); English tooltips are the config comments.
- Fabric: the difficulty meter and the debug overlay are drawn after the whole HUD (Forge: after the debug text / the
  chat); the armor row moves up exactly as with NeoForge's left status bar height.

### Bugs of 8.0.2 fixed
- Heart crystals stopped working: one flag (`usedForPet`) on the item, shared by every heart crystal, was set by a
  use on a pet and only cleared on the client — on a server every later heart crystal use failed until a restart.
  Removed (a click on a pet already ends the click).
- Bonus regeneration came twice as often as configured: the player tick handler ran in both tick phases.
- Power crystals never dropped: the loot modifiers removed them when power crystals were *enabled* (inverted check).
  Now they are removed only when disabled.
- Every loaded mob was processed again (difficulty, health, effects, a new blight roll): the "processed" flag wasn't
  saved. It is now (`Processed`); data from 1.20.1 without it counts as processed when it has a difficulty or is a
  blight.
- XP hearts changed only on login, death, crystal use or `giveExperienceLevels`: level ups from experience orbs were
  missed. They update now.
- The "level changed" flag was one for all players, so another player's update could take it; now per player.
- Sleeping crashed dedicated servers: the sleep warning read the *client* config, which a dedicated server doesn't
  load. It uses `sleepWarningMessage` from `sh_mechanics/difficulty.json`; the client option `difficulty.warnWhenSleeping`
  is removed.
- The "weighted" average area mode wasn't weighted (1 - distance² / radius² in integers is 1 for everyone in range).
- The extrema area mode with `min: true` always gave 0 (it started from 0).
- Heart crystals were used up past the health limit (hearts compared with max health) and power crystals one or two
  past the damage limit, giving nothing; they aren't used any more there.
- Boss blights got the name key `misc.scalinghealth.blight`, which doesn't exist; they use `blight.scalinghealth`
  ("Blight %s").
- The debug overlay showed the integrated server's world difficulty (0 on a dedicated server); it shows the one the
  server sends.
- A blight killed by a pet whose owner isn't a player crashed (cast to Player).
- The spawner mob list was a list that grew; a set now.
