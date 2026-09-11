package net.susu609.royalarena.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Arena troops follow arena combat rules and must not inherit vanilla daylight
 * burning. This changes only the sunlight check; normal fire/lava/spell damage
 * remains untouched.
 */
@Mixin(Mob.class) public abstract class MobSunBurnMixin {
    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void royalarena$disableSunBurnForArenaTroops(
            CallbackInfoReturnable<Boolean> cir)
    {
        Mob mob = (Mob) (Object) this;
        if (mob.getTags().contains("royalarena_troop")) {
            cir.setReturnValue(false);
        }
    }
}
