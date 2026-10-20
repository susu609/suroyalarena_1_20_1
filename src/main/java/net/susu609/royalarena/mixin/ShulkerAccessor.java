package net.susu609.royalarena.mixin;

import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Opens and shuts a shulker's shell (vanilla keeps the setter package-private). */
@Mixin(Shulker.class)
public interface ShulkerAccessor {
    @Invoker("setRawPeekAmount")
    void royalarena$setRawPeekAmount(int peek);
}
