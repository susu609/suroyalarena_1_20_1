package net.susu609.royalarena.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.susu609.royalarena.network.s2c.S2CBurnPacket;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The burn of a Fireball, drawn instead of vanilla fire (which wraps the
 * whole mob): small flames licking the body, embers popping off and a little
 * smoke, and the body flashing red each second as the burn bites.
 */
public final class BurnEffects {
    private static final Map<Integer, Integer> BURNING = new HashMap<>();
    private static int clientTicks;

    private BurnEffects() {
    }

    public static void burn(S2CBurnPacket p) {
        BURNING.put(p.entityId(), clientTicks + p.ticks());
    }

    public static void clear() {
        BURNING.clear();
    }

    public static void tick() {
        clientTicks++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            BURNING.clear();
            return;
        }
        RandomSource random = mc.level.random;
        Iterator<Map.Entry<Integer, Integer>> it =
                BURNING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> entry = it.next();
            Entity entity = mc.level.getEntity(entry.getKey());
            if (entity == null || !entity.isAlive() ||
                    clientTicks > entry.getValue()) {
                it.remove();
                continue;
            }
            entity.clearFire();
            double w = entity.getBbWidth() * TroopVisuals.scale(entity);
            double h = entity.getBbHeight() * TroopVisuals.scale(entity);
            double x = entity.getX() + (random.nextDouble() - 0.5D) * w;
            double y = entity.getY() + random.nextDouble() * h * 0.8D;
            double z = entity.getZ() + (random.nextDouble() - 0.5D) * w;
            mc.level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0.0D,
                    0.02D, 0.0D);
            if (clientTicks % 6 == 0)
                mc.level.addParticle(ParticleTypes.LAVA, x, y, z, 0.0D, 0.0D,
                        0.0D);
            if (clientTicks % 4 == 0)
                mc.level.addParticle(ParticleTypes.SMOKE, x, y + 0.2D, z,
                        0.0D, 0.03D, 0.0D);
            if (clientTicks % 20 == 0 && entity instanceof LivingEntity living)
                living.hurtTime = 4;
        }
    }
}
