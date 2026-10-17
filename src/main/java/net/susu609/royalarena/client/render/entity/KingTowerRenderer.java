package net.susu609.royalarena.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.susu609.modlib.resource.RL;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.client.model.KingTowerModel;
import net.susu609.royalarena.client.render.WorldHealthBar;
import net.susu609.royalarena.entity.building.KingTowerEntity;

public final class KingTowerRenderer extends EntityRenderer<KingTowerEntity> {
    private static final ResourceLocation TEXTURE =
            RL.of(RoyalArena.MOD_ID, "textures/entity/tower/tower_blocks.png");

    private final KingTowerModel<KingTowerEntity> model;
    private final KingFigure king;

    public KingTowerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new KingTowerModel<>(
                context.bakeLayer(KingTowerModel.LAYER_LOCATION));
        this.king = new KingFigure(context);
        this.shadowRadius = 2.0F;
    }

    @Override public void render(KingTowerEntity entity, float entityYaw,
            float partialTick, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight)
    {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        VertexConsumer consumer = buffer.getBuffer(model.renderType(TEXTURE));
        model.setupAnim(
                entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        model.renderToBuffer(
                poseStack, consumer, packedLight,
                WorldHealthBar.towerOverlay(entity), 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        king.render(entity, entityYaw, partialTick, poseStack, buffer,
                packedLight);
        WorldHealthBar.renderTower(entity, poseStack, buffer);
        super.render(
                entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(KingTowerEntity entity) {
        return TEXTURE;
    }
}
