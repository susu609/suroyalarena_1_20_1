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
            new TroopStats(1, 24, 9, 40, 6.0D, 1.0D, 0.3D)),
    MAGMA_CUBE("Magma Cube", 2, "minecraft:magma_cube", null, 2,
            new TroopStats(2, 12, 3, 30, 1.0D, 1.0D, 0.5D)),
    ENDERMAN("Enderman", 3, "minecraft:enderman", null, 0,
            new TroopStats(1, 40, 6, 24, 1.2D, 1.0D, 0.4D,
                    TroopTraits.teleporting(100))),
    CAVE_SPIDER("Cave Spider", 3, "minecraft:cave_spider", null, 0,
            new TroopStats(3, 12, 2.0D, 20, 1.2D, 1.6D, 0.3D,
                    TroopTraits.poisoning(60, 1.0D))),
    VEX("Vex", 2, "minecraft:vex", "minecraft:iron_sword", 0,
            new TroopStats(2, 14, 4, 20, 1.2D, 2.0D, 0.3D)),
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
                    TroopTraits.NONE)),
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
    SPIDER_JOCKEY("Spider Jockey", 4, "minecraft:spider", null, 0,
            new TroopStats(1, 16, 2, 20, 1.2D, 1.4D, 0.55D,
                    SplitRule.into(SKELETON_ARCHER.stats()))),
    FOX("Fox", 2, "minecraft:fox", null, 0,
            new TroopStats(2, 10, 2, 14, 1.2D, 1.9D, 0.3D)),
    SKELETON_RIDER("Skeleton Rider", 3, "minecraft:skeleton_horse", null, 0,
            new TroopStats(1, 20, 2, 20, 1.2D, 1.4D, 0.45D,
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
    PIGLETS("Piglets", 3, "minecraft:hoglin", null, 0,
            new TroopStats(4, 10, 4, 30, 0.8D, 1.8D, 0.35D,
                    TroopTraits.BUILDINGS)),
    ZOGLIN("Zoglin", 4, "minecraft:zoglin", null, 0,
            new TroopStats(1, 40, 8, 28, 1.2D, 1.4D, 0.65D,
                    new TroopTraits(0.0D, false, 1.0D, false, 0, 0, 0.0D, 0,
                            0.0D, false, false, false, false, false, 0.0D, 0,
                            false, 0))),
    KILLER_RABBITS("Killer Rabbits", 2, "minecraft:rabbit", null, 0,
            new TroopStats(3, 3, 4, 20, 0.8D, 1.8D, 0.25D).withArmor(8)),
    CHICKEN_JOCKEY("Chicken Jockey", 2, "minecraft:chicken", null, 0,
            new TroopStats(1, 4, 1, 20, 1.0D, 1.8D, 0.35D,
                    SplitRule.into(ZOMBIE.stats()), TroopTraits.NONE)),
    PANDA("Panda", 2, "minecraft:panda", null, 0,
            new TroopStats(1, 20, 8, 30, 1.2D, 0.8D, 0.65D)),
    TURTLE("Turtle", 1, "minecraft:turtle", null, 0,
            new TroopStats(1, 30, 1, 40, 1.0D, 0.5D, 0.6D).withArmor(8)),
    CAMEL("Camel", 3, "minecraft:camel", null, 0,
            new TroopStats(1, 32, 2.5D, 20, 1.5D, 1.0D, 0.8D,
                    SplitRule.into(HUSK.stats()), TroopTraits.NONE)),
    SHULKER("Shulker", 2, "minecraft:shulker", null, 0,
            new TroopStats(1, 30, 2.8D, 40, 6.0D, 0.0D, 0.5D)),
    END_CRYSTAL("End Crystal", 2, "minecraft:end_crystal", null, 0,
            new TroopStats(1, 14, 48, 20, 0.5D, 0.0D, 0.5D,
                    new TroopTraits(3.0D, true, 0.0D))),
    WARDEN("Warden", 8, "minecraft:warden", null, 0,
            new TroopStats(1, 150, 15, 55, 1.5D, 0.8D, 0.9D)),
    ILLUSIONER("Illusioner", 3, "minecraft:illusioner", "minecraft:bow", 0,
            new TroopStats(1, 24, 5, 30, 5.5D, 1.0D, 0.3D)),
    AXOLOTL("Axolotl", 2, "minecraft:axolotl", null, 0,
            new TroopStats(1, 18, 4, 20, 1.0D, 1.0D, 0.4D,
                    new TroopTraits(0.0D, false, 0.0D, false, 0, 0, 0.0D, 0,
                            0.0D, false, false, false, false, false, 0.0D, 0,
                            true))),
    ENDERMITE("Endermite", 1, "minecraft:endermite", null, 0,
            new TroopStats(3, 4, 1, 16, 0.8D, 1.6D, 0.25D)),
    ALLAY("Allay", 2, "minecraft:allay", null, 0,
            new TroopStats(1, 16, 2, 20, 0.5D, 1.3D, 0.3D,
                    TroopTraits.hovering(1.0D, true, false, false))),
    GIANT("Giant", 6, "minecraft:giant", null, 0,
            new TroopStats(1, 120, 12, 40, 1.5D, 0.6D, 1.0D,
                    new TroopTraits(1.0D, false, 0.0D))),
    VILLAGER("Villager", 3, "minecraft:villager", null, 0,
            new TroopStats(3, 10, 1, 20, 0.8D, 0.8D, 0.3D)),
    SNIFFER("Sniffer", 2, "minecraft:sniffer", null, 0,
            new TroopStats(1, 60, 4, 100, 0.5D, 0.5D, 0.9D)),
    WANDERING_TRADER("Wandering Trader", 4, "minecraft:wandering_trader",
            null, 0,
            new TroopStats(1, 20, 0, 20, 0.5D, 0.9D, 0.3D)),
    PILLAGER_CAPTAIN("Pillager Captain", 3, "minecraft:pillager",
            "minecraft:crossbow", 0,
            new TroopStats(1, 28, 8, 40, 6.0D, 1.0D, 0.3D)),
    WITHER("Wither", 9, "minecraft:wither", null, 0,
            new TroopStats(1, 100, 5, 30, 5.0D, 0.6D, 0.9D,
                    new TroopTraits(1.0D, false, 0.0D, false, 0, 0, 0.0D, 0,
                            2.0D, true, false, false, false, false, 0.0D, 0,
                            false))),
    ZOMBIE_HORSE("Zombie Horse", 3, "minecraft:zombie_horse", null, 0,
            new TroopStats(1, 24, 3, 20, 1.2D, 1.6D, 0.6D,
                    SplitRule.into(ZOMBIE.stats()), TroopTraits.NONE)),
    BAT("Bat", 2, "minecraft:bat", null, 0,
            new TroopStats(5, 4, 1.5D, 22, 0.8D, 2.0D, 0.25D,
                    TroopTraits.hovering(1.5D, true, false, false))),
    MOOSHROOM("Mooshroom", 2, "minecraft:mooshroom", null, 0,
            new TroopStats(1, 40, 3, 25, 1.0D, 0.7D, 0.65D)),
    PARROT("Parrot", 1, "minecraft:parrot", null, 0,
            new TroopStats(1, 8, 0, 20, 0.5D, 1.4D, 0.25D,
                    TroopTraits.hovering(1.5D, true, false, false))),
    GLOW_SQUID("Glow Squid", 1, "minecraft:glow_squid", null, 0,
            new TroopStats(1, 20, 2, 20, 1.0D, 0.8D, 0.4D,
                    TroopTraits.riverDeploy())),
    SHEEP("Sheep", 1, "minecraft:sheep", null, 0,
            new TroopStats(1, 16, 0, 20, 0.5D, 0.8D, 0.45D)),
    MONSTER_SPAWNER("Monster Spawner", 4, "minecraft:block_display", null, 0,
            new TroopStats(1, 40, 0, 100, 0.5D, 0.0D, 0.5D)),
    CAMPFIRE("Campfire", 2, "minecraft:block_display", null, 0,
            new TroopStats(1, 40, 1, 20, 2.0D, 0.0D, 0.5D)),
    BEACON("Beacon", 1, "minecraft:block_display", null, 0,
            new TroopStats(1, 25, 0, 20, 5.0D, 0.0D, 0.5D)),
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
            new SpellStats(2.5D, 8, 1, 1, 7.0D, SpellStats.TOWER_MULTIPLIER,
                    1.5D, 0.0D, 0, 0, 0, 0, false, false, 30, false, 0)
                    .thrownAt(20.0D)),
    ZAP("Zap", 2, "minecraft:copper_ingot",
            new SpellStats(2.5D, 5, 1, 1, 6.0D, SpellStats.TOWER_MULTIPLIER,
                    0.0D, 0.0D, 0, 0, 10, 0, false)),
    MILK_BUCKET("Milk Bucket", 1, "minecraft:milk_bucket",
            new SpellStats(3.0D, 5, 1, 1, 0.0D, 0.0D, 0.0D, 0.0D, 0, 0, 0, 0,
                    false).thrownAt(16.0D).cleansing()),
    WITHER_SKULL("Wither Skull", 2, "minecraft:wither_skeleton_skull",
            new SpellStats(2.0D, 5, 1, 1, 6.0D, SpellStats.TOWER_MULTIPLIER,
                    0.0D, 0.0D, 0).thrownAt(16.0D)),
    ENDER_PEARL("Ender Pearl", 2, "minecraft:ender_pearl",
            new SpellStats(1.0D, 5, 1, 1, 0.0D, 0.0D, 0.0D, 0.0D, 0)
                    .thrownAt(20.0D)),
    WATER_BUCKET("Water Bucket", 2, "minecraft:water_bucket",
            new SpellStats(2.5D, 5, 1, 1, 0.0D, 0.0D, 1.5D, 0.0D, 0)
                    .thrownAt(16.0D)),
    ANVIL("Anvil", 2, "minecraft:anvil",
            new SpellStats(1.5D, 24, 1, 1, 45.0D, SpellStats.TOWER_MULTIPLIER,
                    0.0D, 0.0D, 0, 1, 20, 0, false)),
    DRAGONS_BREATH("Dragon's Breath", 4, "minecraft:dragon_breath",
            new SpellStats(3.0D, 5, 10, 10, 2.0D, SpellStats.TOWER_MULTIPLIER,
                    0.0D, 0.0D, 0).thrownAt(16.0D)),
    BELL("Bell", 2, "minecraft:bell",
            new SpellStats(6.0D, 10, 1, 1, 0.0D, 0.0D, 0.0D, 0.0D, 0, 0, 10,
                    0, false)),
    LAVA_BUCKET("Lava Bucket", 3, "minecraft:lava_bucket",
            new SpellStats(2.0D, 5, 10, 10, 0.0D, 0.0D, 0.0D, 2.5D, 20, 0, 0,
                    0, false, false, 15, false, 0).thrownAt(16.0D)),
    HARMING_POTION("Potion of Harming", 3, "minecraft:splash_potion",
            new SpellStats(2.5D, 5, 1, 1, 12.0D, SpellStats.TOWER_MULTIPLIER,
                    0.0D, 0.0D, 0).thrownAt(16.0D)),
    FIREWORK_ROCKET("Firework Rocket", 2, "minecraft:firework_rocket",
            new SpellStats(2.5D, 5, 1, 1, 8.0D, SpellStats.TOWER_MULTIPLIER,
                    0.0D, 0.0D, 0).thrownAt(24.0D));

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
            case HUSK, ZOMBIE_VILLAGER, CHICKEN_JOCKEY -> EnumSet.of(Tribe.ZOMBIE);
            case ZOGLIN -> EnumSet.of(Tribe.ZOMBIE, Tribe.NETHER);
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
            case GIANT -> EnumSet.of(Tribe.ZOMBIE);
            case VILLAGER, WANDERING_TRADER -> EnumSet.of(Tribe.VILLAGE);
            case SNIFFER -> EnumSet.of(Tribe.BEAST);
            case PILLAGER_CAPTAIN -> EnumSet.of(Tribe.ILLAGER);
            case WITHER -> EnumSet.of(Tribe.NETHER, Tribe.SKELETON);
            case ZOMBIE_HORSE -> EnumSet.of(Tribe.ZOMBIE, Tribe.BEAST);
            case BAT -> EnumSet.of(Tribe.NIGHT);
            case MOOSHROOM, PARROT, SHEEP -> EnumSet.of(Tribe.BEAST);
            case GLOW_SQUID -> EnumSet.of(Tribe.AQUATIC);
            case MONSTER_SPAWNER -> EnumSet.of(Tribe.NIGHT);
            case CAMPFIRE -> EnumSet.of(Tribe.VILLAGE);
            case BEACON -> EnumSet.of(Tribe.GOLEM);
            case BEE -> EnumSet.of(Tribe.ARTHROPOD, Tribe.BEAST);
            case WOLVES, FOX, POLAR_BEAR, GOAT, LLAMA, CAT, PIGLETS,
                    KILLER_RABBITS, PANDA, CAMEL ->
                    EnumSet.of(Tribe.BEAST);
            case FROG, TURTLE -> EnumSet.of(Tribe.AQUATIC);
            case WARDEN -> EnumSet.of(Tribe.NIGHT);
            case ILLUSIONER -> EnumSet.of(Tribe.ILLAGER);
            case AXOLOTL -> EnumSet.of(Tribe.AQUATIC);
            case ENDERMITE -> EnumSet.of(Tribe.END, Tribe.ARTHROPOD);
            case ALLAY -> EnumSet.of(Tribe.GOLEM);
            case CREEPER -> EnumSet.of(Tribe.NIGHT);
            case ENDERMAN -> EnumSet.of(Tribe.NIGHT, Tribe.END);
            case SHULKER, END_CRYSTAL -> EnumSet.of(Tribe.END);
            case BIG_SLIME -> EnumSet.of(Tribe.NIGHT, Tribe.SLIME);
            case IRON_GOLEM -> EnumSet.of(Tribe.GOLEM, Tribe.VILLAGE);
            case SNOW_GOLEM -> EnumSet.of(Tribe.GOLEM);
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
            case CAVE_SPIDER -> 0.6F;
            case VEX -> 0.8F;
            case ZOMBIE_VILLAGER -> 0.85F;
            case WITHER_SKELETON -> 0.85F;
            case POLAR_BEAR -> 0.8F;
            case GIANT -> 0.4F;
            case WITHER -> 0.8F;
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
                    ZOMBIFIED_PIGLIN, HUSK, SILVERFISH, GUARDIAN, GOAT, CAT, FROG,
                    KILLER_RABBITS, PANDA, TURTLE, SHULKER, END_CRYSTAL, AXOLOTL,
                    ENDERMITE, SHEEP, MONSTER_SPAWNER, CAMPFIRE, FIREWORK_ROCKET ->
                    EnumSet.of(CardTactic.DEFENSE);
            case SKELETON_ARCHER, STRAY, WITCH, PILLAGER, GHAST, BEE, EVOKER, LLAMA, PIGLIN,
                    SNOW_GOLEM, ARROW_STORM, COBWEB, ZAP, ILLUSIONER, WATER_BUCKET,
                    WANDERING_TRADER, BELL, PILLAGER_CAPTAIN, GLOW_SQUID ->
                    EnumSet.of(CardTactic.DEFENSE, CardTactic.SUPPORT);
            case SPIDER, SPIDER_JOCKEY, FOX, SKELETON_RIDER, PIGLETS, ZOGLIN,
                    CHICKEN_JOCKEY, CAMEL, WARDEN, STRIDER, IRON_GOLEM, WOLVES, RAVAGER, HOGLIN, ENDERMAN,
                    PIGLIN_BRUTE, PHANTOM, LIGHTNING, GIANT, WITHER, ZOMBIE_HORSE ->
                    EnumSet.of(CardTactic.ATTACK);
            case BIG_SLIME, CREEPER, MAGMA_CUBE, WITHER_SKELETON, POLAR_BEAR,
                    FIREBALL, TNT, POISON_CLOUD, WITHER_SKULL, ANVIL,
                    DRAGONS_BREATH, BAT, LAVA_BUCKET, HARMING_POTION ->
                    EnumSet.of(CardTactic.ATTACK, CardTactic.DEFENSE);
            case DROWNED, BLAZE, FREEZE, VILLAGER, SNIFFER, MOOSHROOM ->
                    EnumSet.of(CardTactic.ATTACK, CardTactic.SUPPORT);
            case HEALING_POTION, SWIFTNESS_POTION, MILK_BUCKET, ALLAY,
                    ENDER_PEARL, PARROT, BEACON ->
                    EnumSet.of(CardTactic.SUPPORT);
        };
    }

    /**
     * What the card is made of, for poison: living flesh, undead or spirit,
     * or a construct (golems, immune). Spells count as living (unused).
     */
    public Body body() {
        return switch (this) {
            case IRON_GOLEM, SNOW_GOLEM, END_CRYSTAL, MONSTER_SPAWNER, CAMPFIRE,
                    BEACON -> Body.CONSTRUCT;
            case ZOMBIE, SKELETON_ARCHER, SKELETONS,
                    STRAY, ZOMBIFIED_PIGLIN, HUSK,
                    WITHER_SKELETON, DROWNED, PHANTOM,
                    ZOMBIE_VILLAGER, BLAZE, VEX, SKELETON_RIDER, ZOGLIN, GIANT,
                    WITHER, ZOMBIE_HORSE ->
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
                    WITHER_SKELETON, PIGLIN_BRUTE, STRIDER, PIGLIN, ZOGLIN, WITHER,
                    CAMPFIRE -> true;
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

    /**
     * A Zoglin's rage: it attacks faster the more health it has lost, up to
     * {@link #RAGE_MAX} faster at a quarter of its health or less.
     */
    public boolean rages() {
        return this == ZOGLIN;
    }

    public static final double RAGE_MAX = 0.6D;

    /**
     * A Camel's dash: its first blow lands {@link #DASH_DAMAGE} times as
     * hard and knocks the target {@link #DASH_KNOCKBACK} blocks back.
     */
    public boolean dashes() {
        return this == CAMEL;
    }

    public static final double DASH_DAMAGE = 2.0D;
    public static final double DASH_KNOCKBACK = 1.5D;

    /**
     * A Panda sneezes every {@link #SNEEZE_TICKS}: small enemies (body
     * radius under {@link #SNEEZE_SMALL}) within {@link #SNEEZE_RANGE}
     * blocks of it are blown {@link #SNEEZE_PUSH} blocks away.
     */
    public boolean sneezes() {
        return this == PANDA;
    }

    public static final int SNEEZE_TICKS = 80;
    public static final double SNEEZE_SMALL = 0.5D;
    public static final double SNEEZE_RANGE = 1.5D;
    public static final double SNEEZE_PUSH = 1.5D;

    /** A Turtle swims: it walks {@link #RIVER_SWIM} times as fast on the river. */
    public boolean swimsFast() {
        return this == TURTLE || this == GLOW_SQUID;
    }

    public static final double RIVER_SWIM = 3.0D;

    /**
     * A building (docs/CARDS.md): it never moves, loses its health over
     * {@link #BUILDING_LIFETIME_TICKS}, and troops that only hit towers go
     * for it first when they see it.
     */
    public boolean isBuilding() {
        return this == SHULKER || this == END_CRYSTAL || this == MONSTER_SPAWNER ||
                this == CAMPFIRE || this == BEACON;
    }

    /**
     * The block a building made of a block shows ({@code minecraft:
     * block_display} entities), or null for a mob.
     */
    public String displayBlock() {
        return switch (this) {
            case MONSTER_SPAWNER -> "minecraft:spawner";
            case CAMPFIRE -> "minecraft:campfire";
            case BEACON -> "minecraft:beacon";
            default -> null;
        };
    }

    public static final int BUILDING_LIFETIME_TICKS = 20 * 30;

    /**
     * A Shulker's bullets lift a ground troop for {@link #LEVITATE_TICKS}
     * (no walking, no attacking); its shell takes {@link #SHELL_FACTOR} of a
     * hit unless it has fired within {@link #SHELL_OPEN_TICKS}.
     */
    public boolean levitates() {
        return this == SHULKER;
    }

    public static final int LEVITATE_TICKS = 16;
    public static final double SHELL_FACTOR = 0.6D;
    public static final int SHELL_OPEN_TICKS = 15;

    /**
     * An End Crystal never attacks; when it breaks it blows up, hurting
     * every troop of both sides within its splash radius by its damage.
     */
    public boolean explodesOnDeath() {
        return this == END_CRYSTAL;
    }

    /**
     * The Warden is blind: it walks for the towers and turns only on enemies
     * that made a sound (attacked within {@link #NOISE_TICKS}) within
     * {@link #HEAR_RANGE} blocks. Every {@link #BOOM_TICKS} its Sonic Boom
     * hits its target within {@link #BOOM_RANGE} for {@link #BOOM_DAMAGE},
     * through armour.
     */
    public boolean hearsNoise() {
        return this == WARDEN;
    }

    public static final int NOISE_TICKS = 40;

    /**
     * An Illusioner's tricks: the first time it falls under
     * {@link #ILLUSION_BELOW} of its health it splits off
     * {@link #ILLUSION_COUNT} mirror images (1 health, no attack) that draw
     * shots and troops for {@link #ILLUSION_TICKS}; every
     * {@link #BLIND_EVERY}-th arrow blinds its target, cutting its range to
     * {@link #BLIND_RANGE} for {@link #BLIND_TICKS}.
     */
    public boolean castsIllusions() {
        return this == ILLUSIONER;
    }

    public static final double ILLUSION_BELOW = 0.5D;
    public static final int ILLUSION_COUNT = 3;
    public static final int ILLUSION_TICKS = 80;
    public static final int BLIND_EVERY = 3;
    public static final int BLIND_TICKS = 60;
    public static final double BLIND_RANGE = 0.6D;

    /**
     * An Axolotl plays dead once: hurt under {@link #PLAY_DEAD_BELOW} of its
     * health, it lies still for {@link #PLAY_DEAD_TICKS}, cannot be picked
     * as a target, and gets {@link #PLAY_DEAD_HEAL} of its health back.
     */
    public boolean playsDead() {
        return this == AXOLOTL;
    }

    public static final double PLAY_DEAD_BELOW = 0.3D;
    public static final int PLAY_DEAD_TICKS = 60;
    public static final double PLAY_DEAD_HEAL = 0.5D;

    /**
     * Endermites vanish after {@link #ENDERMITE_TICKS}, and an enemy
     * Enderman within {@link #ENDERMITE_LURE} blocks drops its target to
     * chase them (vanilla endermen hate endermites).
     */
    public boolean luresEndermen() {
        return this == ENDERMITE;
    }

    public static final int ENDERMITE_TICKS = 200;
    public static final double ENDERMITE_LURE = 6.0D;

    /** A Vex hits again and again for this long, then fades (not one blow). */
    public static final int VEX_TICKS = 20 * 10;

    /**
     * An Allay does not fight: it flies beside the nearest own troop and
     * heals every own troop within {@link #ALLAY_RADIUS} by its damage once
     * per attack interval.
     */
    public boolean healsAround() {
        return this == ALLAY;
    }

    public static final double ALLAY_RADIUS = 2.0D;

    /**
     * A Magma Cube leaps: an enemy within {@link #LEAP_RANGE} blocks (an
     * archer first) gets jumped on, the landing hurting everything within
     * {@link #LEAP_SPLASH} for {@link #LEAP_DAMAGE} times its hit; once per
     * {@link #LEAP_TICKS}.
     */
    public boolean leaps() {
        return this == MAGMA_CUBE;
    }

    public static final double LEAP_RANGE = 3.0D;
    public static final int LEAP_TICKS = 60;
    public static final double LEAP_SPLASH = 1.0D;
    public static final double LEAP_DAMAGE = 1.5D;

    /**
     * Wither Skull: every unit it hits withers for {@link #WITHER_TICKS},
     * losing {@link #WITHER_PER_SECOND} a second through armour and healing
     * nothing meanwhile. A Wither Skeleton is immune.
     */
    public boolean withers() {
        return this == WITHER_SKULL;
    }

    public static final int WITHER_TICKS = 100;
    public static final double WITHER_PER_SECOND = 1.0D;

    /** Wither Skeletons shrug off the wither, as in vanilla. */
    public boolean witherImmune() {
        return this == WITHER_SKELETON || this == WITHER;
    }

    /**
     * Ender Pearl: the one troop nearest where it lands is moved: an own
     * troop {@link #PEARL_DISTANCE} blocks toward the enemy (never past the
     * river), an enemy as far back toward its own King. Buildings stay.
     */
    public boolean pearls() {
        return this == ENDER_PEARL;
    }

    public static final double PEARL_DISTANCE = 5.0D;

    /**
     * Water Bucket: puts out every fire in its circle (burning troops of
     * both sides, a Strider's trail), pushes enemies away and hurts the
     * enemies that water hurts ({@link #hurtByWater}) by
     * {@link #WATER_DAMAGE}.
     */
    public boolean douses() {
        return this == WATER_BUCKET;
    }

    public static final double WATER_DAMAGE = 12.0D;

    /** Mobs vanilla water hurts: Blaze, Enderman, Snow Golem, Strider. */
    public boolean hurtByWater() {
        return this == BLAZE || this == ENDERMAN || this == SNOW_GOLEM ||
                this == STRIDER;
    }
    /**
     * Villagers: {@link #VILLAGER_CALL_TICKS} after they are played, if at
     * least {@link #VILLAGER_CALL_ALIVE} of the three still stand, they call
     * one Iron Golem (with {@link #CALLED_GOLEM_HEALTH} of a played Golem's
     * health) where they are. A Villager killed by a zombie
     * ({@link #infects}) gets up as the zombie side's Zombie Villager.
     */
    public boolean callsGolem() {
        return this == VILLAGER;
    }

    public static final int VILLAGER_CALL_TICKS = 120;
    public static final int VILLAGER_CALL_ALIVE = 2;
    public static final double CALLED_GOLEM_HEALTH = 0.4D;

    /**
     * Zombies turn the Villagers they kill into Zombie Villagers, and go for
     * a Villager before anything else in sight.
     */
    public boolean infects() {
        return this == ZOMBIE || this == HUSK || this == DROWNED ||
                this == ZOMBIE_VILLAGER;
    }

    /**
     * A Sniffer never attacks: every enemy troop and tower that can reach
     * it goes for it first, and every {@code attackTicks} it digs up a
     * flower that heals each other own troop within {@link #FLOWER_RADIUS}
     * by its damage.
     */
    public boolean digsFlowers() {
        return this == SNIFFER;
    }

    public static final double FLOWER_RADIUS = 2.0D;

    /**
     * A Wandering Trader never attacks. Two Trader Llamas
     * ({@link #TRADER_LLAMA}) come with it, spitting slowing spit; the
     * first time it is hurt it drinks invisibility and cannot be picked as
     * a target for {@link #HIDE_TICKS}.
     */
    public boolean bringsLlamas() {
        return this == WANDERING_TRADER;
    }

    public static final int TRADER_LLAMAS = 2;
    public static final int HIDE_TICKS = 60;

    /** A Trader Llama, the Wandering Trader's escort (not a card of its own). */
    public static final TroopStats TRADER_LLAMA = new TroopStats(1, 22, 3, 30,
            4.5D, 0.9D, 0.45D, TroopTraits.slowing(15));

    /** Troops that never attack (they walk, heal or draw fire). */
    public boolean peaceful() {
        return this == SNIFFER || this == WANDERING_TRADER || this == SHEEP ||
                this == MONSTER_SPAWNER || this == CAMPFIRE || this == BEACON;
    }

    /**
     * Anvil and Dragon's Breath go through half of a troop's armour. The
     * Anvil falls on the one enemy troop nearest where it lands (towers in
     * its circle take their share too).
     */
    public boolean halfArmor() {
        return this == ANVIL || this == DRAGONS_BREATH;
    }

    public boolean dropsOnOne() {
        return this == ANVIL;
    }

    /** Enemies inside Dragon's Breath heal nothing for this long after a pulse. */
    public boolean blocksHealing() {
        return this == DRAGONS_BREATH;
    }

    public static final int BREATH_HEAL_BLOCK_TICKS = 15;

    /**
     * Bell: every enemy in its circle is stunned for its stun time and then
     * walks to the bell for {@link #BELL_LURE_TICKS} without attacking;
     * invisible troops are seen again, an Axolotl stops playing dead,
     * mirror images vanish; the enemy side's Shadow dodge is off for
     * {@link #BELL_DODGE_OFF_TICKS}; an enemy Warden within
     * {@link #BELL_WARDEN_RANGE} comes to the bell.
     */
    public boolean rings() {
        return this == BELL;
    }

    public static final int BELL_LURE_TICKS = 40;
    public static final int BELL_DODGE_OFF_TICKS = 100;
    public static final double BELL_WARDEN_RANGE = 12.0D;
    public static final int BELL_WARDEN_TICKS = 60;

    /**
     * Pillager Captain: its banner gives every other own Illager within
     * {@link #BANNER_RADIUS} +{@link #BANNER_DAMAGE} damage and
     * +{@link #BANNER_SPEED} speed; when it dies the banner stays where it
     * fell for {@link #BANNER_DROP_TICKS}.
     */
    public boolean carriesBanner() {
        return this == PILLAGER_CAPTAIN;
    }

    public static final double BANNER_RADIUS = 4.0D;
    public static final double BANNER_DAMAGE = 0.15D;
    public static final double BANNER_SPEED = 0.10D;
    public static final int BANNER_DROP_TICKS = 100;

    /**
     * Wither: each of its {@link #WITHER_HEADS} heads shoots a skull at its
     * own target in range (splash, and the target withers like the Wither
     * Skull spell); under half health its armour halves every arrow, shot
     * and tower shot.
     */
    public boolean threeHeads() {
        return this == WITHER;
    }

    /** A hit withers its target: the Wither's heads and the Wither Skeleton. */
    public boolean withersOnHit() {
        return threeHeads() || this == WITHER_SKELETON;
    }

    public static final int WITHER_HEADS = 3;
    public static final double WITHER_SHIELD = 0.5D;

    /**
     * Mooshroom: every {@link #STEW_TICKS} it shares stew: each other own
     * troop within {@link #STEW_RADIUS} heals {@link #STEW_HEAL}.
     */
    public boolean sharesStew() {
        return this == MOOSHROOM;
    }

    public static final int STEW_TICKS = 100;
    public static final double STEW_RADIUS = 2.5D;
    public static final double STEW_HEAL = 3.0D;

    /**
     * Parrot: never attacks; it flies beside the nearest own troop and
     * dances: own troops within {@link #DANCE_RADIUS} attack
     * {@link #DANCE_HASTE} times as fast.
     */
    public boolean dances() {
        return this == PARROT;
    }

    public static final double DANCE_RADIUS = 3.0D;
    public static final double DANCE_HASTE = 1.2D;

    /** Troops that never fight and fly beside their own (Allay, Parrot). */
    public boolean followsTroops() {
        return healsAround() || dances();
    }

    /**
     * Glow Squid: the first time it is hurt it squirts ink: enemies within
     * {@link #INK_RADIUS} are blinded (range cut to
     * {@link #BLIND_RANGE}) for {@link #INK_TICKS}. Swims fast on the river
     * and may be played there.
     */
    public boolean squirtsInk() {
        return this == GLOW_SQUID;
    }

    public static final double INK_RADIUS = 2.0D;
    public static final int INK_TICKS = 60;

    /**
     * Sheep: never attacks; its wool takes the first {@link #WOOL_HITS}
     * hits whole (burning and poison go through), then it is sheared.
     */
    public boolean woolly() {
        return this == SHEEP;
    }

    public static final int WOOL_HITS = 2;

    /**
     * Monster Spawner: every {@code attackTicks} a mob a vanilla spawner can
     * hold comes out ({@link #SPAWNER_MOBS}, at {@link #SPAWNED_HEALTH} of
     * its health); {@link #SPAWNER_BREAK_MOBS} more when it breaks.
     */
    public boolean spawnsMobs() {
        return this == MONSTER_SPAWNER;
    }

    /** What vanilla spawners hold, as a weighted list (Zombie twice). */
    public static final java.util.List<BattleCard> SPAWNER_MOBS =
            java.util.List.of(ZOMBIE, ZOMBIE, SKELETON_ARCHER, SPIDER,
                    CAVE_SPIDER, SILVERFISH, BLAZE);
    public static final double SPAWNED_HEALTH = 0.5D;
    public static final int SPAWNER_BREAK_MOBS = 2;

    /**
     * Campfire: enemies within its range burn {@code damage} a second;
     * own troops within {@link #CAMPFIRE_HEAL_RADIUS} heal
     * {@link #CAMPFIRE_HEAL} a second.
     */
    public boolean warms() {
        return this == CAMPFIRE;
    }

    public static final double CAMPFIRE_HEAL_RADIUS = 2.5D;
    public static final double CAMPFIRE_HEAL = 1.0D;

    /**
     * Beacon: own troops within its range get one effect, a new one every
     * {@link #BEACON_PHASE_TICKS}: Speed, Strength, Regeneration,
     * Resistance ({@link BeaconEffect}).
     */
    public boolean beams() {
        return this == BEACON;
    }

    public static final int BEACON_PHASE_TICKS = 200;

    /** The Beacon's effects in turn. */
    public enum BeaconEffect {
        SPEED, STRENGTH, REGENERATION, RESISTANCE;

        public static BeaconEffect at(int ageTicks) {
            return values()[Math.floorMod(ageTicks / BEACON_PHASE_TICKS,
                    values().length)];
        }
    }

    /** Speed and Strength x1.2, Regeneration 1 a second, Resistance -20%. */
    public static final double BEACON_SPEED = 1.2D;
    public static final double BEACON_STRENGTH = 1.2D;
    public static final double BEACON_REGEN = 1.0D;
    public static final double BEACON_RESIST = 0.8D;

    /**
     * Lava Bucket: a pool that burns and slows enemies standing in it;
     * fire-immune troops do not care; towers are left alone.
     */
    public boolean lava() {
        return this == LAVA_BUCKET;
    }

    /**
     * Potion of Harming: enemies take its damage through armour; own undead
     * in it heal by as much (instant damage heals the undead in vanilla).
     */
    public boolean harms() {
        return this == HARMING_POTION;
    }

    /** Firework Rocket: flying troops take {@link #FIREWORK_FLYING} times as much. */
    public boolean firework() {
        return this == FIREWORK_ROCKET;
    }

    public static final double FIREWORK_FLYING = 2.0D;

    public static final double HEAR_RANGE = 8.0D;
    public static final int BOOM_TICKS = 20 * 15;
    public static final double BOOM_RANGE = 6.0D;
    public static final double BOOM_DAMAGE = 20.0D;

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
