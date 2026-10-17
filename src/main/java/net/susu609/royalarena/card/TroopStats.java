package net.susu609.royalarena.card;

/**
 * Per-unit stats of a troop card, on the vanilla health scale (a Zombie has
 * 20). {@code count} units appear per card. Ranges are measured from the
 * edge of the attacker to the edge of its target, in blocks; speed is in
 * blocks per second.
 */
public record TroopStats(int count, double health, double damage,
                         int attackTicks, double range, double speed,
                         double radius, SplitRule split,
                         TroopTraits traits)
{
    public static final double DEFAULT_SIGHT = 5.5D;

    /** Units notice enemies this far away (at least a little past range). */
    public double sight() {
        return Math.max(DEFAULT_SIGHT, range + 1.0D);
    }

    public double speedPerTick() {
        return speed / 20.0D;
    }

    public boolean melee() {
        return range < 3.0D;
    }

    public TroopStats(int count, double health, double damage,
            int attackTicks, double range, double speed, double radius)
    {
        this(count, health, damage, attackTicks, range, speed, radius, null,
                TroopTraits.NONE);
    }

    public TroopStats(int count, double health, double damage,
            int attackTicks, double range, double speed, double radius,
            SplitRule split)
    {
        this(count, health, damage, attackTicks, range, speed, radius, split,
                TroopTraits.NONE);
    }

    public TroopStats(int count, double health, double damage,
            int attackTicks, double range, double speed, double radius,
            TroopTraits traits)
    {
        this(count, health, damage, attackTicks, range, speed, radius, null,
                traits);
    }
}
