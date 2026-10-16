package net.susu609.royalarena.card;

/**
 * How a troop splits when it dies. Each generation has {@code children}
 * times more bodies; every child gets {@code healthFactor}, {@code
 * damageFactor} and {@code radiusFactor} of its parent's values, and nothing
 * splits past {@code generations}.
 */
public record SplitRule(int generations, int children, double healthFactor,
                        double damageFactor, double radiusFactor,
                        double spread, int invulnerableTicks)
{
}
