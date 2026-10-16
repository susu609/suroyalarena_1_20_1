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
        hudKeepsReferenceProportions();
        troopSpawnsAfterOneSecondDeployTime();
        cameraMirrorsAroundArena();
        deckCyclesPlayedCard();
        matchLifecycleAndElixirAreDeterministic();
        deploymentIsServerValidatedAndCyclesDeck();
        towerTargetingMirrorsForBothSides();
        towerVisualTypesMatchUploadedModels();
        kingTowerEndsMatch();
        CombatSmokeTest.run();
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
        for (TowerId id : TowerId.values()) {
            ArenaPoint p = arena.towerPoint(id);
            check(arena.towerAt(p.x() + 0.2D, p.z() - 0.2D) == id,
                    "tower lookup by position");
        }
        check(arena.towerAt(0.0D, 0.0D) == null, "no tower at the centre");
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

    private static void hudKeepsReferenceProportions() {
        var hud = net.susu609.royalarena.client.hud.HudLayout.of(1920, 1080);
        var card = hud.card(0);
        check(Math.abs(hud.u() - 1080.0F / 356.0F) < 1.0E-6F,
                "u is screen height / 356");
        check(card.w() == 94 && card.h() == 118,
                "card is 31x39 units (94x118 px at 1080p)");
        check(Math.abs(card.w() / (double) card.h() - 0.8D) < 0.01D,
                "card keeps the 4:5 portrait ratio");
        var last = hud.card(3);
        check(card.right() < hud.card(1).x() && last.right() <= 1920 &&
                card.x() >= 0, "cards do not overlap and stay on screen");
        int columnLeft = Math.round((1920 - 200 * hud.u()) / 2.0F);
        check(Math.abs(hud.panel().right() - (1920 - columnLeft)) <= 1,
                "reference column is centred");
        check(hud.panel().bottom() == 1080, "panel reaches the bottom");
        check(hud.nextCard().w() < card.w() * 0.6D,
                "next card is about 58% of a hand card");
        check(hud.elixirBar().y() > card.bottom() - 1,
                "elixir bar sits under the cards");
        for (int slot = 0; slot < 4; slot++) {
            var r = hud.card(slot);
            check(hud.cardSlotAt(r.centerX(), r.centerY()) == slot,
                    "card hit test");
        }
        check(hud.cardSlotAt(5, 5) == -1, "no card outside the hand");
        var raised = hud.card(2,
                net.susu609.royalarena.client.hud.HudLayout.SELECT_LIFT);
        check(raised.y() < hud.card(2).y() && raised.h() == hud.card(2).h(),
                "selected card rises without changing size");
        check(hud.cardSlotAt(raised.centerX(), raised.y() + 1) == 2,
                "raised card is still clickable at its top edge");
        var small = net.susu609.royalarena.client.hud.HudLayout.of(480, 270);
        check(small.card(0).h() == 30, "layout scales with GUI height");
    }

    private static void troopSpawnsAfterOneSecondDeployTime() {
        MatchInstance match = MatchInstance.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ArenaLayout.standard(new ArenaPoint(0, 80, 0)));
        match.startBattle();
        BattleCard card = match.blue().deck().cardAt(0);
        int elixir = match.blue().elixirTenths();
        check(match.tryDeploy(TeamSide.BLUE, 0, -3, 6).accepted(),
                "deploy accepted");
        check(match.blue().elixirTenths() == elixir - card.cost() * 10,
                "elixir is spent on click");
        check(match.pendingDeploys().size() == 1 &&
                match.drainReadyDeploys().isEmpty(),
                "troop waits instead of spawning at once");
        var snapshot = net.susu609.royalarena.match.MatchSnapshot.from(
                match, TeamSide.RED);
        check(snapshot.pending().size() == 1,
                "both players see the pending deploy");
        for (int i = 0; i < MatchInstance.DEPLOY_TICKS - 1; i++)
            match.tick();
        check(match.drainReadyDeploys().isEmpty() &&
                match.pendingDeploys().get(0).progress() > 0.9F,
                "still pending one tick before the deploy time");
        match.tick();
        var ready = match.drainReadyDeploys();
        check(ready.size() == 1 && ready.get(0).card() == card &&
                ready.get(0).x() == -3 && ready.get(0).z() == 6 &&
                match.pendingDeploys().isEmpty(),
                "troop is ready after exactly one second");
        check(match.drainReadyDeploys().isEmpty(), "ready list drains once");
        int cheapest = 0;
        for (int slot = 1; slot < 4; slot++) {
            if (match.blue().deck().cardAt(slot).cost() <
                    match.blue().deck().cardAt(cheapest).cost())
                cheapest = slot;
        }
        check(match.tryDeploy(TeamSide.BLUE, cheapest, -3, 6).accepted() &&
                match.pendingDeploys().size() == 1, "second deploy pending");
        match.finish(TeamSide.RED);
        check(match.pendingDeploys().isEmpty(),
                "finishing the match drops pending deploys");
    }


    private static void cameraMirrorsAroundArena() {
        ArenaLayout arena = ArenaLayout.standard(new ArenaPoint(0, 100, 0));
        var blue = ArenaCameraProfile.DEFAULT.transform(arena, TeamSide.BLUE);
        var red = ArenaCameraProfile.DEFAULT.transform(arena, TeamSide.RED);
        check(Math.abs(blue.x() + red.x()) < 0.0001D &&
                Math.abs(blue.z() + red.z()) < 0.0001D,
                "RED camera is BLUE's rotated 180 degrees");
        check(blue.yaw() == 190.0F && red.yaw() == 10.0F,
                "camera swings 10 degrees for each side");
        check(blue.x() < 0.0D && blue.z() > 20.0D,
                "BLUE camera sits behind BLUE, slightly to one side");
        double yaw = Math.toRadians(blue.yaw());
        double lookX = -Math.sin(yaw);
        double lookZ = Math.cos(yaw);
        double len = Math.hypot(blue.x(), blue.z());
        check(Math.abs(lookX + blue.x() / len) < 1.0E-6D &&
                Math.abs(lookZ + blue.z() / len) < 1.0E-6D,
                "camera yaw points at the arena centre");
    }

    private static void deckCyclesPlayedCard() {
        BattleDeck deck = new BattleDeck(List.of(BattleCard.values()));
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
