package net.susu609.royalarena.card;

/**
 * Elixir cost derived from stats (docs/BALANCE.md). Constants match
 * data/royalarena/balance.json.
 */
public final class CostFormula {
    public static final double COST_DIVISOR = 5.4D;

    private CostFormula() {
    }

    /** Unrounded cost; {@link #elixir} rounds it half up. */
    public static double raw(TroopStats s) {
        int generations = 1 + (s.split() == null ? 0 : s.split().generations());
        double totalHealth = s.health() * s.count() * generations;
        double dps = s.damage() / (s.attackTicks() / 20.0D) * s.count();
        double modifier = 1.0D;
        if (s.range() >= 4.0D)
            modifier *= 1.25D;
        if (s.speed() >= 1.33D)
            modifier *= 1.1D;
        if (s.count() >= 5)
            modifier *= 0.85D;
        else if (s.count() >= 3)
            modifier *= 0.93D;
        return Math.sqrt(totalHealth * dps) * modifier / COST_DIVISOR;
    }

    public static int elixir(TroopStats s) {
        return (int) Math.floor(raw(s) + 0.5D);
    }
}
