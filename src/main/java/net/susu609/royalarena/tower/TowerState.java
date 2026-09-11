package net.susu609.royalarena.tower;

public final class TowerState {
    private final TowerId id;
    private final int maxHealth;
    private int health;

    public TowerState(TowerId id, int maxHealth) {
        if (maxHealth <= 0)
            throw new IllegalArgumentException("maxHealth must be positive");
        this.id = id;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    public int health() {
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

    public int damage(int amount) {
        if (amount <= 0 || isDestroyed())
            return health;
        health = Math.max(0, health - amount);
        return health;
    }
}
