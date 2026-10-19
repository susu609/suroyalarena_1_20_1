package net.susu609.royalarena.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.susu609.royalarena.client.render.SpellTargetHighlighter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lights up white the whole model of a troop the selected spell would hit
 * ({@link SpellTargetHighlighter}), with vanilla's white overlay (the one a
 * Creeper flashes before it explodes). The red hurt tint still shows.
 */
@Mixin(LivingEntityRenderer.class) public abstract class LivingEntityRendererMixin {
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
}
