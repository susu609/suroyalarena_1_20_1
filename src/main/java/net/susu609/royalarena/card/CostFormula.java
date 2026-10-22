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
    /** Leaves burning patches behind it (Strider). */
    public static final double FIRE_TRAIL = 1.15D;
    /** Buildings (Shulker, End Crystal): never move, decay in 30 s. */
    public static final double BUILDING = 0.8D;

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
        // A burst (Piglin) fires its shots within one attack interval.
        double dps = perHit * s.traits().shots() / (s.attackTicks() / 20.0D) *
                s.count();
        // A jockey's rider fights from the saddle too, from the start.
        double range = s.range();
        if (s.split() != null && s.split().into() != null) {
            TroopStats rider = s.split().into();
            dps += rider.damage() / (rider.attackTicks() / 20.0D) * s.count();
            range = Math.max(range, rider.range());
        }
        double modifier = traitModifier(s);
        if (range >= 4.0D)
            modifier *= RANGED * (1.0D + RANGE_STEP * (range - 4.0D));
        if (s.speed() >= 1.33D)
            modifier *= 1.1D;
        // A building stands still and lives 30 s.
        if (s.speed() <= 0.0D)
            modifier *= BUILDING;
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
        if (s.traits().leavesFireTrail())
            modifier *= FIRE_TRAIL;
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

    /**
     * A card's own abilities, beyond its stats and traits (docs/CARDS.md):
     * what each is judged worth, as a cost multiplier. The Warden's Sonic
     * Boom counts as the extra damage per second it adds.
     */
    public static double abilityModifier(BattleCard card) {
        if (card == null || card.isSpell())
            return 1.0D;
        TroopStats s = card.stats();
        return switch (card) {
            // Keeps Creepers and Phantoms away: a hard counter.
            case CAT -> 1.3D;
            // Kills the smallest slimes and magma cubes in one lick.
            case FROG -> 1.2D;
            // Up to 60% faster swings as it bleeds.
            case ZOGLIN -> 1.15D;
            // A double-damage first blow with knockback.
            case CAMEL -> 1.1D;
            // Blows small enemies back every 4 s.
            case PANDA -> 1.1D;
            // Three times as fast on the river.
            case TURTLE -> 1.05D;
            // Lifts what it hits for 0.8 s; its shell blocks 40% of a hit.
            case SHULKER -> 1.4D;
            // The Sonic Boom's damage per second (through armour), and
            // hearing: silent troops and buildings cannot distract it.
            case WARDEN -> Math.sqrt(1.0D + BattleCard.BOOM_DAMAGE /
                    (BattleCard.BOOM_TICKS / 20.0D) /
                    (s.damage() / (s.attackTicks() / 20.0D))) * HEARING;
            // Three images that draw fire, and arrows that blind.
            case ILLUSIONER -> 1.25D;
            // Plays dead once and gets half its health back.
            case AXOLOTL -> 1.2D;
            // Gone after 10 s, but drags enemy Endermen off their target.
            case ENDERMITE -> LIFETIME_10S * 1.1D;
            // Its "damage" heals every own troop within 2 blocks.
            case ALLAY -> 2.0D;
            // Leaps onto an archer and splashes where it lands.
            case MAGMA_CUBE -> 1.2D;
            // Draws every troop's and tower's fire; its flowers heal.
            case SNIFFER -> TAUNT;
            default -> 1.0D;
        };
    }

    /** A troop that vanishes after 10 s does a share of its work. */
    public static final double LIFETIME_10S = 0.7D;

    /**
     * What a spell's own rule adds to its cost, in elixir (docs/BALANCE.md):
     * the Wither Skull's wither (its damage, and the healing it blocks), the
     * Ender Pearl's move, the Water Bucket's push, fire put out and the
     * damage to the few mobs water hurts (about a quarter of a crowd).
     */
    public static double spellAbility(BattleCard card) {
        SpellStats s = card.spell();
        return switch (card) {
            case WITHER_SKULL -> (BattleCard.WITHER_PER_SECOND *
                    BattleCard.WITHER_TICKS / 20.0D + HEAL_BLOCK_VALUE) *
                    s.radius() / SPELL_COST_DIVISOR;
            case ENDER_PEARL -> PEARL_VALUE;
            case WATER_BUCKET -> (s.knockback() * PUSH_VALUE_PER_BLOCK +
                    DOUSE_VALUE + BattleCard.WATER_DAMAGE * 0.25D) *
                    s.radius() / SPELL_COST_DIVISOR;
            // Half the armour ignored: on an average 2-point troop, 4% more.
            case ANVIL -> s.damagePerPulse() * HALF_ARMOR_GAIN *
                    s.radius() * 0.25D / SPELL_COST_DIVISOR;
            case DRAGONS_BREATH -> (s.damagePerPulse() * s.pulses() *
                    HALF_ARMOR_GAIN + HEAL_BLOCK_VALUE) * s.radius() /
                    SPELL_COST_DIVISOR;
            // Each enemy walks to the bell for 2 s instead of fighting; the
            // reveals (invisible, playing dead, images, Shadow) are a bonus.
            case BELL -> (BattleCard.BELL_LURE_TICKS / 20.0D *
                    SpellStats.SLOW_VALUE_PER_SECOND + REVEAL_VALUE) *
                    s.radius() / SPELL_COST_DIVISOR;
            default -> 0.0D;
        };
    }

    /** Blocking a target's healing for a few seconds, in damage. */
    public static final double HEAL_BLOCK_VALUE = 1.5D;
    /** Moving one troop 5 blocks (forward, or an enemy back), in elixir. */
    public static final double PEARL_VALUE = 1.0D;
    /** A block of push on one target, in damage. */
    public static final double PUSH_VALUE_PER_BLOCK = 2.0D;
    /** Putting one target's fire out, in damage. */
    public static final double DOUSE_VALUE = 1.0D;

    /** Going through half the armour, as extra damage on a 2-point troop. */
    public static final double HALF_ARMOR_GAIN = 0.04D;
    /** What a Bell's reveals are worth per target, in damage. */
    public static final double REVEAL_VALUE = 0.3D;
    /** Drawing every enemy's fire (the Sniffer). */
    public static final double TAUNT = 1.55D;
    /**
     * Chance the Villagers call their Golem (two of three must stand for
     * 6 s): what the called Golem counts for in their cost.
     */
    public static final double GOLEM_CALL_CHANCE = 0.5D;
    /** A Wandering Trader's body counts this much: it vanishes when hit. */
    public static final double TRADER_DECOY = 1.5D;

    /** Hearing only enemies that make a sound (the Warden). */
    public static final double HEARING = 1.3D;

    /** A troop card's unrounded cost: its stats, traits and abilities. */
    public static double raw(BattleCard card) {
        if (card.isSpell())
            return raw(card.spell()) + spellAbility(card);
        if (card.bringsLlamas())
            return traderRaw(card.stats());
        double raw = raw(card.stats()) * abilityModifier(card);
        // The Golem the Villagers may call, at its called health.
        if (card.callsGolem()) {
            TroopStats g = BattleCard.IRON_GOLEM.stats();
            raw += GOLEM_CALL_CHANCE * raw(new TroopStats(g.count(),
                    g.health() * BattleCard.CALLED_GOLEM_HEALTH, g.damage(),
                    g.attackTicks(), g.range(), g.speed(), g.radius(),
                    g.split(), g.traits(), g.armor()));
        }
        return raw;
    }

    /**
     * A Wandering Trader does not fight: its two llamas' fire, over their
     * bodies and its own (worth {@link #TRADER_DECOY} times its health),
     * with the llamas' range and slow.
     */
    private static double traderRaw(TroopStats trader) {
        TroopStats llama = BattleCard.TRADER_LLAMA;
        int n = BattleCard.TRADER_LLAMAS;
        double health = llama.effectiveHealth() * n +
                trader.effectiveHealth() * TRADER_DECOY;
        double dps = llama.damage() / (llama.attackTicks() / 20.0D) * n;
        double modifier = traitModifier(llama) * RANGED *
                (1.0D + RANGE_STEP * (llama.range() - 4.0D)) * 0.93D;
        return Math.sqrt(health * dps) * modifier / COST_DIVISOR;
    }

    public static int elixir(BattleCard card) {
        return (int) Math.floor(raw(card) + 0.5D);
    }

    public static int elixir(SpellStats s) {
        return (int) Math.floor(raw(s) + 0.5D);
    }
}
