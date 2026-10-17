package net.susu609.royalarena.card;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * What the deck screen's library shows: one {@link Tab} (card kind) and any
 * number of {@link Chip}s, which must all match, optionally sorted by elixir,
 * cut into pages.
 */
public final class CardFilter {
    /** Library cards per page: two rows of eight. */
    public static final int PAGE_SIZE = 16;

    private CardFilter() {
    }

    /** Kind of card. Buildings exist as a tab before any building card. */
    public enum Tab {
        ALL("All"), TROOPS("Troops"), SPELLS("Spells"),
        BUILDINGS("Buildings");

        private final String label;

        Tab(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public boolean matches(BattleCard card) {
            return switch (this) {
                case ALL -> true;
                case TROOPS -> !card.isSpell();
                case SPELLS -> card.isSpell();
                // No building cards yet (docs/CARDS.md).
                case BUILDINGS -> false;
            };
        }
    }

    /** A filter button: a hand-written tactic or a role from the stats. */
    public enum Chip {
        ATTACK(CardTactic.ATTACK, null), DEFENSE(CardTactic.DEFENSE, null),
        SUPPORT(CardTactic.SUPPORT, null), TANK(null, CardRole.TANK),
        SWARM(null, CardRole.SWARM), RANGED(null, CardRole.RANGED),
        AREA(null, CardRole.AREA);

        private final CardTactic tactic;
        private final CardRole role;

        Chip(CardTactic tactic, CardRole role) {
            this.tactic = tactic;
            this.role = role;
        }

        public String label() {
            return tactic != null ? tactic.label() :
                    role == CardRole.AREA ? "Area" : role.label();
        }

        public boolean matches(BattleCard card) {
            return tactic != null ? card.tactics().contains(tactic) :
                    CardRole.of(card).contains(role);
        }
    }

    /** The cards shown, in library order or cheapest first. */
    public static List<BattleCard> shown(Tab tab, Set<Chip> chips,
            boolean byElixir)
    {
        List<BattleCard> cards = new ArrayList<>();
        for (BattleCard card : BattleCard.values()) {
            if (tab.matches(card) &&
                    chips.stream().allMatch(c -> c.matches(card)))
                cards.add(card);
        }
        if (byElixir)
            cards.sort(Comparator.comparingInt(BattleCard::cost));
        return cards;
    }

    /** Pages needed for {@code count} cards (at least one, maybe empty). */
    public static int pages(int count) {
        return Math.max(1, (count + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    /** The cards of page {@code page} (0-based, clamped). */
    public static List<BattleCard> page(List<BattleCard> cards, int page) {
        int p = Math.max(0, Math.min(page, pages(cards.size()) - 1));
        int from = p * PAGE_SIZE;
        return cards.subList(Math.min(from, cards.size()),
                Math.min(from + PAGE_SIZE, cards.size()));
    }
}
