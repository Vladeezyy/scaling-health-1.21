package net.silentchaos512.scalinghealth.neoforge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.silentchaos512.scalinghealth.loot.TableGlobalModifier;

/** "scalinghealth:table_loot_mod" (1.20.1 TableGlobalModifier): {"table": loot table id} plus the conditions. */
public class NeoTableLootModifier extends LootModifier {
    public static final MapCodec<NeoTableLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst).and(
                    ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("table").forGetter(m -> m.table)
            ).apply(inst, NeoTableLootModifier::new));

    private final ResourceKey<LootTable> table;

    public NeoTableLootModifier(LootItemCondition[] conditions, ResourceKey<LootTable> table) {
        super(conditions);
        this.table = table;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        return TableGlobalModifier.doApply(table, generatedLoot, context);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
