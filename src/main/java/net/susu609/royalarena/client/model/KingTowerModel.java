package net.susu609.royalarena.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.susu609.modlib.resource.RL;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.entity.building.KingTowerEntity;

public class KingTowerModel<T extends KingTowerEntity> extends EntityModel<T> {
    // This layer location should be baked with EntityRendererProvider
    // .Context in the entity renderer and passed into this model's constructor
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(
                    RL.of(RoyalArena.MOD_ID, "king_tower"),
                    "main");
    private final ModelPart stone_bricks_block;
    private final ModelPart stone_bricks_block2;
    private final ModelPart stone_bricks_block17;
    private final ModelPart stone_bricks_block5;
    private final ModelPart stone_bricks_block9;
    private final ModelPart stone_bricks_block13;
    private final ModelPart stone_bricks_block6;
    private final ModelPart stone_bricks_block10;
    private final ModelPart stone_bricks_block14;
    private final ModelPart stone_bricks_block3;
    private final ModelPart stone_bricks_block7;
    private final ModelPart stone_bricks_block11;
    private final ModelPart stone_bricks_block15;
    private final ModelPart stone_bricks_block4;
    private final ModelPart stone_bricks_block8;
    private final ModelPart stone_bricks_block12;
    private final ModelPart stone_bricks_block16;
    private final ModelPart bone;

    public KingTowerModel(ModelPart root) {
        this.stone_bricks_block = root.getChild("stone_bricks_block");
        this.stone_bricks_block2 = root.getChild("stone_bricks_block2");
        this.stone_bricks_block17 = root.getChild("stone_bricks_block17");
        this.stone_bricks_block5 = root.getChild("stone_bricks_block5");
        this.stone_bricks_block9 = root.getChild("stone_bricks_block9");
        this.stone_bricks_block13 = root.getChild("stone_bricks_block13");
        this.stone_bricks_block6 = root.getChild("stone_bricks_block6");
        this.stone_bricks_block10 = root.getChild("stone_bricks_block10");
        this.stone_bricks_block14 = root.getChild("stone_bricks_block14");
        this.stone_bricks_block3 = root.getChild("stone_bricks_block3");
        this.stone_bricks_block7 = root.getChild("stone_bricks_block7");
        this.stone_bricks_block11 = root.getChild("stone_bricks_block11");
        this.stone_bricks_block15 = root.getChild("stone_bricks_block15");
        this.stone_bricks_block4 = root.getChild("stone_bricks_block4");
        this.stone_bricks_block8 = root.getChild("stone_bricks_block8");
        this.stone_bricks_block12 = root.getChild("stone_bricks_block12");
        this.stone_bricks_block16 = root.getChild("stone_bricks_block16");
        this.bone = root.getChild("bone");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition stone_bricks_block =
                partdefinition.addOrReplaceChild(
                        "stone_bricks_block",
                        CubeListBuilder.create().texOffs(0, 34)
                                .addBox(
                                        -16.0F, -8.0F, -16.0F, 16.0F, 16.0F,
                                        16.0F, new CubeDeformation(0.0F)),
                        PartPose.offset(-8.0F, 16.0F, -8.0F));
        PartDefinition stone_bricks_block2 =
                partdefinition.addOrReplaceChild(
                        "stone_bricks_block2",
                        CubeListBuilder.create().texOffs(0, 34)
                                .addBox(
                                        -16.0F, -8.0F, -16.0F, 16.0F, 16.0F,
                                        16.0F, new CubeDeformation(0.0F)),
                        PartPose.offset(-8.0F, 16.0F, 8.0F));
        PartDefinition stone_bricks_block17 =
                partdefinition.addOrReplaceChild(
                        "stone_bricks_block17",
                        CubeListBuilder.create().texOffs(0, 34)
                                .addBox(
                                        -16.0F, -8.0F, -16.0F, 16.0F, 16.0F,
                                        16.0F, new CubeDeformation(0.0F)),
                        PartPose.offset(-8.0F, 16.0F, 24.0F));
        PartDefinition stone_bricks_block5 =
                partdefinition.addOrReplaceChild(
                        "stone_bricks_block5",
                        CubeListBuilder.create().texOffs(0, 34)
                                .addBox(
                                        -16.0F, -8.0F, -16.0F, 16.0F, 16.0F,
                                        16.0F, new CubeDeformation(0.0F)),
                        PartPose.offset(-8.0F, 0.0F, -8.0F));
        PartDefinition stone_bricks_block9 =
                partdefinition.addOrReplaceChild(
                        "stone_bricks_block9",
                        CubeListBuilder.create().texOffs(0, 34)
                                .addBox(
                                        -16.0F, -8.0F, -16.0F, 16.0F, 16.0F,
                                        16.0F, new CubeDeformation(0.0F)),
                        PartPose.offset(-8.0F, -16.0F, -8.0F));
        PartDefinition stone_bricks_block13 = partdefinition.addOrReplaceChild(
                "stone_bricks_block13", CubeListBuilder.create().texOffs(0, 34)
                        .addBox(
                                0.0F, -8.0F, -31.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                -16.0F, -8.0F, -31.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                -16.0F, -8.0F, -15.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                0.0F, -8.0F, 17.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                -16.0F, -8.0F, 17.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                -16.0F, -8.0F, 1.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(-16.0F, -32.0F, -1.0F));
        PartDefinition stone_bricks_block6 = partdefinition.addOrReplaceChild(
                "stone_bricks_block6", CubeListBuilder.create().texOffs(0, 34)
                        .addBox(
                                -16.0F, -8.0F, -16.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                -16.0F, -8.0F, 0.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(-8.0F, 0.0F, 8.0F));
        PartDefinition stone_bricks_block10 = partdefinition.addOrReplaceChild(
                "stone_bricks_block10", CubeListBuilder.create().texOffs(0, 34)
                        .addBox(
                                -16.0F, -8.0F, -16.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                -16.0F, -8.0F, 0.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(-8.0F, -16.0F, 8.0F));
        PartDefinition stone_bricks_block14 =
                partdefinition.addOrReplaceChild(
                        "stone_bricks_block14",
                        CubeListBuilder.create().texOffs(0, 34)
                                .addBox(
                                        -8.0F, -8.0F, -8.0F, 16.0F, 16.0F,
                                        16.0F, new CubeDeformation(0.0F)),
                        PartPose.offset(-16.0F, -32.0F, 15.0F));
        PartDefinition stone_bricks_block3 = partdefinition.addOrReplaceChild(
                "stone_bricks_block3", CubeListBuilder.create().texOffs(0, 34)
                        .addBox(
                                0.0F, -8.0F, -16.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                -16.0F, -8.0F, 0.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                0.0F, -8.0F, 0.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(8.0F, 16.0F, 8.0F));
        PartDefinition stone_bricks_block7 = partdefinition.addOrReplaceChild(
                "stone_bricks_block7", CubeListBuilder.create().texOffs(0, 34)
                        .addBox(
                                0.0F, -8.0F, -16.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                0.0F, -8.0F, 0.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                -16.0F, -8.0F, 0.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(8.0F, 0.0F, 8.0F));
        PartDefinition stone_bricks_block11 = partdefinition.addOrReplaceChild(
                "stone_bricks_block11", CubeListBuilder.create().texOffs(0, 34)
                        .addBox(
                                0.0F, -8.0F, -16.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                -16.0F, -8.0F, 0.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                0.0F, -8.0F, 0.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(8.0F, -16.0F, 8.0F));
        PartDefinition stone_bricks_block15 = partdefinition.addOrReplaceChild(
                "stone_bricks_block15", CubeListBuilder.create().texOffs(0, 34)
                        .addBox(
                                -8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -32.0F, 15.0F));
        PartDefinition stone_bricks_block4 = partdefinition.addOrReplaceChild(
                "stone_bricks_block4", CubeListBuilder.create().texOffs(0, 34)
                        .addBox(
                                -16.0F, -8.0F, -16.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                0.0F, -8.0F, -16.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(8.0F, 16.0F, -8.0F));
        PartDefinition stone_bricks_block8 = partdefinition.addOrReplaceChild(
                "stone_bricks_block8", CubeListBuilder.create().texOffs(0, 34)
                        .addBox(
                                -16.0F, -8.0F, -16.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                0.0F, -8.0F, -16.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(8.0F, 0.0F, -8.0F));
        PartDefinition stone_bricks_block12 = partdefinition.addOrReplaceChild(
                "stone_bricks_block12", CubeListBuilder.create().texOffs(0, 34)
                        .addBox(
                                -16.0F, -8.0F, -16.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                0.0F, -8.0F, -16.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(8.0F, -16.0F, -8.0F));
        PartDefinition stone_bricks_block16 = partdefinition.addOrReplaceChild(
                "stone_bricks_block16", CubeListBuilder.create().texOffs(0, 34)
                        .addBox(
                                0.0F, -8.0F, -31.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                16.0F, -8.0F, -31.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                16.0F, -8.0F, -15.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                16.0F, -8.0F, 17.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                16.0F, -8.0F, 1.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(0, 34)
                        .addBox(
                                0.0F, -8.0F, 17.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -32.0F, -1.0F));
        PartDefinition bone = partdefinition.addOrReplaceChild(
                "bone", CubeListBuilder.create().texOffs(16, 0)
                        .addBox(
                                -24.0F, 15.99F, -8.0F, 16.0F, 2.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(16, 0)
                        .addBox(
                                -8.0F, 15.99F, -8.0F, 16.0F, 2.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(16, 0)
                        .addBox(
                                8.0F, 15.99F, -8.0F, 16.0F, 2.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(16, 0)
                        .addBox(
                                -24.0F, 15.99F, -24.0F, 16.0F, 2.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(16, 0)
                        .addBox(
                                -8.0F, 15.99F, -24.0F, 16.0F, 2.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(16, 0)
                        .addBox(
                                8.0F, 15.99F, -24.0F, 16.0F, 2.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(16, 0)
                        .addBox(
                                8.0F, 15.99F, 8.0F, 16.0F, 2.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(16, 0)
                        .addBox(
                                -8.0F, 15.99F, 8.0F, 16.0F, 2.0F, 16.0F,
                                new CubeDeformation(0.0F)).texOffs(16, 0)
                        .addBox(
                                -24.0F, 15.99F, 8.0F, 16.0F, 2.0F, 16.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -56.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 128, 128);
    }
    @Override public void renderToBuffer(PoseStack poseStack,
            VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha)
    {
        stone_bricks_block.render(
                poseStack, vertexConsumer, packedLight, packedOverlay, red,
                green, blue, alpha);
        stone_bricks_block2.render(
                poseStack, vertexConsumer, packedLight, packedOverlay, red,
                green, blue, alpha);
        stone_bricks_block17.render(
                poseStack, vertexConsumer, packedLight, packedOverlay, red,
                green, blue, alpha);
        stone_bricks_block5.render(
                poseStack, vertexConsumer, packedLight, packedOverlay, red,
                green, blue, alpha);
        stone_bricks_block9.render(
                poseStack, vertexConsumer, packedLight, packedOverlay, red,
                green, blue, alpha);
        stone_bricks_block13.render(
                poseStack, vertexConsumer, packedLight, packedOverlay, red,
                green, blue, alpha);
        stone_bricks_block6.render(
                poseStack, vertexConsumer, packedLight, packedOverlay, red,
                green, blue, alpha);
        stone_bricks_block10.render(
                poseStack, vertexConsumer, packedLight, packedOverlay, red,
                green, blue, alpha);
        stone_bricks_block14.render(
                poseStack, vertexConsumer, packedLight, packedOverlay, red,
                green, blue, alpha);
        stone_bricks_block3.render(
                poseStack, vertexConsumer, packedLight, packedOverlay, red,
                green, blue, alpha);
        stone_bricks_block7.render(
                poseStack, vertexConsumer, packedLight, packedOverlay, red,
                green, blue, alpha);
        stone_bricks_block11.render(
                poseStack, vertexConsumer, packedLight, packedOverlay, red,
                green, blue, alpha);
        stone_bricks_block15.render(
                poseStack, vertexConsumer, packedLight, packedOverlay, red,
                green, blue, alpha);
        stone_bricks_block4.render(
                poseStack, vertexConsumer, packedLight, packedOverlay, red,
                green, blue, alpha);
        stone_bricks_block8.render(
                poseStack, vertexConsumer, packedLight, packedOverlay, red,
                green, blue, alpha);
        stone_bricks_block12.render(
                poseStack, vertexConsumer, packedLight, packedOverlay, red,
                green, blue, alpha);
        stone_bricks_block16.render(
                poseStack, vertexConsumer, packedLight, packedOverlay, red,
                green, blue, alpha);
        bone.render(
                poseStack, vertexConsumer, packedLight, packedOverlay, red,
                green, blue, alpha);
    }
    @Override public void setupAnim(KingTowerEntity entity, float limbSwing,
            float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch)
    {
    }
}