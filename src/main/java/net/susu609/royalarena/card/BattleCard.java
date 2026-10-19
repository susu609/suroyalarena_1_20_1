package net.susu609.royalarena.card;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Troop and spell cards. Troop Stats mirror data/royalarena/cards/*.json (a test
 * keeps them in sync) and every cost follows {@link CostFormula}. A spell
 * has {@link #spell()} instead of {@link #stats()} and no mob.
 */
public enum BattleCard {
    ZOMBIE("Zombie", 3, "minecraft:zombie", null, 0,
            new TroopStats(2, 20, 3, 20, 1.2D, 0.83D, 0.3D).withArmor(2)),
    SKELETON_ARCHER("Skeleton Archer", 2, "minecraft:skeleton",
            "minecraft:bow", 0,
            new TroopStats(1, 20, 4, 20, 4.5D, 1.0D, 0.3D)),
    SPIDER("Spider", 3, "minecraft:spider", null, 0,
            new TroopStats(3, 16, 2, 20, 1.2D, 1.5D, 0.45D)),
    SKELETONS("Skeletons", 2, "minecraft:skeleton", null, 0,
            new TroopStats(2, 20, 1.5D, 18, 1.2D, 1.33D, 0.27D)),
    BIG_SLIME("Big Slime", 4, "minecraft:slime", null, 4,
            new TroopStats(1, 36, 5, 28, 1.2D, 0.75D, 1.0D,
                    new SplitRule(2, 2, 0.5D, 0.5D, 0.5D, 1.0D, 6))),
    STRAY("Stray", 2, "minecraft:stray", "minecraft:bow", 0,
            new TroopStats(1, 20, 3, 24, 6.0D, 1.0D, 0.3D,
                    TroopTraits.slowing(20))),
    VINDICATOR("Vindicator", 2, "minecraft:vindicator",
            "minecraft:iron_axe", 0,
            new TroopStats(1, 24, 12, 48, 1.2D, 1.1D, 0.3D)),
    IRON_GOLEM("Iron Golem", 5, "minecraft:iron_golem", null, 0,
            new TroopStats(1, 100, 15, 55, 1.5D, 0.9D, 0.7D,
                    TroopTraits.NONE).withArmor(4)),
    CREEPER("Creeper", 3, "minecraft:creeper", null, 0,
            new TroopStats(1, 20, 30, 20, 0.5D, 1.8D, 0.3D,
                    new TroopTraits(3.0D, true, 0.0D))),
    WITCH("Witch", 4, "minecraft:witch", null, 0,
            new TroopStats(1, 26, 8, 30, 5.0D, 0.8D, 0.3D,
                    new TroopTraits(1.5D, false, 0.0D))),
    WOLVES("Wolves", 3, "minecraft:wolf", null, 0,
            new TroopStats(3, 8, 3, 16, 1.2D, 1.9D, 0.35D)),
    RAVAGER("Ravager", 5, "minecraft:ravager", null, 0,
            new TroopStats(1, 100, 8, 32, 1.5D, 1.0D, 0.9D,
                    new TroopTraits(0.0D, false, 1.0D))),
    HOGLIN("Hoglin", 3, "minecraft:hoglin", null, 0,
            new TroopStats(1, 40, 10, 28, 1.2D, 1.6D, 0.65D,
                    TroopTraits.BUILDINGS)),
    PILLAGER("Pillager", 3, "minecraft:pillager", "minecraft:crossbow", 0,
            new TroopStats(1, 24, 11, 40, 6.0D, 1.0D, 0.3D)),
    MAGMA_CUBE("Magma Cube", 3, "minecraft:magma_cube", null, 4,
            new TroopStats(1, 16, 7, 28, 1.2D, 0.83D, 1.0D,
                    new SplitRule(1, 2, 0.5D, 0.5D, 0.5D, 1.0D, 6)).withArmor(12)),
    ENDERMAN("Enderman", 3, "minecraft:enderman", null, 0,
            new TroopStats(1, 40, 7, 24, 1.2D, 1.0D, 0.4D,
                    TroopTraits.teleporting(100))),
    CAVE_SPIDER("Cave Spider", 3, "minecraft:cave_spider", null, 0,
            new TroopStats(3, 12, 2.0D, 20, 1.2D, 1.6D, 0.3D,
                    TroopTraits.poisoning(60, 1.0D))),
    VEX("Vex", 2, "minecraft:vex", "minecraft:iron_sword", 0,
            new TroopStats(2, 14, 10, 20, 1.2D, 2.0D, 0.3D,
                    new TroopTraits(0.0D, true, 0.0D))),
    GHAST("Ghast", 3, "minecraft:ghast", null, 0,
            new TroopStats(1, 10, 22, 60, 5.0D, 0.3D, 1.0D,
                    new TroopTraits(1.5D, false, 0.0D))),
    DROWNED("Drowned", 2, "minecraft:drowned", "minecraft:trident", 0,
            new TroopStats(1, 20, 4, 40, 5.0D, 1.0D, 0.3D,
                    TroopTraits.riverDeploy()).withArmor(2)),
    ZOMBIFIED_PIGLIN("Zombified Piglin", 3, "minecraft:zombified_piglin",
            "minecraft:golden_sword", 0,
            new TroopStats(2, 20, 4, 24, 1.2D, 0.9D, 0.3D).withArmor(2)),
    BLAZE("Blaze", 2, "minecraft:blaze", null, 0,
            new TroopStats(1, 20, 8, 40, 4.5D, 1.0D, 0.4D,
                    TroopTraits.hovering(1.5D, true, true, true))),
    HUSK("Husk", 3, "minecraft:husk", null, 0,
            new TroopStats(2, 20, 2.5D, 20, 1.2D, 1.0D, 0.3D,
                    TroopTraits.slowing(40)).withArmor(2)),
    WITHER_SKELETON("Wither Skeleton", 2, "minecraft:wither_skeleton",
            "minecraft:stone_sword", 0,
            new TroopStats(1, 20, 10, 32, 1.5D, 0.85D, 0.35D,
                    TroopTraits.poisoning(60, 1.5D))),
    PIGLIN_BRUTE("Piglin Brute", 4, "minecraft:piglin_brute",
            "minecraft:golden_axe", 0,
            new TroopStats(1, 50, 14, 36, 1.2D, 0.8D, 0.3D).withArmor(7)),
    POLAR_BEAR("Polar Bear", 3, "minecraft:polar_bear", null, 0,
            new TroopStats(1, 30, 7, 24, 1.2D, 1.2D, 0.7D,
                    TroopTraits.frostProof())),
    PHANTOM("Phantom", 2, "minecraft:phantom", null, 0,
            new TroopStats(1, 20, 7, 20, 1.2D, 3.0D, 0.45D,
                    TroopTraits.swooping(2.0D))),
    SILVERFISH("Silverfish", 2, "minecraft:silverfish", null, 0,
            new TroopStats(4, 8, 1, 16, 1.2D, 1.6D, 0.25D)),
    BEE("Bee", 2, "minecraft:bee", null, 0,
            new TroopStats(3, 10, 0.9D, 20, 1.2D, 2.0D, 0.25D,
                    TroopTraits.stinging(60, 1.0D, 1.0D))),
    EVOKER("Evoker", 3, "minecraft:evoker", null, 0,
            new TroopStats(1, 24, 10, 40, 4.0D, 0.5D, 0.3D,
                    TroopTraits.line(1.2D))),
    GUARDIAN("Guardian", 3, "minecraft:guardian", null, 0,
            new TroopStats(1, 30, 14, 40, 5.5D, 0.6D, 0.45D,
                    TroopTraits.laser(40, 0.6D))),
    ZOMBIE_VILLAGER("Zombie Villager", 3, "minecraft:zombie_villager", null, 0,
            new TroopStats(4, 20, 1, 24, 1.2D, 0.8D, 0.3D).withArmor(2)),
    SNOW_GOLEM("Snow Golem", 2, "minecraft:snow_golem", null, 0,
            new TroopStats(1, 40, 1, 20, 5.0D, 1.0D, 0.35D,
                    TroopTraits.slowing(30))),
    SPIDER_JOCKEY("Spider Jockey", 3, "minecraft:spider", null, 0,
            new TroopStats(1, 16, 3, 20, 4.5D, 1.4D, 0.55D,
                    SplitRule.into(SKELETON_ARCHER.stats()))),
    FOX("Fox", 2, "minecraft:fox", null, 0,
            new TroopStats(2, 10, 2, 14, 1.2D, 1.9D, 0.3D)),
    SKELETON_RIDER("Skeleton Rider", 3, "minecraft:skeleton_horse", null, 0,
            new TroopStats(1, 15, 6, 20, 6.0D, 1.4D, 0.45D,
                    SplitRule.into(SKELETON_ARCHER.stats()),
                    TroopTraits.BUILDINGS)),
    STRIDER("Strider", 2, "minecraft:strider", null, 0,
            new TroopStats(1, 20, 3, 20, 1.2D, 1.7D, 0.45D,
                    TroopTraits.fireTrail(40))),
    GOAT("Goat", 1, "minecraft:goat", null, 0,
            new TroopStats(1, 10, 5, 40, 1.2D, 1.3D, 0.4D,
                    new TroopTraits(0.0D, false, 3.0D))),
    LLAMA("Llama", 2, "minecraft:llama", null, 0,
            new TroopStats(1, 22, 3, 30, 4.5D, 0.9D, 0.45D,
                    TroopTraits.slowing(15))),
    PIGLIN("Piglin", 2, "minecraft:piglin", "minecraft:crossbow", 0,
            new TroopStats(1, 16, 5, 60, 5.0D, 1.0D, 0.3D,
                    TroopTraits.burst(2))),
    CAT("Cat", 1, "minecraft:cat", null, 0,
            new TroopStats(1, 10, 3, 20, 1.0D, 1.3D, 0.3D)),
    FROG("Frog", 1, "minecraft:frog", null, 0,
            new TroopStats(1, 10, 3, 24, 2.0D, 1.0D, 0.3D)),
    PIGLETS("Piglets", 3, "minecraft:pig", null, 0,
            new TroopStats(4, 10, 4, 30, 0.8D, 1.8D, 0.35D,
                    TroopTraits.BUILDINGS)),
    ARROW_STORM("Arrow Storm", 3, "minecraft:arrow",
            new SpellStats(2.3D, 6, 3, 8, 7.0D, 0.5D, 0.0D, 0.0D, 0)
                    .thrownAt(24.0D)),
    FIREBALL("Fireball", 4, "minecraft:fire_charge",
            new SpellStats(2.5D, 20, 1, 1, 25.0D, SpellStats.TOWER_MULTIPLIER,
                    1.0D, 1.5D, 60).thrownAt(16.0D)),
    LIGHTNING("Lightning", 6, "minecraft:lightning_rod",
            new SpellStats(3.5D, 15, 1, 1, 40.0D, 0.45D, 0.0D, 0.0D, 0, 3, 10,
                    0, false)),
    HEALING_POTION("Potion of Healing", 3, "minecraft:splash_potion",
            new SpellStats(3.0D, 6, 8, 10, 2.0D, 0.0D, 0.0D, 0.0D, 0, 0, 0,
                    0, true).thrownAt(16.0D)),
    FREEZE("Freeze", 3, "minecraft:powder_snow_bucket",
            new SpellStats(3.0D, 5, 1, 1, 8.0D, 0.8D, 0.0D, 0.0D, 0, 0, 0, 80,
                    false).thrownAt(20.0D)),
    TNT("TNT", 5, "minecraft:tnt",
            new SpellStats(3.0D, 24, 1, 1, 30.0D, 0.5D, 2.0D, 0.0D, 0, 0, 0,
                    0, false, true)),
    POISON_CLOUD("Poison Cloud", 2, "minecraft:lingering_potion",
            new SpellStats(3.8D, 5, 3, 20, 2.0D, 0.0D, 0.0D, 0.0D, 0, 0, 0,
                    0, false, false, 0, true, 0).thrownAt(16.0D)),
    COBWEB("Cobweb", 2, "minecraft:cobweb",
            new SpellStats(3.5D, 8, 5, 20, 0.0D, 0.0D, 0.0D, 0.0D, 0, 0, 0,
                    0, false, false, 35, false, 0).thrownAt(16.0D)),
    SWIFTNESS_POTION("Swiftness Potion", 2, "minecraft:sugar",
            new SpellStats(3.5D, 5, 1, 1, 0.0D, 0.0D, 0.0D, 0.0D, 0, 0, 0,
                    0, false, false, 0, false, 120).thrownAt(16.0D)),
    SNOWBALL_VOLLEY("Snowball Volley", 2, "minecraft:snowball",
            new SpellStats(2.5D, 8, 1, 1, 6.0D, SpellStats.TOWER_MULTIPLIER,
                    1.5D, 0.0D, 0, 0, 0, 0, false, false, 30, false, 0)
                    .thrownAt(20.0D)),
    ZAP("Zap", 2, "minecraft:copper_ingot",
            new SpellStats(2.5D, 5, 1, 1, 6.0D, SpellStats.TOWER_MULTIPLIER,
                    0.0D, 0.0D, 0, 0, 10, 0, false)),
    MILK_BUCKET("Milk Bucket", 1, "minecraft:milk_bucket",
            new SpellStats(3.0D, 5, 1, 1, 0.0D, 0.0D, 0.0D, 0.0D, 0, 0, 0, 0,
                    false).thrownAt(16.0D).cleansing());

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

    /** The troop card these stats belong to (split children too), or null. */
    public static BattleCard forStats(TroopStats stats) {
        for (BattleCard card : values()) {
            if (card.stats != null && card.stats == stats)
                return card;
        }
        return null;
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
    /**
     * Armour pieces the mob wears, head first, as vanilla item ids; their
     * vanilla armour points are part of {@link TroopStats#armor()} (a
     * Zombie's 2 points are its skin and show nothing).
     */
    public java.util.List<String> armorItems() {
        return switch (this) {
            // golden helmet 2 + golden chestplate 5
            case PIGLIN_BRUTE -> java.util.List.of("minecraft:golden_helmet",
                    "minecraft:golden_chestplate");
            default -> java.util.List.of();
        };
    }
    /** The one or two tribes the card belongs to (none for a spell). */
    public java.util.Set<Tribe> tribes() {
        return switch (this) {
            case ZOMBIE -> EnumSet.of(Tribe.ZOMBIE, Tribe.NIGHT);
            case HUSK, ZOMBIE_VILLAGER -> EnumSet.of(Tribe.ZOMBIE);
            case DROWNED -> EnumSet.of(Tribe.ZOMBIE, Tribe.AQUATIC);
            case ZOMBIFIED_PIGLIN -> EnumSet.of(Tribe.ZOMBIE, Tribe.NETHER);
            case PHANTOM -> EnumSet.of(Tribe.ZOMBIE, Tribe.NIGHT);
            case SKELETON_ARCHER -> EnumSet.of(Tribe.SKELETON, Tribe.NIGHT);
            case SKELETONS, STRAY, SKELETON_RIDER -> EnumSet.of(Tribe.SKELETON);
            case WITHER_SKELETON -> EnumSet.of(Tribe.SKELETON, Tribe.NETHER);
            case SPIDER_JOCKEY -> EnumSet.of(Tribe.SKELETON, Tribe.ARTHROPOD);
            case VINDICATOR, PILLAGER, EVOKER, VEX -> EnumSet.of(Tribe.ILLAGER);
            case WITCH -> EnumSet.of(Tribe.ILLAGER, Tribe.NIGHT);
            case RAVAGER -> EnumSet.of(Tribe.ILLAGER, Tribe.BEAST);
            case PIGLIN_BRUTE, BLAZE, GHAST, PIGLIN -> EnumSet.of(Tribe.NETHER);
            case HOGLIN, STRIDER -> EnumSet.of(Tribe.NETHER, Tribe.BEAST);
            case MAGMA_CUBE -> EnumSet.of(Tribe.NETHER, Tribe.SLIME);
            case SPIDER -> EnumSet.of(Tribe.ARTHROPOD, Tribe.NIGHT);
            case CAVE_SPIDER, SILVERFISH -> EnumSet.of(Tribe.ARTHROPOD);
            case BEE -> EnumSet.of(Tribe.ARTHROPOD, Tribe.BEAST);
            case WOLVES, FOX, POLAR_BEAR, GOAT, LLAMA, CAT, PIGLETS ->
                    EnumSet.of(Tribe.BEAST);
            case FROG -> EnumSet.of(Tribe.AQUATIC);
            case CREEPER, ENDERMAN -> EnumSet.of(Tribe.NIGHT);
            case BIG_SLIME -> EnumSet.of(Tribe.NIGHT, Tribe.SLIME);
            case IRON_GOLEM, SNOW_GOLEM -> EnumSet.of(Tribe.GOLEM);
            case GUARDIAN -> EnumSet.of(Tribe.AQUATIC);
            default -> EnumSet.noneOf(Tribe.class);
        };
    }
    /** Vanilla slime or magma cube size (4 = big), or 0 for other mobs. */
    public int mobSize() {
        return mobSize;
    }
    public int cost() {
        return cost;
    }
    /**
     * How big the mob is drawn, relative to vanilla: the Skeletons (90%) and
     * the Spiders (75%) are a small swarm. Mirrors {@code model_scale} in the
     * card's JSON.
     */
    public float modelScale() {
        return switch (this) {
            case SKELETONS -> 0.9F;
            case SPIDER -> 0.75F;
            case SPIDER_JOCKEY -> 0.9F;
            case PIGLETS -> 0.6F;
            case CAVE_SPIDER -> 0.6F;
            case VEX -> 0.8F;
            case ZOMBIE_VILLAGER -> 0.85F;
            case WITHER_SKELETON -> 0.85F;
            case POLAR_BEAR -> 0.8F;
            default -> 1.0F;
        };
    }

    /**
     * How the card is meant to be played (attack, defense, support), as
     * written in the {@code "roles"} list of its JSON.
     */
    public Set<CardTactic> tactics() {
        return switch (this) {
            case SNOWBALL_VOLLEY, ZOMBIE, ZOMBIE_VILLAGER, SKELETONS, VINDICATOR, CAVE_SPIDER, VEX,
                    ZOMBIFIED_PIGLIN, HUSK, SILVERFISH, GUARDIAN, GOAT, CAT, FROG ->
                    EnumSet.of(CardTactic.DEFENSE);
            case SKELETON_ARCHER, STRAY, WITCH, PILLAGER, GHAST, BEE, EVOKER, LLAMA, PIGLIN,
                    SNOW_GOLEM, ARROW_STORM, COBWEB, ZAP ->
                    EnumSet.of(CardTactic.DEFENSE, CardTactic.SUPPORT);
            case SPIDER, SPIDER_JOCKEY, FOX, SKELETON_RIDER, PIGLETS, STRIDER, IRON_GOLEM, WOLVES, RAVAGER, HOGLIN, ENDERMAN,
                    PIGLIN_BRUTE, PHANTOM, LIGHTNING ->
                    EnumSet.of(CardTactic.ATTACK);
            case BIG_SLIME, CREEPER, MAGMA_CUBE, WITHER_SKELETON, POLAR_BEAR,
                    FIREBALL, TNT, POISON_CLOUD ->
                    EnumSet.of(CardTactic.ATTACK, CardTactic.DEFENSE);
            case DROWNED, BLAZE, FREEZE ->
                    EnumSet.of(CardTactic.ATTACK, CardTactic.SUPPORT);
            case HEALING_POTION, SWIFTNESS_POTION, MILK_BUCKET ->
                    EnumSet.of(CardTactic.SUPPORT);
        };
    }

    /**
     * What the card is made of, for poison: living flesh, undead or spirit,
     * or a construct (golems, immune). Spells count as living (unused).
     */
    public Body body() {
        return switch (this) {
            case IRON_GOLEM, SNOW_GOLEM ->
                    Body.CONSTRUCT;
            case ZOMBIE, SKELETON_ARCHER, SKELETONS,
                    STRAY, ZOMBIFIED_PIGLIN, HUSK,
                    WITHER_SKELETON, DROWNED, PHANTOM,
                    ZOMBIE_VILLAGER, BLAZE, VEX, SKELETON_RIDER ->
                    Body.UNDEAD;
            default -> Body.LIVING;
        };
    }

    /**
     * Nether mobs do not burn: a Fireball still hurts them, but sets nothing
     * alight. Mirrors {@code fire_immune} in the card's JSON.
     */
    public boolean fireImmune() {
        return switch (this) {
            case BLAZE, GHAST, MAGMA_CUBE, ZOMBIFIED_PIGLIN, HOGLIN,
                    WITHER_SKELETON, PIGLIN_BRUTE, STRIDER, PIGLIN -> true;
            default -> false;
        };
    }

    /**
     * Creepers and Phantoms will not come near a Cat (vanilla: they flee
     * cats and ocelots): within {@link #CAT_SCARE_RADIUS} of an enemy Cat
     * they turn away and do not attack.
     */
    public boolean fearsCats() {
        return this == CREEPER || this == PHANTOM;
    }

    /** How close a Creeper or Phantom will come to an enemy Cat. */
    public static final double CAT_SCARE_RADIUS = 4.0D;

    /**
     * A Frog swallows the smallest slimes and magma cubes whole, as in
     * vanilla: one lick kills a slime that can split no more.
     */
    public boolean swallowsSmallSlimes() {
        return this == FROG;
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
