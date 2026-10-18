package net.susu609.royalarena.match;

import java.util.List;
import java.util.Locale;

/**
 * What the spectator picked for a bot-versus-bot match: each bot's deck
 * ({@link #DECKS}) and personality ("random" or a personality id).
 */
public record WatchSetup(String blueDeck, String redDeck, String blueStyle,
                         String redStyle)
{
    /** A random balanced bot deck, the spectator's own saved deck, or the starter deck. */
    public static final List<String> DECKS = List.of("random", "mine",
            "starter");
    public static final String RANDOM = "random";
    public static final WatchSetup DEFAULT =
            new WatchSetup(RANDOM, RANDOM, RANDOM, RANDOM);

    public WatchSetup {
        blueDeck = deck(blueDeck);
        redDeck = deck(redDeck);
        blueStyle = style(blueStyle);
        redStyle = style(redStyle);
    }

    private static String deck(String id) {
        String clean = id == null ? RANDOM : id.trim().toLowerCase(Locale.ROOT);
        return DECKS.contains(clean) ? clean : RANDOM;
    }

    private static String style(String id) {
        return id == null || id.isBlank() ? RANDOM :
                id.trim().toLowerCase(Locale.ROOT);
    }

    /** The name a deck choice shows on the setup screen. */
    public static String deckLabel(String id) {
        return switch (id) {
            case "mine" -> "My deck";
            case "starter" -> "Starter";
            default -> "Random";
        };
    }
}
