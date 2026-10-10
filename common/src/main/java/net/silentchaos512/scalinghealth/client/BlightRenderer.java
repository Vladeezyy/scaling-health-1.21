package net.silentchaos512.scalinghealth.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.utils.config.EnabledFeatures;
import net.silentchaos512.scalinghealth.utils.config.SHDifficulty;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * The purple flames on blights (1.20.1 BlightRenderEvent, on RenderLivingEvent). Called after the entity is drawn,
 * with the entity renderer's pose (NeoForge: RenderLivingEvent.Post, Fabric: LivingEntityRendererMixin).
 */
public final class BlightRenderer {
   private static final float FIRE_SCALE = 1.8F;
   private static final ResourceLocation TEXTURE = ScalingHealth.getId("textures/entity/blightfire.png");
   private static RenderType RENDER_TYPE;

   private BlightRenderer() {}

   public static void renderBlight(LivingEntity entity, PoseStack stack, MultiBufferSource buffers, int light) {
      if (RENDER_TYPE == null)
         RENDER_TYPE = RenderType.entityCutout(TEXTURE);

      if(EnabledFeatures.shouldRenderBlights() && entity instanceof Mob mob && SHDifficulty.affected(entity).isBlight()){
         stack.pushPose();
         float w = mob.getBbWidth() * FIRE_SCALE;
         stack.scale(w, w, w);
         float hwRatio = mob.getBbHeight() / w;
         float xOffset = 0.5F;
         float yOffset = (float) (mob.getY() - mob.getBoundingBox().minY);
         float zOffset = 0.0F;
         Quaternionf cam = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
         stack.mulPose(new Quaternionf(0, cam.y, 0, cam.w));
         // 1.20.5 turned the camera orientation half a turn (vanilla name tags flipped their x scale then); without
         // this the flames face away from the camera and are culled
         stack.mulPose(Axis.YP.rotationDegrees(180f));
         stack.translate(0, 0, hwRatio * 0.02f);
         int i = 0;

         VertexConsumer vertexBuilder = buffers.getBuffer(RENDER_TYPE);
         PoseStack.Pose pose = stack.last();
         Matrix4f matrix4f = pose.pose();

         while (hwRatio > 0.0F) {
            boolean swapU = i % 2 == 0;
            int frame = ClientTicks.ticksInGame() % 32;
            float minU = swapU ? 0.5f : 0.0f;
            float minV = frame / 32f;
            float maxU = swapU ? 1.0f : 0.5f;
            float maxV = (frame + 1) / 32f;

            if (swapU) {
               float swap = maxU;
               maxU = minU;
               minU = swap;
            }
            vertexBuilder.addVertex(matrix4f, xOffset,0.0F - yOffset, zOffset).setColor(255,255,255,255).setUv(maxU, maxV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
            vertexBuilder.addVertex(matrix4f,-xOffset,0.0F - yOffset, zOffset).setColor(255,255,255,255).setUv(minU, maxV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
            vertexBuilder.addVertex(matrix4f,-xOffset,1.4F - yOffset, zOffset).setColor(255,255,255,255).setUv(minU, minV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
            vertexBuilder.addVertex(matrix4f, xOffset,1.4F - yOffset, zOffset).setColor(255,255,255,255).setUv(maxU, minV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
            hwRatio -= 0.45F;
            yOffset -= 0.45F;
            xOffset *= 0.9F;
            zOffset += 0.03F;
            ++i;
         }
         stack.popPose();
      }
   }
}
