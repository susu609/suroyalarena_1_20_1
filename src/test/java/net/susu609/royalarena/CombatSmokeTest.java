package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.arena.ArenaWaypoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.CardTactic;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.card.CostFormula;
import net.susu609.royalarena.combat.ChildSpawn;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.combat.Formation;
import net.susu609.royalarena.combat.Separation;
import net.susu609.royalarena.combat.TickResult;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;
import net.susu609.royalarena.tower.TowerKind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure-Java checks for the combat rules; run from DomainSmokeTest. */
public final class CombatSmokeTest {
    private CombatSmokeTest() {
    }

    public static void run() {
        costsFollowTheFormula();
        cardJsonMatchesCode();
        formationsAreCentredAndSpaced();
        routingCrossesTheRiverOnBridges();
        spawnPointsStayOnLand();
        unitsTradeBlowsOnTheirInterval();
        meleeIgnoresEnemiesAcrossTheRiver();
        archersShootAcrossTheRiver();
        unitsPreferEnemyTroopsOverTowers();
        overlappingUnitsSpreadOut();
        troopsMarchOverTheBridgeToTheirTower();
        speedMultiplierScalesWalking();
        crowdedUnitsStillGetThroughTheBridge();
        tanksReachTheTowerHealthy();
        towersShootTheNearestEnemyOnTheirInterval();
        kingTowerSleepsUntilHitOrPrincessFalls();
        troopsDestroyTowersAndRetargetKing();
        bigSlimeSplitsTwiceThenDies();
        System.out.println("RoyalArena combat smoke tests: PASS");
    }

    // ------------------------------------------------------------ helpers

    private static MatchInstance battle() {
        MatchInstance match = MatchInstance.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ArenaLayout.standard(new ArenaPoint(0, 80, 0)));
        match.startBattle();
        return match;
    }

    private static CombatUnit spawn(CombatEngine engine, BattleCard card,
            TeamSide side, double x, double z)
    {
        return engine.add(UUID.randomUUID(), side, card.stats(), x, z);
    }

    /**
     * One combat tick, then every unit walks one step toward its waypoint,
     * nudged apart from its neighbours exactly like BattleField does.
     */
    private static TickResult step(CombatEngine engine, ArenaLayout arena) {
        TickResult result = engine.tick();
        java.util.Map<UUID, double[]> walk = new java.util.HashMap<>();
        for (TickResult.Move move : result.moves()) {
            CombatUnit unit = engine.unit(move.unit());
            if (unit == null)
                continue;
            double dx = move.x() - unit.x();
            double dz = move.z() - unit.z();
            double distance = Math.hypot(dx, dz);
            if (distance < 1.0E-9D)
                continue;
            double stride = Math.min(move.speed(), distance);
            walk.put(unit.id(),
                    new double[] {dx / distance * stride, dz / distance * stride});
        }
        List<CombatUnit> units = engine.units();
        double[][] motion = new double[units.size()][];
        for (int i = 0; i < units.size(); i++) {
            CombatUnit unit = units.get(i);
            double[] own = walk.getOrDefault(unit.id(), new double[2]);
            double[] push = Separation.offset(unit, units, arena);
            motion[i] = new double[] {own[0] + push[0], own[1] + push[1]};
        }
        for (int i = 0; i < units.size(); i++) {
            units.get(i).setPosition(units.get(i).x() + motion[i][0],
                    units.get(i).z() + motion[i][1]);
        }
        return result;
    }

    /** Ticks without moving anyone, so ranges stay fixed. */
    private static TickResult hold(CombatEngine engine) {
        return engine.tick();
    }

    private static void near(double actual, double expected, String name) {
        if (Math.abs(actual - expected) > 1.0E-6D)
            throw new AssertionError(name + ": " + actual + " != " + expected);
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }

    // -------------------------------------------------------------- cards

    private static void costsFollowTheFormula() {
        for (BattleCard card : BattleCard.values()) {
            if (card.isSpell()) {
                check(CostFormula.elixir(card.spell()) == card.cost(),
                        card.id() + " cost " + card.cost() + " vs formula " +
                                CostFormula.raw(card.spell()));
                continue;
            }
            check(CostFormula.elixir(card.stats()) == card.cost(),
                    card.id() + " cost " + card.cost() + " vs formula " +
                            CostFormula.raw(card.stats()));
        }
        var starter = BattleDeck.starter().snapshotCycle();
        check(starter.size() == 8, "starter deck has 8 cards");
        check(starter.contains(BattleCard.ARROW_STORM) &&
                starter.contains(BattleCard.FIREBALL),
                "starter deck carries both spells");
    }

    private static void cardJsonMatchesCode() {
        Path dir = Path.of("src/main/resources/data/royalarena/cards");
        if (!Files.isDirectory(dir))
            return;
        for (BattleCard card : BattleCard.values()) {
            Path file = dir.resolve(card.id() + ".json");
            check(Files.exists(file), "card json exists: " + card.id());
            String json;
            try {
                json = Files.readString(file);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
            near(number(json, "elixir"), card.cost(), card.id() + " elixir");
            check(roles(json).equals(card.tactics()), card.id() +
                    " roles in the JSON " + roles(json) + " vs the code " +
                    card.tactics());
            if (card.isSpell()) {
                spellJsonMatches(card, json);
                continue;
            }
            var s = card.stats();
            near(number(json, "count"), s.count(), card.id() + " count");
            near(number(json, "model_scale"), card.modelScale(),
                    card.id() + " model scale");
            near(number(json, "health"), s.health(), card.id() + " health");
            near(number(json, "damage"), s.damage(), card.id() + " damage");
            near(numberOr(json, "armor", 0.0D), s.armor(), card.id() + " armor");
            near(number(json, "attack_interval_ticks"), s.attackTicks(),
                    card.id() + " interval");
            near(number(json, "range"), s.range(), card.id() + " range");
            near(number(json, "move_speed"), s.speed(), card.id() + " speed");
            near(json.contains("\"slow_ticks\"") ? number(json, "slow_ticks")
                    : 0.0D, s.traits().slowTicks(), card.id() + " slow");
            var t = s.traits();
            near(numberOr(json, "splash_radius", 0.0D), t.splashRadius(),
                    card.id() + " splash");
            near(numberOr(json, "knockback", 0.0D), t.knockback(),
                    card.id() + " knockback");
            near(numberOr(json, "poison_ticks", 0.0D), t.poisonTicks(),
                    card.id() + " poison ticks");
            near(numberOr(json, "poison_per_second", 0.0D), t.poisonPerSecond(),
                    card.id() + " poison per second");
            near(numberOr(json, "teleport_cooldown_ticks", 0.0D),
                    t.teleportCooldownTicks(), card.id() + " teleport cooldown");
            near(numberOr(json, "hover", 0.0D), t.hover(), card.id() + " hover");
            flag(json, "fire_immune", card.fireImmune(), card.id());
            check(json.contains("\"body\": \"" + card.body().id() + "\""),
                    card.id() + " body in the JSON is " + card.body().id());
            flag(json, "self_destruct", t.selfDestruct(), card.id());
            flag(json, "crosses_river", t.crossesRiver(), card.id());
            flag(json, "deploy_on_river", t.deployOnRiver(), card.id());
            flag(json, "frost_weak", t.frostWeak(), card.id());
            flag(json, "water_weak", t.waterWeak(), card.id());
            flag(json, "frost_immune", t.frostImmune(), card.id());
            flag(json, "river_strong", t.riverStrong(), card.id());
            near(numberOr(json, "line_width", 0.0D), t.lineWidth(),
                    card.id() + " line width");
            near(numberOr(json, "charge_ticks", 0.0D), t.chargeTicks(),
                    card.id() + " charge");
            near(numberOr(json, "fire_trail_ticks", 0.0D), t.fireTrailTicks(),
                    card.id() + " fire trail");
            check(json.contains("\"targets\": \"buildings\"") ==
                    t.buildingsOnly(), card.id() + " targets buildings");
        }
    }

    /** The {@code "roles"} list of a card JSON as tactics. */
    private static java.util.Set<CardTactic> roles(String json) {
        java.util.Set<CardTactic> set = java.util.EnumSet.noneOf(
                CardTactic.class);
        java.util.regex.Matcher list = Pattern.compile(
                "\"roles\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(json);
        check(list.find(), "card json has a roles list");
        for (CardTactic tactic : CardTactic.values()) {
            if (list.group(1).contains("\"" + tactic.id() + "\""))
                set.add(tactic);
        }
        return set;
    }

    private static double numberOr(String json, String key, double fallback) {
        return json.contains("\"" + key + "\"") ? number(json, key) : fallback;
    }

    private static void flag(String json, String key, boolean expected,
            String card)
    {
        boolean found = Pattern.compile("\"" + Pattern.quote(key) +
                "\"\\s*:\\s*true").matcher(json).find();
        check(found == expected, card + " " + key + " is " + expected);
    }

    private static void spellJsonMatches(BattleCard card, String json) {
        var s = card.spell();
        String id = card.id();
        check(json.contains("\"type\": \"spell\""), id + " is a spell");
        near(number(json, "radius"), s.radius(), id + " radius");
        near(number(json, "travel_ticks"), s.castTicks(), id + " travel");
        if (s.pulses() > 1) {
            near(number(json, "duration_ticks"),
                    s.pulses() * s.pulseInterval(), id + " duration");
            near(number(json, "pulse_interval_ticks"), s.pulseInterval(),
                    id + " pulse interval");
            near(number(json, s.heal() ? "heal_per_pulse" :
                    "damage_per_pulse"), s.damagePerPulse(),
                    id + " pulse damage");
        } else {
            near(number(json, "damage"), s.damagePerPulse(), id + " damage");
        }
        if (json.contains("\"knockback\""))
            near(number(json, "knockback"), s.knockback(), id + " knockback");
        check(s.heal() == json.contains("heal_per_pulse"), id + " heals");
        near(json.contains("\"slow_ticks\"") ? number(json, "slow_ticks") : 0.0D,
                s.slowTicks(), id + " slow");
        near(json.contains("\"haste_ticks\"") ? number(json, "haste_ticks") :
                0.0D, s.hasteTicks(), id + " haste");
        check(s.poison() == json.contains("\"poison\": true"), id + " poison");
        if (json.contains("\"burn\""))
            near(number(json, "damage_per_second"), s.burnPerSecond(),
                    id + " burn");
        near(json.contains("\"tower_multiplier\"") ?
                number(json, "tower_multiplier") : s.heal() ? 0.0D : 0.3D,
                s.towerMultiplier(), id + " tower multiplier");
        near(json.contains("\"max_targets\"") ? number(json, "max_targets")
                : 0.0D, s.maxTargets(), id + " max targets");
        near(json.contains("\"stun_ticks\"") ? number(json, "stun_ticks")
                : 0.0D, s.stunTicks(), id + " stun");
        near(json.contains("\"freeze_ticks\"") ? number(json, "freeze_ticks")
                : 0.0D, s.freezeTicks(), id + " freeze");
        if (s.burnTicks() > 0) {
            near(number(json, "damage_per_second"), s.burnPerSecond(),
                    id + " burn");
            near(number(json, "duration_ticks"), s.burnTicks(),
                    id + " burn time");
        }
    }

    private static double number(String json, String key) {
        Matcher m = Pattern.compile(
                "\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?[0-9.]+)")
                .matcher(json);
        check(m.find(), "json key " + key);
        return Double.parseDouble(m.group(1));
    }

    private static void formationsAreCentredAndSpaced() {
        for (int count = 1; count <= 6; count++) {
            double[][] offsets = Formation.offsets(count, 1.0D);
            check(offsets.length == count, "one offset per unit " + count);
            double mx = 0;
            double mz = 0;
            for (double[] o : offsets) {
                mx += o[0];
                mz += o[1];
            }
            near(mx / count, 0.0D, "formation centred X for " + count);
            near(mz / count, 0.0D, "formation centred Z for " + count);
            for (int i = 0; i < count; i++) {
                for (int j = i + 1; j < count; j++) {
                    double d = Math.hypot(offsets[i][0] - offsets[j][0],
                            offsets[i][1] - offsets[j][1]);
                    check(d >= 0.99D, "formation spacing for " + count);
                }
            }
        }
    }

    // ------------------------------------------------------------ routing

    private static void routingCrossesTheRiverOnBridges() {
        ArenaLayout arena = ArenaLayout.standard(new ArenaPoint(0, 80, 0));
        check(arena.sideOf(5.0D) == 1 && arena.sideOf(-5.0D) == -1 &&
                arena.sideOf(0.0D) == 0 && arena.sideOf(1.0D) == 0,
                "river band is sideOf 0");

        ArenaWaypoint near = arena.route(-2.0D, 10.0D, -6.0D, -10.0D);
        check(near.x() == -6.0D && near.z() == 1.5D,
                "BLUE heads for the near end of the bridge");
        ArenaWaypoint across = arena.route(-6.0D, 1.5D, -6.0D, -10.0D);
        check(across.x() == -6.0D && across.z() == -2.0D,
                "BLUE lined up crosses to the far side");
        ArenaWaypoint onBridge = arena.route(-6.0D, 0.0D, -6.0D, -10.0D);
        check(onBridge.z() == -2.0D, "BLUE on the bridge keeps crossing");
        ArenaWaypoint past = arena.route(-6.0D, -2.0D, -6.0D, -10.0D);
        check(past.x() == -6.0D && past.z() == -10.0D,
                "past the river the target itself is the waypoint");
        ArenaWaypoint red = arena.route(4.0D, -10.0D, 6.0D, 10.0D);
        check(red.x() == 6.0D && red.z() == -1.5D,
                "RED mirrors the BLUE route");
        ArenaWaypoint back = arena.route(4.0D, -3.0D, 6.0D, 5.0D);
        check(back.x() == 6.0D && back.z() == -1.5D,
                "RED walking toward BLUE heads for its near bridge end");
        ArenaWaypoint backAcross = arena.route(6.0D, -3.0D, 6.0D, 5.0D);
        check(backAcross.z() == 2.0D,
                "lined up RED crosses to BLUE's bank");
        ArenaWaypoint overTheBridge = arena.route(-5.2D, 1.2D, -6.0D, -10.0D);
        check(overTheBridge.x() == -6.0D && overTheBridge.z() == -2.0D,
                "anywhere over the bridge's width counts as lined up");
        ArenaWaypoint slide = arena.route(-3.0D, 1.2D, -6.0D, -10.0D);
        check(slide.x() == -6.0D && slide.z() == 1.2D,
                "at the bank but off the bridge: slide sideways, never back");
        ArenaWaypoint redSlide = arena.route(3.0D, -1.2D, 6.0D, 10.0D);
        check(redSlide.x() == 6.0D && redSlide.z() == -1.2D,
                "RED slides along its own bank the same way");
        ArenaWaypoint sameBank = arena.route(0.0D, 5.0D, 3.0D, 9.0D);
        check(sameBank.x() == 3.0D && sameBank.z() == 9.0D,
                "same bank goes straight to the target");
    }

    private static void spawnPointsStayOnLand() {
        ArenaLayout arena = ArenaLayout.standard(new ArenaPoint(0, 80, 0));
        ArenaWaypoint outside = arena.clampSpawn(50.0D, 50.0D, 1);
        check(outside.x() == arena.maxX() + 0.5D &&
                outside.z() == arena.maxZ() + 0.5D, "clamped to the arena");
        ArenaWaypoint water = arena.clampSpawn(2.0D, 0.2D, 1);
        check(water.z() == 1.5D, "water off the bridge moves to the bank");
        ArenaWaypoint bridge = arena.clampSpawn(-6.5D, 0.2D, 1);
        check(bridge.z() == 0.2D, "bridge tiles are kept");
    }

    // ------------------------------------------------------------ fighting

    private static void unitsTradeBlowsOnTheirInterval() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        // Out of every tower's range, on BLUE's bank next to the river.
        CombatUnit blue = spawn(engine, BattleCard.ZOMBIE, TeamSide.BLUE,
                0.0D, 3.2D);
        CombatUnit red = spawn(engine, BattleCard.ZOMBIE, TeamSide.RED,
                0.0D, 2.0D);

        TickResult first = hold(engine);
        check(first.attacks().size() == 2 && first.attacks().stream()
                .allMatch(a -> a.attackerUnit() != null),
                "both zombies swing at once and no tower is involved");
        // A zombie's 2 armour points take 8% off each 3 damage hit.
        double hit = 3.0D * BattleCard.ZOMBIE.stats().armorFactor();
        near(blue.health(), 20.0D - hit, "blue hit once, through its armour");
        near(red.health(), 20.0D - hit, "red hit once");
        check(blue.target().equals(red.id()), "blue chases the red zombie");

        for (int tick = 2; tick <= 20; tick++) {
            check(hold(engine).attacks().isEmpty(),
                    "no swing before the interval, tick " + tick);
        }
        near(blue.health(), 20.0D - hit, "no damage between swings");
        check(hold(engine).attacks().size() == 2, "swing again on tick 21");
        near(blue.health(), 20.0D - 2.0D * hit, "second hit lands after 20 ticks");
    }

    private static void meleeIgnoresEnemiesAcrossTheRiver() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit blue = spawn(engine, BattleCard.ZOMBIE, TeamSide.BLUE,
                0.0D, 1.6D);
        spawn(engine, BattleCard.ZOMBIE, TeamSide.RED, 0.0D, -1.6D);
        TickResult result = hold(engine);
        check(blue.target() == null && result.attacks().isEmpty(),
                "melee zombies do not chase across the river");
        check(result.moves().size() == 2, "they walk toward the bridges");
    }

    private static void archersShootAcrossTheRiver() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        spawn(engine, BattleCard.SKELETON_ARCHER, TeamSide.BLUE, 0.0D, 3.0D);
        CombatUnit red = spawn(engine, BattleCard.ZOMBIE, TeamSide.RED,
                0.0D, -1.5D);
        TickResult result = hold(engine);
        check(result.attacks().size() == 1 && result.attacks().get(0).ranged(),
                "archer fires a ranged attack over the river");
        int flight = result.attacks().get(0).flightTicks();
        check(flight == CombatEngine.flightTicks(0.0D, 3.0D, 0.0D, -1.5D) &&
                flight >= 2, "the arrow flies for its distance: " + flight);
        check(result.landed().isEmpty() && engine.shotsInFlight() == 1,
                "the arrow is in the air");
        near(red.health(), 20.0D, "no damage before the arrow lands");
        for (int i = 1; i < flight; i++)
            check(hold(engine).landed().isEmpty(), "still flying");
        TickResult hit = hold(engine);
        check(hit.landed().size() == 1 && hit.landed().get(0).ranged(),
                "the arrow lands after its flight");
        near(red.health(), 20.0D - 4.0D * BattleCard.ZOMBIE.stats().armorFactor(),
                "archer arrow does 4 damage, less the zombie's armour");
    }

    private static void unitsPreferEnemyTroopsOverTowers() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit blue = spawn(engine, BattleCard.VINDICATOR, TeamSide.BLUE,
                0.0D, -9.0D);
        CombatUnit red = spawn(engine, BattleCard.ZOMBIE, TeamSide.RED,
                0.0D, -13.0D);
        hold(engine);
        check(red.id().equals(blue.target()),
                "enemy troop in sight is chased before the tower");
        CombatUnit far = spawn(engine, BattleCard.ZOMBIE, TeamSide.BLUE,
                8.0D, 8.0D);
        hold(engine);
        check(far.target() == null, "nothing in sight means no target");
    }

    private static void crowdedUnitsStillGetThroughTheBridge() {
        // Tough or numerous enough to survive the walk to the tower.
        for (BattleCard card : new BattleCard[] {BattleCard.IRON_GOLEM,
                BattleCard.SPIDER})
        {
            MatchInstance match = battle();
            CombatEngine engine = new CombatEngine(match);
            // Two cards on the very same tile: everything overlaps at first.
            for (int copy = 0; copy < 2; copy++) {
                for (double[] o : Formation.offsets(card.stats().count(), 0.8D))
                    spawn(engine, card, TeamSide.BLUE, -6.0D + o[0], 3.0D + o[1]);
            }
            int total = engine.units().size();
            boolean hit = false;
            int across = 0;
            for (int tick = 0; tick < 4000 && !hit; tick++) {
                TickResult result = step(engine, match.arena());
                hit = result.attacks().stream().anyMatch(
                        a -> TowerId.RED_RIGHT.equals(a.victimTower()));
            }
            for (CombatUnit unit : engine.units()) {
                if (unit.z() < -1.0D)
                    across++;
            }
            check(hit, card.id() + ": crowded units reached the tower (" +
                    across + "/" + total + " across the river)");
        }
    }

    private static void overlappingUnitsSpreadOut() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        ArenaLayout arena = match.arena();
        CombatUnit a = spawn(engine, BattleCard.ZOMBIE, TeamSide.BLUE, 0.0D, 5.0D);
        CombatUnit b = spawn(engine, BattleCard.ZOMBIE, TeamSide.BLUE, 0.0D, 5.0D);
        CombatUnit c = spawn(engine, BattleCard.SPIDER, TeamSide.BLUE, 0.2D, 5.1D);
        for (int tick = 0; tick < 60; tick++) {
            for (CombatUnit unit : engine.units()) {
                double[] push = Separation.offset(unit, engine.units(), arena);
                check(Math.hypot(push[0], push[1]) <=
                        unit.stats().speedPerTick() * 0.6D + 1.0E-9D,
                        "separation push stays below the unit's walking speed");
                unit.setPosition(unit.x() + push[0], unit.z() + push[1]);
            }
        }
        check(Math.hypot(a.x() - b.x(), a.z() - b.z()) >= 0.6D - 0.05D,
                "two zombies standing on one spot are pushed apart");
        check(Math.hypot(a.x() - c.x(), a.z() - c.z()) >=
                a.radius() + c.radius() - 0.05D,
                "a zombie and a spider do not overlap");

        CombatUnit onBridge = spawn(engine, BattleCard.ZOMBIE, TeamSide.BLUE,
                -6.0D, 0.0D);
        CombatUnit beside = spawn(engine, BattleCard.ZOMBIE, TeamSide.BLUE,
                -6.1D, 0.1D);
        double[] push = Separation.offset(
                onBridge, engine.units(), arena);
        check(push[0] == 0.0D && push[1] == 0.0D,
                "units on a bridge are not pushed toward the water");
        CombatUnit alone = spawn(engine, BattleCard.ZOMBIE, TeamSide.BLUE,
                8.0D, 12.0D);
        double[] none = Separation.offset(
                alone, engine.units(), arena);
        check(none[0] == 0.0D && none[1] == 0.0D,
                "a unit with nobody close is not pushed");
        check(beside.alive(), "second bridge unit exists");
    }

    private static void speedMultiplierScalesWalking() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit zombie = spawn(engine, BattleCard.ZOMBIE, TeamSide.BLUE,
                -6.0D, 8.0D);
        double normal = engine.tick().moves().get(0).speed();
        engine.setSpeedMultiplier(2.0D);
        double fast = engine.tick().moves().get(0).speed();
        near(normal, BattleCard.ZOMBIE.stats().speedPerTick(),
                "default walking speed comes from the card");
        near(fast, normal * 2.0D, "speedMultiplier doubles the walk");
        check(zombie.alive(), "zombie still alive");
    }

    private static void troopsMarchOverTheBridgeToTheirTower() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit golem = spawn(engine, BattleCard.IRON_GOLEM,
                TeamSide.BLUE, -6.0D, 3.0D);
        boolean hitTower = false;
        boolean crossed = false;
        for (int tick = 0; tick < 4000 && !hitTower && golem.alive(); tick++) {
            TickResult result = step(engine, match.arena());
            if (Math.abs(golem.z()) < 1.0D) {
                crossed = true;
                check(Math.abs(golem.x() + 6.0D) <= 1.0D,
                        "river crossing happens on the bridge, x=" + golem.x());
            }
            hitTower = result.attacks().stream().anyMatch(
                    a -> TowerId.RED_RIGHT.equals(a.victimTower()));
        }
        check(crossed, "the golem walked over the river");
        check(hitTower, "the golem reached and hit its lane tower");
        check(golem.lockedTower() == TowerId.RED_RIGHT,
                "BLUE troops on the left lane attack RED's right tower");
        check(golem.z() < -6.0D, "it stopped at the tower, deep in RED's half");
    }

    /**
     * Played at the bridge with nobody defending, a tank reaches its tower
     * with most of its health, and an Iron Golem takes the tower alone.
     */
    private static void tanksReachTheTowerHealthy() {
        for (BattleCard card : new BattleCard[] {BattleCard.IRON_GOLEM,
                BattleCard.RAVAGER, BattleCard.HOGLIN})
        {
            MatchInstance match = battle();
            CombatEngine engine = new CombatEngine(match);
            CombatUnit tank = spawn(engine, card, TeamSide.BLUE, -6.0D, 2.0D);
            int tick = 0;
            while (tick < 600 && tank.alive() && match.tower(
                    TowerId.RED_RIGHT).health() == TowerKind.PRINCESS
                    .maxHealth())
            {
                step(engine, match.arena());
                tick++;
            }
            check(tick < 240, card.id() + " hits the tower within 12 s: " +
                    tick / 20.0D);
            check(tank.health() >= card.stats().health() * 0.6D,
                    card.id() + " arrives with 60% health: " + tank.health());
            if (card != BattleCard.IRON_GOLEM)
                continue;
            for (int i = 0; i < 1200 && tank.alive(); i++)
                step(engine, match.arena());
            check(match.tower(TowerId.RED_RIGHT).isDestroyed(),
                    "an undefended Iron Golem takes the tower");
        }
    }

    private static void towersShootTheNearestEnemyOnTheirInterval() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit red = spawn(engine, BattleCard.ZOMBIE, TeamSide.RED,
                -6.0D, 3.0D);
        TickResult first = hold(engine);
        check(first.attacks().stream().anyMatch(
                a -> TowerId.BLUE_LEFT.equals(a.attackerTower())),
                "Princess tower fires at an enemy in range");
        int flight = first.attacks().get(0).flightTicks();
        // Tower arrows are hits, so the Zombie's armour takes its share.
        double zombieArmor = BattleCard.ZOMBIE.stats().armorFactor();
        double shot1 = 20.0D - TowerKind.PRINCESS.damage() * zombieArmor;
        near(red.health(), 20.0D, "the arrow is still in the air");
        // Shot on tick 1 lands on tick 1 + flight; the next shot on tick 17.
        int tick = 1;
        while (tick < 1 + flight) {
            hold(engine);
            tick++;
        }
        near(red.health(), shot1,
                "a tower shot does its damage on landing, less the zombie's armour");
        while (tick < 16 + flight) {
            hold(engine);
            tick++;
        }
        near(red.health(), shot1, "no second shot before 16 ticks");
        hold(engine);
        near(red.health(), shot1 - TowerKind.PRINCESS.damage() * zombieArmor,
                "second shot fired on tick 17 lands, less the zombie's armour");

        CombatUnit out = spawn(engine, BattleCard.ZOMBIE, TeamSide.RED,
                -6.0D, 0.5D);
        for (int i = 0; i < 40; i++)
            hold(engine);
        near(out.health(), 20.0D, "enemy beyond tower range is not shot");

        MatchInstance duel = battle();
        CombatEngine duelEngine = new CombatEngine(duel);
        CombatUnit archer = spawn(duelEngine, BattleCard.SKELETON_ARCHER,
                TeamSide.RED, -6.0D, 3.0D);
        CombatUnit zombie = spawn(duelEngine, BattleCard.ZOMBIE,
                TeamSide.RED, -6.0D, 4.0D);
        TickResult shot = hold(duelEngine);
        for (int i = 0; i < shot.attacks().get(0).flightTicks(); i++)
            hold(duelEngine);
        near(zombie.health(), shot1,
                "tower picks the nearest enemy when several are in range");
        near(archer.health(), 20.0D, "farther enemy is untouched");
    }

    private static void kingTowerSleepsUntilHitOrPrincessFalls() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit red = spawn(engine, BattleCard.ZOMBIE, TeamSide.RED,
                0.0D, 5.0D);
        boolean kingShot = false;
        for (int i = 0; i < 80 && red.alive(); i++) {
            kingShot |= hold(engine).attacks().stream().anyMatch(
                    a -> TowerId.BLUE_KING.equals(a.attackerTower()));
        }
        check(!kingShot, "sleeping King does not shoot");
        check(!engine.kingAwake(TeamSide.BLUE), "King starts asleep");

        match.damageTower(TowerId.BLUE_KING, 1.0D);
        check(engine.kingAwake(TeamSide.BLUE), "a hit wakes the King");
        CombatUnit second = spawn(engine, BattleCard.ZOMBIE, TeamSide.RED,
                0.0D, 5.0D);
        TickResult result = hold(engine);
        check(result.attacks().stream().anyMatch(
                a -> TowerId.BLUE_KING.equals(a.attackerTower()) &&
                        second.id().equals(a.victimUnit())),
                "awake King shoots");

        MatchInstance other = battle();
        CombatEngine otherEngine = new CombatEngine(other);
        other.damageTower(TowerId.RED_LEFT, 1000.0D);
        check(otherEngine.kingAwake(TeamSide.RED),
                "losing a Princess tower wakes the King");
        check(!otherEngine.kingAwake(TeamSide.BLUE),
                "the other King stays asleep");
    }

    private static void troopsDestroyTowersAndRetargetKing() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        match.damageTower(TowerId.RED_LEFT, 95.0D);
        check(match.tower(TowerId.RED_LEFT).health() == 15, "15 HP left");
        CombatUnit blue = spawn(engine, BattleCard.VINDICATOR, TeamSide.BLUE,
                6.0D, -8.2D);

        TickResult first = hold(engine);
        check(blue.lockedTower() == TowerId.RED_LEFT,
                "lane tower is locked from the spawn side");
        check(first.attacks().stream().anyMatch(
                a -> TowerId.RED_LEFT.equals(a.victimTower())),
                "vindicator hits the tower");
        check(match.tower(TowerId.RED_LEFT).health() == 3, "12 damage dealt");

        TickResult destroyed = null;
        for (int tick = 2; tick <= 49 && destroyed == null; tick++) {
            TickResult result = hold(engine);
            if (!result.destroyedTowers().isEmpty())
                destroyed = result;
        }
        check(destroyed != null &&
                destroyed.destroyedTowers().contains(TowerId.RED_LEFT),
                "tower is destroyed on the second axe swing (tick 49)");
        check(match.tower(TowerId.RED_LEFT).isDestroyed(),
                "tower state is destroyed");
        check(blue.alive(), "vindicator survived the tower");
        hold(engine);
        check(blue.lockedTower() == TowerId.RED_KING,
                "troops retarget the King after the Princess falls");
        check(engine.kingAwake(TeamSide.RED), "King wakes up");
    }

    private static void bigSlimeSplitsTwiceThenDies() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        // A RED Princess tower shoots the slime and every child it splits
        // into; nobody moves, so only the splitting is under test.
        CombatUnit slime = spawn(engine, BattleCard.BIG_SLIME, TeamSide.BLUE,
                -6.0D, -3.0D);

        int deaths = 0;
        int children = 0;
        int maxGeneration = 0;
        List<CombatUnit> seen = new ArrayList<>();
        Set<Integer> generations = new HashSet<>();
        for (int tick = 0; tick < 3000 && deaths < 7; tick++) {
            TickResult result = hold(engine);
            for (TickResult.Death death : result.deaths()) {
                CombatUnit unit = null;
                for (CombatUnit candidate : seen) {
                    if (candidate.id().equals(death.unit()))
                        unit = candidate;
                }
                if (death.unit().equals(slime.id()))
                    unit = slime;
                if (unit == null)
                    continue;
                deaths++;
                for (ChildSpawn child : death.children()) {
                    CombatUnit registered =
                            engine.add(UUID.randomUUID(), child);
                    seen.add(registered);
                    children++;
                    maxGeneration = Math.max(maxGeneration,
                            child.generation());
                    generations.add(child.generation());
                    check(registered.invulnerable(),
                            "children start invulnerable");
                    check(child.lockedTower() == unit.lockedTower(),
                            "children keep the lane tower");
                    check(child.side() == TeamSide.BLUE, "children keep team");
                    double expectedHealth = unit.maxHealth() * 0.5D;
                    near(child.health(), expectedHealth,
                            "child gets half the health");
                    near(child.damage(), unit.damage() * 0.5D,
                            "child gets half the damage");
                }
            }
        }
        check(deaths == 7, "1 big + 2 medium + 4 small slimes die: " + deaths);
        check(children == 6, "6 children are created in total: " + children);
        check(maxGeneration == 2, "slimes split only twice");
        check(generations.equals(Set.of(1, 2)), "generations 1 and 2 exist");
        check(engine.units().stream().noneMatch(
                u -> u.side() == TeamSide.BLUE),
                "no blue slime is left");
    }
}
