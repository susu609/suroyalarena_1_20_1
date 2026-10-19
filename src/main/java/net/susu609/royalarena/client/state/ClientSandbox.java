package net.susu609.royalarena.client.state;

import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.match.MatchSnapshot;
import net.susu609.royalarena.match.TeamSide;

/**
 * This client's side of a sandbox (docs/SANDBOX.md): the card picked on the
 * panel, the side it will be played for (Tab), and whether the stats
 * overlay is on. The switches themselves live on the server and come back
 * in every snapshot.
 */
public final class ClientSandbox {
    private static BattleCard card;
    private static TeamSide side = TeamSide.BLUE;
    private static boolean stats;

    private ClientSandbox() {
    }

    /** Whether the current match is a sandbox. */
    public static boolean active() {
        return ClientMatchState.active() && ClientMatchState.sandbox() != null;
    }

    /** The server's switches (null outside a sandbox). */
    public static MatchSnapshot.Sandbox settings() {
        return ClientMatchState.sandbox();
    }

    public static BattleCard card() {
        return card;
    }

    public static void select(BattleCard picked) {
        card = picked;
    }

    public static void clearCard() {
        card = null;
    }

    public static TeamSide side() {
        return side;
    }

    public static void toggleSide() {
        side = side.opponent();
    }

    public static boolean stats() {
        return stats;
    }

    public static void toggleStats() {
        stats = !stats;
    }

    /** Forgets the pick when the sandbox ends; the side and overlay stay. */
    public static void reset() {
        card = null;
    }
}
