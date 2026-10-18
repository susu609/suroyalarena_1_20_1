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
    /** Range of 4 blocks or more (balance.json ranged). */
    public static final double RANGED = 1.25D;
    /** On top of {@link #RANGED}: each block of range past 4 is worth 4%. */
    public static final double RANGE_STEP = 0.04D;
    /** Pushing the victim back delays it and breaks its attack. */
    public static final double KNOCKBACK = 1.1D;
    /** Slowing what it hits. */
    public static final double SLOW = 1.15D;
    /** Dodging ranged shots and spells by teleporting (Enderman). */
    public static final double TELEPORT = 1.2D;
    /** Crossing the river anywhere (Blaze). */
    public static final double CROSSES_RIVER = 1.15D;
    /** Playable on the river too (Drowned). */
    public static final double DEPLOY_ON_RIVER = 1.05D;
    /** Takes extra damage from frost or water (Blaze). */
    public static final double WEAKNESS = 0.9D;
    /** Never frozen or slowed (Polar Bear). */
    public static final double FROST_IMMUNE = 1.05D;
    /** Hits every enemy on a line toward its target (Evoker). */
    public static final double LINE_DAMAGE = 1.25D;
    /** Locks on before hitting each new target (Guardian). */
    public static final double CHARGE = 0.85D;
    /** Hits harder from the river (Guardian). */
    public static final double RIVER_STRONG = 1.05D;

    private CostFormula() {
    }

    /** Unrounded cost; {@link #elixir} rounds it half up. */
    public static double raw(TroopStats s) {
        int generations = 1 + (s.split() == null ? 0 : s.split().generations());
        double totalHealth = s.effectiveHealth() * s.count() * generations;
        if (s.split() != null && s.split().into() != null)
            totalHealth = (s.effectiveHealth() +
                    s.split().into().effectiveHealth()) * s.count();
        // Poison lands with every hit, so it counts as part of the damage;
        // the next hit replaces it, so only what runs until then counts (all
        // of it for a unit that dies on its hit, like a Creeper).
        int poisonTicks = s.traits().selfDestruct() ? s.traits().poisonTicks() :
                Math.min(s.traits().poisonTicks(), s.attackTicks());
        double perHit = s.damage() +
                s.traits().poisonPerSecond() * poisonTicks / 20.0D;
        double dps = perHit / (s.attackTicks() / 20.0D) * s.count();
        double modifier = traitModifier(s);
        if (s.range() >= 4.0D)
            modifier *= RANGED * (1.0D + RANGE_STEP * (s.range() - 4.0D));
        if (s.speed() >= 1.33D)
            modifier *= 1.1D;
        if (s.count() >= 5)
            modifier *= 0.85D;
        else if (s.count() >= 3)
            modifier *= 0.93D;
        return Math.sqrt(totalHealth * dps) * modifier / COST_DIVISOR;
    }

    /**
     * The modifiers that come from the troop's traits, not from its range,
     * speed or count (the card config page recomputes those).
     */
    public static double traitModifier(TroopStats s) {
        double modifier = 1.0D;
        if (s.traits().splash())
            modifier *= AREA_DAMAGE;
        if (s.traits().selfDestruct())
            modifier *= SELF_DESTRUCT;
        if (s.traits().buildingsOnly())
            modifier *= BUILDINGS_ONLY;
        if (s.traits().knockback() > 0.0D)
            modifier *= KNOCKBACK;
        if (s.traits().slows())
            modifier *= SLOW;
        if (s.traits().teleports())
            modifier *= TELEPORT;
        if (s.traits().crossesRiver())
            modifier *= CROSSES_RIVER;
        if (s.traits().deployOnRiver())
            modifier *= DEPLOY_ON_RIVER;
        if (s.traits().frostWeak())
            modifier *= WEAKNESS;
        if (s.traits().waterWeak())
            modifier *= WEAKNESS;
        if (s.traits().frostImmune())
            modifier *= FROST_IMMUNE;
        if (s.traits().lines())
            modifier *= LINE_DAMAGE;
        if (s.traits().charges())
            modifier *= CHARGE;
        if (s.traits().riverStrong())
            modifier *= RIVER_STRONG;
        return modifier;
    }

    public static int elixir(TroopStats s) {
        return (int) Math.floor(raw(s) + 0.5D);
    }

    /**
     * Unrounded spell cost: 1 + value per target x radius / 20.8, where the
     * value is the damage (or healing) per target, burn excluded, plus
     * {@link SpellStats#controlValue} for freezing and stunning. A spell
     * that hits only a few targets counts a quarter of the circle per target
     * (up to all of it). Burn is a bonus on top, like the knockback.
     */
    public static double raw(SpellStats s) {
        double value = s.damagePerPulse() * s.pulses() + s.controlValue();
        double share = s.maxTargets() > 0 ?
                Math.min(1.0D, s.maxTargets() / 4.0D) : 1.0D;
        return 1.0D + value * s.radius() * share / SPELL_COST_DIVISOR;
    }

    public static int elixir(SpellStats s) {
        return (int) Math.floor(raw(s) + 0.5D);
    }
}
