package net.susu609.royalarena.client.render;

import net.minecraft.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.combat.TroopFlags;

import java.util.HashMap;
import java.util.Map;

/**
 * What the server says about each troop's state (S2CTroopStatePacket), and
 * how it is drawn: a colour over the whole model (light blue frozen, pale
 * yellow stunned, green poisoned, grey withered, warm gold hastened, a gold
 * shimmer while invulnerable, purple for a raging Zoglin, lilac while
 * Dragon's Breath blocks healing), a mirror image see-through, an invisible
 * troop almost gone, a lifted troop drawn higher. Vanilla's red hurt tint still
 * shows over the colour.
 */
public final class TroopStatus {
    private static final Map<Integer, Integer> FLAGS = new HashMap<>();
    /** How high a lifted troop is drawn, in blocks. */
    private static final double LIFT = 0.6D;
    /** A raging Zoglin grows up to this much bigger. */
    private static final float RAGE_GROWTH = 0.25F;
    /** See-through share of a mirror image. */
    private static final float IMAGE_ALPHA = 0.45F;
    /** What is left to see of an invisible troop. */
    private static final float HIDDEN_ALPHA = 0.18F;

    private TroopStatus() {
    }

    public static void set(int entityId, int flags) {
        if (flags == 0)
            FLAGS.remove(entityId);
        else
            FLAGS.put(entityId, flags);
    }

    /** Once per client tick: forget everything outside a match. */
    public static void tick() {
        if (!ClientMatchState.active())
            FLAGS.clear();
    }

    public static int flags(Entity entity) {
        return FLAGS.getOrDefault(entity.getId(), 0);
    }

    public static boolean has(Entity entity, int flag) {
        return TroopFlags.has(flags(entity), flag);
    }

    /** A mirror image of an Illusioner, drawn see-through. */
    public static boolean illusion(Entity entity) {
        return has(entity, TroopFlags.ILLUSION);
    }

    /** Drawn see-through: a mirror image, or an invisible troop. */
    public static boolean seeThrough(Entity entity) {
        return has(entity, TroopFlags.ILLUSION) || has(entity, TroopFlags.HIDDEN);
    }

    /** How far up to draw the troop (a Shulker bullet lifts it). */
    public static double lift(Entity entity) {
        return has(entity, TroopFlags.LEVITATING) ? LIFT : 0.0D;
    }

    /**
     * A Zoglin's rage, 0 to 1: as in the engine, full at a quarter of its
     * health or less.
     */
    public static float rage(Entity entity) {
        if (!(entity instanceof LivingEntity living) ||
                TroopVisuals.cardOf(entity) != BattleCard.ZOGLIN ||
                living.getMaxHealth() <= 0.0F)
            return 0.0F;
        float lost = 1.0F - living.getHealth() / living.getMaxHealth();
        return Math.max(0.0F, Math.min(1.0F, lost / 0.75F));
    }

    /** Extra size: a raging Zoglin grows. */
    public static float growth(Entity entity) {
        return 1.0F + RAGE_GROWTH * rage(entity);
    }

    /**
     * Red, green, blue and alpha to multiply the model by. The strongest
     * state wins: stunned, frozen, withered, poisoned, hastened, then rage;
     * invulnerability shimmers gold on top.
     */
    public static float[] color(LivingEntity entity) {
        int flags = flags(entity);
        float r = 1.0F;
        float g = 1.0F;
        float b = 1.0F;
        if (TroopFlags.has(flags, TroopFlags.STUNNED)) {
            r = 1.0F; g = 1.0F; b = 0.45F;
        } else if (TroopFlags.has(flags, TroopFlags.FROZEN)) {
            r = 0.55F; g = 0.8F; b = 1.0F;
        } else if (TroopFlags.has(flags, TroopFlags.WITHERED)) {
            r = 0.45F; g = 0.42F; b = 0.45F;
        } else if (TroopFlags.has(flags, TroopFlags.POISONED)) {
            r = 0.6F; g = 1.0F; b = 0.5F;
        } else if (TroopFlags.has(flags, TroopFlags.HASTED)) {
            r = 1.0F; g = 0.92F; b = 0.6F;
        } else if (TroopFlags.has(flags, TroopFlags.HEAL_BLOCKED)) {
            r = 0.85F; g = 0.6F; b = 1.0F;
        } else {
            float rage = rage(entity);
            r = 1.0F - 0.15F * rage;
            g = 1.0F - 0.5F * rage;
        }
        if (TroopFlags.has(flags, TroopFlags.INVULNERABLE)) {
            float pulse = 0.5F + 0.5F * (float) Math.sin(
                    (Util.getMillis() % 400L) / 400.0D * Math.PI * 2.0D);
            g *= 1.0F - 0.08F * pulse;
            b *= 1.0F - 0.45F * pulse;
        }
        float a = TroopFlags.has(flags, TroopFlags.HIDDEN) ? HIDDEN_ALPHA :
                TroopFlags.has(flags, TroopFlags.ILLUSION) ? IMAGE_ALPHA : 1.0F;
        return new float[] {r, g, b, a};
    }
}
