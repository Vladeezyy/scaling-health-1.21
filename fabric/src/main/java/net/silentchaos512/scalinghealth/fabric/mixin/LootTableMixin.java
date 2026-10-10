package net.silentchaos512.scalinghealth.fabric.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.silentchaos512.scalinghealth.fabric.FabricLootModifiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Where NeoForge applies the global loot modifiers (CommonHooks.modifyLoot): the loot of every rolled table. */
@Mixin(LootTable.class)
public abstract class LootTableMixin {
    @Inject(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;", at = @At("RETURN"), cancellable = true)
    private void scalinghealth$modifyLoot(LootContext context, CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
        cir.setReturnValue(FabricLootModifiers.modifyLoot((LootTable) (Object) this, cir.getReturnValue(), context));
    }
}
