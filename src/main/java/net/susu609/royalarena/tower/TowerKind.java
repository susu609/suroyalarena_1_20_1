package net.susu609.royalarena.tower;

public enum TowerKind {
    PRINCESS(1400), KING(2400);

    private final int maxHealth;

    TowerKind(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public int maxHealth() {
        return maxHealth;
    }
}
