package net.susu609.royalarena;

import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.Tribe;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.MatchPlayer;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.SandboxSettings;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.UUID;

/**
 * The sandbox (docs/SANDBOX.md): any card for either side, no clock,
 * unbreakable towers, endless elixir, tribe steps by hand.
 */
public final class SandboxSmokeTest {
    private SandboxSmokeTest() {
    }

    public static void run() {
        anyCardForEitherSide();
        towersCanBeUnbreakable();
        elixirIsEndlessOrSpent();
        noClockAndNoEnd();
        tribesAreChosenByHand();
        speedsAreFixedSteps();
        System.out.println("RoyalArena sandbox smoke tests: PASS");
    }

    private static MatchInstance sandbox() {
        MatchInstance match = Sim.battle();
        match.enableSandbox();
        return match;
    }

    private static void anyCardForEitherSide() {
        MatchInstance plain = Sim.battle();
        check(!plain.sandboxDeploy(TeamSide.BLUE, BattleCard.ZOMBIE, 0, 6)
                .accepted(), "a real match refuses sandbox deploys");

        MatchInstance match = sandbox();
        // Warden-sized cards outside the hand: the Iron Golem for BLUE, the
        // Ravager for RED, each on its own half.
        check(match.sandboxDeploy(TeamSide.BLUE, BattleCard.IRON_GOLEM, 0, 6)
                .accepted(), "BLUE plays a card it does not hold");
        check(match.sandboxDeploy(TeamSide.RED, BattleCard.RAVAGER, 0, -6)
                .accepted(), "and so does RED");
        check(!match.sandboxDeploy(TeamSide.RED, BattleCard.RAVAGER, 0, 6)
                .accepted(), "troops still go on their own half");
        check(match.sandboxDeploy(TeamSide.RED, BattleCard.FIREBALL, 0, 6)
                .accepted(), "spells anywhere");
        check(match.pendingDeploys().size() == 3, "three cards on the way");
        check(match.player(TeamSide.BLUE).deck().hand()
                        .equals(Sim.battle().player(TeamSide.BLUE).deck().hand()),
                "the hand does not cycle");
        match.clearPending();
        check(match.pendingDeploys().isEmpty(), "clear drops them");
    }

    private static void towersCanBeUnbreakable() {
        MatchInstance match = sandbox();
        SandboxSettings settings = match.sandbox();
        check(settings.towersInvulnerable(), "towers start unbreakable");
        CombatEngine engine = new CombatEngine(match);
        check(!engine.kingAwake(TeamSide.RED), "the King sleeps");
        match.damageTower(TowerId.RED_KING, 50.0D);
        check(match.tower(TowerId.RED_KING).health() ==
                        TowerId.RED_KING.kind().maxHealth(),
                "an unbreakable tower takes nothing");
        check(engine.kingAwake(TeamSide.RED), "but a hit King wakes up");

        settings.setTowersInvulnerable(false);
        match.damageTower(TowerId.RED_KING, 1.0E6D);
        check(match.tower(TowerId.RED_KING).isDestroyed(),
                "breakable towers fall");
        check(match.state() == MatchState.BATTLE,
                "a fallen King does not end a sandbox");
        match.resetTowers();
        check(match.tower(TowerId.RED_KING).health() ==
                        TowerId.RED_KING.kind().maxHealth() &&
                        !engine.kingAwake(TeamSide.RED),
                "restore brings it back, asleep");
    }

    private static void elixirIsEndlessOrSpent() {
        MatchInstance match = sandbox();
        MatchPlayer blue = match.player(TeamSide.BLUE);
        for (int i = 0; i < 5; i++)
            check(match.sandboxDeploy(TeamSide.BLUE, BattleCard.LIGHTNING, 0,
                    -6).accepted(), "endless elixir: Lightning again and again");
        match.tick();
        check(blue.elixirTenths() == 100, "and the meter stays full");

        match.sandbox().setInfiniteElixir(false);
        check(match.sandboxDeploy(TeamSide.BLUE, BattleCard.LIGHTNING, 0, -6)
                .accepted(), "one Lightning from 10 elixir");
        check(blue.elixirTenths() == 40, "costs 6");
        check(!match.sandboxDeploy(TeamSide.BLUE, BattleCard.LIGHTNING, 0, -6)
                .accepted(), "not enough for a second");
    }

    private static void noClockAndNoEnd() {
        MatchInstance match = sandbox();
        int start = match.ticksRemaining();
        for (int i = 0; i < MatchInstance.DEFAULT_BATTLE_TICKS +
                MatchInstance.OVERTIME_TICKS + 10; i++)
            match.tick();
        check(match.state() == MatchState.BATTLE &&
                match.ticksRemaining() == start && !match.overtime(),
                "the clock never runs");
        check(match.elixirRate() == 1, "and elixir never speeds up");
    }

    private static void tribesAreChosenByHand() {
        MatchInstance match = sandbox();
        CombatEngine engine = new CombatEngine(match);
        engine.setTribes(true);
        match.sandbox().setTribeLevel(TeamSide.RED, Tribe.GOLEM, 9);
        check(match.sandbox().tribeLevels(TeamSide.RED).get(Tribe.GOLEM) == 1,
                "a step past the last is clamped");
        CombatUnit red = engine.add(UUID.randomUUID(), TeamSide.RED,
                BattleCard.IRON_GOLEM.stats(), 0.0D, -6.0D);
        CombatUnit blue = engine.add(UUID.randomUUID(), TeamSide.BLUE,
                BattleCard.IRON_GOLEM.stats(), 0.0D, 6.0D);
        check(red.tribe().armor() == 2.0D, "RED's Golem has its +2 armour");
        check(blue.tribe().armor() == 0.0D, "BLUE chose nothing");
        match.sandbox().setTribeLevel(TeamSide.RED, Tribe.GOLEM, 0);
        check(match.sandbox().tribeLevels(TeamSide.RED).isEmpty(),
                "level 0 removes the tribe");
    }

    private static void speedsAreFixedSteps() {
        SandboxSettings settings = sandbox().sandbox();
        check(settings.speed() == SandboxSettings.NORMAL_SPEED, "starts at 1x");
        settings.setSpeed(0);
        check(settings.speed() == 0, "paused");
        settings.setSpeed(3);
        check(settings.speed() == 0, "3 half ticks is not a speed");
        settings.setSpeed(4);
        check(settings.speed() == 4, "2x");
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
