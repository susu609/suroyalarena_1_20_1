package net.susu609.royalarena.card;

/**
 * How a troop splits when it dies. Each generation has {@code children}
 * times more bodies; every child gets {@code healthFactor}, {@code
 * damageFactor} and {@code radiusFactor} of its parent's values, and nothing
 * splits past {@code generations}.
 *
 * <p>With {@code into} set, the unit does not multiply: it leaves exactly one
 * unit with these stats where it died (Spider Jockey: the Skeleton Archer
 * that rode it), and the factors are not used.</p>
 */
public record SplitRule(int generations, int children, double healthFactor,
                        double damageFactor, double radiusFactor,
                        double spread, int invulnerableTicks, TroopStats into)
{
    public SplitRule(int generations, int children, double healthFactor,
            double damageFactor, double radiusFactor, double spread,
            int invulnerableTicks)
    {
        this(generations, children, healthFactor, damageFactor, radiusFactor,
                spread, invulnerableTicks, null);
    }

    /** One unit with these stats takes the dead unit's place. */
    public static SplitRule into(TroopStats rider) {
        return new SplitRule(1, 1, 1.0D, 1.0D, 1.0D, 0.0D, 6, rider);
    }
}
