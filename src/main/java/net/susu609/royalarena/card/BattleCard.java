package net.susu609.royalarena.card;

import java.util.Locale;

public enum BattleCard {
    ZOMBIE("Zombie", 3, "minecraft:zombie", 120, 0.105D),
    SKELETON("Skeletons", 3, "minecraft:skeleton", 95, 0.120D),
    SPIDER("Spider", 3, "minecraft:spider", 105, 0.145D),
    SLIME("Slime", 2, "minecraft:slime", 75, 0.095D),
    WOLF("Wolf", 2, "minecraft:wolf", 80, 0.155D),
    BLAZE("Blaze", 4, "minecraft:blaze", 135, 0.105D),
    VINDICATOR("Vindicator", 4, "minecraft:vindicator", 160, 0.110D),
    IRON_GOLEM("Iron Golem", 5, "minecraft:iron_golem", 220, 0.075D);

    private final String displayName;
    private final int cost;
    private final String entityId;
    private final int towerDamage;
    private final double moveSpeed;

    BattleCard(String displayName, int cost, String entityId, int towerDamage,
            double moveSpeed)
    {
        this.displayName = displayName;
        this.cost = cost;
        this.entityId = entityId;
        this.towerDamage = towerDamage;
        this.moveSpeed = moveSpeed;
    }
    public static BattleCard byId(String id) {
        for (BattleCard card : values()) {
            if (card.id().equals(id))
                return card;
        }
        throw new IllegalArgumentException("Unknown battle card: " + id);
    }
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
    public int cost() {
        return cost;
    }
    public String displayName() {
        return displayName;
    }
    public String entityId() {
        return entityId;
    }
    public double moveSpeed() {
        return moveSpeed;
    }
    public int towerDamage() {
        return towerDamage;
    }
}
