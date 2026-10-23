package net.susu609.royalarena.client.state;

import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.config.ArenaClientConfig;
import net.susu609.royalarena.match.MatchSnapshot;
import net.susu609.royalarena.match.SandboxQuickBar;
import net.susu609.royalarena.match.TeamSide;

import java.util.ArrayList;

/**
 * This client's side of a sandbox (docs/SANDBOX.md): the card picked on the
 * panel, the side it will be played for (Tab), the quick bar (keys 1-8;
 * its pins saved in royalarena-client.toml), and whether the stats overlay
 * is on. The pick stays after a card is played, until a right click. The switches themselves live on the server and come back
 * in every snapshot.
 */
public final class ClientSandbox {
    private static BattleCard card;
    private static TeamSide side = TeamSide.BLUE;
    private static boolean stats;
    private static SandboxQuickBar bar;

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
        quickBar().use(picked);
    }

    /** The quick bar, its pins read from the client config the first time. */
    public static SandboxQuickBar quickBar() {
        if (bar == null) {
            bar = new SandboxQuickBar();
            try {
                bar.loadPinned(new ArrayList<>(
                        ArenaClientConfig.SANDBOX_PINNED.get()));
            } catch (IllegalStateException e) {
                // Config not loaded yet: start with no pins.
            }
        }
        return bar;
    }

    /** Picks the quick bar's card in {@code slot} (keys 1-8). */
    public static void selectQuick(int slot) {
        BattleCard picked = quickBar().at(slot);
        if (picked != null)
            card = picked;
    }

    /** Pins or unpins a card on the quick bar and saves the pins. */
    public static void togglePin(BattleCard target) {
        quickBar().togglePin(target);
        try {
            ArenaClientConfig.SANDBOX_PINNED.set(quickBar().pinnedIds());
            ArenaClientConfig.SANDBOX_PINNED.save();
        } catch (IllegalStateException e) {
            // Not saved: the pin holds for this game only.
        }
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
