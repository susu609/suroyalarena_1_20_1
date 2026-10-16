package net.susu609.royalarena.combat;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.arena.ArenaWaypoint;
import net.susu609.royalarena.card.SplitRule;
import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.combat.TickResult.Attack;
import net.susu609.royalarena.combat.TickResult.Death;
import net.susu609.royalarena.combat.TickResult.Move;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;
import net.susu609.royalarena.tower.TowerKind;
import net.susu609.royalarena.tower.TowerState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pure combat rules for one match: who each unit chases, who is in range,
 * who hits whom and when units and towers die. It owns unit health and all
 * cooldowns and writes tower damage into the {@link MatchInstance}; moving
 * the world entities and drawing effects is left to the caller.
 *
 * <p>Within a tick every decision uses the state at the start of the tick,
 * then all damage is applied at once, so two units that hit each other in
 * the same tick both land their hit.</p>
 */
public final class CombatEngine {
    /** A chased enemy is only dropped once it is this much farther than sight. */
    private static final double LEASH_FACTOR = 1.25D;
    private static final double EPSILON = 1.0E-6D;

    private final MatchInstance match;
    private final ArenaLayout arena;
    private final List<CombatUnit> units = new ArrayList<>();
    private final Map<TowerId, Integer> towerCooldown =
            new EnumMap<>(TowerId.class);
    private final Map<TowerId, UUID> towerTarget =
            new EnumMap<>(TowerId.class);
    private double speedMultiplier = 1.0D;

    public CombatEngine(MatchInstance match) {
        this.match = match;
        this.arena = match.arena();
    }

    /** Registers a card's own unit at (x, z). */
    public CombatUnit add(UUID entityId, TeamSide side, TroopStats stats,
            double x, double z)
    {
        return register(new CombatUnit(
                entityId, side, stats, 0, stats.health(), stats.damage(),
                stats.radius(), x, z, 0, null));
    }

    /** Registers a unit created by a split. */
    public CombatUnit add(UUID entityId, ChildSpawn spawn) {
        return register(new CombatUnit(
                entityId, spawn.side(), spawn.stats(), spawn.generation(),
                spawn.health(), spawn.damage(), spawn.radius(), spawn.x(),
                spawn.z(), spawn.invulnerableTicks(), spawn.lockedTower()));
    }

    private CombatUnit register(CombatUnit unit) {
        units.add(unit);
        return unit;
    }

    public void remove(UUID entityId) {
        units.removeIf(u -> u.id().equals(entityId));
    }

    /** Scales every troop's walking speed (config: troops.speedMultiplier). */
    public void setSpeedMultiplier(double speedMultiplier) {
        this.speedMultiplier = Math.max(0.0D, speedMultiplier);
    }

    public CombatUnit unit(UUID entityId) {
        for (CombatUnit unit : units) {
            if (unit.id().equals(entityId))
                return unit;
        }
        return null;
    }

    public List<CombatUnit> units() {
        return Collections.unmodifiableList(units);
    }

    public TickResult tick() {
        List<Move> moves = new ArrayList<>();
        List<Attack> attacks = new ArrayList<>();
        for (CombatUnit unit : units) {
            unit.tickTimers();
            planUnit(unit, moves, attacks);
        }
        planTowers(attacks);

        List<TowerId> destroyed = new ArrayList<>();
        applyDamage(attacks, destroyed);
        List<Death> deaths = collectDeaths();
        return new TickResult(moves, attacks, deaths, destroyed);
    }

    /** Whether a King tower may shoot: it was hit, or a Princess tower fell. */
    public boolean kingAwake(TeamSide side) {
        TowerState king = match.tower(TowerId.king(side));
        return king.health() < king.maxHealth() ||
                match.tower(TowerId.left(side)).isDestroyed() ||
                match.tower(TowerId.right(side)).isDestroyed();
    }

    // ---------------------------------------------------------------- units

    private void planUnit(CombatUnit unit, List<Move> moves,
            List<Attack> attacks)
    {
        TroopStats stats = unit.stats();
        CombatUnit enemy = pickEnemy(unit);
        unit.setTarget(enemy == null ? null : enemy.id());

        double targetX;
        double targetZ;
        boolean inRange;
        TowerId tower = null;
        if (enemy != null) {
            targetX = enemy.x();
            targetZ = enemy.z();
            inRange = edgeDistance(unit, enemy) <= stats.range() + EPSILON;
        } else {
            tower = laneTower(unit);
            ArenaPoint point = arena.towerPoint(tower);
            targetX = point.x();
            targetZ = point.z();
            inRange = edgeDistanceToTower(unit, tower) <=
                    stats.range() + EPSILON;
        }

        if (!inRange) {
            ArenaWaypoint waypoint = arena.route(
                    unit.x(), unit.z(), targetX, targetZ);
            moves.add(new Move(unit.id(), waypoint.x(), waypoint.z(),
                    stats.speedPerTick() * speedMultiplier));
            return;
        }
        if (!unit.ready())
            return;
        unit.startCooldown();
        boolean ranged = !stats.melee();
        if (enemy != null) {
            attacks.add(new Attack(unit.id(), null, enemy.id(), null,
                    unit.damage(), ranged));
        } else {
            attacks.add(new Attack(unit.id(), null, null, tower,
                    unit.damage(), ranged));
        }
    }

    /**
     * Enemy to chase: the one already chased while it stays within the
     * leash, otherwise the nearest one in sight. Melee units ignore enemies
     * across the river unless those stand on the bridges.
     */
    private CombatUnit pickEnemy(CombatUnit unit) {
        double sight = unit.stats().sight();
        CombatUnit current = unit.target() == null ? null :
                unit(unit.target());
        if (current != null && current.alive() &&
                current.side() != unit.side() && canTarget(unit, current) &&
                distance(unit, current) <= sight * LEASH_FACTOR)
            return current;

        CombatUnit best = null;
        double bestDistance = Double.MAX_VALUE;
        for (CombatUnit other : units) {
            if (other.side() == unit.side() || !other.alive() ||
                    !canTarget(unit, other))
                continue;
            double d = distance(unit, other);
            if (d <= sight && d < bestDistance) {
                best = other;
                bestDistance = d;
            }
        }
        return best;
    }

    private boolean canTarget(CombatUnit unit, CombatUnit other) {
        if (!unit.stats().melee())
            return true;
        int mine = arena.sideOf(unit.z());
        int theirs = arena.sideOf(other.z());
        return mine == theirs || mine == 0 || theirs == 0;
    }

    /** The lane tower chosen at first sight, then the King once it falls. */
    private TowerId laneTower(CombatUnit unit) {
        TowerId locked = unit.lockedTower();
        if (locked == null) {
            locked = match.chooseTowerTarget(unit.side(), unit.x());
            unit.lockTower(locked);
        } else if (match.tower(locked).isDestroyed()) {
            locked = TowerId.king(unit.side().opponent());
            unit.lockTower(locked);
        }
        return locked;
    }

    private static double distance(CombatUnit a, CombatUnit b) {
        return Math.hypot(a.x() - b.x(), a.z() - b.z());
    }

    private static double edgeDistance(CombatUnit a, CombatUnit b) {
        return distance(a, b) - a.radius() - b.radius();
    }

    /** Distance from the unit's edge to the nearest edge of the tower. */
    private double edgeDistanceToTower(CombatUnit unit, TowerId id) {
        ArenaPoint p = arena.towerPoint(id);
        double half = ArenaLayout.towerHalfSize(id.kind());
        double dx = Math.max(Math.abs(unit.x() - p.x()) - half, 0.0D);
        double dz = Math.max(Math.abs(unit.z() - p.z()) - half, 0.0D);
        return Math.hypot(dx, dz) - unit.radius();
    }

    // --------------------------------------------------------------- towers

    private void planTowers(List<Attack> attacks) {
        for (TowerId id : TowerId.values()) {
            TowerState state = match.tower(id);
            if (state.isDestroyed())
                continue;
            if (id.kind() == TowerKind.KING && !kingAwake(id.side()))
                continue;
            int cooldown = towerCooldown.getOrDefault(id, 0);
            if (cooldown > 0) {
                towerCooldown.put(id, cooldown - 1);
                if (cooldown - 1 > 0)
                    continue;
            }
            CombatUnit target = pickTowerTarget(id);
            towerTarget.put(id, target == null ? null : target.id());
            if (target == null)
                continue;
            towerCooldown.put(id, id.kind().attackTicks());
            attacks.add(new Attack(null, id, target.id(), null,
                    id.kind().damage(), true));
        }
    }

    /**
     * Keeps shooting the same enemy while in range, else the nearest. Tower
     * range is measured from the tower's edge to the unit's edge, like the
     * ranges of units, so a tower (7.5) always outranges an archer (6).
     */
    private CombatUnit pickTowerTarget(TowerId id) {
        double range = id.kind().range();
        UUID currentId = towerTarget.get(id);
        CombatUnit current = currentId == null ? null : unit(currentId);
        if (current != null && current.alive() &&
                edgeDistanceToTower(current, id) <= range + EPSILON)
            return current;
        CombatUnit best = null;
        double bestDistance = Double.MAX_VALUE;
        for (CombatUnit unit : units) {
            if (unit.side() == id.side() || !unit.alive())
                continue;
            double d = edgeDistanceToTower(unit, id);
            if (d <= range + EPSILON && d < bestDistance) {
                best = unit;
                bestDistance = d;
            }
        }
        return best;
    }

    // ----------------------------------------------------- damage and deaths

    private void applyDamage(List<Attack> attacks, List<TowerId> destroyed) {
        for (Attack attack : attacks) {
            if (attack.victimUnit() != null) {
                CombatUnit victim = unit(attack.victimUnit());
                if (victim != null)
                    victim.hurt(attack.damage());
            } else {
                TowerState tower = match.tower(attack.victimTower());
                boolean wasStanding = !tower.isDestroyed();
                match.damageTower(attack.victimTower(), attack.damage());
                if (wasStanding && tower.isDestroyed())
                    destroyed.add(attack.victimTower());
            }
        }
    }

    private List<Death> collectDeaths() {
        List<Death> deaths = new ArrayList<>();
        for (int i = 0; i < units.size(); i++) {
            CombatUnit unit = units.get(i);
            if (unit.alive())
                continue;
            units.remove(i--);
            deaths.add(new Death(unit.id(), unit.x(), unit.z(),
                    childrenOf(unit)));
        }
        return deaths;
    }

    private List<ChildSpawn> childrenOf(CombatUnit parent) {
        SplitRule rule = parent.stats().split();
        if (rule == null || parent.generation() >= rule.generations())
            return List.of();
        List<ChildSpawn> children = new ArrayList<>(rule.children());
        int sign = ArenaLayout.sideSign(parent.side());
        for (int i = 0; i < rule.children(); i++) {
            double angle = Math.PI * 2.0D * i / rule.children() +
                    parent.generation() * 0.8D;
            double x = parent.x() + Math.cos(angle) * rule.spread() * 0.5D;
            double z = parent.z() + Math.sin(angle) * rule.spread() * 0.5D;
            ArenaWaypoint spot = arena.clampSpawn(x, z, sign);
            children.add(new ChildSpawn(
                    parent.side(), parent.stats(), parent.generation() + 1,
                    parent.maxHealth() * rule.healthFactor(),
                    parent.damage() * rule.damageFactor(),
                    parent.radius() * rule.radiusFactor(), spot.x(), spot.z(),
                    rule.invulnerableTicks(), parent.lockedTower()));
        }
        return children;
    }
}
