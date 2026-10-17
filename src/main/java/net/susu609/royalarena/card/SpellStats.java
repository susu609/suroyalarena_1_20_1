package net.susu609.royalarena.card;

/**
 * What a spell card does where it lands, after {@code castTicks} (arrows in
 * flight, the fireball travelling). It then hits every enemy unit whose body
 * touches the circle of {@code radius} blocks, {@code pulses} times,
 * {@code pulseInterval} ticks apart, for {@code damagePerPulse} each. Towers
 * whose footprint touches the circle take {@code towerMultiplier} of that.
 * Units hit are pushed {@code knockback} blocks away from the centre and burn
 * for {@code burnTicks} ticks at {@code burnPerSecond}.
 */
public record SpellStats(double radius, int castTicks, int pulses,
                         int pulseInterval, double damagePerPulse,
                         double towerMultiplier, double knockback,
                         double burnPerSecond, int burnTicks)
{
    /** Spells deal this share of their damage to towers (balance.json). */
    public static final double TOWER_MULTIPLIER = 0.3D;

    /** Damage one unit takes if it stays inside for the whole spell. */
    public double damagePerTarget() {
        return damagePerPulse * pulses + burnPerSecond * burnTicks / 20.0D;
    }

    /** Ticks from the first pulse to the last. */
    public int durationTicks() {
        return (pulses - 1) * pulseInterval;
    }
}
