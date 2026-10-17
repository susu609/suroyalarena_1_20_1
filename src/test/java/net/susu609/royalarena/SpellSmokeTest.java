package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.bot.BotBrain;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.combat.TickResult;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.PendingDeploy;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.List;
import java.util.UUID;

/**
 * Pure-Java checks for the spell cards. The test units stand around the
 * river, out of reach of every tower, and the harness never moves them, so
 * only the spells change their health.
 */
public final class SpellSmokeTest {
    private SpellSmokeTest() {
    }

    public static void run() {
        spellsArePlayedAnywhereOnTheArena();
        arrowStormDealsSixteenOverFourSeconds();
        fireballHitsHardKnocksBackAndBurns();
        spellsDealThirtyPercentToTowers();
        botCastsASpellOnAGroup();
        System.out.println("RoyalArena spell smoke tests: PASS");
    }

    private static MatchInstance battle() {
        MatchInstance match = MatchInstance.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ArenaLayout.standard(new ArenaPoint(0, 80, 0)));
        match.startBattle();
        return match;
    }

    /** Plays slot 0 four times so the hand is Big Slime, Golem, Arrows, Fireball. */
    private static void cycleToSpells(MatchInstance match, TeamSide side) {
        for (int i = 0; i < 4; i++)
            match.player(side).deck().play(0);
        List<BattleCard> hand = match.player(side).deck().hand();
        check(hand.get(2) == BattleCard.ARROW_STORM &&
                hand.get(3) == BattleCard.FIREBALL, "spells reach the hand");
    }

    private static CombatUnit unit(CombatEngine engine, BattleCard card,
            TeamSide side, double x, double z)
    {
        return engine.add(UUID.randomUUID(), side, card.stats(), x, z);
    }

    private static void spellsArePlayedAnywhereOnTheArena() {
        MatchInstance match = battle();
        cycleToSpells(match, TeamSide.BLUE);
        check(!match.tryDeploy(TeamSide.BLUE, 0, 0, -8).accepted(),
                "a troop still cannot go on the enemy half");
        check(!match.tryDeploy(TeamSide.BLUE, 2, 0, -17).accepted(),
                "a spell must land on the arena");
        int elixir = match.blue().elixirTenths();
        check(match.tryDeploy(TeamSide.BLUE, 2, 0, -8).accepted(),
                "a spell can land on the enemy half");
        check(match.blue().elixirTenths() == elixir - 30,
                "Arrow Storm costs 3 elixir");
        PendingDeploy pending = match.pendingDeploys().get(0);
        check(pending.card() == BattleCard.ARROW_STORM &&
                pending.ticksLeft() == BattleCard.ARROW_STORM.deployTicks(),
                "the arrows fly for their travel time");
        for (int i = 0; i < BattleCard.ARROW_STORM.deployTicks() - 1; i++)
            match.tick();
        check(match.drainReadyDeploys().isEmpty() &&
                match.pendingDeploys().get(0).progress() > 0.85F,
                "still in flight one tick early");
        match.tick();
        check(match.drainReadyDeploys().size() == 1,
                "the spell lands after its travel time");
        check(match.arena().canPlay(TeamSide.RED, BattleCard.FIREBALL, 0, 5) &&
                match.arena().canPlay(TeamSide.RED, BattleCard.FIREBALL, 0, 0),
                "spells may land on the other half and on the river");
    }

    private static void arrowStormDealsSixteenOverFourSeconds() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                0.5D, 0.0D);
        CombatUnit spider = unit(engine, BattleCard.SPIDER, TeamSide.RED,
                2.0D, -0.5D);
        CombatUnit own = unit(engine, BattleCard.ZOMBIE, TeamSide.BLUE,
                -1.0D, 1.5D);
        CombatUnit outside = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                5.0D, 0.0D);
        engine.castSpell(TeamSide.BLUE, BattleCard.ARROW_STORM, 0.5D, 0.5D);
        int pulses = 0;
        int firstTick = -1;
        int lastTick = -1;
        for (int tick = 1; tick <= 120; tick++) {
            TickResult result = engine.tick();
            for (TickResult.SpellPulse pulse : result.spellPulses()) {
                pulses++;
                if (firstTick < 0)
                    firstTick = tick;
                lastTick = tick;
                check(pulse.card() == BattleCard.ARROW_STORM,
                        "the pulse names its card");
            }
        }
        check(pulses == 16, "16 pulses: " + pulses);
        check(firstTick == 1 && lastTick - firstTick == 75,
                "a pulse every 5 ticks for 4 s: " + firstTick + ".." +
                        lastTick);
        near(zombie.health(), 4.0D, "a Zombie inside takes 16 and survives");
        check(!spider.alive() && engine.unit(spider.id()) == null,
                "a Spider inside dies");
        near(own.health(), 20.0D, "own troops are not hit");
        near(outside.health(), 20.0D, "units outside the circle are not hit");
        check(engine.activeSpells() == 0, "the spell ends after its pulses");
    }

    private static void fireballHitsHardKnocksBackAndBurns() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit golem = unit(engine, BattleCard.IRON_GOLEM, TeamSide.RED,
                0.5D, 0.0D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                -1.0D, -1.5D);
        engine.castSpell(TeamSide.BLUE, BattleCard.FIREBALL, 0.0D, 0.0D);
        TickResult first = engine.tick();
        check(first.spellPulses().size() == 1 &&
                first.spellPulses().get(0).hits() == 2,
                "one explosion hitting both units");
        check(first.deaths().stream().anyMatch(
                d -> d.unit().equals(zombie.id())),
                "Fireball kills a Zombie outright");
        near(golem.health(), 135.0D, "25 damage on the Iron Golem");
        check(first.pushes().size() == 1 &&
                first.pushes().get(0).unit().equals(golem.id()),
                "the survivor is knocked back");
        near(golem.x(), 1.5D, "knocked 1 block straight away from the centre");
        check(golem.burnTicks() > 0, "the Iron Golem is burning");
        for (int i = 0; i < 80; i++)
            engine.tick();
        near(golem.health(), 132.0D, "the burn deals 1 per second for 3 s");
        check(golem.burnTicks() == 0, "the burn ends");
    }

    private static void spellsDealThirtyPercentToTowers() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        ArenaPoint princess = match.arena().towerPoint(TowerId.RED_LEFT);
        engine.castSpell(TeamSide.BLUE, BattleCard.FIREBALL, princess.x(),
                princess.z() + 3.5D);
        engine.tick();
        check(match.tower(TowerId.RED_LEFT).health() ==
                        (int) Math.ceil(110 - 25 * 0.3D),
                "Fireball deals 30% to a tower it touches: " +
                        match.tower(TowerId.RED_LEFT).health());

        ArenaPoint king = match.arena().towerPoint(TowerId.RED_KING);
        check(!engine.kingAwake(TeamSide.RED), "the King sleeps at first");
        engine.castSpell(TeamSide.BLUE, BattleCard.ARROW_STORM, king.x(),
                king.z());
        for (int i = 0; i < 100; i++)
            engine.tick();
        check(match.tower(TowerId.RED_KING).health() ==
                        (int) Math.ceil(185 - 16 * 0.3D),
                "Arrow Storm deals 30% of 16 to the King");
        check(engine.kingAwake(TeamSide.RED), "a spell on the King wakes it");

        ArenaPoint own = match.arena().towerPoint(TowerId.BLUE_LEFT);
        engine.castSpell(TeamSide.BLUE, BattleCard.FIREBALL, own.x(), own.z());
        engine.tick();
        check(match.tower(TowerId.BLUE_LEFT).health() == 110,
                "a spell never hits its own towers");
    }

    private static void botCastsASpellOnAGroup() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        cycleToSpells(match, TeamSide.RED);
        for (int i = 0; i < 3; i++)
            unit(engine, BattleCard.SKELETONS, TeamSide.BLUE, 0.5D + i * 0.8D,
                    -6.0D);
        BotBrain bot = new BotBrain(TeamSide.RED, 5L);
        BotBrain.Play play = null;
        for (int i = 0; i < BotBrain.THINK_EVERY_TICKS && play == null; i++)
            play = bot.tick(match, engine);
        check(play != null, "the bot answers a group of three");
        BattleCard card = match.red().deck().cardAt(play.slot());
        check(card == BattleCard.ARROW_STORM,
                "Arrow Storm is worth it on three Skeletons: " + card);
        check(Math.abs(play.x() - 1) <= 2 && Math.abs(play.z() + 6) <= 2,
                "aimed at the group: " + play);
        check(match.tryDeploy(TeamSide.RED, play.slot(), play.x(), play.z())
                .accepted(), "the server accepts the bot's spell");

        MatchInstance lone = battle();
        CombatEngine loneEngine = new CombatEngine(lone);
        cycleToSpells(lone, TeamSide.RED);
        unit(loneEngine, BattleCard.IRON_GOLEM, TeamSide.BLUE, 0.5D, 6.0D);
        BotBrain calm = new BotBrain(TeamSide.RED, 5L);
        for (int i = 0; i < 200; i++) {
            BotBrain.Play p = calm.tick(lone, loneEngine);
            if (p != null)
                check(!lone.red().deck().cardAt(p.slot()).isSpell(),
                        "no spell wasted on a single unit");
        }
    }

    private static void near(double actual, double expected, String name) {
        check(Math.abs(actual - expected) < 1.0E-6D,
                name + ": " + actual + " vs " + expected);
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
