package net.susu609.royalarena.server.battle;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Drives the vanilla animations of NoAI troop mobs from what the combat
 * engine decides. Vanilla plays these from server-side state the AI would
 * normally set, so each one needs its own trigger:
 * <ul>
 * <li>attack poses (Zombie arms up, Vindicator axe, sword Skeletons) and the
 * bow aiming pose come from {@link Mob#setAggressive};</li>
 * <li>the bow draw is "using the bow item": started when a Skeleton or Stray
 * takes aim, released when its arrow flies;</li>
 * <li>the Iron Golem arm swing is entity event 4 (it ignores {@code
 * swing}), and a golem hit tosses the victim up a little;</li>
 * <li>a Slime only squishes when it leaves and lands on the ground, so it
 * moves in hops; it also hops when it attacks, and a Spider pounces.</li>
 * </ul>
 * Hops and tosses only change the height of a mob, never the position the
 * engine works with.
 */
final class TroopAnimator {
    /** Vertical speed of a golem toss, a slime attack bounce, a pounce. */
    private static final double TOSS = 0.45D;
    private static final double SQUISH_HOP = 0.25D;
    private static final double POUNCE = 0.22D;
    private static final double GRAVITY = 0.08D;

    private final Map<UUID, Double> lift = new HashMap<>();
    private final Map<UUID, Integer> groundTicks = new HashMap<>();

    /** Upward speed queued for this mob's next step (0 if none), consumed. */
    double takeLift(UUID id) {
        Double vy = lift.remove(id);
        return vy == null ? 0.0D : vy;
    }

    private void hop(Entity entity, double vy) {
        if (entity.onGround())
            lift.merge(entity.getUUID(), vy, Math::max);
    }

    /**
     * Horizontal factor for a slime's walk this tick: it waits on the ground
     * for a few ticks, jumps, and covers ground only while in the air, fast
     * enough that its average speed stays the engine's speed.
     */
    double slimeStride(Slime slime, boolean wantsToMove) {
        UUID id = slime.getUUID();
        double vy = jumpSpeed(slime);
        // Ticks in the air per hop (measured for this step physics: within
        // 2% of the engine speed for every slime size).
        double air = 2.0D * vy / GRAVITY + 3.0D;
        int wait = groundWait(slime);
        if (!slime.onGround()) {
            groundTicks.put(id, 0);
            return (air + wait) / air;
        }
        if (!wantsToMove) {
            groundTicks.put(id, 0);
            return 0.0D;
        }
        int ticks = groundTicks.merge(id, 1, Integer::sum);
        if (ticks >= wait) {
            groundTicks.put(id, 0);
            lift.merge(id, vy, Math::max);
            return (air + wait) / air;
        }
        return 0.0D;
    }

    /** Bigger slimes jump higher and wait longer between jumps. */
    private static double jumpSpeed(Slime slime) {
        return 0.36D + 0.04D * slime.getSize();
    }

    private static int groundWait(Slime slime) {
        return 4 + slime.getSize();
    }

    /**
     * Once per tick for every troop: the attack pose while it has an enemy
     * or stands attacking, and the bow draw for archers taking aim.
     */
    void updatePose(Entity entity, boolean hasTarget, boolean walking) {
        if (!(entity instanceof Mob mob))
            return;
        boolean aggressive = hasTarget || !walking;
        if (mob.isAggressive() != aggressive)
            mob.setAggressive(aggressive);
        if (!holdsBow(mob))
            return;
        if (aggressive && !mob.isUsingItem())
            mob.startUsingItem(InteractionHand.MAIN_HAND);
        else if (!aggressive && mob.isUsingItem())
            mob.stopUsingItem();
    }

    /** The attacker strikes or shoots. */
    void onAttack(Entity attacker) {
        if (attacker instanceof IronGolem golem) {
            golem.level().broadcastEntityEvent(golem, (byte) 4);
        } else if (attacker instanceof Slime) {
            hop(attacker, SQUISH_HOP);
        } else if (attacker instanceof Spider) {
            hop(attacker, POUNCE);
        } else if (attacker instanceof Mob mob && holdsBow(mob)) {
            // Let go of the string; the draw starts again next tick.
            mob.stopUsingItem();
        } else if (attacker instanceof LivingEntity living) {
            living.swing(InteractionHand.MAIN_HAND);
        }
    }

    /** A melee blow lands on {@code victim}. */
    void onHit(Entity attacker, Entity victim) {
        if (attacker instanceof IronGolem && victim != null)
            hop(victim, TOSS);
    }

    void forget(UUID id) {
        lift.remove(id);
        groundTicks.remove(id);
    }

    private static boolean holdsBow(Mob mob) {
        return mob.getMainHandItem().is(Items.BOW);
    }
}
