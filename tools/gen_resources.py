#!/usr/bin/env python3
"""Writes common/src/main/resources/{assets,data} from Scaling Health 8.0.2 (1.20.1), converted to 1.21.1.

    python3 -I tools/gen_resources.py [--original DIR] [--jar-dir DIR]

--original: the ScalingHealth checkout (branch 1.20, default ~/Desktop/mod-scout/src/ScalingHealth); its
src/main/resources and src/generated/resources are 8.0.0. --jar-dir: the unpacked 8.0.2 jar (default .ref/jar-8.0.2),
the source of the world generation files 8.0.1 changed. Never edit the output by hand: change this script.
"""
import argparse
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "common/src/main/resources"
EXTRA = ROOT / "tools/resources"  # port-only files (gametest structure, icon), copied as they are
LANG_EXTRA = ROOT / "tools/lang"  # port-only translations (the config screen), merged into the lang files

# 1.21 renamed the data folders (tags: singular registry names)
DATA_RENAMES = {
    "recipes": "recipe",
    "advancements": "advancement",
    "loot_tables": "loot_table",
    "tags/blocks": "tags/block",
    "tags/items": "tags/item",
    "tags/entity_types": "tags/entity_type",
}
# forge:* tags became c:* (the convention tags NeoForge and Fabric share)
TAG_RENAMES = {"forge:ingots/iron": "c:ingots/iron"}


def dump(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def walk(node, fn):
    """Applies fn to every dict of a JSON tree (bottom-up), fn returns the replacement."""
    if isinstance(node, dict):
        return fn({k: walk(v, fn) for k, v in node.items()})
    if isinstance(node, list):
        return [walk(v, fn) for v in node]
    return node


def convert_recipe(recipe):
    # result {"item": x, "count": n} -> {"id": x, "count": n}
    result = recipe.get("result")
    if isinstance(result, dict) and "item" in result:
        recipe["result"] = {"id": result["item"], **({"count": result["count"]} if "count" in result else {})}

    def tags(d):
        if "tag" in d and d["tag"] in TAG_RENAMES:
            d["tag"] = TAG_RENAMES[d["tag"]]
        return d
    return walk(recipe, tags)


def convert_loot(table):
    def fix(d):
        cond = d.get("condition")
        if cond == "minecraft:random_chance_with_looting":
            # vanilla's 1.21 data uses unenchanted_chance = chance, enchanted_chance = chance + multiplier * level
            chance, mult = d["chance"], d["looting_multiplier"]
            return {
                "condition": "minecraft:random_chance_with_enchanted_bonus",
                "enchantment": "minecraft:looting",
                "unenchanted_chance": chance,
                "enchanted_chance": {"type": "minecraft:linear", "base": round(chance + mult, 6), "per_level_above_first": mult},
            }
        if cond == "minecraft:match_tool" and "enchantments" in d.get("predicate", {}):
            # item predicate enchantments moved to the "minecraft:enchantments" sub-predicate
            pred = dict(d["predicate"])
            enchantments = [{"enchantments": e["enchantment"], **({"levels": e["levels"]} if "levels" in e else {})}
                            for e in pred.pop("enchantments")]
            pred["predicates"] = {"minecraft:enchantments": enchantments}
            return {**d, "predicate": pred}
        return d
    return walk(table, fix)


def convert_loot_modifier(modifier):
    def fix(d):
        if d.get("condition") == "forge:loot_table_id":
            d["condition"] = "neoforge:loot_table_id"
        return d
    return walk(modifier, fix)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--original", default=str(Path.home() / "Desktop/mod-scout/src/ScalingHealth"))
    ap.add_argument("--jar-dir", default=str(ROOT / ".ref/jar-8.0.2"))
    args = ap.parse_args()
    original = Path(args.original)
    sources = [original / "src/main/resources", original / "src/generated/resources"]
    jar = Path(args.jar_dir)

    for sub in ("assets", "data"):
        shutil.rmtree(OUT / sub, ignore_errors=True)

    count = 0
    for src in sources:
        for f in sorted(src.rglob("*")):
            if not f.is_file():
                continue
            rel = f.relative_to(src)
            parts = rel.parts
            if parts[0] not in ("assets", "data"):
                continue  # META-INF, pack.mcmeta
            if parts[0] == "assets":
                if parts[2] == "particles":
                    # the four sparkle frames, from the particle atlas (1.20.1 bound the png files while rendering)
                    dump(OUT / rel, {"textures": [f"scalinghealth:generic{i}" for i in range(4)]})
                elif f.suffix == ".json":
                    dump(OUT / rel, json.loads(f.read_text(encoding="utf-8")))
                else:
                    (OUT / rel).parent.mkdir(parents=True, exist_ok=True)
                    shutil.copy2(f, OUT / rel)
                count += 1
                continue

            # data/<namespace>/...
            namespace, path = parts[1], "/".join(parts[2:])
            if namespace == "forge" and path.startswith("loot_modifiers/"):
                namespace = "neoforge"
            if path.startswith("forge/biome_modifier/"):
                path = "neoforge/" + path[len("forge/"):]
            if path.startswith("worldgen/"):
                continue  # from the 8.0.2 jar below
            for old, new in DATA_RENAMES.items():
                if path.startswith(old + "/"):
                    path = new + path[len(old):]
                    break
            data = json.loads(f.read_text(encoding="utf-8"))
            if path.startswith("recipe/"):
                data = convert_recipe(data)
            elif path.startswith("loot_table/"):
                data = convert_loot(data)
            elif path.startswith("loot_modifiers/"):
                data = convert_loot_modifier(data)
            elif path.startswith("neoforge/biome_modifier/"):
                data["type"] = data["type"].replace("forge:", "neoforge:")
            dump(OUT / "data" / namespace / path, data)
            count += 1

    # 8.0.2 world generation (8.0.1: the deepslate heart crystal feature placed power crystal ore; more deepslate ores)
    for f in sorted((jar / "data/scalinghealth/worldgen").rglob("*.json")):
        dump(OUT / "data/scalinghealth/worldgen" / f.relative_to(jar / "data/scalinghealth/worldgen"),
             json.loads(f.read_text(encoding="utf-8")))
        count += 1

    for f in sorted(EXTRA.rglob("*")):
        if f.is_file():
            target = OUT / f.relative_to(EXTRA)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(f, target)
            count += 1

    lang_dir = OUT / "assets/scalinghealth/lang"
    for f in sorted(LANG_EXTRA.glob("*.json")):
        target = lang_dir / f.name
        data = json.loads(target.read_text(encoding="utf-8")) if target.exists() else {}
        data.update(json.loads(f.read_text(encoding="utf-8")))
        dump(target, data)

    print(f"wrote {count} files to {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
