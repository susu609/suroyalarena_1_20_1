package net.susu609.royalarena.card;

/**
 * Per-unit stats of a troop card, on the vanilla health scale (a Zombie has
 * 20). {@code count} units appear per card. Ranges are measured from the
 * edge of the attacker to the edge of its target, in blocks; speed is in
 * blocks per second. {@code armor} is in vanilla armour points (a Zombie
 * has 2): each point takes {@link #ARMOR_PER_POINT} off every hit and direct
 * spell damage, up to {@link #MAX_ARMOR} points; poison, burning and
 * withering go through it, as in vanilla.
 */
public record TroopStats(int count, double health, double damage,
                         int attackTicks, double range, double speed,
                         double radius, SplitRule split,
                         TroopTraits traits, double armor)
{
    public static final double DEFAULT_SIGHT = 5.5D;
    /** Damage one armour point takes off a hit (vanilla: 4%). */
    public static final double ARMOR_PER_POINT = 0.04D;
    /** Vanilla's cap: 20 points take off 80%. */
    public static final double MAX_ARMOR = 20.0D;

    public TroopStats(int count, double health, double damage,
            int attackTicks, double range, double speed, double radius,
            SplitRule split, TroopTraits traits)
    {
        this(count, health, damage, attackTicks, range, speed, radius, split,
                traits, 0.0D);
    }

    /** The same troop wearing {@code points} of armour. */
    public TroopStats withArmor(double points) {
        return new TroopStats(count, health, damage, attackTicks, range,
                speed, radius, split, traits, points);
    }

    /** Share of a hit that gets through the armour (1 = no armour). */
    public double armorFactor() {
        return 1.0D - ARMOR_PER_POINT * Math.min(MAX_ARMOR, Math.max(0.0D, armor));
    }

    /** Health counting the armour: what it takes to kill one unit with hits. */
    public double effectiveHealth() {
        return health / armorFactor();
    }

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
