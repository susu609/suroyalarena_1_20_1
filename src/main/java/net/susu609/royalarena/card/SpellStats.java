package net.susu609.royalarena.card;

/**
 * What a spell card does where it lands, after {@code castTicks} (arrows in
 * flight, the fireball travelling). It then hits every enemy unit whose body
 * touches the circle of {@code radius} blocks, {@code pulses} times,
 * {@code pulseInterval} ticks apart, for {@code damagePerPulse} each. Towers
 * whose footprint touches the circle take {@code towerMultiplier} of that.
 * Units hit are pushed {@code knockback} blocks away from the centre and burn
 * for {@code burnTicks} ticks at {@code burnPerSecond}.
 *
 * @param maxTargets  only this many targets with the most health are hit
 *                    (Lightning); 0 = everything in the circle
 * @param stunTicks   targets hit stop and restart their attack (Lightning)
 * @param freezeTicks targets hit, towers too, cannot move or attack for this
 *                    long (Freeze)
 * @param heal        the spell heals its caster's own troops by
 *                    {@code damagePerPulse} each pulse instead of hurting
 *                    the enemy (Potion of Healing)
 * @param middleReach the spell can be cast only on the caster's own half
 *                    and the river up to the middle of the bridge, not on
 *                    the enemy half (TNT)
 */
public record SpellStats(double radius, int castTicks, int pulses,
                         int pulseInterval, double damagePerPulse,
                         double towerMultiplier, double knockback,
                         double burnPerSecond, int burnTicks, int maxTargets,
                         int stunTicks, int freezeTicks, boolean heal,
                         boolean middleReach)
{
    /** Spells deal this share of their damage to towers (balance.json). */
    public static final double TOWER_MULTIPLIER = 0.3D;
    /** A second of a target frozen or stunned counts as this much damage. */
    public static final double CONTROL_VALUE_PER_SECOND = 2.0D;

    public SpellStats(double radius, int castTicks, int pulses,
            int pulseInterval, double damagePerPulse, double towerMultiplier,
            double knockback, double burnPerSecond, int burnTicks, int maxTargets,
            int stunTicks, int freezeTicks, boolean heal)
    {
        this(radius, castTicks, pulses, pulseInterval, damagePerPulse,
                towerMultiplier, knockback, burnPerSecond, burnTicks,
                maxTargets, stunTicks, freezeTicks, heal, false);
    }

    public SpellStats(double radius, int castTicks, int pulses,
            int pulseInterval, double damagePerPulse, double towerMultiplier,
            double knockback, double burnPerSecond, int burnTicks)
    {
        this(radius, castTicks, pulses, pulseInterval, damagePerPulse,
                towerMultiplier, knockback, burnPerSecond, burnTicks, 0, 0, 0,
                false);
    }

    /**
     * Damage one unit takes if it stays inside for the whole spell (health
     * it gets back for a healing spell).
     */
    public double damagePerTarget() {
        return damagePerPulse * pulses + burnPerSecond * burnTicks / 20.0D;
    }

    /** What freezing or stunning one target is worth, in damage. */
    public double controlValue() {
        return (stunTicks + freezeTicks) / 20.0D * CONTROL_VALUE_PER_SECOND;
    }

    /** Ticks from the first pulse to the last. */
    public int durationTicks() {
        return (pulses - 1) * pulseInterval;
    }
}
