package net.susu609.royalarena.combat;

import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.UUID;

/**
 * One fighting body (the unit of a troop card, or a slime child). It is
 * keyed by the id of the world entity that shows it; its position is copied
 * from that entity before every combat tick.
 */
public final class CombatUnit {
    private final UUID id;
    private final TeamSide side;
    private final TroopStats stats;
    private final int generation;
    private final double maxHealth;
    private final double damage;
    private final double radius;

    private double health;
    private double x;
    private double z;
    private int cooldown;
    private int invulnerableTicks;
    private TowerId lockedTower;
    private UUID target;

    CombatUnit(UUID id, TeamSide side, TroopStats stats, int generation,
            double health, double damage, double radius, double x, double z,
            int invulnerableTicks, TowerId lockedTower)
    {
        this.id = id;
        this.side = side;
        this.stats = stats;
        this.generation = generation;
        this.maxHealth = health;
        this.health = health;
        this.damage = damage;
        this.radius = radius;
        this.x = x;
        this.z = z;
        this.invulnerableTicks = invulnerableTicks;
        this.lockedTower = lockedTower;
    }

    public UUID id() {
        return id;
    }
    public TeamSide side() {
        return side;
    }
    public TroopStats stats() {
        return stats;
    }
    /** 0 for the card's own units, 1.. for slime children. */
    public int generation() {
        return generation;
    }
    public double maxHealth() {
        return maxHealth;
    }
    public double health() {
        return health;
    }
    public double damage() {
        return damage;
    }
    public double radius() {
        return radius;
    }
    public double x() {
        return x;
    }
    public double z() {
        return z;
    }
    public boolean alive() {
        return health > 0.0D;
    }
    public boolean invulnerable() {
        return invulnerableTicks > 0;
    }
    public TowerId lockedTower() {
        return lockedTower;
    }
    /** Enemy unit currently chased, or null when heading for a tower. */
    public UUID target() {
        return target;
    }

    public void setPosition(double x, double z) {
        this.x = x;
        this.z = z;
    }

    /** Applies damage unless the unit is still invulnerable. */
    boolean hurt(double amount) {
        if (amount <= 0.0D || !alive() || invulnerable())
            return false;
        health = Math.max(0.0D, health - amount);
        return true;
    }

    void tickTimers() {
        if (cooldown > 0)
            cooldown--;
        if (invulnerableTicks > 0)
            invulnerableTicks--;
    }

    boolean ready() {
        return cooldown <= 0;
    }
    void startCooldown() {
        cooldown = stats.attackTicks();
    }
    void lockTower(TowerId id) {
        lockedTower = id;
    }
    void setTarget(UUID target) {
        this.target = target;
    }
}
