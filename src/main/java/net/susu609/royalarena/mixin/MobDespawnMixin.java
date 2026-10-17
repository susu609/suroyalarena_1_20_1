package net.susu609.royalarena.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Arena troops live as long as the combat engine says. Vanilla would discard
 * every hostile troop on peaceful difficulty (and far from players), which
 * left the engine fighting with units that no longer exist.
 */
@Mixin(Mob.class) public abstract class MobDespawnMixin {
    @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
    private void royalarena$keepArenaTroops(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (mob.getTags().contains("royalarena_troop"))
            ci.cancel();
    }
}
