# Scaling Health for Minecraft 1.21.1 (NeoForge and Fabric)

An unofficial port of [Scaling Health](https://github.com/SilentChaos512/ScalingHealth) 8.0.2 by SilentChaos512
(Minecraft 1.20.1, Forge) to Minecraft 1.21.1, for NeoForge and for Fabric. License: LGPL-3.0-only (see License).
Downloads: [releases](../../releases).

<details>
<summary><b>🇬🇧 English — click to open</b></summary>

## About

Extra health and damage for players and mobs, with a highly customizable difficulty system:

- **Difficulty** grows with time played and distance from spawn (five area modes), changes with kills, deaths and
  sleep, and with the moon phase, biome and dimension. A meter shows it (key Z).
- **Mobs** get more health, damage and potion effects as the difficulty rises; some become **blights** (purple flames,
  much stronger, better loot).
- **Players** raise their max health with **heart crystals** (and XP levels) and their attack damage with **power
  crystals**; bandages and medkits heal over time; cursed / enchanted / chance hearts change the difficulty.
- A compact **heart HUD**: colored rows instead of a wall of hearts, heart tanks, absorption shields.
- Languages: English, Russian, Ukrainian, German, Spanish, Portuguese (Brazil); Italian, Polish, Chinese and Venezuelan
  Spanish from 8.0.2 (partial).
- Everything is tunable: client / server configs (in game: the mod list on NeoForge, Mod Menu on Fabric) and the
  `sh_mechanics` data pack files (expressions included).

The behaviour is that of 8.0.2; every difference is listed in [PORTLOG.md](PORTLOG.md).

## Requirements

- **NeoForge** 21.1.197 or newer for Minecraft 1.21.1.
- **Fabric**: Fabric Loader 0.16.14+, Fabric API, and Forge Config API Port (the configs use NeoForge's config API);
  Mod Menu for the config screen (optional).

Needed on the server and on the clients.

## What this port fixes

Open issues of the original:
- Returning from the End reset max health and attack damage to the base values
  ([#458](https://github.com/SilentChaos512/ScalingHealth/issues/458)).
- A wrong value in a data pack's `sh_mechanics` file made `/reload` fail and the world impossible to join again
  ([#438](https://github.com/SilentChaos512/ScalingHealth/issues/438),
  [#443](https://github.com/SilentChaos512/ScalingHealth/issues/443)): the error is logged with the file name and the
  mod's own file is used.
- Mobs didn't apply their effects (poison, wither) with a damage scaling mode other than `MAX_HEALTH`
  ([#441](https://github.com/SilentChaos512/ScalingHealth/issues/441)).
- The sleep warning can be turned off ([#459](https://github.com/SilentChaos512/ScalingHealth/issues/459)): it is
  `sleepWarningMessage` in `sh_mechanics/difficulty.json`; it also no longer crashes dedicated servers.

Found in the code of 8.0.2 while porting: heart crystals stopping on servers, double bonus regeneration, power
crystals never dropping, mobs processed again on every load, XP hearts missed for experience orbs, the weighted and
extrema area modes, crystals wasted past the limits, a crash when a pet of a non-player killed a blight, the boss blight
name, the debug overlay on dedicated servers. Causes and fixes: [PORTLOG.md](PORTLOG.md).

## Testing

22 GameTests on a server of each loader (crystals, difficulty, mob scaling, blights, death and End return, the
Nether, broken data pack files, loot, ores, commands), run by CI on every push; the release jars are checked on real
NeoForge and Fabric servers.

## Bug reports

Found a bug in this port? Please report it in [our issues](https://github.com/Vladeezyy/scaling-health-1.21/issues),
not to the original author. Add the loader, the mod list and `logs/latest.log` (or the crash report).

## Credits

Scaling Health is by SilentChaos512 (and contributors); this port updates it. SilentLib / silent-utils helpers
(MIT, SilentChaos512) and EvalEx (MIT, Udo Klimaschewski) are included.

## License

**LGPL-3.0-only** ([LICENSE](LICENSE), GPL-3.0 text in [COPYING](licenses/COPYING)), Copyright (c) 2026 Vladeezyy.
The original Scaling Health, SilentLib / silent-utils and EvalEx are under the MIT License; the original license text
and copyright notice are kept in [LICENSE-MIT](licenses/LICENSE-MIT). Forks and modified versions must stay open under
the same license and credit the authors. Please download the mod only from the official release pages.

</details>

<details>
<summary><b>🇷🇺 Русский — нажмите, чтобы открыть</b></summary>

## О моде

Дополнительное здоровье и урон для игроков и мобов с гибко настраиваемой системой сложности:

- **Сложность** растёт со временем игры и с расстоянием от точки появления (пять режимов области), меняется от
  убийств, смертей и сна, от фазы луны, биома и измерения. Шкала показывает её (клавиша Z).
- **Мобы** с ростом сложности получают больше здоровья, урона и эффектов; некоторые становятся **блайтами**
  (фиолетовое пламя, намного сильнее, лучше добыча).
- **Игроки** поднимают максимальное здоровье **кристаллами сердца** (и уровнями опыта), а урон — **кристаллами силы**;
  бинты и аптечки лечат со временем; проклятые, зачарованные и случайные сердца меняют сложность.
- Компактный **HUD сердец**: цветные ряды вместо стены сердец, запасы сердец, щиты поглощения.
- Языки: английский, русский, украинский, немецкий, испанский, португальский (Бразилия); итальянский, польский,
  китайский и венесуэльский испанский из 8.0.2 (частично).
- Всё настраивается: конфиги клиента и сервера (в игре — список модов на NeoForge, Mod Menu на Fabric) и файлы
  датапака `sh_mechanics` (с выражениями).

Поведение как в 8.0.2; все отличия перечислены в [PORTLOG.md](PORTLOG.md).

## Требования

- **NeoForge** 21.1.197 или новее для Minecraft 1.21.1.
- **Fabric**: Fabric Loader 0.16.14+, Fabric API и Forge Config API Port (конфиги на API настроек NeoForge);
  Mod Menu для экрана настроек (по желанию).

Нужен на сервере и у игроков.

## Что исправлено в порте

Открытые issues оригинала:
- После возвращения из Энда максимальное здоровье и урон сбрасывались до базовых
  ([#458](https://github.com/SilentChaos512/ScalingHealth/issues/458)).
- Ошибка в файле `sh_mechanics` датапака ломала `/reload`, и в мир нельзя было зайти снова
  ([#438](https://github.com/SilentChaos512/ScalingHealth/issues/438),
  [#443](https://github.com/SilentChaos512/ScalingHealth/issues/443)): ошибка пишется в лог с именем файла, а
  используется файл самого мода.
- Мобы не накладывали эффекты (яд, иссушение) в режимах масштабирования урона, кроме `MAX_HEALTH`
  ([#441](https://github.com/SilentChaos512/ScalingHealth/issues/441)).
- Предупреждение о сне отключается ([#459](https://github.com/SilentChaos512/ScalingHealth/issues/459)): это
  `sleepWarningMessage` в `sh_mechanics/difficulty.json`; выделенный сервер оно больше не роняет.

Найдено в коде 8.0.2 при портировании: кристаллы сердца переставали работать на сервере, бонусная регенерация шла
вдвое чаще, кристаллы силы не выпадали, мобы заново обрабатывались при каждой загрузке, сердца за опыт не учитывали
сферы опыта, взвешенный и экстремальный режимы области, кристаллы тратились сверх лимита, краш при убийстве блайта
питомцем не игрока, имя босса-блайта, отладочный оверлей на выделенном сервере. Причины и исправления:
[PORTLOG.md](PORTLOG.md).

## Тестирование

22 GameTest на сервере каждого загрузчика (кристаллы, сложность, мобы, блайты, смерть и возвращение из Энда, Незер,
сломанные файлы датапака, добыча, руды, команды), их запускает CI при каждом пуше; релизные jar проверяются на
настоящих серверах NeoForge и Fabric.

## Сообщить об ошибке

Нашли ошибку в порте? Пишите в [наши issues](https://github.com/Vladeezyy/scaling-health-1.21/issues), а не автору
оригинала. Укажите загрузчик, список модов и `logs/latest.log` (или отчёт о краше).

## Авторы

Scaling Health — SilentChaos512 (и соавторы); порт только обновляет мод. Включены помощники SilentLib / silent-utils
(MIT, SilentChaos512) и EvalEx (MIT, Udo Klimaschewski).

## Лицензия

**LGPL-3.0-only** ([LICENSE](LICENSE), текст GPL-3.0 в [COPYING](licenses/COPYING)), Copyright (c) 2026 Vladeezyy.
Оригинальный Scaling Health, SilentLib / silent-utils и EvalEx под лицензией MIT; её текст и уведомление об авторских
правах сохранены в [LICENSE-MIT](licenses/LICENSE-MIT). Форки и изменённые версии должны оставаться открытыми под той
же лицензией и с указанием авторов. Скачивайте мод только с официальных страниц.

</details>
