package net.silentchaos512.scalinghealth.loot;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.silentchaos512.scalinghealth.objects.item.DifficultyMutatorItem;
import net.silentchaos512.scalinghealth.objects.item.PowerCrystal;
import net.silentchaos512.scalinghealth.utils.config.EnabledFeatures;

/**
 * What the "scalinghealth:table_loot_mod" loot modifier does (1.20.1 TableGlobalModifier.doApply): rolls another
 * loot table into the drops. NeoForge wraps this in a global loot modifier, Fabric applies the same modifier files in
 * LootTableEvents.MODIFY_DROPS.
 */
public final class TableGlobalModifier {
   private TableGlobalModifier() {}

   public static ObjectArrayList<ItemStack> doApply(ResourceKey<LootTable> table, ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
      // 1.20.1 LootTableReference.createItemStack (1.21 NestedLootTable)
      context.getResolver().get(Registries.LOOT_TABLE, table)
              .map(Holder::value)
              .orElse(LootTable.EMPTY)
              .getRandomItemsRaw(context, generatedLoot::add);
      generatedLoot.forEach(stack -> {
         // (sic) 1.20.1 removes power crystals when they ARE enabled
         if ((EnabledFeatures.powerCrystalEnabled() && stack.getItem() instanceof PowerCrystal)
                 || (!EnabledFeatures.difficultyEnabled() && stack.getItem() instanceof DifficultyMutatorItem)) {
            stack.setCount(0);
         }
      });
      return generatedLoot;
   }
}
