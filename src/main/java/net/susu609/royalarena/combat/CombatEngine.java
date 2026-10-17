package net.susu609.royalarena.combat;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.arena.ArenaWaypoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.SpellStats;
import net.susu609.royalarena.card.SplitRule;
import net.susu609.royalarena.card.TroopTraits;
import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.combat.TickResult.Attack;
import net.susu609.royalarena.combat.TickResult.Death;
import net.susu609.royalarena.combat.TickResult.Move;
import net.susu609.royalarena.combat.TickResult.Push;
import net.susu609.royalarena.combat.TickResult.SpellPulse;
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
 * the same tick both land their hit. Spell pulses are decided the same way
 * and land together with the attacks.</p>
 */
public final class CombatEngine {
    /** A chased enemy is only dropped once it is this much farther than sight. */
    private static final double LEASH_FACTOR = 1.25D;
    private static final double EPSILON = 1.0E-6D;
    /** Arrow speed in blocks per tick (16 blocks per second). */
    public static final double PROJECTILE_SPEED = 0.8D;
    public static final int MIN_FLIGHT_TICKS = 2;
    public static final int MAX_FLIGHT_TICKS = 15;

    private final MatchInstance match;
    private final ArenaLayout arena;
    private final List<CombatUnit> units = new ArrayList<>();
    private final Map<TowerId, Integer> towerCooldown =
            new EnumMap<>(TowerId.class);
    private final Map<TowerId, UUID> towerTarget =
            new EnumMap<>(TowerId.class);
    private final List<ActiveSpell> spells = new ArrayList<>();
    /** Shots in the air, with the ticks until they land. */
    private final List<InFlight> flying = new ArrayList<>();
    private double speedMultiplier = 1.0D;

    /** A spell that has landed and still has pulses to deal. */
    private static final class ActiveSpell {
        final TeamSide side;
        final BattleCard card;
        final double x;
        final double z;
        int pulsesDone;
        int wait;

        ActiveSpell(TeamSide side, BattleCard card, double x, double z) {
            this.side = side;
            this.card = card;
            this.x = x;
            this.z = z;
        }
    }

    private static final class InFlight {
        final Attack attack;
        int ticksLeft;

        InFlight(Attack attack) {
            this.attack = attack;
            this.ticksLeft = attack.flightTicks();
        }
    }

    /** Damage decided this tick, applied after every decision is made. */
    private record Hit(CombatUnit unit, TowerId tower, double damage,
                       ActiveSpell spell)
    {
    }

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

    /**
     * A spell card lands centred on (x, z). Its first pulse is dealt on the
     * next {@link #tick()}.
     */
    public void castSpell(TeamSide side, BattleCard card, double x, double z) {
        if (!card.isSpell())
            throw new IllegalArgumentException(card.id() + " is not a spell");
        spells.add(new ActiveSpell(side, card, x, z));
    }

    /** Spells that have landed and are still pulsing. */
    public int activeSpells() {
        return spells.size();
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
        List<SpellPulse> pulses = new ArrayList<>();
        List<Hit> hits = planSpells(pulses);

        List<TowerId> destroyed = new ArrayList<>();
        List<Attack> landed = new ArrayList<>();
        // Shots already in the air land first; this tick's shots take off.
        for (int i = 0; i < flying.size(); i++) {
            InFlight shot = flying.get(i);
            if (--shot.ticksLeft <= 0) {
                landed.add(shot.attack);
                flying.remove(i--);
            }
        }
        for (Attack attack : attacks) {
            if (attack.flightTicks() > 0)
                flying.add(new InFlight(attack));
            else
                landed.add(attack);
        }
        List<Push> pushes = new ArrayList<>();
        applyDamage(landed, destroyed, pushes);
        applyBurns();
        applySpellHits(hits, destroyed, pushes);
        List<Death> deaths = collectDeaths();
        return new TickResult(moves, attacks, deaths, destroyed, pulses,
                pushes, landed);
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
        int flight = ranged ? flightTicks(unit.x(), unit.z(), targetX,
                targetZ) : 0;
        TroopTraits traits = stats.traits();
        attacks.add(new Attack(unit.id(), null,
                enemy == null ? null : enemy.id(), enemy == null ? tower : null,
                unit.damage(), ranged, flight, unit.side(), targetX, targetZ,
                traits.splashRadius(), ranged ? 0.0D : traits.knockback()));
        if (traits.selfDestruct())
            unit.expire();
    }

    /**
     * Enemy to chase: the one already chased while it stays within the
     * leash, otherwise the nearest one in sight. Melee units ignore enemies
     * across the river unless those stand on the bridges, and a unit that
     * only targets buildings ignores every troop.
     */
    private CombatUnit pickEnemy(CombatUnit unit) {
        if (unit.stats().traits().buildingsOnly())
            return null;
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

    /** Ticks an arrow needs from (fx, fz) to (tx, tz). */
    public static int flightTicks(double fx, double fz, double tx,
            double tz)
    {
        int ticks = (int) Math.round(Math.hypot(tx - fx, tz - fz) /
                PROJECTILE_SPEED);
        return Math.max(MIN_FLIGHT_TICKS, Math.min(MAX_FLIGHT_TICKS, ticks));
    }

    /** Shots still in the air. */
    public int shotsInFlight() {
        return flying.size();
    }

    private static double distance(CombatUnit a, CombatUnit b) {
        return Math.hypot(a.x() - b.x(), a.z() - b.z());
    }

    private static double edgeDistance(CombatUnit a, CombatUnit b) {
        return distance(a, b) - a.radius() - b.radius();
    }

    /** Distance from the unit's edge to the nearest edge of the tower. */
    private double edgeDistanceToTower(CombatUnit unit, TowerId id) {
        return distanceToTower(unit.x(), unit.z(), id) - unit.radius();
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
            ArenaPoint from = arena.towerPoint(id);
            attacks.add(new Attack(null, id, target.id(), null,
                    id.kind().damage(), true,
                    flightTicks(from.x(), from.z(), target.x(),
                            target.z()), id.side(), target.x(), target.z()));
        }
    }

    /**
     * Keeps shooting the same enemy while in range, else the nearest. Tower
     * range is measured from the tower's edge to the unit's edge, like the
     * ranges of units, so a Princess tower (7.0) outranges every troop.
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

    // --------------------------------------------------------------- spells

    /** Pulses every landed spell whose wait ran out; drops finished ones. */
    private List<Hit> planSpells(List<SpellPulse> pulses) {
        List<Hit> hits = new ArrayList<>();
        for (int i = 0; i < spells.size(); i++) {
            ActiveSpell active = spells.get(i);
            if (active.wait > 0) {
                active.wait--;
                continue;
            }
            SpellStats spell = active.card.spell();
            int before = hits.size();
            for (CombatUnit unit : units) {
                if (unit.side() == active.side || !unit.alive())
                    continue;
                double reach = spell.radius() + unit.radius();
                if (Math.hypot(unit.x() - active.x, unit.z() - active.z) <=
                        reach + EPSILON)
                    hits.add(new Hit(unit, null, spell.damagePerPulse(),
                            active));
            }
            for (TowerId id : TowerId.values()) {
                if (id.side() == active.side ||
                        match.tower(id).isDestroyed())
                    continue;
                if (distanceToTower(active.x, active.z, id) <=
                        spell.radius() + EPSILON)
                    hits.add(new Hit(null, id, spell.damagePerPulse() *
                            spell.towerMultiplier(), active));
            }
            pulses.add(new SpellPulse(active.side, active.card, active.x,
                    active.z, active.pulsesDone, hits.size() - before));
            active.pulsesDone++;
            active.wait = spell.pulseInterval() - 1;
            if (active.pulsesDone >= spell.pulses())
                spells.remove(i--);
        }
        return hits;
    }

    private void applySpellHits(List<Hit> hits, List<TowerId> destroyed,
            List<Push> pushes)
    {
        for (Hit hit : hits) {
            SpellStats spell = hit.spell().card.spell();
            if (hit.tower() != null) {
                hitTower(hit.tower(), hit.damage(), destroyed);
                continue;
            }
            CombatUnit unit = hit.unit();
            if (!unit.hurt(hit.damage()))
                continue;
            unit.ignite(spell.burnTicks(), spell.burnPerSecond());
            if (spell.knockback() > 0.0D && unit.alive())
                knockBack(unit, hit.spell().x, hit.spell().z,
                        -ArenaLayout.sideSign(hit.spell().side),
                        spell.knockback(), pushes);
        }
    }

    /**
     * Pushes a unit straight away from (fx, fz), kept on land; a unit right
     * on that point goes {@code fallbackSign} along Z.
     */
    private void knockBack(CombatUnit unit, double fx, double fz,
            int fallbackSign, double blocks, List<Push> pushes)
    {
        double dx = unit.x() - fx;
        double dz = unit.z() - fz;
        double length = Math.hypot(dx, dz);
        if (length < 1.0E-3D) {
            dx = 0.0D;
            dz = fallbackSign;
            length = 1.0D;
        }
        ArenaWaypoint spot = arena.clampSpawn(
                unit.x() + dx / length * blocks,
                unit.z() + dz / length * blocks,
                ArenaLayout.sideSign(unit.side()));
        pushes.add(new Push(unit.id(), spot.x() - unit.x(),
                spot.z() - unit.z()));
        unit.setPosition(spot.x(), spot.z());
    }

    private void applyBurns() {
        for (CombatUnit unit : units) {
            double burn = unit.burnStep();
            if (burn > 0.0D)
                unit.hurt(burn);
        }
    }

    /** Distance from a point to the nearest edge of a tower's footprint. */
    private double distanceToTower(double x, double z, TowerId id) {
        return arena.distanceToTower(x, z, id);
    }

    // ----------------------------------------------------- damage and deaths

    private void applyDamage(List<Attack> attacks, List<TowerId> destroyed,
            List<Push> pushes)
    {
        for (Attack attack : attacks) {
            CombatUnit victim = null;
            if (attack.victimUnit() != null) {
                victim = unit(attack.victimUnit());
                if (victim != null)
                    victim.hurt(attack.damage());
            } else {
                hitTower(attack.victimTower(), attack.damage(), destroyed);
            }
            if (attack.splash() > 0.0D)
                splash(attack, victim, destroyed);
            if (attack.knockback() > 0.0D && victim != null && victim.alive()) {
                CombatUnit attacker = attack.attackerUnit() == null ? null :
                        unit(attack.attackerUnit());
                double fx = attacker != null ? attacker.x() : attack.x();
                double fz = attacker != null ? attacker.z() : attack.z();
                knockBack(victim, fx, fz,
                        -ArenaLayout.sideSign(attack.side()),
                        attack.knockback(), pushes);
            }
        }
    }

    private void hitTower(TowerId id, double damage, List<TowerId> destroyed) {
        TowerState tower = match.tower(id);
        boolean wasStanding = !tower.isDestroyed();
        match.damageTower(id, damage);
        if (wasStanding && tower.isDestroyed())
            destroyed.add(id);
    }

    /**
     * The hit also lands on every other enemy within the splash radius of
     * the target: units whose body touches the circle, towers whose
     * footprint does. A target that died meanwhile still splashes where it
     * stood when it was hit.
     */
    private void splash(Attack attack, CombatUnit victim,
            List<TowerId> destroyed)
    {
        double cx = victim != null ? victim.x() : attack.x();
        double cz = victim != null ? victim.z() : attack.z();
        for (CombatUnit other : units) {
            if (other == victim || other.side() == attack.side() ||
                    !other.alive())
                continue;
            if (Math.hypot(other.x() - cx, other.z() - cz) <=
                    attack.splash() + other.radius() + EPSILON)
                other.hurt(attack.damage());
        }
        for (TowerId id : TowerId.values()) {
            if (id == attack.victimTower() || id.side() == attack.side() ||
                    match.tower(id).isDestroyed())
                continue;
            if (distanceToTower(cx, cz, id) <= attack.splash() + EPSILON)
                hitTower(id, attack.damage(), destroyed);
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
