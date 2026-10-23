package net.susu609.royalarena.combat;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.arena.ArenaWaypoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.Body;
import net.susu609.royalarena.card.SpellStats;
import net.susu609.royalarena.card.SplitRule;
import net.susu609.royalarena.card.Tribe;
import net.susu609.royalarena.card.TribeBonus;
import net.susu609.royalarena.card.TroopTraits;
import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.combat.TickResult.Attack;
import net.susu609.royalarena.combat.TickResult.Death;
import net.susu609.royalarena.combat.TickResult.Move;
import net.susu609.royalarena.combat.TickResult.Push;
import net.susu609.royalarena.combat.TickResult.SpellPulse;
import net.susu609.royalarena.combat.TickResult.TribeEvent;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;
import net.susu609.royalarena.tower.TowerKind;
import net.susu609.royalarena.tower.TowerState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    /** How far an Enderman steps when it teleports, in blocks. */
    public static final double BLINK_DISTANCE = 4.5D;

    private final MatchInstance match;
    private final ArenaLayout arena;
    private final List<CombatUnit> units = new ArrayList<>();
    private final Map<TowerId, Integer> towerCooldown =
            new EnumMap<>(TowerId.class);
    private final Map<TowerId, UUID> towerTarget =
            new EnumMap<>(TowerId.class);
    /** Ticks each frozen (or stunned) tower stays silent. */
    private final Map<TowerId, Integer> towerFrozen =
            new EnumMap<>(TowerId.class);
    private final List<ActiveSpell> spells = new ArrayList<>();
    /** Walking speed of a hastened troop (Swiftness), times its own. */
    public static final double HASTE_SPEED = 1.5D;
    /** Walking speed left in a web (Cobweb, Snowball Volley). */
    public static final double WEB_SPEED = 0.4D;
    /** The card every unit came from (split children included). */
    private final Map<UUID, BattleCard> cardOf = new HashMap<>();
    private final CombatStats stats = new CombatStats();
    /** Teleports made during the tick being computed. */
    private final List<TickResult.Teleport> teleports = new ArrayList<>();
    /** Shots in the air, with the ticks until they land. */
    private final List<InFlight> flying = new ArrayList<>();
    private double speedMultiplier = 1.0D;
    /** Tribe bonuses (docs/TRIBES.md); off unless the match turns them on. */
    private boolean tribes;
    private final Map<TeamSide, Map<Tribe, Integer>> tribeLevels =
            new EnumMap<>(TeamSide.class);
    private final List<TribeEvent> tribeEvents = new ArrayList<>();
    /** Burning patches a Strider leaves behind it. */
    private final List<FirePatch> firePatches = new ArrayList<>();
    /** Units that appear this tick without a death (mirror images). */
    private final List<ChildSpawn> spawns = new ArrayList<>();
    /** Engine tick each unit was added (Endermites and images vanish). */
    private final Map<UUID, Integer> bornAt = new HashMap<>();
    /** Shots that blind where they land (an Illusioner's every 3rd). */
    private final Set<Attack> blinding =
            Collections.newSetFromMap(new IdentityHashMap<>());
    /** Shots each Illusioner has fired. */
    private final Map<UUID, Integer> illusionerShots = new HashMap<>();
    /** Illusioners that have already split off their images. */
    private final Set<UUID> illusionsCast = new HashSet<>();
    /** When each Magma Cube may leap again (engine tick). */
    private final Map<UUID, Integer> leapReady = new HashMap<>();
    /** Units to appear on the next tick (a Wandering Trader's llamas). */
    private final List<ChildSpawn> pendingSpawns = new ArrayList<>();
    /** The Villagers of each play (side and tick), until they call a Golem. */
    private final Map<String, List<UUID>> villages = new HashMap<>();
    private final Map<String, Integer> villageBorn = new HashMap<>();
    /** Villagers killed by a zombie, and the side they rise for. */
    private final Map<UUID, TeamSide> converted = new HashMap<>();
    /** Banners Pillager Captains dropped as they fell. */
    private final List<Banner> banners = new ArrayList<>();
    /** Sheep wool hits left. */
    private final Map<UUID, Integer> wool = new HashMap<>();
    /** Glow Squids that have squirted their ink. */
    private final Set<UUID> inked = new HashSet<>();
    /** Spawns a Monster Spawner makes: which mob comes out next. */
    private final java.util.Random spawnerDice = new java.util.Random(609L);

    private static final class Banner {
        final TeamSide side;
        final double x;
        final double z;
        int ticksLeft;

        Banner(TeamSide side, double x, double z, int ticksLeft) {
            this.side = side;
            this.x = x;
            this.z = z;
            this.ticksLeft = ticksLeft;
        }
    }

    private static final class FirePatch {
        final TeamSide side;
        final BattleCard source;
        final double x;
        final double z;
        int ticksLeft;

        FirePatch(TeamSide side, BattleCard source, double x, double z,
                int ticksLeft)
        {
            this.side = side;
            this.source = source;
            this.x = x;
            this.z = z;
            this.ticksLeft = ticksLeft;
        }
    }

    /** Ticks played, and when each side may use a saving rule again. */
    private int now;
    private final Map<TeamSide, Integer> reviveReady = new EnumMap<>(TeamSide.class);
    private final Map<TeamSide, Integer> totemReady = new EnumMap<>(TeamSide.class);
    private final Map<TeamSide, Integer> dodgeReady = new EnumMap<>(TeamSide.class);

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

    /**
     * Turns the tribe bonuses on or off for the units added from now on
     * (the game and the bot matches turn them on; most tests leave them off
     * so a card's numbers stay its own).
     */
    public void setTribes(boolean on) {
        this.tribes = on;
        tribeLevels.clear();
    }

    public boolean tribes() {
        return tribes;
    }

    /** The tribe steps a side's deck has reached (empty when off). */
    public Map<Tribe, Integer> tribeLevels(TeamSide side) {
        if (!tribes)
            return Map.of();
        // A sandbox picks each side's steps by hand, not from a deck.
        if (match.sandbox() != null)
            return match.sandbox().tribeLevels(side);
        return tribeLevels.computeIfAbsent(side,
                s -> Tribe.levels(match.player(s).deck().cards()));
    }

    /** What a card's tribes give it on {@code side}. */
    public TribeBonus tribeBonus(BattleCard card, TeamSide side) {
        return tribes ? Tribe.bonusFor(card, tribeLevels(side)) :
                TribeBonus.NONE;
    }

    /** Registers a card's own unit at (x, z). */
    public CombatUnit add(UUID entityId, TeamSide side, TroopStats stats,
            double x, double z)
    {
        TribeBonus bonus = tribeBonus(BattleCard.forStats(stats), side);
        CombatUnit unit = new CombatUnit(entityId, side, stats, 0,
                stats.health() * bonus.health(),
                stats.damage() * bonus.damage(), stats.radius(), x, z, 0, null);
        unit.setTribe(bonus);
        return register(unit);
    }

    /**
     * Registers a unit created by a split; its health and damage already
     * carry its tribe bonus ({@link #childrenOf}).
     */
    /** The card a rider about to be added fights for (the played card). */
    private final Map<ChildSpawn, BattleCard> riderCredit =
            new java.util.IdentityHashMap<>();
    /** Riders' played cards, by unit (everyone else: {@link #cardOf}). */
    private final Map<UUID, BattleCard> creditOf = new java.util.HashMap<>();

    /** The played card a unit's damage counts for in the match stats. */
    private BattleCard creditOf(UUID unit) {
        BattleCard credit = creditOf.get(unit);
        return credit != null ? credit : cardOf.get(unit);
    }

    /** Mounts that got their rider at deploy (none splits off at death). */
    private final Set<UUID> carrying = new HashSet<>();

    /**
     * A jockey's rider ({@code SplitRule.into}), on {@code mount} from the
     * start: two lives. It rides and fights from the saddle (shooting only
     * buildings while its mount goes for buildings only); whichever dies
     * first, the other goes on alone. Its damage counts for the played card.
     */
    public CombatUnit addRider(UUID entityId, CombatUnit mount) {
        TroopStats rider = mount.stats().split().into();
        TribeBonus bonus = tribeBonus(BattleCard.forStats(rider), mount.side());
        CombatUnit unit = new CombatUnit(entityId, mount.side(), rider, 1,
                rider.health() * bonus.health(), rider.damage() * bonus.damage(),
                rider.radius(), mount.x(), mount.z(), 0, mount.lockedTower());
        unit.setTribe(bonus);
        unit.setMount(mount.id());
        creditOf.put(entityId, creditOf(mount.id()));
        carrying.add(mount.id());
        return register(unit);
    }

    /** Riders sit where their mount is; one whose mount fell walks on. */
    private void seatRiders() {
        for (CombatUnit unit : units) {
            if (unit.mount() == null)
                continue;
            CombatUnit mount = unit(unit.mount());
            if (mount == null || !mount.alive())
                unit.setMount(null);
            else
                unit.setPosition(mount.x(), mount.z());
        }
    }

    public CombatUnit add(UUID entityId, ChildSpawn spawn) {
        BattleCard credit = riderCredit.remove(spawn);
        if (credit != null)
            creditOf.put(entityId, credit);
        CombatUnit unit = new CombatUnit(
                entityId, spawn.side(), spawn.stats(), spawn.generation(),
                spawn.health(), spawn.damage(), spawn.radius(), spawn.x(),
                spawn.z(), spawn.invulnerableTicks(), spawn.lockedTower());
        unit.setTribe(tribeBonus(BattleCard.forStats(spawn.stats()),
                spawn.side()));
        return register(unit);
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
        BattleCard card = BattleCard.forStats(unit.stats());
        cardOf.put(unit.id(), card);
        bornAt.put(unit.id(), now);
        if (card != null && card.bringsLlamas() && unit.generation() == 0)
            escorts(unit);
        if (card != null && card.callsGolem() && unit.generation() == 0) {
            String key = unit.side() + ":" + now;
            villages.computeIfAbsent(key, k -> new ArrayList<>()).add(unit.id());
            villageBorn.putIfAbsent(key, now);
        }
        return unit;
    }

    /**
     * A Wandering Trader's two Trader Llamas, one at each side of it; they
     * appear on the next tick and fight for the Wandering Trader card.
     */
    private void escorts(CombatUnit trader) {
        TroopStats llama = BattleCard.TRADER_LLAMA;
        int sign = ArenaLayout.sideSign(trader.side());
        for (int i = 0; i < BattleCard.TRADER_LLAMAS; i++) {
            double dx = (i % 2 == 0 ? -1.0D : 1.0D) * (0.9D + 0.4D * (i / 2));
            ArenaWaypoint spot = arena.clampSpawn(trader.x() + dx,
                    trader.z() + sign * 0.4D, sign);
            ChildSpawn child = new ChildSpawn(trader.side(), llama, 1,
                    llama.health(), llama.damage(), llama.radius(), spot.x(),
                    spot.z(), 0, trader.lockedTower());
            riderCredit.put(child, BattleCard.WANDERING_TRADER);
            pendingSpawns.add(child);
        }
    }

    /**
     * Villagers that have stood {@link BattleCard#VILLAGER_CALL_TICKS} call
     * an Iron Golem if enough of them are still up.
     */
    private void villageCalls() {
        var it = villages.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (now - villageBorn.get(entry.getKey()) <
                    BattleCard.VILLAGER_CALL_TICKS)
                continue;
            it.remove();
            villageBorn.remove(entry.getKey());
            List<CombatUnit> standing = new ArrayList<>();
            for (UUID id : entry.getValue()) {
                CombatUnit villager = unit(id);
                if (villager != null && villager.alive())
                    standing.add(villager);
            }
            if (standing.size() < BattleCard.VILLAGER_CALL_ALIVE)
                continue;
            double x = 0.0D;
            double z = 0.0D;
            for (CombatUnit villager : standing) {
                x += villager.x() / standing.size();
                z += villager.z() / standing.size();
            }
            CombatUnit first = standing.get(0);
            TroopStats golem = BattleCard.IRON_GOLEM.stats();
            TribeBonus bonus = tribeBonus(BattleCard.IRON_GOLEM, first.side());
            ArenaWaypoint spot = arena.clampSpawn(x, z,
                    ArenaLayout.sideSign(first.side()));
            ChildSpawn child = new ChildSpawn(first.side(), golem, 0,
                    golem.health() * BattleCard.CALLED_GOLEM_HEALTH *
                            bonus.health(), golem.damage() * bonus.damage(),
                    golem.radius(), spot.x(), spot.z(), 0,
                    first.lockedTower());
            riderCredit.put(child, creditOf(first.id()));
            spawns.add(child);
            tribeEvents.add(new TribeEvent(first.id(),
                    TribeEvent.Kind.GOLEM_CALL, spot.x(), spot.z()));
        }
    }

    /**
     * Every Sniffer digs up a flower once per its attack interval (each
     * other own troop within {@link BattleCard#FLOWER_RADIUS} heals by its
     * damage), and every Mooshroom shares stew.
     */
    private void snifferFlowers() {
        for (CombatUnit source : units) {
            BattleCard card = cardOf.get(source.id());
            if (card == null || !(card.digsFlowers() || card.sharesStew()) ||
                    !source.alive() || source.frozen())
                continue;
            // A Sniffer's flower heals its damage once per attack interval;
            // a Mooshroom (which fights) shares stew on a clock of its own.
            boolean stew = card.sharesStew();
            int every = stew ? BattleCard.STEW_TICKS :
                    source.stats().attackTicks();
            double radius = stew ? BattleCard.STEW_RADIUS :
                    BattleCard.FLOWER_RADIUS;
            double amount = stew ? BattleCard.STEW_HEAL : source.damage();
            int age = now - bornAt.getOrDefault(source.id(), now);
            if (age <= 0 || age % every != 0)
                continue;
            for (CombatUnit other : units) {
                if (other == source || other.side() != source.side() ||
                        !other.alive() || Math.hypot(other.x() - source.x(),
                                other.z() - source.z()) > radius + other.radius())
                    continue;
                heal(source, other, amount);
            }
            tribeEvents.add(new TribeEvent(source.id(), stew ?
                    TribeEvent.Kind.STEW : TribeEvent.Kind.FLOWER,
                    source.x(), source.z()));
        }
    }

    /** What each card has done so far this match. */
    public CombatStats stats() {
        return stats;
    }

    public void remove(UUID entityId) {
        units.removeIf(u -> u.id().equals(entityId));
    }

    /** Drops every landed spell and shot in the air (sandbox "clear"). */
    public void clearSpellsAndShots() {
        spells.clear();
        flying.clear();
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
        teleports.clear();
        tribeEvents.clear();
        spawns.clear();
        spawns.addAll(pendingSpawns);
        pendingSpawns.clear();
        now++;
        seatRiders();
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
        allayAuras();
        snifferFlowers();
        buildingAuras();
        applySpellHits(hits, destroyed, pushes);
        applyBooms(destroyed);
        sneezes(pushes);
        decayBuildings();
        villageCalls();
        List<Death> deaths = collectDeaths();
        return new TickResult(moves, attacks, deaths, destroyed, pulses,
                pushes, landed, List.copyOf(teleports),
                List.copyOf(tribeEvents), List.copyOf(spawns));
    }

    /** Whether a King tower may shoot: it was hit, or a Princess tower fell. */
    public boolean kingAwake(TeamSide side) {
        TowerState king = match.tower(TowerId.king(side));
        return king.health() < king.maxHealth() ||
                match.hitWhileUnbreakable(TowerId.king(side)) ||
                match.tower(TowerId.left(side)).isDestroyed() ||
                match.tower(TowerId.right(side)).isDestroyed();
    }

    // ---------------------------------------------------------------- units

    private void planUnit(CombatUnit unit, List<Move> moves,
            List<Attack> attacks)
    {
        TroopStats stats = unit.stats();
        BattleCard self = cardOf.get(unit.id());
        // Endermites and mirror images vanish when their time is up.
        if (self != null && outlived(unit, self)) {
            unit.expire();
            tribeEvents.add(new TribeEvent(unit.id(), TribeEvent.Kind.VANISH,
                    unit.x(), unit.z()));
            return;
        }
        // Frozen or stunned: no walking, no attacking, the same target.
        if (unit.frozen())
            return;
        // An Axolotl playing dead lies still and gets its health back.
        if (unit.playingDead()) {
            unit.heal(unit.maxHealth() * BattleCard.PLAY_DEAD_HEAL /
                    BattleCard.PLAY_DEAD_TICKS);
            if (unit.tickPlayDead())
                tribeEvents.add(new TribeEvent(unit.id(),
                        TribeEvent.Kind.WAKE, unit.x(), unit.z()));
            return;
        }
        // An End Crystal only waits to be broken.
        if (self != null && self.explodesOnDeath())
            return;
        // Called by a Bell: it walks there and attacks nothing.
        if (unit.lured()) {
            unit.setTarget(null);
            if (unit.mount() == null && stats.speed() > 0.0D &&
                    Math.hypot(unit.lureX() - unit.x(),
                            unit.lureZ() - unit.z()) > 0.5D) {
                ArenaWaypoint waypoint = arena.route(unit.x(), unit.z(),
                        unit.lureX(), unit.lureZ());
                moves.add(new Move(unit.id(), waypoint.x(), waypoint.z(),
                        stats.speedPerTick() * speedMultiplier *
                                unit.tribe().speed()));
            }
            return;
        }
        // An Allay or a Parrot only follows its troops (they heal or dance
        // from there).
        if (self != null && self.followsTroops()) {
            followTroops(unit, moves);
            return;
        }
        // A Creeper or Phantom near an enemy Cat backs away from it.
        CombatUnit cat = scaringCat(unit);
        if (cat != null) {
            unit.setTarget(null);
            double dx = unit.x() - cat.x();
            double dz = unit.z() - cat.z();
            double length = Math.hypot(dx, dz);
            if (length < 1.0E-3D) {
                dx = 0.0D;
                dz = -ArenaLayout.sideSign(unit.side());
                length = 1.0D;
            }
            moves.add(new Move(unit.id(), unit.x() + dx / length * 2.0D,
                    unit.z() + dz / length * 2.0D,
                    stats.speedPerTick() * speedMultiplier));
            return;
        }
        // A Sniffer or a Wandering Trader picks no one: it walks on.
        CombatUnit enemy = self != null && self.peaceful() ? null :
                pickEnemy(unit);
        unit.setTarget(enemy == null ? null : enemy.id());

        double targetX;
        double targetZ;
        boolean inRange;
        TowerId tower = null;
        if (enemy != null) {
            targetX = enemy.x();
            targetZ = enemy.z();
            inRange = edgeDistance(unit, enemy) <= unit.range() + EPSILON;
        } else {
            tower = laneTower(unit);
            ArenaPoint point = arena.towerPoint(tower);
            targetX = point.x();
            targetZ = point.z();
            inRange = edgeDistanceToTower(unit, tower) <=
                    unit.range() + EPSILON;
        }
        // A rider does not walk: its mount carries it.
        if (!inRange && unit.mount() != null)
            return;
        // A Magma Cube jumps onto a near enemy instead of walking up.
        if (!inRange && self != null && self.leaps() && leap(unit, attacks))
            return;

        if (self != null && self.hearsNoise())
            planBoom(unit, enemy, tower);
        if (!inRange) {
            // Crossing units (Blaze) go straight over the water; the rest
            // use the bridges.
            ArenaWaypoint waypoint = stats.traits().crossesRiver() ?
                    new ArenaWaypoint(targetX, targetZ) :
                    arena.route(unit.x(), unit.z(), targetX, targetZ);
            double slow = (unit.webbed() ? WEB_SPEED : unit.slowed() ?
                    TroopTraits.SLOW_SPEED : 1.0D) *
                    (unit.hasted() ? HASTE_SPEED : 1.0D);
            BattleCard walker = cardOf.get(unit.id());
            if (walker != null && walker.swimsFast() &&
                    arena.isRiver((int) Math.floor(unit.z())))
                slow *= BattleCard.RIVER_SWIM;
            moves.add(new Move(unit.id(), waypoint.x(), waypoint.z(),
                    stats.speedPerTick() * speedMultiplier * slow *
                            unit.tribe().speed() * auraSpeed(unit)));
            return;
        }
        // Mirror images draw fire; they do not fight.
        if (self != null && self.castsIllusions() && unit.generation() > 0)
            return;
        if (self != null && self.peaceful())
            return;
        TroopTraits traits = stats.traits();
        // A Guardian first locks on to a new target.
        if (traits.charges() &&
                !unit.lockOn(enemy != null ? enemy.id() : tower))
            return;
        if (!unit.ready())
            return;
        unit.startShotCooldown();
        // A Parrot dancing close by: the next attack comes sooner.
        if (dancerNear(unit))
            unit.hasteCooldown(BattleCard.DANCE_HASTE);
        // Every attack is a sound the Warden can hear (and opens a shell).
        lastAttackAt.put(unit.id(), now);
        BattleCard own = cardOf.get(unit.id());
        if (own != null && own.rages())
            unit.hasteCooldown(1.0D + BattleCard.RAGE_MAX * Math.min(1.0D,
                    Math.max(0.0D, (1.0D - unit.health() / unit.maxHealth()) /
                            0.75D)));
        boolean ranged = !stats.melee();
        // Laser beams and fangs hit at once; arrows and fireballs fly.
        int flight = ranged && !traits.charges() && !traits.lines() ?
                flightTicks(unit.x(), unit.z(), targetX, targetZ) : 0;
        double damage = unit.damage();
        boolean onRiver = arena.isRiver((int) Math.floor(unit.z()));
        if (traits.riverStrong() && onRiver)
            damage *= TroopTraits.RIVER_STRENGTH;
        if (onRiver)
            damage *= unit.tribe().riverDamage();
        if (unit.tribe().packDamage() > 0.0D)
            damage *= 1.0D + unit.tribe().packDamage() * packmates(unit);
        damage *= auraDamage(unit);
        if (enemy != null && swallows(unit, enemy))
            damage = SWALLOW_DAMAGE;
        double knockback = ranged ? 0.0D : traits.knockback();
        if (own != null && own.dashes() && enemy != null && unit.dash()) {
            damage *= BattleCard.DASH_DAMAGE;
            knockback = BattleCard.DASH_KNOCKBACK;
            tribeEvents.add(new TribeEvent(unit.id(), TribeEvent.Kind.DASH,
                    unit.x(), unit.z()));
        }
        Attack attack = new Attack(unit.id(), null,
                enemy == null ? null : enemy.id(), enemy == null ? tower : null,
                damage, ranged, flight, unit.side(), targetX, targetZ,
                traits.splashRadius(), knockback,
                traits.slowTicks(), traits.lineWidth(),
                stats.range() + unit.radius() + 1.0D, unit.x(), unit.z());
        attacks.add(attack);
        // Every 3rd arrow of an Illusioner blinds what it hits.
        if (own != null && own.castsIllusions() && enemy != null &&
                illusionerShots.merge(unit.id(), 1, Integer::sum) %
                        BattleCard.BLIND_EVERY == 0)
            blinding.add(attack);
        if (ranged && unit.volley(unit.tribe().extraShotEvery()))
            volley(unit, enemy, damage, attacks);
        // The Wither's other two heads each pick a target of their own.
        if (own != null && own.threeHeads())
            otherHeads(unit, enemy, damage, attacks);
        if (traits.selfDestruct())
            unit.expire();
    }

    /** The engine tick each unit last attacked (a sound; a Shulker's shell opens). */
    private final Map<UUID, Integer> lastAttackAt = new java.util.HashMap<>();
    /** When each Warden's Sonic Boom is ready again. */
    private final Map<UUID, Integer> boomReady = new java.util.HashMap<>();
    /** Sonic Booms decided this tick: the Warden and its target. */
    private final List<Object[]> booms = new ArrayList<>();

    /** A Pillager Captain or its dropped banner close to an own Illager. */
    private boolean underBanner(CombatUnit unit) {
        BattleCard card = cardOf.get(unit.id());
        if (card == null || !card.tribes().contains(Tribe.ILLAGER))
            return false;
        for (CombatUnit other : units) {
            BattleCard of = cardOf.get(other.id());
            if (other != unit && other.alive() && other.side() == unit.side() &&
                    of != null && of.carriesBanner() &&
                    distance(unit, other) <= BattleCard.BANNER_RADIUS)
                return true;
        }
        for (Banner banner : banners) {
            if (banner.side == unit.side() && Math.hypot(unit.x() - banner.x,
                    unit.z() - banner.z) <= BattleCard.BANNER_RADIUS)
                return true;
        }
        return false;
    }

    /** The Beacon effect on a unit now, or null when no own Beacon reaches it. */
    private BattleCard.BeaconEffect beaconOn(CombatUnit unit) {
        for (CombatUnit other : units) {
            BattleCard card = cardOf.get(other.id());
            if (other != unit && other.alive() && other.side() == unit.side() &&
                    card != null && card.beams() &&
                    distance(unit, other) <= other.stats().range() +
                            unit.radius())
                return BattleCard.BeaconEffect.at(
                        now - bornAt.getOrDefault(other.id(), now));
        }
        return null;
    }

    /** Walking speed from a Captain's banner and a Beacon's Speed. */
    private double auraSpeed(CombatUnit unit) {
        double speed = underBanner(unit) ? 1.0D + BattleCard.BANNER_SPEED : 1.0D;
        if (beaconOn(unit) == BattleCard.BeaconEffect.SPEED)
            speed *= BattleCard.BEACON_SPEED;
        return speed;
    }

    /** Damage from a Captain's banner and a Beacon's Strength. */
    private double auraDamage(CombatUnit unit) {
        double damage = underBanner(unit) ? 1.0D + BattleCard.BANNER_DAMAGE :
                1.0D;
        if (beaconOn(unit) == BattleCard.BeaconEffect.STRENGTH)
            damage *= BattleCard.BEACON_STRENGTH;
        return damage;
    }

    /** Whether an own Parrot dances within reach of a unit. */
    private boolean dancerNear(CombatUnit unit) {
        for (CombatUnit other : units) {
            BattleCard card = cardOf.get(other.id());
            if (other != unit && other.alive() && other.side() == unit.side() &&
                    card != null && card.dances() && !other.frozen() &&
                    distance(unit, other) <= BattleCard.DANCE_RADIUS)
                return true;
        }
        return false;
    }

    /**
     * The Wither's other heads: a skull each at the nearest other enemy
     * troops in its range, the same damage, splash and wither.
     */
    private void otherHeads(CombatUnit wither, CombatUnit first, double damage,
            List<Attack> attacks)
    {
        List<CombatUnit> picked = new ArrayList<>();
        for (int head = 1; head < BattleCard.WITHER_HEADS; head++) {
            CombatUnit best = null;
            double bestDistance = Double.MAX_VALUE;
            for (CombatUnit other : units) {
                if (other == first || picked.contains(other) ||
                        other.side() == wither.side() || !other.alive() ||
                        other.untargetable())
                    continue;
                double d = edgeDistance(wither, other);
                if (d <= wither.range() + EPSILON && d < bestDistance) {
                    best = other;
                    bestDistance = d;
                }
            }
            if (best == null)
                return;
            picked.add(best);
            attacks.add(new Attack(wither.id(), null, best.id(), null, damage,
                    true, flightTicks(wither.x(), wither.z(), best.x(),
                            best.z()), wither.side(), best.x(), best.z(),
                    wither.stats().traits().splashRadius(), 0.0D, 0, 0.0D,
                    0.0D, wither.x(), wither.z()));
        }
    }

    /**
     * Mooshroom stew, Campfire warmth and burn, Beacon regeneration, Monster
     * Spawner mobs, and dropped banners running out.
     */
    private void buildingAuras() {
        for (int i = 0; i < banners.size(); i++) {
            if (--banners.get(i).ticksLeft <= 0)
                banners.remove(i--);
        }
        for (CombatUnit source : new ArrayList<>(units)) {
            BattleCard card = cardOf.get(source.id());
            if (card == null || !source.alive() || source.frozen())
                continue;
            int age = now - bornAt.getOrDefault(source.id(), now);
            if (card.warms() && age > 0 && age % 20 == 0)
                campfire(source);
            if (card.beams())
                for (CombatUnit other : units) {
                    if (other != source && other.side() == source.side() &&
                            other.alive() && beaconOn(other) ==
                                    BattleCard.BeaconEffect.REGENERATION)
                        heal(source, other, BattleCard.BEACON_REGEN / 20.0D);
                }
            if (card.spawnsMobs() && age > 0 &&
                    age % source.stats().attackTicks() == 0)
                spawnFrom(source, 1);
        }
    }

    /** Heals {@code target} by {@code amount}, credited to {@code source}'s card. */
    private void heal(CombatUnit source, CombatUnit target, double amount) {
        double before = target.health();
        if (target.heal(amount))
            stats.healing(source.side(), creditOf(source.id()),
                    target.health() - before);
    }

    /** One second of a Campfire: enemies close burn, own troops heal. */
    private void campfire(CombatUnit fire) {
        BattleCard source = creditOf(fire.id());
        for (CombatUnit other : units) {
            if (other == fire || !other.alive())
                continue;
            double d = Math.hypot(other.x() - fire.x(), other.z() - fire.z()) -
                    other.radius();
            if (other.side() != fire.side()) {
                BattleCard card = cardOf.get(other.id());
                if (d <= fire.stats().range() &&
                        (card == null || !card.fireImmune()))
                    other.ignite(25, fire.damage(), source);
            } else if (d <= BattleCard.CAMPFIRE_HEAL_RADIUS) {
                heal(fire, other, BattleCard.CAMPFIRE_HEAL);
            }
        }
        tribeEvents.add(new TribeEvent(fire.id(), TribeEvent.Kind.WARMTH,
                fire.x(), fire.z()));
    }

    /**
     * {@code count} mobs out of a Monster Spawner, each a vanilla spawner
     * mob picked at random, at {@link BattleCard#SPAWNED_HEALTH} of its
     * health, in front of the spawner. They fight for the spawner card.
     */
    private void spawnFrom(CombatUnit spawner, int count) {
        int sign = ArenaLayout.sideSign(spawner.side());
        for (int i = 0; i < count; i++) {
            BattleCard mob = BattleCard.SPAWNER_MOBS.get(
                    spawnerDice.nextInt(BattleCard.SPAWNER_MOBS.size()));
            TroopStats st = mob.stats();
            TribeBonus bonus = tribeBonus(mob, spawner.side());
            ArenaWaypoint spot = arena.clampSpawn(spawner.x() +
                            (i - (count - 1) / 2.0D) * 0.8D,
                    spawner.z() - sign * (spawner.radius() + st.radius() + 0.2D),
                    sign);
            ChildSpawn child = new ChildSpawn(spawner.side(), st, 0,
                    st.health() * BattleCard.SPAWNED_HEALTH * bonus.health(),
                    st.damage() * bonus.damage(), st.radius(), spot.x(),
                    spot.z(), 0, null);
            riderCredit.put(child, creditOf(spawner.id()));
            spawns.add(child);
        }
        tribeEvents.add(new TribeEvent(spawner.id(), TribeEvent.Kind.SPAWN,
                spawner.x(), spawner.z()));
    }

    /**
     * For a troop that only hits towers: the nearest enemy building in
     * sight (they go for it first, as in Clash Royale), or none.
     */
    private CombatUnit nearestBuilding(CombatUnit unit) {
        CombatUnit best = null;
        double bestDistance = unit.stats().sight() + EPSILON;
        for (CombatUnit other : units) {
            BattleCard card = cardOf.get(other.id());
            if (other.side() == unit.side() || !other.alive() ||
                    card == null || !card.isBuilding())
                continue;
            double d = distance(unit, other);
            if (d <= bestDistance) {
                best = other;
                bestDistance = d;
            }
        }
        return best;
    }

    /**
     * The Warden's target: the troop it is on while that stays close, else
     * the nearest enemy that made a sound within its hearing.
     */
    private CombatUnit heard(CombatUnit unit) {
        CombatUnit current = unit.target() == null ? null :
                unit(unit.target());
        if (current != null && current.alive() && !current.untargetable() &&
                current.side() != unit.side() && canTarget(unit, current) &&
                distance(unit, current) <= BattleCard.HEAR_RANGE * LEASH_FACTOR)
            return current;
        CombatUnit best = null;
        double bestDistance = BattleCard.HEAR_RANGE + EPSILON;
        for (CombatUnit other : units) {
            if (other.side() == unit.side() || !other.alive() ||
                    other.untargetable() || !canTarget(unit, other) ||
                    now - lastAttackAt.getOrDefault(other.id(), -1000) >
                            BattleCard.NOISE_TICKS)
                continue;
            double d = distance(unit, other);
            if (d <= bestDistance) {
                best = other;
                bestDistance = d;
            }
        }
        return best;
    }

    /** Plans a Warden's Sonic Boom on its target, if it is ready and in reach. */
    private void planBoom(CombatUnit warden, CombatUnit enemy, TowerId tower) {
        int ready = boomReady.computeIfAbsent(warden.id(), id -> now);
        if (now < ready)
            return;
        double reach = enemy != null ? edgeDistance(warden, enemy) :
                edgeDistanceToTower(warden, tower);
        if (reach > BattleCard.BOOM_RANGE)
            return;
        boomReady.put(warden.id(), now + BattleCard.BOOM_TICKS);
        booms.add(new Object[] {warden, enemy, tower});
    }

    /** Lands this tick's Sonic Booms: through armour, on troops and towers. */
    private void applyBooms(List<TowerId> destroyed) {
        for (Object[] boom : booms) {
            CombatUnit warden = (CombatUnit) boom[0];
            CombatUnit enemy = (CombatUnit) boom[1];
            TowerId tower = (TowerId) boom[2];
            BattleCard source = creditOf(warden.id());
            double x;
            double z;
            if (enemy != null) {
                if (!enemy.alive())
                    continue;
                damageUnit(enemy, BattleCard.BOOM_DAMAGE, warden.side(),
                        source, true);
                x = enemy.x();
                z = enemy.z();
            } else {
                hitTower(tower, BattleCard.BOOM_DAMAGE, destroyed,
                        warden.side(), source);
                ArenaPoint p = arena.towerPoint(tower);
                x = p.x();
                z = p.z();
            }
            tribeEvents.add(new TribeEvent(warden.id(),
                    TribeEvent.Kind.SONIC_BOOM, x, z));
        }
        booms.clear();
    }

    /** Buildings lose their health over their lifetime. */
    private void decayBuildings() {
        for (CombatUnit unit : units) {
            BattleCard card = cardOf.get(unit.id());
            if (card != null && card.isBuilding() && unit.alive())
                unit.hurt(unit.maxHealth() / BattleCard.BUILDING_LIFETIME_TICKS);
        }
    }

    /**
     * A broken End Crystal blows up: every troop of both sides within its
     * splash radius takes its damage.
     */
    private void blast(CombatUnit crystal) {
        BattleCard source = creditOf(crystal.id());
        double radius = crystal.stats().traits().splashRadius();
        for (CombatUnit other : units) {
            if (other == crystal || !other.alive())
                continue;
            if (Math.hypot(other.x() - crystal.x(), other.z() - crystal.z()) <=
                    radius + other.radius() + EPSILON)
                damageUnit(other, crystal.damage(), crystal.side(), source);
        }
        tribeEvents.add(new TribeEvent(crystal.id(), TribeEvent.Kind.BLAST,
                crystal.x(), crystal.z()));
    }

    /** When each Panda next sneezes (engine tick). */
    private final Map<UUID, Integer> sneezeAt = new java.util.HashMap<>();

    /**
     * Every Panda sneezes each {@link BattleCard#SNEEZE_TICKS}: small
     * enemies close to it are blown back.
     */
    private void sneezes(List<Push> pushes) {
        for (CombatUnit panda : units) {
            BattleCard card = cardOf.get(panda.id());
            if (card == null || !card.sneezes() || !panda.alive() ||
                    panda.frozen())
                continue;
            int at = sneezeAt.computeIfAbsent(panda.id(),
                    id -> now + BattleCard.SNEEZE_TICKS);
            if (now < at)
                continue;
            sneezeAt.put(panda.id(), now + BattleCard.SNEEZE_TICKS);
            tribeEvents.add(new TribeEvent(panda.id(), TribeEvent.Kind.SNEEZE,
                    panda.x(), panda.z()));
            for (CombatUnit other : units) {
                if (other.side() == panda.side() || !other.alive() ||
                        other.radius() >= BattleCard.SNEEZE_SMALL ||
                        edgeDistance(panda, other) > BattleCard.SNEEZE_RANGE)
                    continue;
                knockBack(other, panda.x(), panda.z(),
                        -ArenaLayout.sideSign(panda.side()),
                        BattleCard.SNEEZE_PUSH, pushes);
            }
        }
    }

    /** Damage of a Frog's lick on a slime it swallows: always a kill. */
    private static final double SWALLOW_DAMAGE = 1000.0D;

    /** The nearest enemy Cat that keeps this Creeper or Phantom away. */
    private CombatUnit scaringCat(CombatUnit unit) {
        BattleCard card = cardOf.get(unit.id());
        if (card == null || !card.fearsCats())
            return null;
        CombatUnit best = null;
        double bestDistance = BattleCard.CAT_SCARE_RADIUS + EPSILON;
        for (CombatUnit other : units) {
            if (other.side() == unit.side() || !other.alive() ||
                    cardOf.get(other.id()) != BattleCard.CAT)
                continue;
            double d = distance(unit, other);
            if (d <= bestDistance) {
                best = other;
                bestDistance = d;
            }
        }
        return best;
    }

    /**
     * Whether a Frog's lick swallows this enemy: a slime or magma cube that
     * can split no more (the smallest, as vanilla frogs eat).
     */
    private boolean swallows(CombatUnit frog, CombatUnit enemy) {
        BattleCard card = cardOf.get(frog.id());
        BattleCard prey = cardOf.get(enemy.id());
        if (card == null || !card.swallowsSmallSlimes() ||
                (prey != BattleCard.BIG_SLIME && prey != BattleCard.MAGMA_CUBE))
            return false;
        SplitRule rule = enemy.stats().split();
        return rule == null || enemy.generation() >= rule.generations();
    }

    /**
     * Enemy to chase: the one already chased while it stays within the
     * leash, otherwise the nearest one in sight. Melee units ignore enemies
     * across the river unless those stand on the bridges, and a unit that
     * only targets buildings ignores every troop.
     */
    private CombatUnit pickEnemy(CombatUnit unit) {
        if (unit.stats().traits().buildingsOnly() || ridesForTowers(unit))
            return nearestBuilding(unit);
        BattleCard self = cardOf.get(unit.id());
        if (self != null && self.hearsNoise())
            return heard(unit);
        // An Enderman drops everything to chase an Endermite near it.
        if (self == BattleCard.ENDERMAN) {
            CombatUnit mite = nearestEndermite(unit);
            if (mite != null)
                return mite;
        }
        double sight = unit.stats().sight();
        // A zombie goes for a Villager first; everyone goes for a Sniffer.
        if (self != null && self.infects()) {
            CombatUnit villager = nearestOf(unit, BattleCard.VILLAGER, sight);
            if (villager != null)
                return villager;
        }
        CombatUnit sniffer = nearestOf(unit, BattleCard.SNIFFER, sight);
        if (sniffer != null)
            return sniffer;
        CombatUnit current = unit.target() == null ? null :
                unit(unit.target());
        if (current != null && current.alive() && !current.untargetable() &&
                current.side() != unit.side() && canTarget(unit, current) &&
                distance(unit, current) <= sight * LEASH_FACTOR)
            return current;

        CombatUnit best = null;
        double bestDistance = Double.MAX_VALUE;
        for (CombatUnit other : units) {
            if (other.side() == unit.side() || !other.alive() ||
                    other.untargetable() || !canTarget(unit, other))
                continue;
            double d = distance(unit, other);
            if (d <= sight && d < bestDistance) {
                best = other;
                bestDistance = d;
            }
        }
        return best;
    }

    /** The nearest enemy of {@code card} within {@code sight} it can go for. */
    private CombatUnit nearestOf(CombatUnit unit, BattleCard card,
            double sight)
    {
        CombatUnit best = null;
        double bestDistance = sight + EPSILON;
        for (CombatUnit other : units) {
            if (other.side() == unit.side() || !other.alive() ||
                    other.untargetable() || cardOf.get(other.id()) != card ||
                    !canTarget(unit, other))
                continue;
            double d = distance(unit, other);
            if (d <= bestDistance) {
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
            int frozen = towerFrozen.getOrDefault(id, 0);
            if (frozen > 0) {
                towerFrozen.put(id, frozen - 1);
                continue;
            }
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
        // A Sniffer in range draws the tower's shots first.
        CombatUnit sniffer = null;
        double snifferDistance = Double.MAX_VALUE;
        CombatUnit best = null;
        double bestDistance = Double.MAX_VALUE;
        for (CombatUnit unit : units) {
            if (unit.side() == id.side() || !unit.alive() || unit.untargetable())
                continue;
            double d = edgeDistanceToTower(unit, id);
            if (d > range + EPSILON)
                continue;
            if (cardOf.get(unit.id()) == BattleCard.SNIFFER &&
                    d < snifferDistance) {
                sniffer = unit;
                snifferDistance = d;
            }
            if (d < bestDistance) {
                best = unit;
                bestDistance = d;
            }
        }
        if (sniffer != null)
            return sniffer;
        if (current != null && current.alive() && !current.untargetable() &&
                edgeDistanceToTower(current, id) <= range + EPSILON)
            return current;
        return best;
    }

    /** Ticks a tower stays frozen or stunned (0 when it can shoot). */
    public int towerFrozenTicks(TowerId id) {
        return towerFrozen.getOrDefault(id, 0);
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
            if (active.card.rings() && active.pulsesDone == 0)
                ringFar(active);
            List<Hit> pulseHits = spell.cleanse() ? cleanseTargets(active) :
                    active.card.pearls() ? pearlTarget(active) :
                    active.card.dropsOnOne() ? anvilTargets(active) :
                    active.card.rings() ? bellTargets(active) :
                    active.card.douses() ? waterTargets(active) :
                    spell.friendly() ? healTargets(active) :
                    spellTargets(active);
            // Water puts out a Strider's trail in its circle.
            if (active.card.douses())
                firePatches.removeIf(patch -> Math.hypot(patch.x - active.x,
                        patch.z - active.z) <= spell.radius() +
                        TroopTraits.TRAIL_RADIUS);
            List<TickResult.Strike> strikes = new ArrayList<>();
            for (Hit hit : pulseHits) {
                if (hit.unit() != null) {
                    strikes.add(new TickResult.Strike(hit.unit().id(), null,
                            hit.unit().x(), hit.unit().z()));
                } else {
                    ArenaPoint p = arena.towerPoint(hit.tower());
                    strikes.add(new TickResult.Strike(null, hit.tower(),
                            p.x(), p.z()));
                }
            }
            hits.addAll(pulseHits);
            pulses.add(new SpellPulse(active.side, active.card, active.x,
                    active.z, active.pulsesDone, pulseHits.size(),
                    List.copyOf(strikes)));
            active.pulsesDone++;
            active.wait = spell.pulseInterval() - 1;
            if (active.pulsesDone >= spell.pulses())
                spells.remove(i--);
        }
        return hits;
    }

    /**
     * Enemy units whose body touches the circle and enemy towers whose
     * footprint does; a spell with {@code maxTargets} keeps only that many,
     * those with the most health.
     */
    private List<Hit> spellTargets(ActiveSpell active) {
        SpellStats spell = active.card.spell();
        List<Hit> hits = new ArrayList<>();
        List<Double> health = new ArrayList<>();
        for (CombatUnit unit : units) {
            if (unit.side() == active.side || !unit.alive())
                continue;
            // Lava does nothing to a fire-immune troop.
            BattleCard target = cardOf.get(unit.id());
            if (active.card.lava() && target != null && target.fireImmune())
                continue;
            double reach = spell.radius() + unit.radius();
            if (Math.hypot(unit.x() - active.x, unit.z() - active.z) <=
                    reach + EPSILON) {
                // Poison (Poison Cloud) works by what the target is made of.
                double damage = spell.poison() ? spell.damagePerPulse() *
                        bodyOf(unit).poisonFactor() : spell.damagePerPulse();
                hits.add(new Hit(unit, null, damage, active));
                health.add(unit.health());
            }
        }
        // Harming heals the caster's own undead in it.
        if (active.card.harms()) {
            for (CombatUnit unit : units) {
                if (unit.side() == active.side && unit.alive() &&
                        bodyOf(unit) == Body.UNDEAD &&
                        Math.hypot(unit.x() - active.x, unit.z() - active.z) <=
                                spell.radius() + unit.radius() + EPSILON) {
                    hits.add(new Hit(unit, null, spell.damagePerPulse(), active));
                    health.add(unit.health());
                }
            }
        }
        for (TowerId id : TowerId.values()) {
            if (id.side() == active.side || match.tower(id).isDestroyed() ||
                    spell.poison() || active.card.lava())
                continue;
            if (distanceToTower(active.x, active.z, id) <=
                    spell.radius() + EPSILON) {
                hits.add(new Hit(null, id, spell.damagePerPulse() *
                        spell.towerMultiplier(), active));
                health.add((double) match.tower(id).health());
            }
        }
        if (spell.maxTargets() <= 0 || hits.size() <= spell.maxTargets())
            return hits;
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < hits.size(); i++)
            order.add(i);
        // Most health first; ties keep the scan order.
        order.sort((a, b) -> Double.compare(health.get(b), health.get(a)));
        List<Hit> kept = new ArrayList<>();
        for (int i = 0; i < spell.maxTargets(); i++)
            kept.add(hits.get(order.get(i)));
        return kept;
    }

    /**
     * The caster's own units inside the circle, for healing or haste
     * (towers are never healed).
     */
    private List<Hit> healTargets(ActiveSpell active) {
        SpellStats spell = active.card.spell();
        List<Hit> hits = new ArrayList<>();
        for (CombatUnit unit : units) {
            if (unit.side() != active.side || !unit.alive())
                continue;
            double reach = spell.radius() + unit.radius();
            if (Math.hypot(unit.x() - active.x, unit.z() - active.z) <=
                    reach + EPSILON)
                hits.add(new Hit(unit, null, spell.damagePerPulse(), active));
        }
        return hits;
    }

    /**
     * The Anvil: the one enemy troop nearest where it lands, and every enemy
     * tower whose footprint touches its circle.
     */
    private List<Hit> anvilTargets(ActiveSpell active) {
        SpellStats spell = active.card.spell();
        CombatUnit best = null;
        double bestDistance = Double.MAX_VALUE;
        for (CombatUnit unit : units) {
            if (unit.side() == active.side || !unit.alive())
                continue;
            double d = Math.hypot(unit.x() - active.x, unit.z() - active.z);
            if (d <= spell.radius() + unit.radius() + EPSILON &&
                    d < bestDistance) {
                best = unit;
                bestDistance = d;
            }
        }
        List<Hit> hits = new ArrayList<>();
        if (best != null)
            hits.add(new Hit(best, null, spell.damagePerPulse(), active));
        for (TowerId id : TowerId.values()) {
            if (id.side() != active.side && !match.tower(id).isDestroyed() &&
                    distanceToTower(active.x, active.z, id) <=
                            spell.radius() + EPSILON)
                hits.add(new Hit(null, id, spell.damagePerPulse() *
                        spell.towerMultiplier(), active));
        }
        return hits;
    }

    /** Every enemy troop whose body touches a Bell's circle. */
    private List<Hit> bellTargets(ActiveSpell active) {
        double radius = active.card.spell().radius();
        List<Hit> hits = new ArrayList<>();
        for (CombatUnit unit : units) {
            if (unit.side() != active.side && unit.alive() &&
                    Math.hypot(unit.x() - active.x, unit.z() - active.z) <=
                            radius + unit.radius() + EPSILON)
                hits.add(new Hit(unit, null, 0.0D, active));
        }
        return hits;
    }

    /**
     * What a Bell does beyond its circle, once: the enemy side's Shadow
     * dodge is off for a while, and an enemy Warden in earshot comes.
     */
    private void ringFar(ActiveSpell active) {
        TeamSide enemy = active.side.opponent();
        dodgeReady.merge(enemy, now + BattleCard.BELL_DODGE_OFF_TICKS,
                Math::max);
        for (CombatUnit unit : units) {
            BattleCard card = cardOf.get(unit.id());
            if (unit.side() == enemy && unit.alive() && card != null &&
                    card.hearsNoise() && Math.hypot(unit.x() - active.x,
                            unit.z() - active.z) <= BattleCard.BELL_WARDEN_RANGE)
                unit.lure(BattleCard.BELL_WARDEN_TICKS, active.x, active.z);
        }
    }

    /**
     * A Bell rings over an enemy troop: a short stun, then it walks to the
     * bell; it is seen again if invisible, gets up if playing dead, and a
     * mirror image vanishes.
     */
    private void ring(CombatUnit unit, ActiveSpell bell) {
        BattleCard card = cardOf.get(unit.id());
        if (card != null && card.castsIllusions() && unit.generation() > 0) {
            unit.expire();
            tribeEvents.add(new TribeEvent(unit.id(), TribeEvent.Kind.VANISH,
                    unit.x(), unit.z()));
            return;
        }
        if (unit.hidden() || unit.playingDead()) {
            unit.reveal();
            unit.wake();
            tribeEvents.add(new TribeEvent(unit.id(), TribeEvent.Kind.REVEAL,
                    unit.x(), unit.z()));
        }
        int stun = bell.card.spell().stunTicks();
        unit.stun(stun);
        if (unit.stats().speed() > 0.0D)
            unit.lure(stun + BattleCard.BELL_LURE_TICKS, bell.x, bell.z);
    }

    /**
     * Every unit of both sides whose body touches the circle, and every
     * standing tower whose footprint does (Milk Bucket).
     */
    private List<Hit> cleanseTargets(ActiveSpell active) {
        SpellStats spell = active.card.spell();
        List<Hit> hits = new ArrayList<>();
        for (CombatUnit unit : units) {
            if (unit.alive() && Math.hypot(unit.x() - active.x,
                    unit.z() - active.z) <= spell.radius() + unit.radius() +
                    EPSILON)
                hits.add(new Hit(unit, null, 0.0D, active));
        }
        for (TowerId id : TowerId.values()) {
            if (!match.tower(id).isDestroyed() &&
                    distanceToTower(active.x, active.z, id) <=
                            spell.radius() + EPSILON)
                hits.add(new Hit(null, id, 0.0D, active));
        }
        return hits;
    }

    private void applySpellHits(List<Hit> hits, List<TowerId> destroyed,
            List<Push> pushes)
    {
        for (Hit hit : hits) {
            SpellStats spell = hit.spell().card.spell();
            if (spell.cleanse()) {
                // Milk: every effect off, good or bad, on both sides.
                if (hit.tower() != null)
                    towerFrozen.remove(hit.tower());
                else
                    hit.unit().clearEffects();
                continue;
            }
            if (hit.spell().card.pearls()) {
                pearl(hit.unit(), hit.spell().side);
                continue;
            }
            if (hit.spell().card.douses()) {
                douse(hit, pushes);
                continue;
            }
            if (hit.spell().card.rings()) {
                ring(hit.unit(), hit.spell());
                continue;
            }
            if (hit.tower() != null) {
                hitTower(hit.tower(), hit.damage(), destroyed,
                        hit.spell().side, hit.spell().card);
                int stop = Math.max(spell.freezeTicks(), spell.stunTicks());
                if (stop > 0) {
                    towerFrozen.merge(hit.tower(), stop, Math::max);
                    if (spell.stunTicks() > 0)
                        towerCooldown.put(hit.tower(),
                                hit.tower().kind().attackTicks());
                }
                continue;
            }
            CombatUnit unit = hit.unit();
            // Harming on an own undead troop: it heals instead.
            if (hit.spell().card.harms() && unit.side() == hit.spell().side) {
                double before = unit.health();
                unit.heal(hit.damage());
                stats.healing(hit.spell().side, hit.spell().card,
                        unit.health() - before);
                continue;
            }
            if (spell.friendly()) {
                if (spell.heal()) {
                    double before = unit.health();
                    unit.heal(hit.damage());
                    stats.healing(hit.spell().side, hit.spell().card,
                            unit.health() - before);
                }
                if (spell.hasteTicks() > 0)
                    unit.haste(spell.hasteTicks());
                continue;
            }
            if (unit.canTeleport()) {
                // Out of the circle before it reaches the unit.
                blinkAway(unit, hit.spell().x, hit.spell().z);
                continue;
            }
            boolean frostWeak = spell.freezeTicks() > 0 &&
                    unit.stats().traits().frostWeak();
            double damage = frostWeak ? hit.damage() *
                    TroopTraits.FROST_WEAKNESS : hit.damage();
            // Anvil and Dragon's Breath go through half the armour, Harming
            // through all of it; a Firework hits flyers twice as hard.
            boolean half = hit.spell().card.halfArmor();
            if (half)
                damage *= 0.5D + 0.5D * unit.armorFactor();
            if (hit.spell().card.firework() &&
                    unit.stats().traits().hover() > 0.0D)
                damage *= BattleCard.FIREWORK_FLYING;
            half |= hit.spell().card.harms();
            if (hit.spell().card.blocksHealing() && unit.alive())
                unit.blockHeal(BattleCard.BREATH_HEAL_BLOCK_TICKS);
            // A spell that only slows (Cobweb) deals nothing; a poison that
            // a golem shrugs off does nothing at all.
            if (damage > 0.0D) {
                if (!damageUnit(unit, damage, hit.spell().side,
                        hit.spell().card, spell.poison() || half))
                    continue;
            } else if (spell.damagePerPulse() > 0.0D || !unit.alive()) {
                continue;
            }
            if (spell.slowTicks() > 0)
                unit.web(spell.slowTicks());
            if (spell.freezeTicks() > 0 && !unit.stats().traits().frostImmune())
                unit.freeze(frostWeak ? spell.freezeTicks() * 2 :
                        spell.freezeTicks());
            if (spell.stunTicks() > 0)
                unit.stun(spell.stunTicks());
            BattleCard own = cardOf.get(unit.id());
            if (hit.spell().card.withers() && unit.alive() &&
                    (own == null || !own.witherImmune()))
                unit.wither(BattleCard.WITHER_TICKS,
                        BattleCard.WITHER_PER_SECOND, hit.spell().card);
            if (own == null || !own.fireImmune())
                unit.ignite(spell.burnTicks(), spell.burnPerSecond(),
                        hit.spell().card);
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

    /** What a unit is made of; a unit of no card (a test dummy) is living. */
    private Body bodyOf(CombatUnit unit) {
        BattleCard card = cardOf.get(unit.id());
        return card == null ? Body.LIVING : card.body();
    }

    /** Where the fire trail patches burn now, as {x, z} (for the world). */
    public List<double[]> firePatches() {
        List<double[]> out = new ArrayList<>(firePatches.size());
        for (FirePatch patch : firePatches)
            out.add(new double[] {patch.x, patch.z});
        return out;
    }

    /**
     * A Strider drops a burning patch every {@link
     * TroopTraits#TRAIL_EVERY_TICKS}; an enemy standing on a patch burns
     * (unless fire-immune), and keeps burning a moment after leaving it.
     */
    private void applyFireTrails() {
        if (now % TroopTraits.TRAIL_EVERY_TICKS == 0) {
            for (CombatUnit unit : units) {
                if (unit.alive() && unit.stats().traits().leavesFireTrail() &&
                        !unit.frozen())
                    firePatches.add(new FirePatch(unit.side(),
                            creditOf(unit.id()), unit.x(), unit.z(),
                            unit.stats().traits().fireTrailTicks()));
            }
        }
        for (int i = 0; i < firePatches.size(); i++) {
            FirePatch patch = firePatches.get(i);
            if (--patch.ticksLeft <= 0) {
                firePatches.remove(i--);
                continue;
            }
            for (CombatUnit other : units) {
                if (other.side() == patch.side || !other.alive())
                    continue;
                BattleCard card = cardOf.get(other.id());
                if (card != null && card.fireImmune())
                    continue;
                // A longer burn (a Fireball's) is not cut short.
                if (other.burnTicks() <= TroopTraits.TRAIL_BURN_TICKS &&
                        Math.hypot(other.x() - patch.x, other.z() - patch.z) <=
                        TroopTraits.TRAIL_RADIUS + other.radius())
                    other.ignite(TroopTraits.TRAIL_BURN_TICKS,
                            TroopTraits.TRAIL_BURN_PER_SECOND, patch.source);
            }
        }
    }

    private void applyBurns() {
        applyFireTrails();
        for (CombatUnit unit : units) {
            double burn = unit.burnStep();
            if (burn > 0.0D)
                damageUnit(unit, burn, unit.side().opponent(),
                        unit.burnSource(), true);
            if (unit.tribe().regen() > 0.0D && unit.alive())
                unit.heal(unit.tribe().regen() / 20.0D);
            double poison = unit.poisonStep();
            if (poison > 0.0D)
                damageUnit(unit, poison * bodyOf(unit).poisonFactor(),
                        unit.side().opponent(), unit.poisonSource(), true);
            double wither = unit.witherStep();
            if (wither > 0.0D)
                damageUnit(unit, wither, unit.side().opponent(),
                        unit.witherSource(), true);
        }
    }

    /**
     * Moves a unit {@link #BLINK_DISTANCE} blocks straight away from (fx, fz),
     * kept on land, and starts its cooldown. A unit right on that point goes
     * toward its own side.
     */
    private void blinkAway(CombatUnit unit, double fx, double fz) {
        double dx = unit.x() - fx;
        double dz = unit.z() - fz;
        double length = Math.hypot(dx, dz);
        if (length < 1.0E-3D) {
            dx = 0.0D;
            dz = ArenaLayout.sideSign(unit.side());
            length = 1.0D;
        }
        ArenaWaypoint spot = arena.clampSpawn(
                unit.x() + dx / length * BLINK_DISTANCE,
                unit.z() + dz / length * BLINK_DISTANCE,
                ArenaLayout.sideSign(unit.side()));
        unit.setPosition(spot.x(), spot.z());
        unit.startTeleportCooldown();
        teleports.add(new TickResult.Teleport(unit.id(), spot.x(), spot.z()));
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
            boolean blinds = blinding.remove(attack);
            // The shooter may have died while its arrow flew; it still counts.
            BattleCard source = attack.attackerUnit() == null ? null :
                    creditOf(attack.attackerUnit());
            CombatUnit victim = null;
            if (attack.victimUnit() != null) {
                victim = unit(attack.victimUnit());
                if (victim != null && attack.ranged() && victim.canTeleport()) {
                    // The shot lands on the spot it left: step out of the way.
                    ArenaPoint from = attack.attackerTower() != null ?
                            arena.towerPoint(attack.attackerTower()) : null;
                    CombatUnit shooter = attack.attackerUnit() == null ? null :
                            unit(attack.attackerUnit());
                    double fx = from != null ? from.x() :
                            shooter != null ? shooter.x() : attack.x();
                    double fz = from != null ? from.z() :
                            shooter != null ? shooter.z() : attack.z();
                    blinkAway(victim, fx, fz);
                    continue;
                }
                if (victim != null && attack.ranged() && dodge(victim)) {
                    tribeEvents.add(new TribeEvent(victim.id(),
                            TribeEvent.Kind.DODGE, victim.x(), victim.z()));
                    continue;
                }
                // Slowing hits are frost (Stray arrows, snowballs): a
                // frost-weak victim takes double. A Wither under half
                // health shrugs off half of every shot.
                if (victim != null)
                    damageUnit(victim, (attack.slowTicks() > 0 &&
                                    victim.stats().traits().frostWeak() ?
                                    attack.damage() * TroopTraits.FROST_WEAKNESS :
                                    attack.damage()) *
                                    (attack.ranged() ? witherShield(victim) : 1.0D),
                            attack.side(), source);
                if (victim != null && victim.alive() && source != null &&
                        source.threeHeads())
                    witherOn(victim, source);
                if (victim != null && victim.alive() && source != null &&
                        bodyOf(victim).poisonable() &&
                        source.stats().traits().poisons())
                    victim.poison(source.stats().traits().poisonTicks(),
                            source.stats().traits().poisonPerSecond(), source);
                else if (victim != null)
                    tribeHit(attack, victim, source);
            } else {
                hitTower(attack.victimTower(), attack.damage() *
                                attackerTribe(attack, source).towerDamage(),
                        destroyed, attack.side(), source);
            }
            if (attack.splash() > 0.0D)
                splash(attack, victim, destroyed, source);
            if (attack.line() > 0.0D)
                line(attack, victim, source);
            if (attack.slowTicks() > 0 && victim != null && victim.alive())
                victim.slow(attack.slowTicks());
            if (blinds && victim != null && victim.alive()) {
                victim.blind(BattleCard.BLIND_TICKS);
                tribeEvents.add(new TribeEvent(victim.id(),
                        TribeEvent.Kind.BLIND, victim.x(), victim.z()));
            }
            if (source != null && source.levitates() && victim != null &&
                    victim.alive() && victim.stats().traits().hover() <= 0.0D) {
                victim.levitate(BattleCard.LEVITATE_TICKS);
                tribeEvents.add(new TribeEvent(victim.id(),
                        TribeEvent.Kind.LEVITATE, victim.x(), victim.z()));
            }
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

    private boolean damageUnit(CombatUnit victim, double amount, TeamSide by,
            BattleCard source)
    {
        return damageUnit(victim, amount, by, source, false);
    }

    /** What reaches a Wither through its armour (under half health: half). */
    private double witherShield(CombatUnit victim) {
        BattleCard card = cardOf.get(victim.id());
        return card != null && card.threeHeads() &&
                victim.health() < victim.maxHealth() / 2.0D ?
                BattleCard.WITHER_SHIELD : 1.0D;
    }

    /** A Wither skull's wither, as the Wither Skull spell's. */
    private void witherOn(CombatUnit victim, BattleCard source) {
        BattleCard card = cardOf.get(victim.id());
        if (card == null || !card.witherImmune())
            victim.wither(BattleCard.WITHER_TICKS, BattleCard.WITHER_PER_SECOND,
                    source);
    }

    /**
     * A troop was just killed: its tribe may bring it back (Illager totem,
     * Zombie rising again), once per troop and once per cooldown per side.
     */
    private void cheatDeath(CombatUnit unit) {
        TribeBonus bonus = unit.tribe();
        if (unit.wasSaved() || (!bonus.totem() && bonus.revive() <= 0.0D))
            return;
        TeamSide side = unit.side();
        if (bonus.totem() && now >= totemReady.getOrDefault(side, 0)) {
            unit.save(1.0D, TribeBonus.TOTEM_TICKS);
            totemReady.put(side, now + TribeBonus.TOTEM_COOLDOWN);
            tribeEvents.add(new TribeEvent(unit.id(), TribeEvent.Kind.TOTEM,
                    unit.x(), unit.z()));
        } else if (bonus.revive() > 0.0D &&
                now >= reviveReady.getOrDefault(side, 0)) {
            unit.save(unit.maxHealth() * bonus.revive(), 0);
            reviveReady.put(side, now + TribeBonus.REVIVE_COOLDOWN);
            tribeEvents.add(new TribeEvent(unit.id(), TribeEvent.Kind.REVIVE,
                    unit.x(), unit.z()));
        }
    }

    /** A Night troop with Shadow dodges a shot, if its side's dodge is ready. */
    private boolean dodge(CombatUnit victim) {
        TeamSide side = victim.side();
        if (!victim.tribe().dodgeFirstShot() ||
                now < dodgeReady.getOrDefault(side, 0))
            return false;
        dodgeReady.put(side, now + TribeBonus.DODGE_COOLDOWN);
        return true;
    }

    /** The tribe bonus of whoever made an attack (it may have died since). */
    private TribeBonus attackerTribe(Attack attack, BattleCard source) {
        CombatUnit attacker = attack.attackerUnit() == null ? null :
                unit(attack.attackerUnit());
        return attacker != null ? attacker.tribe() :
                attack.attackerUnit() == null ? TribeBonus.NONE :
                        tribeBonus(source, attack.side());
    }

    /** Other pack troops (Beast tribe step 5) near a unit, up to the cap. */
    private int packmates(CombatUnit unit) {
        int n = 0;
        for (CombatUnit other : units) {
            if (other != unit && other.alive() && other.side() == unit.side() &&
                    other.tribe().packDamage() > 0.0D &&
                    distance(unit, other) <= TribeBonus.PACK_RADIUS)
                n++;
        }
        return Math.min(n, TribeBonus.PACK_MAX);
    }

    /**
     * Skeleton volley: a second arrow at the nearest other enemy troop in
     * range, for {@link TribeBonus#EXTRA_SHOT_SHARE} of the damage.
     */
    private void volley(CombatUnit unit, CombatUnit first, double damage,
            List<Attack> attacks)
    {
        CombatUnit best = null;
        double bestDistance = Double.MAX_VALUE;
        for (CombatUnit other : units) {
            if (other == first || other.side() == unit.side() || !other.alive() ||
                    other.untargetable() || unit.stats().traits().buildingsOnly())
                continue;
            double d = distance(unit, other);
            if (d <= unit.stats().range() + unit.radius() + other.radius() &&
                    d < bestDistance) {
                best = other;
                bestDistance = d;
            }
        }
        if (best == null)
            return;
        attacks.add(new Attack(unit.id(), null, best.id(), null,
                damage * TribeBonus.EXTRA_SHOT_SHARE, true,
                flightTicks(unit.x(), unit.z(), best.x(), best.z()),
                unit.side(), best.x(), best.z()));
        tribeEvents.add(new TribeEvent(unit.id(), TribeEvent.Kind.VOLLEY,
                unit.x(), unit.z()));
    }

    /**
     * A troop's hit landed on {@code victim}: its tribe's lifesteal, poison,
     * burn, slow and first-hit freeze.
     */
    private void tribeHit(Attack attack, CombatUnit victim, BattleCard source) {
        TribeBonus bonus = attackerTribe(attack, source);
        if (bonus.none())
            return;
        CombatUnit attacker = unit(attack.attackerUnit());
        if (attacker != null && attacker.alive() && bonus.lifesteal() > 0.0D)
            attacker.heal(attack.damage() * victim.armorFactor() *
                    bonus.lifesteal());
        if (!victim.alive())
            return;
        if (bonus.poisonTicks() > 0 && bodyOf(victim).poisonable())
            victim.poison(bonus.poisonTicks(), bonus.poisonPerSecond(), source);
        BattleCard hurt = cardOf.get(victim.id());
        if (bonus.burnTicks() > 0 && (hurt == null || !hurt.fireImmune()))
            victim.ignite(bonus.burnTicks(), bonus.burnPerSecond(), source);
        // Freezes without restarting the victim's swing (a stun would).
        if (bonus.slowOnHit() > 0)
            victim.slow(bonus.slowOnHit());
        if (bonus.firstHitStun() > 0 && attacker != null && attacker.firstHit())
            victim.freeze(bonus.firstHitStun());
    }

    /**
     * Hurts a unit and credits {@code source} of side {@code by} (null: the
     * towers of that side) with the health really taken off. Armour takes
     * its share off unless {@code throughArmor} (poison, burning). A
     * water-weak unit on the river takes extra.
     */
    private boolean damageUnit(CombatUnit victim, double amount, TeamSide by,
            BattleCard source, boolean throughArmor)
    {
        // A Skeleton tribe's hits go through armour (docs/TRIBES.md).
        if (!throughArmor && !(tribes && source != null && by != null &&
                tribeBonus(source, by).pierce()))
            amount *= victim.armorFactor();
        if (victim.stats().traits().waterWeak() &&
                arena.isRiver((int) Math.floor(victim.z())))
            amount *= TroopTraits.WATER_WEAKNESS;
        // A Shulker shut in its shell takes little.
        BattleCard hit = cardOf.get(victim.id());
        // A Sheep's wool takes a whole hit (not a burn or poison).
        if (hit != null && hit.woolly() && !throughArmor && amount > 0.0D &&
                victim.alive()) {
            int left = wool.getOrDefault(victim.id(), BattleCard.WOOL_HITS);
            if (left > 0) {
                wool.put(victim.id(), left - 1);
                if (left == 1)
                    tribeEvents.add(new TribeEvent(victim.id(),
                            TribeEvent.Kind.SHEARED, victim.x(), victim.z()));
                return false;
            }
        }
        if (beaconOn(victim) == BattleCard.BeaconEffect.RESISTANCE)
            amount *= BattleCard.BEACON_RESIST;
        if (hit != null && hit.levitates() && now -
                lastAttackAt.getOrDefault(victim.id(), -1000) >
                BattleCard.SHELL_OPEN_TICKS)
            amount *= BattleCard.SHELL_FACTOR;
        double before = victim.health();
        if (!victim.hurt(amount))
            return false;
        if (!victim.alive())
            cheatDeath(victim);
        // A Villager a zombie kills rises as one of the zombies.
        if (!victim.alive() && hit != null && hit.callsGolem() &&
                source != null && source.infects() && by != null &&
                by != victim.side())
            converted.put(victim.id(), by);
        stats.troopDamage(by, source, before - victim.health(),
                !victim.alive());
        if (victim.alive())
            afterHurt(victim);
        return true;
    }

    /**
     * A troop was hurt and lives: an Illusioner under half health splits off
     * its images (once), an Axolotl under 30% plays dead (once).
     */
    private void afterHurt(CombatUnit victim) {
        BattleCard card = cardOf.get(victim.id());
        if (card == null)
            return;
        if (card.castsIllusions() && victim.generation() == 0 &&
                victim.health() < victim.maxHealth() * BattleCard.ILLUSION_BELOW &&
                illusionsCast.add(victim.id()))
            castIllusions(victim);
        if (card.squirtsInk() && inked.add(victim.id())) {
            for (CombatUnit other : units) {
                if (other.side() != victim.side() && other.alive() &&
                        edgeDistance(victim, other) <= BattleCard.INK_RADIUS)
                    other.blind(BattleCard.INK_TICKS);
            }
            tribeEvents.add(new TribeEvent(victim.id(), TribeEvent.Kind.INK,
                    victim.x(), victim.z()));
        }
        if (card.bringsLlamas() && victim.hideOnce(BattleCard.HIDE_TICKS)) {
            for (CombatUnit other : units) {
                if (victim.id().equals(other.target()))
                    other.setTarget(null);
            }
            towerTarget.values().removeIf(victim.id()::equals);
            tribeEvents.add(new TribeEvent(victim.id(), TribeEvent.Kind.HIDE,
                    victim.x(), victim.z()));
        }
        if (card.playsDead() &&
                victim.health() < victim.maxHealth() * BattleCard.PLAY_DEAD_BELOW &&
                victim.playDead(BattleCard.PLAY_DEAD_TICKS)) {
            victim.setTarget(null);
            tribeEvents.add(new TribeEvent(victim.id(),
                    TribeEvent.Kind.PLAY_DEAD, victim.x(), victim.z()));
        }
    }

    /**
     * Mirror images around an Illusioner (1 health, no attack): whoever was
     * on the Illusioner looks again, and may pick an image.
     */
    private void castIllusions(CombatUnit illusioner) {
        int sign = ArenaLayout.sideSign(illusioner.side());
        for (int i = 0; i < BattleCard.ILLUSION_COUNT; i++) {
            double angle = Math.PI * 2.0D * i / BattleCard.ILLUSION_COUNT;
            ArenaWaypoint spot = arena.clampSpawn(
                    illusioner.x() + Math.cos(angle) * 0.9D,
                    illusioner.z() + Math.sin(angle) * 0.9D, sign);
            spawns.add(new ChildSpawn(illusioner.side(), illusioner.stats(), 1,
                    1.0D, 0.0D, illusioner.radius(), spot.x(), spot.z(), 0,
                    illusioner.lockedTower()));
        }
        for (CombatUnit other : units) {
            if (illusioner.id().equals(other.target()))
                other.setTarget(null);
        }
        towerTarget.values().removeIf(illusioner.id()::equals);
        tribeEvents.add(new TribeEvent(illusioner.id(),
                TribeEvent.Kind.ILLUSION, illusioner.x(), illusioner.z()));
    }

    /** A rider on a mount that only goes for buildings shoots only those. */
    private boolean ridesForTowers(CombatUnit unit) {
        CombatUnit mount = unit.mount() == null ? null : unit(unit.mount());
        return mount != null && mount.stats().traits().buildingsOnly();
    }

    /** Whether an Endermite's or a mirror image's time is up. */
    private boolean outlived(CombatUnit unit, BattleCard card) {
        int age = now - bornAt.getOrDefault(unit.id(), now);
        if (card.luresEndermen())
            return age >= BattleCard.ENDERMITE_TICKS;
        return card.castsIllusions() && unit.generation() > 0 &&
                age >= BattleCard.ILLUSION_TICKS;
    }

    /** The nearest enemy Endermite an Enderman can go for, or none. */
    private CombatUnit nearestEndermite(CombatUnit enderman) {
        CombatUnit best = null;
        double bestDistance = BattleCard.ENDERMITE_LURE + EPSILON;
        for (CombatUnit other : units) {
            if (other.side() == enderman.side() || !other.alive() ||
                    cardOf.get(other.id()) != BattleCard.ENDERMITE ||
                    !canTarget(enderman, other))
                continue;
            double d = distance(enderman, other);
            if (d <= bestDistance) {
                best = other;
                bestDistance = d;
            }
        }
        return best;
    }

    /**
     * A Magma Cube's leap: onto an enemy within {@link BattleCard#LEAP_RANGE}
     * (an archer before anyone else, then the nearest), landing beside it
     * with a splash. False when it is not ready or nothing is close.
     */
    private boolean leap(CombatUnit cube, List<Attack> attacks) {
        if (now < leapReady.getOrDefault(cube.id(), 0))
            return false;
        CombatUnit prey = null;
        boolean preyRanged = false;
        double bestDistance = Double.MAX_VALUE;
        for (CombatUnit other : units) {
            if (other.side() == cube.side() || !other.alive() ||
                    other.untargetable() || !canTarget(cube, other))
                continue;
            double d = edgeDistance(cube, other);
            if (d > BattleCard.LEAP_RANGE + EPSILON)
                continue;
            boolean ranged = !other.stats().melee();
            if ((ranged && !preyRanged) ||
                    (ranged == preyRanged && d < bestDistance)) {
                prey = other;
                preyRanged = ranged;
                bestDistance = d;
            }
        }
        if (prey == null)
            return false;
        double dx = prey.x() - cube.x();
        double dz = prey.z() - cube.z();
        double length = Math.max(1.0E-3D, Math.hypot(dx, dz));
        double gap = cube.radius() + prey.radius() + 0.1D;
        ArenaWaypoint spot = arena.clampSpawn(prey.x() - dx / length * gap,
                prey.z() - dz / length * gap, ArenaLayout.sideSign(cube.side()));
        cube.setPosition(spot.x(), spot.z());
        teleports.add(new TickResult.Teleport(cube.id(), spot.x(), spot.z()));
        leapReady.put(cube.id(), now + BattleCard.LEAP_TICKS);
        cube.setTarget(prey.id());
        cube.startCooldown();
        lastAttackAt.put(cube.id(), now);
        attacks.add(new Attack(cube.id(), null, prey.id(), null,
                cube.damage() * BattleCard.LEAP_DAMAGE, false, 0, cube.side(),
                prey.x(), prey.z(), BattleCard.LEAP_SPLASH, 0.0D, 0, 0.0D, 0.0D,
                spot.x(), spot.z()));
        tribeEvents.add(new TribeEvent(cube.id(), TribeEvent.Kind.LEAP,
                spot.x(), spot.z()));
        return true;
    }

    /**
     * An Allay flies to just behind the nearest own troop (not another
     * Allay, not a building); with none, it holds still.
     */
    private void followTroops(CombatUnit allay, List<Move> moves) {
        CombatUnit best = null;
        double bestDistance = Double.MAX_VALUE;
        for (CombatUnit other : units) {
            BattleCard card = cardOf.get(other.id());
            if (other == allay || other.side() != allay.side() ||
                    !other.alive() || (card != null &&
                            (card.followsTroops() || card.isBuilding())))
                continue;
            double d = distance(allay, other);
            if (d < bestDistance) {
                best = other;
                bestDistance = d;
            }
        }
        if (best == null)
            return;
        double tz = best.z() + ArenaLayout.sideSign(allay.side()) * 0.8D;
        if (Math.hypot(best.x() - allay.x(), tz - allay.z()) < 0.3D)
            return;
        moves.add(new Move(allay.id(), best.x(), tz,
                allay.stats().speedPerTick() * speedMultiplier *
                        allay.tribe().speed()));
    }

    /**
     * Every Allay heals, once per its attack interval, each own troop within
     * {@link BattleCard#ALLAY_RADIUS} by its damage.
     */
    private void allayAuras() {
        for (CombatUnit allay : units) {
            BattleCard card = cardOf.get(allay.id());
            if (card == null || !card.healsAround() || !allay.alive() ||
                    allay.frozen() || (now - bornAt.getOrDefault(allay.id(),
                            now)) % allay.stats().attackTicks() != 0)
                continue;
            boolean healed = false;
            for (CombatUnit other : units) {
                if (other == allay || other.side() != allay.side() ||
                        !other.alive() || Math.hypot(other.x() - allay.x(),
                                other.z() - allay.z()) >
                                BattleCard.ALLAY_RADIUS + other.radius())
                    continue;
                double before = other.health();
                if (other.heal(allay.damage())) {
                    stats.healing(allay.side(), creditOf(allay.id()),
                            other.health() - before);
                    healed = true;
                }
            }
            if (healed)
                tribeEvents.add(new TribeEvent(allay.id(),
                        TribeEvent.Kind.HEAL_AURA, allay.x(), allay.z()));
        }
    }

    /** The one troop an Ender Pearl lands nearest (no building), if any. */
    private List<Hit> pearlTarget(ActiveSpell active) {
        CombatUnit best = null;
        double bestDistance = Double.MAX_VALUE;
        for (CombatUnit unit : units) {
            BattleCard card = cardOf.get(unit.id());
            if (!unit.alive() || (card != null && card.isBuilding()))
                continue;
            double d = Math.hypot(unit.x() - active.x, unit.z() - active.z);
            if (d <= active.card.spell().radius() + unit.radius() + EPSILON &&
                    d < bestDistance) {
                best = unit;
                bestDistance = d;
            }
        }
        // A pearled rider takes its mount along: the pearl moves the mount.
        if (best != null && best.mount() != null && unit(best.mount()) != null)
            best = unit(best.mount());
        return best == null ? List.of() :
                List.of(new Hit(best, null, 0.0D, active));
    }

    /**
     * Moves the pearled troop: an own troop forward toward the enemy, as far
     * as it can go up to {@link BattleCard#PEARL_DISTANCE} without leaving
     * its own half; an enemy as far back toward its own King.
     */
    private void pearl(CombatUnit unit, TeamSide caster) {
        int sign = ArenaLayout.sideSign(unit.side());
        double z;
        if (unit.side() == caster) {
            if (arena.sideOf(unit.z()) != sign)
                return;
            double step = BattleCard.PEARL_DISTANCE;
            while (step > 0.0D && arena.sideOf(unit.z() - sign * step) != sign)
                step -= 0.25D;
            if (step <= 0.0D)
                return;
            z = unit.z() - sign * step;
        } else {
            z = unit.z() + sign * BattleCard.PEARL_DISTANCE;
            unit.setTarget(null);
        }
        ArenaWaypoint spot = arena.clampSpawn(unit.x(), z, sign);
        unit.setPosition(spot.x(), spot.z());
        teleports.add(new TickResult.Teleport(unit.id(), spot.x(), spot.z()));
    }

    /**
     * Every unit Water Bucket touches is put out; an enemy is pushed away,
     * and hurt if water hurts it (an Enderman that can still teleport gets
     * away first).
     */
    private void douse(Hit hit, List<Push> pushes) {
        CombatUnit unit = hit.unit();
        unit.douse();
        if (unit.side() == hit.spell().side || !unit.alive())
            return;
        if (unit.canTeleport()) {
            blinkAway(unit, hit.spell().x, hit.spell().z);
            return;
        }
        if (hit.damage() > 0.0D)
            damageUnit(unit, hit.damage(), hit.spell().side, hit.spell().card,
                    true);
        if (unit.alive())
            knockBack(unit, hit.spell().x, hit.spell().z,
                    -ArenaLayout.sideSign(hit.spell().side),
                    hit.spell().card.spell().knockback(), pushes);
    }

    /**
     * Every unit of both sides inside a Water Bucket's circle (towers are
     * left alone); enemies that water hurts take its damage.
     */
    private List<Hit> waterTargets(ActiveSpell active) {
        List<Hit> hits = new ArrayList<>();
        for (CombatUnit unit : units) {
            if (!unit.alive() || Math.hypot(unit.x() - active.x,
                    unit.z() - active.z) > active.card.spell().radius() +
                    unit.radius() + EPSILON)
                continue;
            BattleCard card = cardOf.get(unit.id());
            boolean hurts = unit.side() != active.side && card != null &&
                    card.hurtByWater();
            hits.add(new Hit(unit, null, hurts ? BattleCard.WATER_DAMAGE : 0.0D,
                    active));
        }
        return hits;
    }

    private void hitTower(TowerId id, double damage, List<TowerId> destroyed,
            TeamSide by, BattleCard source)
    {
        TowerState tower = match.tower(id);
        boolean wasStanding = !tower.isDestroyed();
        double before = tower.remaining();
        match.damageTower(id, damage);
        stats.towerDamage(by, source, before - tower.remaining());
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
            List<TowerId> destroyed, BattleCard source)
    {
        double cx = victim != null ? victim.x() : attack.x();
        double cz = victim != null ? victim.z() : attack.z();
        for (CombatUnit other : units) {
            if (other == victim || other.side() == attack.side() ||
                    !other.alive())
                continue;
            if (Math.hypot(other.x() - cx, other.z() - cz) <=
                    attack.splash() + other.radius() + EPSILON)
                damageUnit(other, attack.damage(), attack.side(), source);
        }
        for (TowerId id : TowerId.values()) {
            if (id == attack.victimTower() || id.side() == attack.side() ||
                    match.tower(id).isDestroyed())
                continue;
            if (distanceToTower(cx, cz, id) <= attack.splash() + EPSILON)
                hitTower(id, attack.damage() *
                                attackerTribe(attack, source).towerDamage(),
                        destroyed, attack.side(), source);
        }
    }

    /**
     * Fangs along the line from where the attacker stood, through the
     * target, to {@code lineLength}: every other enemy whose body touches
     * the line is bitten too.
     */
    private void line(Attack attack, CombatUnit victim, BattleCard source) {
        double dx = attack.x() - attack.fromX();
        double dz = attack.z() - attack.fromZ();
        double length = Math.hypot(dx, dz);
        if (length < EPSILON)
            return;
        dx /= length;
        dz /= length;
        double reach = Math.max(length, attack.lineLength());
        for (CombatUnit other : units) {
            if (other == victim || other.side() == attack.side() ||
                    !other.alive())
                continue;
            double ox = other.x() - attack.fromX();
            double oz = other.z() - attack.fromZ();
            double along = ox * dx + oz * dz;
            if (along < 0.0D || along > reach)
                continue;
            double across = Math.abs(ox * dz - oz * dx);
            if (across <= attack.line() / 2.0D + other.radius() + EPSILON)
                damageUnit(other, attack.damage(), attack.side(), source);
        }
    }

    private List<Death> collectDeaths() {
        List<Death> deaths = new ArrayList<>();
        for (int i = 0; i < units.size(); i++) {
            CombatUnit unit = units.get(i);
            if (unit.alive())
                continue;
            units.remove(i--);
            if (unit.tribe().deathBurnTicks() > 0)
                lastFlame(unit);
            BattleCard dead = cardOf.get(unit.id());
            if (dead != null && dead.explodesOnDeath())
                blast(unit);
            TeamSide zombies = converted.remove(unit.id());
            if (zombies != null)
                rise(unit, zombies);
            if (dead != null && dead.carriesBanner()) {
                banners.add(new Banner(unit.side(), unit.x(), unit.z(),
                        BattleCard.BANNER_DROP_TICKS));
                tribeEvents.add(new TribeEvent(unit.id(),
                        TribeEvent.Kind.BANNER, unit.x(), unit.z()));
            }
            if (dead != null && dead.spawnsMobs() && unit.health() <= 0.0D &&
                    now - bornAt.getOrDefault(unit.id(), now) <
                            BattleCard.BUILDING_LIFETIME_TICKS)
                spawnFrom(unit, BattleCard.SPAWNER_BREAK_MOBS);
            deaths.add(new Death(unit.id(), unit.x(), unit.z(),
                    childrenOf(unit)));
        }
        return deaths;
    }

    /** A Villager killed by a zombie gets up as a Zombie Villager of its side. */
    private void rise(CombatUnit villager, TeamSide side) {
        TroopStats zombie = BattleCard.ZOMBIE_VILLAGER.stats();
        TribeBonus bonus = tribeBonus(BattleCard.ZOMBIE_VILLAGER, side);
        ArenaWaypoint spot = arena.clampSpawn(villager.x(), villager.z(),
                ArenaLayout.sideSign(side));
        spawns.add(new ChildSpawn(side, zombie, 0,
                zombie.health() * bonus.health(), zombie.damage() * bonus.damage(),
                zombie.radius(), spot.x(), spot.z(), 6, null));
        tribeEvents.add(new TribeEvent(villager.id(), TribeEvent.Kind.CONVERT,
                spot.x(), spot.z()));
    }

    /** Nether tribe step 5: a dying troop sets the enemies near it burning. */
    private void lastFlame(CombatUnit dead) {
        BattleCard source = creditOf(dead.id());
        for (CombatUnit other : units) {
            if (other.side() == dead.side() || !other.alive())
                continue;
            BattleCard card = cardOf.get(other.id());
            if (card != null && card.fireImmune())
                continue;
            if (distance(dead, other) <= TribeBonus.DEATH_BURN_RADIUS)
                other.ignite(dead.tribe().deathBurnTicks(),
                        dead.tribe().deathBurnPerSecond(), source);
        }
        tribeEvents.add(new TribeEvent(dead.id(), TribeEvent.Kind.LAST_FLAME,
                dead.x(), dead.z()));
    }

    private List<ChildSpawn> childrenOf(CombatUnit parent) {
        SplitRule rule = parent.stats().split();
        if (rule == null || parent.generation() >= rule.generations())
            return List.of();
        List<ChildSpawn> children = new ArrayList<>(rule.children());
        int sign = ArenaLayout.sideSign(parent.side());
        // A mount that carried its rider from the start leaves nobody: the
        // rider is already fighting on its own.
        if (rule.into() != null && carrying.remove(parent.id()))
            return List.of();
        if (rule.into() != null) {
            TroopStats rider = rule.into();
            ArenaWaypoint spot = arena.clampSpawn(parent.x(), parent.z(), sign);
            TribeBonus bonus = tribeBonus(BattleCard.forStats(rider),
                    parent.side());
            ChildSpawn child = new ChildSpawn(parent.side(), rider,
                    parent.generation() + 1, rider.health() * bonus.health(),
                    rider.damage() * bonus.damage(),
                    rider.radius(), spot.x(), spot.z(),
                    rule.invulnerableTicks(), parent.lockedTower());
            // A rider fights for the card that was played (the Camel's
            // Husk is the Camel's damage), not for its own card.
            riderCredit.put(child, creditOf(parent.id()));
            children.add(child);
            return children;
        }
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
