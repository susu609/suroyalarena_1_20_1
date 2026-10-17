package net.susu609.royalarena.combat;

import net.susu609.royalarena.card.BattleCard;
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
    private int burnTicks;
    private double burnPerTick;
    private BattleCard burnSource;
    private int frozenTicks;
    private int slowTicks;
    private int teleportCooldown;
    private int poisonTicks;
    private double poisonPerTick;
    private BattleCard poisonSource;

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
    /** Ticks of burning left (Fireball). */
    public int burnTicks() {
        return burnTicks;
    }
    /** Ticks the unit stays frozen or stunned (Freeze, Lightning). */
    public int frozenTicks() {
        return frozenTicks;
    }
    public boolean frozen() {
        return frozenTicks > 0;
    }
    /** Ticks the unit still walks slowed (a Stray's arrow). */
    public int slowTicks() {
        return slowTicks;
    }
    public boolean slowed() {
        return slowTicks > 0;
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
        if (invulnerableTicks > 0)
            invulnerableTicks--;
        if (slowTicks > 0)
            slowTicks--;
        if (teleportCooldown > 0)
            teleportCooldown--;
        // A frozen unit's attack does not charge.
        if (frozenTicks > 0)
            frozenTicks--;
        else if (cooldown > 0)
            cooldown--;
    }

    /** Whether it may teleport away now (an Enderman, off cooldown). */
    boolean canTeleport() {
        return stats.traits().teleports() && teleportCooldown <= 0;
    }

    void startTeleportCooldown() {
        teleportCooldown = stats.traits().teleportCooldownTicks();
    }

    /** Ticks until it may teleport again (0 when ready). */
    public int teleportCooldown() {
        return teleportCooldown;
    }

    /**
     * Poisons the unit for {@code ticks}, {@code perSecond} damage a second,
     * credited to {@code source}; a new poison replaces an old one.
     */
    void poison(int ticks, double perSecond, BattleCard source) {
        if (ticks <= 0 || perSecond <= 0.0D)
            return;
        poisonTicks = ticks;
        poisonPerTick = perSecond / 20.0D;
        poisonSource = source;
    }

    /** Whether poison is on the unit now. */
    public boolean poisoned() {
        return poisonTicks > 0;
    }

    /** The card whose poison is on the unit, or null. */
    BattleCard poisonSource() {
        return poisonSource;
    }

    /** Poison damage this tick (0 when not poisoned). */
    double poisonStep() {
        if (poisonTicks <= 0)
            return 0.0D;
        poisonTicks--;
        return poisonPerTick;
    }

    /** Heals up to the unit's starting health. */
    boolean heal(double amount) {
        if (amount <= 0.0D || !alive() || health >= maxHealth)
            return false;
        health = Math.min(maxHealth, health + amount);
        return true;
    }

    /** Slows the unit's walk for {@code ticks}; a longer slow wins. */
    void slow(int ticks) {
        slowTicks = Math.max(slowTicks, ticks);
    }

    /** Stops the unit for {@code ticks}; a longer freeze wins. */
    void freeze(int ticks) {
        frozenTicks = Math.max(frozenTicks, ticks);
    }

    /** Stops the unit for {@code ticks} and restarts its attack. */
    void stun(int ticks) {
        freeze(ticks);
        startCooldown();
    }

    /** Kills the unit outright (a Creeper that has exploded). */
    void expire() {
        health = 0.0D;
    }

    /**
     * Sets the unit burning; a new burn replaces the old one. {@code source}
     * is the card that set it alight, credited with the burn damage.
     */
    void ignite(int ticks, double perSecond, BattleCard source) {
        if (ticks <= 0 || perSecond <= 0.0D)
            return;
        burnTicks = ticks;
        burnPerTick = perSecond / 20.0D;
        burnSource = source;
    }

    /** The card whose burn is on the unit, or null. */
    BattleCard burnSource() {
        return burnSource;
    }

    /** Damage the burn deals this tick (0 when not burning). */
    double burnStep() {
        if (burnTicks <= 0)
            return 0.0D;
        burnTicks--;
        return burnPerTick;
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
