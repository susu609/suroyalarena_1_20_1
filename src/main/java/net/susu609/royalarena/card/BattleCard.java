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
            new TroopStats(4, 10, 2, 20, 1.2D, 1.5D, 0.45D)),
    SKELETONS("Skeletons", 2, "minecraft:skeleton", null, 0,
            new TroopStats(3, 11, 1.5D, 18, 1.2D, 1.33D, 0.27D)),
    BIG_SLIME("Big Slime", 3, "minecraft:slime", null, 4,
            new TroopStats(1, 20, 5, 28, 1.2D, 0.85D, 1.0D,
                    new SplitRule(2, 2, 0.5D, 0.5D, 0.5D, 1.0D, 6))),
    STRAY("Stray", 2, "minecraft:stray", "minecraft:bow", 0,
            new TroopStats(1, 20, 3, 24, 6.0D, 1.0D, 0.3D)),
    VINDICATOR("Vindicator", 2, "minecraft:vindicator",
            "minecraft:iron_axe", 0,
            new TroopStats(1, 24, 12, 48, 1.2D, 1.1D, 0.3D)),
    IRON_GOLEM("Iron Golem", 5, "minecraft:iron_golem", null, 0,
            new TroopStats(1, 160, 12, 32, 1.5D, 0.9D, 0.7D,
                    TroopTraits.BUILDINGS)),
    CREEPER("Creeper", 3, "minecraft:creeper", null, 0,
            new TroopStats(1, 20, 30, 20, 0.5D, 1.33D, 0.3D,
                    new TroopTraits(2.5D, true, 0.0D))),
    WITCH("Witch", 4, "minecraft:witch", null, 0,
            new TroopStats(1, 26, 8, 30, 5.0D, 0.8D, 0.3D,
                    new TroopTraits(1.5D, false, 0.0D))),
    WOLVES("Wolves", 3, "minecraft:wolf", null, 0,
            new TroopStats(3, 8, 3, 16, 1.2D, 1.6D, 0.35D)),
    RAVAGER("Ravager", 5, "minecraft:ravager", null, 0,
            new TroopStats(1, 130, 10, 32, 1.5D, 1.0D, 0.9D,
                    new TroopTraits(0.0D, false, 1.0D))),
    HOGLIN("Hoglin", 3, "minecraft:hoglin", null, 0,
            new TroopStats(1, 45, 8, 28, 1.2D, 1.6D, 0.65D,
                    TroopTraits.BUILDINGS)),
    PILLAGER("Pillager", 3, "minecraft:pillager", "minecraft:crossbow", 0,
            new TroopStats(1, 30, 10, 40, 6.5D, 1.0D, 0.3D)),
    MAGMA_CUBE("Magma Cube", 4, "minecraft:magma_cube", null, 4,
            new TroopStats(1, 36, 7, 28, 1.2D, 0.83D, 1.0D,
                    new SplitRule(1, 2, 0.5D, 0.5D, 0.5D, 1.0D, 6))),
    ARROW_STORM("Arrow Storm", 3, "minecraft:arrow",
            new SpellStats(3.0D, 10, 16, 5, 1.0D, SpellStats.TOWER_MULTIPLIER,
                    0.0D, 0.0D, 0)),
    FIREBALL("Fireball", 4, "minecraft:fire_charge",
            new SpellStats(2.5D, 20, 1, 1, 25.0D, SpellStats.TOWER_MULTIPLIER,
                    1.0D, 1.0D, 60)),
    LIGHTNING("Lightning", 6, "minecraft:lightning_rod",
            new SpellStats(3.5D, 15, 1, 1, 40.0D, SpellStats.TOWER_MULTIPLIER,
                    0.0D, 0.0D, 0, 3, 10, 0, false)),
    HEALING_POTION("Potion of Healing", 3, "minecraft:splash_potion",
            new SpellStats(3.0D, 10, 8, 10, 2.0D, 0.0D, 0.0D, 0.0D, 0, 0, 0,
                    0, true)),
    FREEZE("Freeze", 4, "minecraft:powder_snow_bucket",
            new SpellStats(3.0D, 10, 1, 1, 4.0D, SpellStats.TOWER_MULTIPLIER,
                    0.0D, 0.0D, 0, 0, 0, 80, false)),
    TNT("TNT", 5, "minecraft:tnt",
            new SpellStats(3.0D, 30, 1, 1, 30.0D, 0.5D, 2.0D, 0.0D, 0, 0, 0,
                    0, false));

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

    /** The card whose display name is {@code name}, or null. */
    public static BattleCard byDisplayName(String name) {
        for (BattleCard card : values()) {
            if (card.displayName.equals(name))
                return card;
        }
        return null;
    }

    /** Mob of a troop card; null for a spell. */
    public String entityId() {
        return entityId;
    }
    /** Item put in the mob's main hand, or null. */
    public String heldItemId() {
        return heldItemId;
    }
    /** Vanilla slime or magma cube size (4 = big), or 0 for other mobs. */
    public int mobSize() {
        return mobSize;
    }
    public int cost() {
        return cost;
    }
    /**
     * How big the mob is drawn, relative to vanilla: the three Skeletons (90%) and
     * the four Spiders (75%) are a small swarm. Mirrors {@code model_scale} in the
     * card's JSON.
     */
    public float modelScale() {
        return switch (this) {
            case SKELETONS -> 0.9F;
            case SPIDER -> 0.75F;
            default -> 1.0F;
        };
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
