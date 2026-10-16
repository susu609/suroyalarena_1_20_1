package net.susu609.royalarena.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.susu609.modlib.resource.RL;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.client.model.CrownTowerModel;
import net.susu609.royalarena.client.render.WorldHealthBar;
import net.susu609.royalarena.entity.building.CrownTowerEntity;
import net.susu609.royalarena.entity.building.TowerVisualType;

public final class CrownTowerRenderer extends EntityRenderer<CrownTowerEntity> {
    private static final ResourceLocation TEXTURE =
            RL.of(RoyalArena.MOD_ID, "textures/entity/tower/tower_blocks.png");

    private final CrownTowerModel<CrownTowerEntity> model;

    public CrownTowerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new CrownTowerModel<>(
                context.bakeLayer(CrownTowerModel.LAYER_LOCATION));
        this.shadowRadius = 2.0F;
    }

    @Override public void render(CrownTowerEntity entity, float entityYaw,
            float partialTick, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight)
    {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        float scaleXZ = TowerVisualType.CROWN.modelScaleXZ();
        poseStack.scale(scaleXZ, 1.0F, scaleXZ);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        VertexConsumer consumer = buffer.getBuffer(model.renderType(TEXTURE));
        model.setupAnim(
                entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        model.renderToBuffer(
                poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        WorldHealthBar.renderTower(entity, poseStack, buffer);
        super.render(
                entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CrownTowerEntity entity) {
        return TEXTURE;
    }
}
