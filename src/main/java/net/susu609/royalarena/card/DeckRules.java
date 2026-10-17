package net.susu609.royalarena.card;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** What makes a deck playable: exactly 8 different cards. */
public final class DeckRules {
    private DeckRules() {
    }

    /** Why the deck cannot be played, or null when it can. */
    public static String problem(List<BattleCard> cards) {
        if (cards == null || cards.size() != BattleDeck.DECK_SIZE)
            return "A deck needs exactly " + BattleDeck.DECK_SIZE + " cards";
        Set<BattleCard> seen = EnumSet.noneOf(BattleCard.class);
        for (BattleCard card : cards) {
            if (card == null)
                return "A deck needs exactly " + BattleDeck.DECK_SIZE +
                        " cards";
            if (!seen.add(card))
                return "Each card can be in the deck only once";
        }
        return null;
    }

    /** Cards for these ids, or null if an id is unknown. */
    public static List<BattleCard> parse(List<String> ids) {
        List<BattleCard> cards = new ArrayList<>(ids.size());
        for (String id : ids) {
            BattleCard card = find(id);
            if (card == null)
                return null;
            cards.add(card);
        }
        return cards;
    }

    public static BattleCard find(String id) {
        for (BattleCard card : BattleCard.values()) {
            if (card.id().equals(id))
                return card;
        }
        return null;
    }

    public static List<String> ids(List<BattleCard> cards) {
        List<String> ids = new ArrayList<>(cards.size());
        for (BattleCard card : cards)
            ids.add(card.id());
        return ids;
    }

    /** Average elixir cost of the cards present (empty slots skipped). */
    public static double averageElixir(List<BattleCard> cards) {
        int total = 0;
        int count = 0;
        for (BattleCard card : cards) {
            if (card == null)
                continue;
            total += card.cost();
            count++;
        }
        return count == 0 ? 0.0D : total / (double) count;
    }
}
