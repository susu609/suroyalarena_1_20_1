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
        lightningStrikesTheThreeStrongest();
        healingPotionHealsOwnTroops();
        freezeStopsTroopsAndTowers();
        tntHitsHardAndPushesFar();
        tntReachesOnlyTheMiddleOfTheBridge();
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

    private static void lightningStrikesTheThreeStrongest() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit golem = unit(engine, BattleCard.IRON_GOLEM, TeamSide.RED,
                0.5D, 0.0D);
        CombatUnit ravager = unit(engine, BattleCard.RAVAGER, TeamSide.RED,
                2.5D, 1.0D);
        CombatUnit hoglin = unit(engine, BattleCard.HOGLIN, TeamSide.RED,
                -1.5D, -1.0D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                -1.0D, 1.5D);
        CombatUnit spider = unit(engine, BattleCard.SPIDER, TeamSide.RED,
                1.5D, -1.5D);
        engine.castSpell(TeamSide.BLUE, BattleCard.LIGHTNING, 0.5D, 0.0D);
        TickResult first = engine.tick();
        check(first.spellPulses().size() == 1 &&
                first.spellPulses().get(0).hits() == 3 &&
                first.spellPulses().get(0).strikes().size() == 3,
                "Lightning strikes three targets");
        near(golem.health(), 120.0D, "the Golem is struck");
        near(ravager.health(), 60.0D, "the Ravager is struck");
        near(hoglin.health(), 10.0D, "the Hoglin is struck (50 health - 40)");
        near(zombie.health(), 20.0D, "the Zombie is spared");
        near(spider.health(), 10.0D, "the Spider is spared");
        check(golem.frozen() && !zombie.frozen(), "struck units are stunned");
        for (int i = 0; i < 9; i++) {
            TickResult r = engine.tick();
            check(r.moves().stream().noneMatch(
                    m -> m.unit().equals(golem.id())) &&
                    r.attacks().stream().noneMatch(
                            a -> golem.id().equals(a.attackerUnit())),
                    "a stunned unit neither walks nor attacks");
        }
        engine.tick();
        check(!golem.frozen(), "the stun lasts half a second");

        MatchInstance siege = battle();
        CombatEngine siegeEngine = new CombatEngine(siege);
        ArenaPoint princess = siege.arena().towerPoint(TowerId.RED_LEFT);
        siegeEngine.castSpell(TeamSide.BLUE, BattleCard.LIGHTNING,
                princess.x(), princess.z() + 3.5D);
        siegeEngine.tick();
        check(siege.tower(TowerId.RED_LEFT).health() == 110 - 12,
                "Lightning deals 30% of 40 to a tower: " +
                        siege.tower(TowerId.RED_LEFT).health());
        check(siegeEngine.towerFrozenTicks(TowerId.RED_LEFT) > 0,
                "and stuns it");
    }

    private static void healingPotionHealsOwnTroops() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit low = unit(engine, BattleCard.ZOMBIE, TeamSide.BLUE,
                -4.0D, 1.0D);
        CombatUnit scratched = unit(engine, BattleCard.ZOMBIE, TeamSide.BLUE,
                4.0D, 1.0D);
        CombatUnit enemy = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                0.5D, -1.0D);
        // The enemy hurts them first: Arrow Storm on one, Freeze on the other.
        engine.castSpell(TeamSide.RED, BattleCard.ARROW_STORM, -4.0D, 1.0D);
        engine.castSpell(TeamSide.RED, BattleCard.FREEZE, 4.0D, 1.0D);
        for (int i = 0; i < 100; i++)
            engine.tick();
        near(low.health(), 4.0D, "Arrow Storm left 4");
        near(scratched.health(), 12.0D, "Freeze left 12");
        low.setPosition(-1.0D, 1.0D);
        scratched.setPosition(1.0D, 1.0D);
        engine.castSpell(TeamSide.BLUE, BattleCard.HEALING_POTION, 0.0D,
                1.0D);
        int pulses = 0;
        for (int i = 0; i < 100; i++) {
            TickResult r = engine.tick();
            for (TickResult.SpellPulse p : r.spellPulses()) {
                pulses++;
                check(p.hits() == 2, "both own Zombies are healed");
            }
        }
        check(pulses == 8, "8 healing pulses: " + pulses);
        near(low.health(), 20.0D, "4 + 8 x 2 = 20");
        near(scratched.health(), 20.0D, "healing stops at full health");
        near(enemy.health(), 20.0D, "enemies are not touched");
    }

    private static void freezeStopsTroopsAndTowers() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        ArenaPoint princess = match.arena().towerPoint(TowerId.RED_LEFT);
        // In front of the tower, in its range, so the tower would shoot.
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.BLUE,
                princess.x(), princess.z() + 4.0D);
        engine.castSpell(TeamSide.BLUE, BattleCard.FREEZE, princess.x(),
                princess.z() + 3.0D);
        // The tower may fire on this very tick, before the freeze lands.
        engine.tick();
        double health = zombie.health();
        check(match.tower(TowerId.RED_LEFT).health() < 110,
                "Freeze damages the tower a little");
        check(engine.towerFrozenTicks(TowerId.RED_LEFT) == 80,
                "and freezes it for 4 s");
        for (int i = 0; i < 79; i++) {
            TickResult r = engine.tick();
            check(r.attacks().stream().noneMatch(
                    a -> TowerId.RED_LEFT.equals(a.attackerTower())),
                    "a frozen tower does not shoot (tick " + i + ")");
        }
        boolean shot = false;
        for (int i = 0; i < 40 && !shot; i++)
            shot = engine.tick().attacks().stream().anyMatch(
                    a -> TowerId.RED_LEFT.equals(a.attackerTower()));
        check(shot, "the tower shoots again after the freeze");
        check(zombie.health() <= health, "the own Zombie is not frozen");

        MatchInstance other = battle();
        CombatEngine otherEngine = new CombatEngine(other);
        CombatUnit enemy = unit(otherEngine, BattleCard.ZOMBIE, TeamSide.RED,
                0.5D, 0.0D);
        otherEngine.castSpell(TeamSide.BLUE, BattleCard.FREEZE, 0.5D, 0.0D);
        otherEngine.tick();
        near(enemy.health(), 12.0D, "Freeze deals 8 damage");
        check(enemy.frozenTicks() == 80, "and freezes the troop");
        for (int i = 0; i < 79; i++)
            check(otherEngine.tick().moves().isEmpty(),
                    "a frozen troop stands still");
        check(!otherEngine.tick().moves().isEmpty(),
                "it walks again after 4 s");
    }

    private static void tntHitsHardAndPushesFar() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit golem = unit(engine, BattleCard.IRON_GOLEM, TeamSide.RED,
                1.5D, 0.0D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                -1.0D, 0.5D);
        engine.castSpell(TeamSide.BLUE, BattleCard.TNT, 0.0D, 0.0D);
        TickResult first = engine.tick();
        check(first.deaths().stream().anyMatch(
                d -> d.unit().equals(zombie.id())), "TNT kills a Zombie");
        near(golem.health(), 130.0D, "30 damage on the Golem");
        near(golem.x(), 3.5D, "and a 2 block push");

        MatchInstance siege = battle();
        CombatEngine siegeEngine = new CombatEngine(siege);
        ArenaPoint princess = siege.arena().towerPoint(TowerId.RED_LEFT);
        siegeEngine.castSpell(TeamSide.BLUE, BattleCard.TNT, princess.x(),
                princess.z() + 3.5D);
        siegeEngine.tick();
        check(siege.tower(TowerId.RED_LEFT).health() == 110 - 15,
                "TNT deals half its damage to towers");
    }

    private static void tntReachesOnlyTheMiddleOfTheBridge() {
        ArenaLayout arena = battle().arena();
        check(arena.canPlay(TeamSide.BLUE, BattleCard.TNT, 0, 8) &&
                arena.canPlay(TeamSide.BLUE, BattleCard.TNT, 0, 1) &&
                arena.canPlay(TeamSide.BLUE, BattleCard.TNT, 0, 0),
                "BLUE TNT lands on its half and up to the middle of the bridge");
        check(!arena.canPlay(TeamSide.BLUE, BattleCard.TNT, 0, -1) &&
                !arena.canPlay(TeamSide.BLUE, BattleCard.TNT, 0, -8) &&
                !arena.canPlay(TeamSide.BLUE, BattleCard.TNT, 0, -12),
                "BLUE TNT cannot land past the middle of the bridge");
        check(arena.canPlay(TeamSide.RED, BattleCard.TNT, 0, -8) &&
                arena.canPlay(TeamSide.RED, BattleCard.TNT, 0, -1) &&
                !arena.canPlay(TeamSide.RED, BattleCard.TNT, 0, 0) &&
                !arena.canPlay(TeamSide.RED, BattleCard.TNT, 0, 8),
                "RED TNT is the mirror image");
        check(arena.canPlay(TeamSide.BLUE, BattleCard.FIREBALL, 0, -12),
                "other spells still reach the whole arena");
        check(arena.nearestReach(TeamSide.BLUE, BattleCard.TNT, -9) == 0 &&
                arena.nearestReach(TeamSide.RED, BattleCard.TNT, 9) == -1 &&
                arena.nearestReach(TeamSide.BLUE, BattleCard.FIREBALL, -9) == -9,
                "a bot aims at the nearest row TNT reaches");
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
