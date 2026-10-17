package net.susu609.royalarena.client.state;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.match.MatchSnapshot;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.PendingDeploy;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClientMatchState {
    private static MatchSnapshot snapshot = MatchSnapshot.inactive();
    private static int selectedSlot = -1;
    private static int clientTicks;
    private static int appliedAtTick;
    /** Client tick each tower last lost health, for its red flash. */
    private static final int[] TOWER_HURT_AT = new int[TowerId.values().length];

    static {
        java.util.Arrays.fill(TOWER_HURT_AT, Integer.MIN_VALUE / 2);
    }

    private ClientMatchState() {
    }

    public static void apply(MatchSnapshot next) {
        if (next.active() && snapshot.active() &&
                next.matchId().equals(snapshot.matchId())) {
            for (TowerId id : TowerId.values()) {
                if (next.towerHealth()[id.ordinal()] <
                        snapshot.towerHealth()[id.ordinal()])
                    TOWER_HURT_AT[id.ordinal()] = clientTicks;
            }
        } else {
            java.util.Arrays.fill(TOWER_HURT_AT, Integer.MIN_VALUE / 2);
        }
        snapshot = next;
        appliedAtTick = clientTicks;
        if (!next.active())
            selectedSlot = -1;
        if (selectedSlot >= next.hand().size())
            selectedSlot = -1;
    }

    public static void reset() {
        apply(MatchSnapshot.inactive());
    }

    public static boolean active() {
        return snapshot.active();
    }
    public static MatchState state() {
        return snapshot.state();
    }
    public static TeamSide side() {
        return snapshot.localSide();
    }
    public static int ticksRemaining() {
        return snapshot.ticksRemaining();
    }
    public static boolean overtime() {
        return snapshot.overtime();
    }
    /** 1 normal, 2 or 3 for double and triple elixir. */
    public static int elixirRate() {
        return snapshot.elixirRate();
    }
    public static int elixirTenths() {
        return snapshot.elixirTenths();
    }
    public static TeamSide winner() {
        return snapshot.winner();
    }
    public static ArenaLayout arena() {
        return ArenaLayout.standard(snapshot.center());
    }

    public static List<BattleCard> hand() {
        if (!snapshot.active())
            return List.of();
        ArrayList<BattleCard> cards = new ArrayList<>();
        for (String id : snapshot.hand())
            cards.add(BattleCard.byId(id));
        return Collections.unmodifiableList(cards);
    }

    public static BattleCard nextCard() {
        return snapshot.active() && !snapshot.nextCard().isBlank() ?
                BattleCard.byId(snapshot.nextCard()) : null;
    }

    /** The player whose skin the King of {@code side} wears. */
    public static java.util.UUID player(TeamSide side) {
        return snapshot.player(side);
    }

    public static int towerHealth(TowerId id) {
        int[] hp = snapshot.towerHealth();
        return id.ordinal() < hp.length ? hp[id.ordinal()] : 0;
    }

    /** Client ticks since the tower last lost health (huge if never). */
    public static int ticksSinceTowerHurt(TowerId id) {
        return clientTicks - TOWER_HURT_AT[id.ordinal()];
    }

    /**
     * Whether a King tower shoots, as the engine decides it: it has been
     * hit, or one of its Princess towers has fallen.
     */
    public static boolean kingAwake(TeamSide side) {
        TowerId king = TowerId.king(side);
        return towerHealth(king) < king.kind().maxHealth() ||
                towerHealth(TowerId.left(side)) <= 0 ||
                towerHealth(TowerId.right(side)) <= 0;
    }

    public static int selectedSlot() {
        return selectedSlot;
    }

    public static void selectSlot(int slot) {
        if (!active() || state() != MatchState.BATTLE || slot < 0 ||
                slot >= hand().size())
            return;
        selectedSlot = slot;
    }

    /** Called once per client tick to age pending deploys between packets. */
    public static void tick() {
        clientTicks++;
    }

    /** Pending deploys with their timers advanced by local ticks. */
    public static List<PendingDeploy> pendingDeploys() {
        if (!active())
            return List.of();
        int elapsed = clientTicks - appliedAtTick;
        ArrayList<PendingDeploy> out = new ArrayList<>();
        for (PendingDeploy deploy : snapshot.pending()) {
            int left = deploy.ticksLeft() - elapsed;
            if (left > 0) {
                out.add(new PendingDeploy(deploy.card(), deploy.side(),
                        deploy.x(), deploy.z(), left));
            }
        }
        return out;
    }

    /**
     * Client-side check before sending a deploy: own half for a troop, the
     * whole arena for a spell, and enough elixir. The server validates again.
     */
    public static boolean canDeployAt(int slot, int worldX, int worldZ) {
        if (!active() || state() != MatchState.BATTLE || slot < 0 ||
                slot >= hand().size())
            return false;
        BattleCard card = hand().get(slot);
        if (card.cost() * 10 > elixirTenths())
            return false;
        return arena().canPlay(side(), card, worldX, worldZ);
    }

    public static void clearSelection() {
        selectedSlot = -1;
    }
}
