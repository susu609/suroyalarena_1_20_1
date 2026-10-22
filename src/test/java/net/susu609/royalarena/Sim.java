package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.arena.ArenaWaypoint;
import net.susu609.royalarena.bot.BotBrain;
import net.susu609.royalarena.bot.BotPersonality;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.card.CardRole;
import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.combat.Formation;
import net.susu609.royalarena.combat.Separation;
import net.susu609.royalarena.combat.TickResult;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.PendingDeploy;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pure-Java battle simulation for balance measurements: troops walk like
 * {@code BattleField} walks them (one step toward the engine's waypoint,
 * nudged apart), slimes split, spells land. Used by {@link PowerCreepTest}
 * and {@link BalanceReport}.
 */
final class Sim {
    /** Tower health a fight is worth per elixir, in the push score. */
    static final double TOWER_HP_PER_ELIXIR = 25.0D;

    private Sim() {
    }

    static MatchInstance battle() {
        return battle(null, null);
    }

    static MatchInstance battle(BattleDeck blue, BattleDeck red) {
        ArenaLayout arena = ArenaLayout.standard(new ArenaPoint(0, 80, 0));
        MatchInstance match = blue == null ?
                MatchInstance.create(UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), arena) :
                MatchInstance.create(UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), arena, blue, red);
        match.startBattle();
        return match;
    }

    /** One combat tick, the walk, and the split children. */
    static TickResult step(CombatEngine engine, ArenaLayout arena) {
        TickResult result = engine.tick();
        Map<UUID, double[]> walk = new HashMap<>();
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
            walk.put(unit.id(), new double[] {dx / distance * stride,
                    dz / distance * stride});
        }
        List<CombatUnit> units = new ArrayList<>(engine.units());
        double[][] motion = new double[units.size()][];
        for (int i = 0; i < units.size(); i++) {
            double[] own = walk.getOrDefault(units.get(i).id(), new double[2]);
            double[] push = Separation.offset(units.get(i), units, arena);
            motion[i] = new double[] {own[0] + push[0], own[1] + push[1]};
        }
        for (int i = 0; i < units.size(); i++)
            units.get(i).setPosition(units.get(i).x() + motion[i][0],
                    units.get(i).z() + motion[i][1]);
        for (TickResult.Death death : result.deaths()) {
            for (var child : death.children())
                engine.add(UUID.randomUUID(), child);
        }
        // Units that appear without a death (an Illusioner's images).
        for (var spawn : result.spawns())
            engine.add(UUID.randomUUID(), spawn);
        return result;
    }

    /** A card's units in its formation around (x, z). */
    static void spawn(CombatEngine engine, MatchInstance match,
            BattleCard card, TeamSide side, double x, double z)
    {
        TroopStats stats = card.stats();
        double spacing = Math.max(0.8D, stats.radius() * 2.0D + 0.3D);
        int sign = ArenaLayout.sideSign(side);
        for (double[] o : Formation.offsets(stats.count(), spacing)) {
            ArenaWaypoint spot = match.arena().clampSpawn(x + o[0], z + o[1],
                    sign);
            CombatUnit unit = engine.add(UUID.randomUUID(), side, stats,
                    spot.x(), spot.z());
            // A jockey carries its rider from the start, as in the game.
            if (stats.split() != null && stats.split().into() != null)
                engine.addRider(UUID.randomUUID(), unit);
        }
    }

    /** Health of a whole card, split children included. */
    static double cardHealth(BattleCard card) {
        return CardRole.bodyHealth(card.stats()) * card.stats().count();
    }

    static double healthLeft(CombatEngine engine, TeamSide side) {
        double sum = 0.0D;
        for (CombatUnit unit : engine.units()) {
            if (unit.side() == side)
                sum += unit.health();
        }
        return sum;
    }

    static int towerHealth(MatchInstance match) {
        int sum = 0;
        for (TowerId id : TowerId.values())
            sum += match.tower(id).health();
        return sum;
    }

    /**
     * {@code attacker} crosses BLUE's left bridge toward RED's tower while
     * {@code defender} stands in front of that tower, with the towers
     * shooting. Returns the elixir BLUE comes out ahead: the share of the
     * defender destroyed times its cost, plus tower damage, minus the share
     * of its own card lost times its cost.
     */
    static double push(BattleCard attacker, BattleCard defender) {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        spawn(engine, match, attacker, TeamSide.BLUE, -6.0D, 2.0D);
        spawn(engine, match, defender, TeamSide.RED, -6.0D, -7.0D);
        int towersBefore = towerHealth(match);
        for (int tick = 0; tick < 1200; tick++) {
            step(engine, match.arena());
            if (match.state() != MatchState.BATTLE ||
                    healthLeft(engine, TeamSide.BLUE) <= 0.0D)
                break;
        }
        double keptA = healthLeft(engine, TeamSide.BLUE) /
                cardHealth(attacker);
        double keptD = healthLeft(engine, TeamSide.RED) / cardHealth(defender);
        return defender.cost() * (1.0D - keptD) +
                (towersBefore - towerHealth(match)) / TOWER_HP_PER_ELIXIR -
                attacker.cost() * (1.0D - keptA);
    }

    /**
     * Like {@link #push}, with {@code helper} (a support troop that does not
     * fight, the Allay) walking in just behind {@code attacker}; both cards'
     * losses count.
     */
    static double pushWith(BattleCard attacker, BattleCard helper,
            BattleCard defender)
    {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        spawn(engine, match, attacker, TeamSide.BLUE, -6.0D, 2.0D);
        int attackers = engine.units().size();
        spawn(engine, match, helper, TeamSide.BLUE, -6.0D, 3.0D);
        java.util.Set<UUID> helpers = new java.util.HashSet<>();
        for (int i = attackers; i < engine.units().size(); i++)
            helpers.add(engine.units().get(i).id());
        spawn(engine, match, defender, TeamSide.RED, -6.0D, -7.0D);
        int towersBefore = towerHealth(match);
        for (int tick = 0; tick < 1200; tick++) {
            step(engine, match.arena());
            if (match.state() != MatchState.BATTLE ||
                    healthLeft(engine, TeamSide.BLUE) <= 0.0D)
                break;
        }
        double keptA = 0.0D;
        double keptH = 0.0D;
        for (CombatUnit unit : engine.units()) {
            if (unit.side() != TeamSide.BLUE)
                continue;
            if (helpers.contains(unit.id()))
                keptH += unit.health();
            else
                keptA += unit.health();
        }
        keptA = Math.min(1.0D, keptA / cardHealth(attacker));
        keptH = Math.min(1.0D, keptH / cardHealth(helper));
        double keptD = healthLeft(engine, TeamSide.RED) / cardHealth(defender);
        return defender.cost() * (1.0D - keptD) +
                (towersBefore - towerHealth(match)) / TOWER_HP_PER_ELIXIR -
                attacker.cost() * (1.0D - keptA) - helper.cost() * (1.0D - keptH);
    }

    /** Tower health one card alone takes off the tower it walks to. */
    static int soloTowerDamage(BattleCard card) {
        return solo(card).towerDamage();
    }

    /**
     * One card played at its own bridge with nobody defending: seconds until
     * its first blow on the tower (-1 if it never lands one), the share of
     * its health left then, the tower health it takes in all, and the second
     * the tower falls (-1 if it stands).
     */
    record Solo(double reachSeconds, double healthOnArrival, int towerDamage,
                double fallSeconds)
    {
    }

    static Solo solo(BattleCard card) {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        spawn(engine, match, card, TeamSide.BLUE, -6.0D, 2.0D);
        TowerId tower = TowerId.RED_RIGHT;
        int before = match.tower(tower).health();
        int reach = -1;
        double onArrival = 0.0D;
        int tick = 0;
        for (; tick < 2400 && !engine.units().isEmpty() &&
                !match.tower(tower).isDestroyed(); tick++) {
            TickResult result = step(engine, match.arena());
            if (reach < 0 && result.attacks().stream().anyMatch(
                    a -> a.attackerUnit() != null)) {
                reach = tick;
                onArrival = healthLeft(engine, TeamSide.BLUE) /
                        cardHealth(card);
            }
        }
        boolean fell = match.tower(tower).isDestroyed();
        return new Solo(reach < 0 ? -1.0D : reach / 20.0D, onArrival,
                before - Math.max(0, match.tower(tower).health()),
                fell ? tick / 20.0D : -1.0D);
    }

    /** How a bot match went, with the match and engine to look into. */
    record Outcome(TeamSide winner, int ticks, boolean overtime,
                   boolean kingFell, MatchInstance match, CombatEngine engine,
                   BotPersonality blue, BotPersonality red)
    {
    }

    /** A full bot-versus-bot match ({@code winner} null on a draw). */
    static Outcome botMatch(BattleDeck blue, BattleDeck red, long seed,
            Map<BattleCard, Integer> plays)
    {
        // Bot matches play as the game does, tribe bonuses included.
        return botMatch(blue, red, seed, plays, true);
    }

    static Outcome botMatch(BattleDeck blue, BattleDeck red, long seed,
            Map<BattleCard, Integer> plays, boolean tribes)
    {
        MatchInstance match = battle(blue, red);
        CombatEngine engine = new CombatEngine(match);
        engine.setTribes(tribes);
        List<BotBrain> bots = List.of(new BotBrain(TeamSide.BLUE, seed),
                new BotBrain(TeamSide.RED, seed * 31L + 7L));
        int limit = MatchInstance.DEFAULT_BATTLE_TICKS +
                MatchInstance.OVERTIME_TICKS + 20;
        int tick = 0;
        for (; tick < limit && match.state() == MatchState.BATTLE; tick++) {
            match.tick();
            for (PendingDeploy deploy : match.drainReadyDeploys()) {
                if (deploy.card().isSpell())
                    engine.castSpell(deploy.side(), deploy.card(),
                            deploy.x() + 0.5D, deploy.z() + 0.5D);
                else
                    spawn(engine, match, deploy.card(), deploy.side(),
                            deploy.x() + 0.5D, deploy.z() + 0.5D);
            }
            for (BotBrain bot : bots) {
                BotBrain.Play play = bot.tick(match, engine);
                if (play == null)
                    continue;
                BattleCard card = match.player(bot.side()).deck()
                        .cardAt(play.slot());
                if (match.tryDeploy(bot.side(), play.slot(), play.x(),
                        play.z()).accepted())
                    plays.merge(card, 1, Integer::sum);
            }
            step(engine, match.arena());
        }
        boolean kingFell = match.tower(TowerId.BLUE_KING).isDestroyed() ||
                match.tower(TowerId.RED_KING).isDestroyed();
        return new Outcome(match.winner(), tick, match.overtime(), kingFell,
                match, engine, bots.get(0).personality(),
                bots.get(1).personality());
    }
}
