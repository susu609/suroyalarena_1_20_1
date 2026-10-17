package net.susu609.royalarena.tower;

/** Tower stats on the vanilla health scale (docs/BALANCE.md). */
public enum TowerKind {
    PRINCESS(110, 3, 16, 7.0D), KING(185, 4, 20, 7.0D);

    private final int maxHealth;
    private final double damage;
    private final int attackTicks;
    private final double range;

    TowerKind(int maxHealth, double damage, int attackTicks, double range) {
        this.maxHealth = maxHealth;
        this.damage = damage;
        this.attackTicks = attackTicks;
        this.range = range;
    }

    public int maxHealth() {
        return maxHealth;
    }
    public double damage() {
        return damage;
    }
    public int attackTicks() {
        return attackTicks;
    }
    /** Attack range from the tower edge to the target edge, in blocks. */
    public double range() {
        return range;
    }
}
