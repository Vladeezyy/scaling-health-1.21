package net.silentchaos512.scalinghealth.fabric;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.loot.TableGlobalModifier;

import java.io.Reader;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nullable;

/**
 * Fabric has no global loot modifiers: this reads the same files as NeoForge (data/neoforge/loot_modifiers/
 * global_loot_modifiers.json and the "scalinghealth:table_loot_mod" files it lists) and applies them where NeoForge
 * does, at the end of LootTable.getRandomItems (LootTableMixin; Fabric API 1.21.1 has no drop event). The
 * "neoforge:loot_table_id" condition compares the id of the table being rolled.
 */
public final class FabricLootModifiers extends SimplePreparableReloadListener<List<FabricLootModifiers.Modifier>> implements IdentifiableResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final ResourceLocation LIST = ResourceLocation.fromNamespaceAndPath("neoforge", "loot_modifiers/global_loot_modifiers.json");
    private static final String TABLE_ID_CONDITION = "neoforge:loot_table_id";

    private static List<Modifier> modifiers = List.of();

    record Modifier(ResourceLocation id, List<Predicate<ResourceLocation>> tableConditions, List<LootItemCondition> conditions, ResourceKey<LootTable> table) {}

    @Override
    public ResourceLocation getFabricId() {
        return ScalingHealth.getId("loot_modifiers");
    }

    @Override
    protected List<Modifier> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        List<ResourceLocation> entries = new ArrayList<>();
        // NeoForge merges the lists of every pack ("replace": true clears the earlier ones)
        for (var resource : resourceManager.getResourceStack(LIST)) {
            try (Reader reader = resource.openAsReader()) {
                JsonObject json = GsonHelper.fromJson(GSON, reader, JsonObject.class);
                if (GsonHelper.getAsBoolean(json, "replace", false))
                    entries.clear();
                for (JsonElement entry : GsonHelper.getAsJsonArray(json, "entries")) {
                    ResourceLocation id = ResourceLocation.parse(entry.getAsString());
                    entries.remove(id);
                    entries.add(id);
                }
            } catch (Exception e) {
                ScalingHealth.LOGGER.error("Couldn't read {} from {}", LIST, resource.sourcePackId(), e);
            }
        }
        List<Modifier> result = new ArrayList<>();
        for (ResourceLocation id : entries) {
            ResourceLocation file = id.withPath(path -> "loot_modifiers/" + path + ".json");
            Optional<net.minecraft.server.packs.resources.Resource> resource = resourceManager.getResource(file);
            if (resource.isEmpty()) {
                ScalingHealth.LOGGER.warn("Loot modifier {} not found", id);
                continue;
            }
            try (Reader reader = resource.get().openAsReader()) {
                JsonObject json = GsonHelper.fromJson(GSON, reader, JsonObject.class);
                // only Scaling Health's own modifier type is applied here; other mods' files need NeoForge
                if (!GsonHelper.getAsString(json, "type").equals(ScalingHealth.MOD_ID + ":table_loot_mod"))
                    continue;
                List<Predicate<ResourceLocation>> tableConditions = new ArrayList<>();
                List<LootItemCondition> conditions = new ArrayList<>();
                for (JsonElement element : GsonHelper.getAsJsonArray(json, "conditions", new com.google.gson.JsonArray())) {
                    JsonObject condition = element.getAsJsonObject();
                    if (GsonHelper.getAsString(condition, "condition").equals(TABLE_ID_CONDITION)) {
                        ResourceLocation tableId = ResourceLocation.parse(GsonHelper.getAsString(condition, "loot_table_id"));
                        tableConditions.add(tableId::equals);
                    } else {
                        conditions.add(LootItemCondition.DIRECT_CODEC.parse(JsonOps.INSTANCE, condition).getOrThrow());
                    }
                }
                ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(GsonHelper.getAsString(json, "table")));
                result.add(new Modifier(id, tableConditions, conditions, table));
            } catch (Exception e) {
                ScalingHealth.LOGGER.error("Couldn't load loot modifier {}", id, e);
            }
        }
        return result;
    }

    @Override
    protected void apply(List<Modifier> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        modifiers = List.copyOf(prepared);
    }

    private static ReloadableServerRegistries.Holder idsFrom;
    private static Map<LootTable, ResourceLocation> ids = Map.of();

    /** The id of a loaded loot table (NeoForge keeps it in the table; vanilla doesn't). */
    @Nullable
    private static ResourceLocation idOf(LootTable table, LootContext context) {
        ReloadableServerRegistries.Holder registries = context.getLevel().getServer().reloadableRegistries();
        if (registries != idsFrom) {
            Map<LootTable, ResourceLocation> map = new IdentityHashMap<>();
            for (ResourceLocation id : registries.getKeys(Registries.LOOT_TABLE))
                map.put(registries.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id)), id);
            ids = map;
            idsFrom = registries;
        }
        return ids.get(table);
    }

    /** NeoForge's CommonHooks.modifyLoot: each modifier whose conditions pass changes the drops (LootModifier.apply). */
    public static ObjectArrayList<ItemStack> modifyLoot(LootTable table, ObjectArrayList<ItemStack> drops, LootContext context) {
        if (modifiers.isEmpty()) return drops;
        ResourceLocation tableId = idOf(table, context);
        for (Modifier modifier : modifiers) {
            if (!modifier.tableConditions().stream().allMatch(c -> tableId != null && c.test(tableId)))
                continue;
            if (!modifier.conditions().stream().allMatch(c -> c.test(context)))
                continue;
            drops = TableGlobalModifier.doApply(modifier.table(), drops, context);
        }
        return drops;
    }
}
