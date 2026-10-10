package net.silentchaos512.scalinghealth.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.silentchaos512.scalinghealth.client.gui.health.HeartDisplayHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scaling Health's hearts in place of vanilla's (1.20.1 canceled Forge's player health overlay). Vanilla draws the
 * armor row before the hearts, with the heart row count; it is moved to where 1.20.1's leftHeight put it.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
    /** The left status bars start 39 pixels above the bottom (1.20.1 ForgeGui.leftHeight). */
    @Unique
    private static final int scalinghealth$BASE_HEIGHT = 39;

    @Unique
    private boolean scalinghealth$replace;
    @Unique
    private int scalinghealth$height;

    @Shadow
    private Player getCameraPlayer() {
        throw new AssertionError();
    }

    @Inject(method = "renderPlayerHealth", at = @At("HEAD"))
    private void scalinghealth$measure(GuiGraphics graphics, CallbackInfo ci) {
        scalinghealth$replace = getCameraPlayer() != null && HeartDisplayHandler.INSTANCE.replacesHearts();
        if (scalinghealth$replace)
            scalinghealth$height = HeartDisplayHandler.INSTANCE.measure();
    }

    @WrapOperation(method = "renderPlayerHealth", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;renderArmor(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIII)V"))
    private void scalinghealth$moveArmor(GuiGraphics graphics, Player player, int y, int heartRows, int height, int x, Operation<Void> original) {
        if (scalinghealth$replace) {
            // as NeoForge's renderArmorLevel: one row, at the top of the left bars
            original.call(graphics, player, graphics.guiHeight() - (scalinghealth$BASE_HEIGHT + scalinghealth$height) + 10, 1, 0, x);
        } else {
            original.call(graphics, player, y, heartRows, height, x);
        }
    }

    @WrapOperation(method = "renderPlayerHealth", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;renderHearts(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V"))
    private void scalinghealth$hearts(Gui gui, GuiGraphics graphics, Player player, int x, int y, int height, int offsetHeartIndex,
                                     float maxHealth, int currentHealth, int displayHealth, int absorptionAmount, boolean renderHighlight,
                                     Operation<Void> original) {
        if (scalinghealth$replace) {
            HeartDisplayHandler.INSTANCE.render(graphics, player, scalinghealth$BASE_HEIGHT);
        } else {
            original.call(gui, graphics, player, x, y, height, offsetHeartIndex, maxHealth, currentHealth, displayHealth, absorptionAmount, renderHighlight);
        }
    }
}
