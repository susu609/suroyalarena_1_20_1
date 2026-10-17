package net.susu609.royalarena.tower;

public final class TowerState {
    private final TowerId id;
    private final int maxHealth;
    private double health;

    public TowerState(TowerId id, int maxHealth) {
        if (maxHealth <= 0)
            throw new IllegalArgumentException("maxHealth must be positive");
        this.id = id;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    /** Remaining health, rounded up so a living tower never shows 0. */
    public int health() {
        return (int) Math.ceil(health);
    }
    /** Remaining health, unrounded. */
    public double remaining() {
        return health;
    }
    public TowerId id() {
        return id;
    }
    public boolean isDestroyed() {
        return health <= 0;
    }
    public int maxHealth() {
        return maxHealth;
    }

    public int damage(double amount) {
        if (amount <= 0 || isDestroyed())
            return health();
        health = Math.max(0.0D, health - amount);
        return health();
    }
}
