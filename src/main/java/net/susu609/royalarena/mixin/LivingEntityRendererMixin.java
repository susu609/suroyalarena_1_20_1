package net.susu609.royalarena.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.susu609.royalarena.client.render.SpellTargetHighlighter;
import net.susu609.royalarena.client.render.TroopStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lights up white the whole model of a troop the selected spell would hit
 * ({@link SpellTargetHighlighter}), with vanilla's white overlay (the one a
 * Creeper flashes before it explodes). The red hurt tint still shows.
 *
 * <p>Also colours a troop's whole model by its state ({@link TroopStatus}:
 * frozen light blue, stunned yellow...) and draws an Illusioner's mirror
 * images see-through. These injections are optional ({@code require = 0}):
 * if a Forge update moves the code they hook, troops are just drawn plain.</p>
 */
@Mixin(LivingEntityRenderer.class) public abstract class LivingEntityRendererMixin {
    /** The troop being drawn (rendering runs on one thread). */
    @Unique
    private static LivingEntity royalarena$drawing;

    @Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
    private static void royalarena$spellTargetWhite(LivingEntity entity,
            float white, CallbackInfoReturnable<Integer> cir)
    {
        float lit = SpellTargetHighlighter.whiteness(entity);
        if (lit > white) {
            cir.setReturnValue(OverlayTexture.pack(OverlayTexture.u(lit),
                    OverlayTexture.v(entity.hurtTime > 0 ||
                            entity.deathTime > 0)));
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), require = 0)
    private void royalarena$begin(LivingEntity entity, float yaw, float partial,
            PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci)
    {
        royalarena$drawing = entity;
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"), require = 0)
    private void royalarena$end(LivingEntity entity, float yaw, float partial,
            PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci)
    {
        royalarena$drawing = null;
    }

    @Unique
    private static float royalarena$channel(float value, int channel) {
        LivingEntity entity = royalarena$drawing;
        return entity == null ? value :
                value * TroopStatus.color(entity)[channel];
    }

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            index = 4, require = 0)
    private float royalarena$red(float value) {
        return royalarena$channel(value, 0);
    }

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            index = 5, require = 0)
    private float royalarena$green(float value) {
        return royalarena$channel(value, 1);
    }

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            index = 6, require = 0)
    private float royalarena$blue(float value) {
        return royalarena$channel(value, 2);
    }

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            index = 7, require = 0)
    private float royalarena$alpha(float value) {
        return royalarena$channel(value, 3);
    }

    /** A mirror image or an invisible troop: a see-through render type. */
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true,
            require = 0)
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void royalarena$imageRenderType(LivingEntity entity,
            boolean bodyVisible, boolean translucent, boolean glowing,
            CallbackInfoReturnable<RenderType> cir)
    {
        if (bodyVisible && TroopStatus.seeThrough(entity))
            cir.setReturnValue(RenderType.itemEntityTranslucentCull(
                    ((EntityRenderer) (Object) this).getTextureLocation(entity)));
    }
}
