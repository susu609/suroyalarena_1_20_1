package net.susu609.royalarena.card;

import java.util.Locale;

/**
 * Starter troop cards. Stats mirror data/royalarena/cards/*.json (a test
 * keeps them in sync) and every cost follows {@link CostFormula}.
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
            new TroopStats(1, 100, 8, 32, 1.5D, 0.67D, 0.7D));

    private final String displayName;
    private final int cost;
    private final String entityId;
    private final String heldItemId;
    private final int mobSize;
    private final TroopStats stats;

    BattleCard(String displayName, int cost, String entityId,
            String heldItemId, int mobSize, TroopStats stats)
    {
        this.displayName = displayName;
        this.cost = cost;
        this.entityId = entityId;
        this.heldItemId = heldItemId;
        this.mobSize = mobSize;
        this.stats = stats;
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
    public TroopStats stats() {
        return stats;
    }
}
