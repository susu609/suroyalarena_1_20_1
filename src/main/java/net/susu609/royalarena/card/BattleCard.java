package net.susu609.royalarena.card;

import java.util.Locale;

/**
 * Troop and spell cards. Troop Stats mirror data/royalarena/cards/*.json (a test
 * keeps them in sync) and every cost follows {@link CostFormula}. A spell
 * has {@link #spell()} instead of {@link #stats()} and no mob.
 */
public enum BattleCard {
    ZOMBIE("Zombie", 3, "minecraft:zombie", null, 0,
            new TroopStats(2, 20, 3, 20, 1.2D, 0.83D, 0.3D)),
    SKELETON_ARCHER("Skeleton Archer", 2, "minecraft:skeleton",
            "minecraft:bow", 0,
            new TroopStats(1, 20, 4, 20, 6.0D, 1.0D, 0.3D)),
    SPIDER("Spider", 3, "minecraft:spider", null, 0,
            new TroopStats(4, 10, 2, 20, 1.2D, 1.5D, 0.6D)),
    SKELETONS("Skeletons", 2, "minecraft:skeleton", "minecraft:stone_sword", 0,
            new TroopStats(3, 10, 2, 24, 1.2D, 1.33D, 0.3D)),
    BIG_SLIME("Big Slime", 3, "minecraft:slime", null, 4,
            new TroopStats(1, 20, 5, 28, 1.2D, 0.67D, 1.0D,
                    new SplitRule(2, 2, 0.5D, 0.5D, 0.5D, 1.0D, 6))),
    STRAY("Stray", 2, "minecraft:stray", "minecraft:bow", 0,
            new TroopStats(1, 20, 3, 24, 6.0D, 1.0D, 0.3D)),
    VINDICATOR("Vindicator", 2, "minecraft:vindicator",
            "minecraft:iron_axe", 0,
            new TroopStats(1, 24, 7, 28, 1.2D, 1.1D, 0.3D)),
    IRON_GOLEM("Iron Golem", 4, "minecraft:iron_golem", null, 0,
            new TroopStats(1, 100, 8, 32, 1.5D, 0.67D, 0.7D)),
    ARROW_STORM("Arrow Storm", 3, "minecraft:arrow",
            new SpellStats(3.0D, 10, 16, 5, 1.0D, SpellStats.TOWER_MULTIPLIER,
                    0.0D, 0.0D, 0)),
    FIREBALL("Fireball", 4, "minecraft:fire_charge",
            new SpellStats(2.5D, 20, 1, 1, 25.0D, SpellStats.TOWER_MULTIPLIER,
                    1.0D, 1.0D, 60));

    /** Time between a troop card play and the troop appearing (1 s). */
    public static final int DEPLOY_TICKS = 20;

    private final String displayName;
    private final int cost;
    private final String entityId;
    private final String heldItemId;
    private final int mobSize;
    private final TroopStats stats;
    private final SpellStats spell;
    private final String iconItemId;

    BattleCard(String displayName, int cost, String entityId,
            String heldItemId, int mobSize, TroopStats stats)
    {
        this.displayName = displayName;
        this.cost = cost;
        this.entityId = entityId;
        this.heldItemId = heldItemId;
        this.mobSize = mobSize;
        this.stats = stats;
        this.spell = null;
        this.iconItemId = null;
    }

    BattleCard(String displayName, int cost, String iconItemId,
            SpellStats spell)
    {
        this.displayName = displayName;
        this.cost = cost;
        this.entityId = null;
        this.heldItemId = null;
        this.mobSize = 0;
        this.stats = null;
        this.spell = spell;
        this.iconItemId = iconItemId;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        return displayName;
    }

    public static BattleCard byId(String id) {
        for (BattleCard card : values()) {
            if (card.id().equals(id))
                return card;
        }
        throw new IllegalArgumentException("Unknown battle card: " + id);
    }

    /** Mob of a troop card; null for a spell. */
    public String entityId() {
        return entityId;
    }
    /** Item put in the mob's main hand, or null. */
    public String heldItemId() {
        return heldItemId;
    }
    /** Vanilla slime size (4 = big), or 0 for other mobs. */
    public int mobSize() {
        return mobSize;
    }
    public int cost() {
        return cost;
    }
    /** Troop stats; null for a spell. */
    public TroopStats stats() {
        return stats;
    }
    /** Spell effect; null for a troop. */
    public SpellStats spell() {
        return spell;
    }
    public boolean isSpell() {
        return spell != null;
    }
    /** Item shown as a spell's card art when no portrait exists. */
    public String iconItemId() {
        return iconItemId;
    }
    /**
     * Ticks between the card play and its effect: the one-second deploy time
     * of a troop, or the flight time of a spell.
     */
    public int deployTicks() {
        return spell != null ? spell.castTicks() : DEPLOY_TICKS;
    }
}
