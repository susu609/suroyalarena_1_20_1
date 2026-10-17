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
        arenaFitsAboveTheHudPanel();
        deckCyclesPlayedCard();
        matchLifecycleAndElixirAreDeterministic();
        deploymentIsServerValidatedAndCyclesDeck();
        towerTargetingMirrorsForBothSides();
        towerVisualTypesMatchUploadedModels();
        kingTowerEndsMatch();
        noRawResourceLocationConstructors();
        CombatSmokeTest.run();
        BotSmokeTest.run();
        SpellSmokeTest.run();
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
        for (float lift : new float[] {0.0F, 3.0F, 8.0F}) {
            var c = hud.card(1, lift);
            var cost = hud.cardCost(1, lift);
            check(cost.x() < c.x() && cost.y() < c.y() &&
                    cost.right() > c.x() && cost.bottom() > c.y() &&
                    c.x() - cost.x() <= 9 && c.y() - cost.y() <= 9,
                    "cost badge sits on the card's top-left corner");
            check(cost.x() > hud.card(0).right(),
                    "cost badge does not reach the card to the left");
        }
        float lift = 0.0F;
        float previous = -1.0F;
        for (int frame = 0; frame < 30; frame++) {
            lift = net.susu609.royalarena.client.hud.HudLayout.approachLift(
                    lift, 8.0F, 16.0F);
            check(lift >= previous && lift <= 8.0F,
                    "lift rises smoothly without overshooting");
            previous = lift;
        }
        check(lift == 8.0F, "lift settles on its target within half a second");
        check(net.susu609.royalarena.client.hud.HudLayout.approachLift(
                3.0F, 0.0F, 16.0F) < 3.0F,
                "a card sinks back when the hover ends");
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
        check(Math.abs(blue.x() + red.x()) < 1.0E-9D &&
                Math.abs(blue.z() + red.z()) < 1.0E-9D,
                "RED camera is BLUE's rotated 180 degrees");
        check(blue.x() == 0.0D && red.x() == 0.0D,
                "no sideways shift: the view runs along the arena axis");
        check(blue.yaw() == 180.0F && red.yaw() == 0.0F,
                "no yaw swing: BLUE looks due north, RED due south");
        check(blue.pitch() == red.pitch() && blue.pitch() >= 65.0F &&
                blue.pitch() < 90.0F,
                "almost top-down, still tilted a little (65-89 degrees)");
        check(blue.z() > 0.0D && red.z() < 0.0D && blue.y() > 100.0D,
                "each camera sits behind and above its own half");

        // The line of sight lands on the floor, on the arena axis, a few
        // blocks toward the local side (the look-ahead point).
        double[] hit = floorHit(blue, 100.0D);
        check(Math.abs(hit[0]) < 1.0E-6D, "view hits the floor on the axis");
        check(hit[1] > 0.0D && hit[1] < 10.0D,
                "view looks at the local half, slightly past the centre");
        double[] redHit = floorHit(red, 100.0D);
        check(Math.abs(redHit[1] + hit[1]) < 1.0E-6D,
                "RED looks at the mirrored point");
    }

    /** Where the camera's centre ray meets the floor plane, as (x, z). */
    private static double[] floorHit(
            net.susu609.royalarena.client.camera.ArenaCameraTransform c,
            double floorY)
    {
        double yaw = Math.toRadians(c.yaw());
        double pitch = Math.toRadians(c.pitch());
        double dx = -Math.sin(yaw) * Math.cos(pitch);
        double dy = -Math.sin(pitch);
        double dz = Math.cos(yaw) * Math.cos(pitch);
        double t = (floorY - c.y()) / dy;
        return new double[] {c.x() + dx * t, c.z() + dz * t};
    }

    /**
     * On a 16:9 window (vertical FOV 70) the whole arena must sit between the
     * top of the screen and the HUD panel, for both sides, and the arena must
     * not be clipped on the left or right.
     */
    private static void arenaFitsAboveTheHudPanel() {
        ArenaLayout arena = ArenaLayout.standard(new ArenaPoint(0, 100, 0));
        double panelTop = net.susu609.royalarena.client.hud.HudLayout.PANEL_TOP /
                net.susu609.royalarena.client.hud.HudLayout.REF_HEIGHT;
        for (TeamSide side : TeamSide.values()) {
            var cam = ArenaCameraProfile.DEFAULT.transform(arena, side);
            double yaw = Math.toRadians(cam.yaw());
            double pitch = Math.toRadians(cam.pitch());
            double[] fwd = {-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch),
                    Math.cos(yaw) * Math.cos(pitch)};
            double[] right = cross(fwd, new double[] {0, 1, 0});
            normalize(right);
            double[] up = cross(right, fwd);
            normalize(up);
            double tan = Math.tan(Math.toRadians(70.0D) / 2.0D);
            double minY = 1.0D;
            double maxY = 0.0D;
            for (int sx : new int[] {-9, 9}) {
                for (int sz : new int[] {-16, 16}) {
                    double[] d = {sx - cam.x(), 100.0D - cam.y(),
                            sz - cam.z()};
                    double depth = dot(d, fwd);
                    check(depth > 0.1D, "arena corner is in front of the camera");
                    double screenX = dot(d, right) / depth / (tan * 16.0D / 9.0D);
                    double screenY = dot(d, up) / depth / tan;
                    check(Math.abs(screenX) < 1.0D,
                            "arena corner is inside the frame horizontally");
                    double fromTop = (1.0D - screenY) / 2.0D;
                    minY = Math.min(minY, fromTop);
                    maxY = Math.max(maxY, fromTop);
                }
            }
            check(minY > 0.02D, "far arena edge is not cut off at the top: " + minY);
            check(maxY < panelTop,
                    "near arena edge stays above the HUD panel: " + maxY);
        }
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static void normalize(double[] v) {
        double length = Math.sqrt(dot(v, v));
        for (int i = 0; i < 3; i++)
            v[i] /= length;
    }

    private static void deckCyclesPlayedCard() {
        BattleDeck deck = BattleDeck.starter();
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
    /**
     * 1.20.1 deprecates ResourceLocation's constructors for removal. Ids are
     * built with net.susu609.modlib.resource.RL (RL.of / RL.require / RL.mc /
     * RL.parse) instead; see CLAUDE.md.
     */
    private static void noRawResourceLocationConstructors() {
        java.nio.file.Path root = java.nio.file.Path.of("src/main/java");
        if (!java.nio.file.Files.isDirectory(root))
            return;
        java.util.List<String> offenders = new java.util.ArrayList<>();
        try (var files = java.nio.file.Files.walk(root)) {
            for (java.nio.file.Path file : (Iterable<java.nio.file.Path>)
                    files.filter(f -> f.toString().endsWith(".java"))::iterator)
            {
                var lines = java.nio.file.Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).contains("new ResourceLocation("))
                        offenders.add(file + ":" + (i + 1));
                }
            }
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        check(offenders.isEmpty(),
                "use RL.of/RL.require (net.susu609.modlib.resource.RL) " +
                        "instead of new ResourceLocation(...): " + offenders);
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
