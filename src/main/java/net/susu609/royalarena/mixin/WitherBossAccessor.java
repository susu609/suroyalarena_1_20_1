package net.susu609.royalarena.mixin;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The Wither card's boss bar, to hide it (a troop is no boss fight). */
@Mixin(WitherBoss.class)
public interface WitherBossAccessor {
    @Accessor("bossEvent")
    ServerBossEvent royalarena$bossEvent();
}
