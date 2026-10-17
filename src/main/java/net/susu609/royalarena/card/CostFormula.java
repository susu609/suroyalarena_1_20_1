package net.susu609.royalarena.card;

/**
 * Elixir cost derived from stats (docs/BALANCE.md). Constants match
 * data/royalarena/balance.json.
 */
public final class CostFormula {
    public static final double COST_DIVISOR = 5.4D;
    public static final double SPELL_COST_DIVISOR = 20.8D;
    /** Splash damage is worth more (balance.json cost_modifiers). */
    public static final double AREA_DAMAGE = 1.3D;
    /** A unit that dies on its first strike delivers its damage once. */
    public static final double SELF_DESTRUCT = 0.5D;
    /** A unit that only hits towers cannot defend, so it is cheaper. */
    public static final double BUILDINGS_ONLY = 0.85D;

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
        if (s.traits().splash())
            modifier *= AREA_DAMAGE;
        if (s.traits().selfDestruct())
            modifier *= SELF_DESTRUCT;
        if (s.traits().buildingsOnly())
            modifier *= BUILDINGS_ONLY;
        return Math.sqrt(totalHealth * dps) * modifier / COST_DIVISOR;
    }

    public static int elixir(TroopStats s) {
        return (int) Math.floor(raw(s) + 0.5D);
    }

    /**
     * Unrounded spell cost: 1 + damage per target (burn excluded) x radius /
     * 20.8. Burn is a bonus on top, like the knockback.
     */
    public static double raw(SpellStats s) {
        return 1.0D + s.damagePerPulse() * s.pulses() * s.radius() /
                SPELL_COST_DIVISOR;
    }

    public static int elixir(SpellStats s) {
        return (int) Math.floor(raw(s) + 0.5D);
    }
}
