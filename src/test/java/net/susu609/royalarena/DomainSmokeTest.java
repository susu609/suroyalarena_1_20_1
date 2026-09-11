package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.client.camera.ArenaCameraProfile;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.TeamSide;

import java.util.List;
import java.util.UUID;

public final class DomainSmokeTest {
    private DomainSmokeTest() {
    }

    public static void main(String[] args) {
        teamMirrorsCorrectly();
        arenaDeployZonesStayOnOwnHalf();
        cameraMirrorsAroundArena();
        deckCyclesPlayedCard();
        matchLifecycleAndElixirAreDeterministic();
        deploymentIsServerValidatedAndCyclesDeck();
        towerTargetingMirrorsForBothSides();
        kingTowerEndsMatch();
        System.out.println("RoyalArena domain smoke tests: PASS");
    }

    private static void teamMirrorsCorrectly() {
        check(TeamSide.BLUE.opponent() == TeamSide.RED, "BLUE opponent");
        check(TeamSide.RED.opponent() == TeamSide.BLUE, "RED opponent");
    }
    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
    private static void arenaDeployZonesStayOnOwnHalf() {
        ArenaLayout arena = ArenaLayout.standard(new ArenaPoint(100, 80, 200));
        check(arena.canDeploy(TeamSide.BLUE, 100, 207), "BLUE own half");
        check(!arena.canDeploy(TeamSide.BLUE, 100, 193), "BLUE enemy half");
        check(arena.canDeploy(TeamSide.RED, 100, 193), "RED own half");
        check(!arena.canDeploy(TeamSide.RED, 100, 207), "RED enemy half");
        check(
                !arena.canDeploy(TeamSide.BLUE, 109, 207),
                "BLUE border wall excluded");
        check(
                !arena.canDeploy(TeamSide.RED, 100, 185),
                "RED end wall excluded");
    }
    private static void cameraMirrorsAroundArena() {
        ArenaLayout arena = ArenaLayout.standard(new ArenaPoint(0, 100, 0));
        var blue = ArenaCameraProfile.DEFAULT.transform(arena, TeamSide.BLUE);
        var red = ArenaCameraProfile.DEFAULT.transform(arena, TeamSide.RED);
        check(blue.x() == red.x(), "camera X mirror");
        check(Math.abs(blue.z() + red.z()) < 0.0001D, "camera Z mirror");
        check(blue.yaw() == 180.0F && red.yaw() == 0.0F, "camera yaw mirror");
    }
    private static void deckCyclesPlayedCard() {
        BattleDeck deck = new BattleDeck(List.of(
                BattleCard.ZOMBIE, BattleCard.SKELETON, BattleCard.SPIDER,
                BattleCard.SLIME, BattleCard.WOLF, BattleCard.BLAZE,
                BattleCard.VINDICATOR, BattleCard.IRON_GOLEM));
        BattleCard first = deck.hand().get(0);
        BattleCard next = deck.nextCard();
        deck.play(0);
        check(deck.hand().get(3) == next, "next card enters hand");
        check(deck.nextCard() != first, "played card goes behind cycle");
    }
    private static void matchLifecycleAndElixirAreDeterministic() {
        MatchInstance match = MatchInstance.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ArenaLayout.standard(new ArenaPoint(0, 80, 0)));
        match.startCountdown(2);
        match.tick();
        check(match.state() == MatchState.COUNTDOWN, "countdown tick 1");
        match.tick();
        check(
                match.state() == MatchState.BATTLE,
                "countdown transitions to battle");
        int before = match.blue().elixirTenths();
        for (int i = 0; i < 20; i++)
            match.tick();
        check(match.blue().elixirTenths() > before, "elixir regenerates");
    }
    private static void deploymentIsServerValidatedAndCyclesDeck() {
        MatchInstance match = MatchInstance.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ArenaLayout.standard(new ArenaPoint(0, 80, 0)));
        match.startBattle();
        int before = match.blue().elixirTenths();
        BattleCard selected = match.blue().deck().cardAt(0);
        BattleCard next = match.blue().deck().nextCard();
        var rejected = match.tryDeploy(TeamSide.BLUE, 0, 0, -8);
        check(!rejected.accepted(), "reject deployment on enemy half");
        check(
                match.blue().elixirTenths() == before,
                "rejected deployment keeps elixir");
        check(
                match.blue().deck().cardAt(0) == selected,
                "rejected deployment keeps deck order");
        var accepted = match.tryDeploy(TeamSide.BLUE, 0, 0, 8);
        check(
                accepted.accepted() && accepted.card() == selected,
                "accept deployment on own half");
        check(
                match.blue().elixirTenths() == before - selected.cost() * 10,
                "accepted deployment spends elixir");
        check(
                match.blue().deck().cardAt(3) == next,
                "accepted deployment cycles next card into hand");
    }
    private static void towerTargetingMirrorsForBothSides() {
        MatchInstance match = MatchInstance.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ArenaLayout.standard(new ArenaPoint(0, 80, 0)));
        match.startBattle();
        check(
                match.chooseTowerTarget(TeamSide.BLUE, -5) ==
                        net.susu609.royalarena.tower.TowerId.RED_RIGHT,
                "BLUE world-left lane maps to RED right tower");
        check(
                match.chooseTowerTarget(TeamSide.RED, -5) ==
                        net.susu609.royalarena.tower.TowerId.BLUE_LEFT,
                "RED world-left lane maps to BLUE left tower");
    }
    private static void kingTowerEndsMatch() {
        MatchInstance match = MatchInstance.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ArenaLayout.standard(new ArenaPoint(0, 80, 0)));
        match.startBattle();
        match.damageKingTower(TeamSide.RED, 10_000);
        check(
                match.state() == MatchState.FINISHED,
                "king tower finishes match");
        check(
                match.winner() == TeamSide.BLUE,
                "opponent wins after king tower destruction");
    }
}
