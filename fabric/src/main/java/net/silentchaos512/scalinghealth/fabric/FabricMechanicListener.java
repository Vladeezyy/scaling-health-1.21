package net.silentchaos512.scalinghealth.fabric;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.silentchaos512.scalinghealth.resources.mechanics.SHMechanicListener;

/** The sh_mechanics reload listener with the id Fabric's resource API wants. */
class FabricMechanicListener extends SHMechanicListener implements IdentifiableResourceReloadListener {
    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
}
