package net.susu609.royalarena.client.state;

import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.DeckRules;
import net.susu609.royalarena.network.s2c.S2CDeckPacket;

import java.util.List;

/** The player's saved deck as last sent by the server. */
public final class ClientDeckState {
    private static List<BattleCard> deck = List.of();
    private static String message = "";
    private static boolean ok = true;
    private static int version;

    private ClientDeckState() {
    }

    public static void apply(S2CDeckPacket packet) {
        List<BattleCard> cards = DeckRules.parse(packet.cards());
        deck = cards == null ? List.of() : List.copyOf(cards);
        message = packet.message();
        ok = packet.ok();
        version++;
    }

    public static List<BattleCard> deck() {
        return deck;
    }
    public static String message() {
        return message;
    }
    public static boolean ok() {
        return ok;
    }
    /** Grows by one with every packet, so a screen can notice news. */
    public static int version() {
        return version;
    }
}
