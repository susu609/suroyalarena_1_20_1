package net.susu609.royalarena.client.state;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.match.MatchSnapshot;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClientMatchState {
    private static MatchSnapshot snapshot = MatchSnapshot.inactive();
    private static int selectedSlot = -1;

    private ClientMatchState() {
    }
    public static void reset() {
        apply(MatchSnapshot.inactive());
    }
    public static void apply(MatchSnapshot next) {
        snapshot = next;
        if (!next.active())
            selectedSlot = -1;
        if (selectedSlot >= next.hand().size())
            selectedSlot = -1;
    }
    public static TeamSide side() {
        return snapshot.localSide();
    }
    public static int ticksRemaining() {
        return snapshot.ticksRemaining();
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
    public static BattleCard nextCard() {
        return snapshot.active() && !snapshot.nextCard().isBlank() ?
                BattleCard.byId(snapshot.nextCard()) : null;
    }
    public static int towerHealth(TowerId id) {
        int[] hp = snapshot.towerHealth();
        return id.ordinal() < hp.length ? hp[id.ordinal()] : 0;
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
    public static boolean active() {
        return snapshot.active();
    }
    public static MatchState state() {
        return snapshot.state();
    }
    public static List<BattleCard> hand() {
        if (!snapshot.active())
            return List.of();
        ArrayList<BattleCard> cards = new ArrayList<>();
        for (String id : snapshot.hand())
            cards.add(BattleCard.byId(id));
        return Collections.unmodifiableList(cards);
    }
    public static void clearSelection() {
        selectedSlot = -1;
    }
}
