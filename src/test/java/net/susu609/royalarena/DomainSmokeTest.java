package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.client.camera.ArenaCameraProfile;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.List;
import java.util.UUID;

public final class DomainSmokeTest {
    private DomainSmokeTest() {
    }

    public static void main(String[] args) {
        teamMirrorsCorrectly();
        arenaDeployZonesStayOnOwnHalf();
        arenaIsEvenAndSymmetric();
        troopsCrossTheRiverOnBridges();
        cameraMirrorsAroundArena();
        deckCyclesPlayedCard();
        matchLifecycleAndElixirAreDeterministic();
        deploymentIsServerValidatedAndCyclesDeck();
        towerTargetingMirrorsForBothSides();
        troopKeepsTargetUntilTowerIsDestroyed();
        towerVisualTypesMatchUploadedModels();
        kingTowerEndsMatch();
        System.out.println("RoyalArena domain smoke tests: PASS");
    }

    private static void teamMirrorsCorrectly() {
        check(TeamSide.BLUE.opponent() == TeamSide.RED, "BLUE opponent");
        check(TeamSide.RED.opponent() == TeamSide.BLUE, "RED opponent");
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
                !arena.canDeploy(TeamSide.RED, 100, 183),
                "RED end wall excluded");
        check(
                !arena.canDeploy(TeamSide.BLUE, 100, 200) &&
                        !arena.canDeploy(TeamSide.RED, 100, 199),
                "river is not deployable");
    }

    private static void arenaIsEvenAndSymmetric() {
        ArenaPoint c = new ArenaPoint(0, 80, 0);
        ArenaLayout arena = ArenaLayout.standard(c);
        check(arena.maxX() - arena.minX() + 1 == 18, "arena is 18 wide");
        check(arena.maxZ() - arena.minZ() + 1 == 32, "arena is 32 long");
        check(arena.isRiver(-1) && arena.isRiver(0) && !arena.isRiver(1) &&
                !arena.isRiver(-2), "river is two rows on the Z axis");
        for (int x = arena.minX(); x <= arena.maxX(); x++) {
            check(arena.isBridge(x) == arena.isBridge(-1 - x),
                    "bridges mirror across X");
        }
        check(arena.isBridge(-7) && arena.isBridge(-6) && !arena.isBridge(-5) &&
                arena.isBridge(5) && arena.isBridge(6) && !arena.isBridge(7),
                "bridges are two blocks wide");
        for (TeamSide side : TeamSide.values()) {
            TeamSide other = side.opponent();
            ArenaPoint king = arena.towerPoint(TowerId.king(side));
            ArenaPoint otherKing = arena.towerPoint(TowerId.king(other));
            check(king.x() == 0 && king.z() == -otherKing.z(),
                    "king towers mirror on the axis");
            ArenaPoint left = arena.towerPoint(TowerId.left(side));
            ArenaPoint otherLeft = arena.towerPoint(TowerId.left(other));
            check(left.x() == -otherLeft.x() && left.z() == -otherLeft.z(),
                    "each side's left tower is point-symmetric");
            ArenaPoint right = arena.towerPoint(TowerId.right(side));
            check(right.x() == -left.x() && right.z() == left.z(),
                    "left and right towers mirror across X");
        }
        ArenaPoint blueLeft = arena.towerPoint(TowerId.BLUE_LEFT);
        check(blueLeft.x() == arena.bridgeLeft().x(),
                "princess tower lines up with its bridge");
        int princessFront = blueLeft.z() - ArenaLayout.PRINCESS_SIZE / 2;
        check(princessFront - ArenaLayout.RIVER_HALF == 8,
                "eight empty rows between river and princess tower");
        ArenaPoint blueKing = arena.towerPoint(TowerId.BLUE_KING);
        check(arena.maxZ() - (blueKing.z() + ArenaLayout.KING_SIZE / 2 - 1)
                == 1, "one free row behind the king tower");
    }

    private static void troopsCrossTheRiverOnBridges() {
        ArenaLayout arena = ArenaLayout.standard(new ArenaPoint(0, 80, 0));
        var first = arena.riverWaypoint(TeamSide.BLUE, -2.0D, 10.0D);
        check(first != null && first.x() == -6.0D && first.z() == 1.5D,
                "unaligned BLUE troop walks to the near bridge end");
        var second = arena.riverWaypoint(TeamSide.BLUE, -6.0D, 1.5D);
        check(second != null && second.x() == -6.0D && second.z() == -2.0D,
                "aligned BLUE troop walks over the bridge");
        check(arena.riverWaypoint(TeamSide.BLUE, -6.0D, -2.0D) == null,
                "BLUE troop past the river heads for its tower");
        var red = arena.riverWaypoint(TeamSide.RED, 4.0D, -10.0D);
        check(red != null && red.x() == 6.0D && red.z() == -1.5D,
                "RED troop mirrors the BLUE route");
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
    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
    private static void troopKeepsTargetUntilTowerIsDestroyed() {
        MatchInstance match = MatchInstance.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ArenaLayout.standard(new ArenaPoint(0, 80, 0)));
        match.startBattle();
        var troop = new net.susu609.royalarena.server.battle.ServerTroop(
                UUID.randomUUID(), TeamSide.BLUE, 100, 0.1D);
        var first = troop.targetTower(match, -5.0D);
        check(
                first == net.susu609.royalarena.tower.TowerId.RED_RIGHT,
                "troop acquires initial lane tower");
        check(
                troop.targetTower(match, 5.0D) == first,
                "troop keeps target after crossing lane");
        match.damageTower(first, 10_000);
        check(
                troop.targetTower(match, 5.0D) ==
                        net.susu609.royalarena.tower.TowerId.RED_KING,
                "troop retargets king after lane tower destruction");
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
    private static void towerVisualTypesMatchUploadedModels() {
        var crown = net.susu609.royalarena.entity.building.TowerVisualType.from(
                net.susu609.royalarena.tower.TowerKind.PRINCESS);
        var king = net.susu609.royalarena.entity.building.TowerVisualType.from(
                net.susu609.royalarena.tower.TowerKind.KING);
        check(
                crown ==
                        net.susu609.royalarena.entity.building.TowerVisualType.CROWN,
                "Princess tower uses Crown Tower visual");
        check(
                crown.width() == 4.0F && crown.height() == 4.0F,
                "Crown Tower hitbox is the even 4x4 footprint");
        check(
                crown.blockGridCenterOffset() == 0.0D,
                "even-width Crown Tower centers between blocks");
        check(
                Math.abs(crown.modelScaleXZ() * 3.0F - 4.0F) < 1.0E-6F,
                "3-wide Crown model is stretched to 4 blocks");
        check(
                king ==
                        net.susu609.royalarena.entity.building.TowerVisualType.KING,
                "King tower uses King Tower visual");
        check(
                king.width() == 4.0F && king.height() == 4.0F,
                "King Tower hitbox matches 4x4-block model bounds");
        check(
                king.blockGridCenterOffset() == 0.0D,
                "even-width King Tower centers between blocks");
    }
}
