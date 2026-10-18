package net.susu609.royalarena.bot;

import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.card.CardRole;
import net.susu609.royalarena.card.DeckRules;
import net.susu609.royalarena.card.Tribe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A random but balanced deck for the bot: 8 different cards with at least
 * one spell, one tank and one ranged troop, averaging
 * {@link #MIN_AVERAGE}..{@link #MAX_AVERAGE} elixir. The order is random too.
 * Half the decks are built around a tribe: 3 to 6 of its cards first
 * (docs/TRIBES.md), so the bots play the tribe steps too.
 */
public final class BotDeck {
    public static final double MIN_AVERAGE = 2.8D;
    public static final double MAX_AVERAGE = 3.6D;
    private static final int TRIES = 500;

    private BotDeck() {
    }

    public static BattleDeck random(Random random) {
        if (random.nextBoolean()) {
            List<Tribe> tribes = new ArrayList<>();
            for (Tribe tribe : Tribe.values()) {
                if (tribe.members().size() >= 4)
                    tribes.add(tribe);
            }
            Tribe tribe = tribes.get(random.nextInt(tribes.size()));
            // 3 to 6 of its cards (as many as it has).
            int core = 3 + random.nextInt(Math.min(6, tribe.members().size()) - 2);
            BattleDeck deck = around(tribe, core, random);
            if (deck != null)
                return deck;
        }
        return plain(random);
    }

    /** A balanced deck from every card, with no tribe in mind. */
    public static BattleDeck plain(Random random) {
        List<BattleCard> pool = new ArrayList<>(Arrays.asList(
                BattleCard.values()));
        for (int i = 0; i < TRIES; i++) {
            Collections.shuffle(pool, random);
            List<BattleCard> pick = pool.subList(0, BattleDeck.DECK_SIZE);
            if (balanced(pick))
                return new BattleDeck(pick);
        }
        return BattleDeck.starter().shuffled(random);
    }

    /**
     * A balanced deck with {@code core} cards of {@code tribe} and the rest
     * from every other card, or null when none was found.
     */
    public static BattleDeck around(Tribe tribe, int core, Random random) {
        List<BattleCard> members = new ArrayList<>(tribe.members());
        List<BattleCard> others = new ArrayList<>(Arrays.asList(
                BattleCard.values()));
        others.removeAll(members);
        for (int i = 0; i < TRIES; i++) {
            Collections.shuffle(members, random);
            Collections.shuffle(others, random);
            List<BattleCard> pick = new ArrayList<>(members.subList(0,
                    Math.min(core, members.size())));
            for (BattleCard card : others) {
                if (pick.size() >= BattleDeck.DECK_SIZE)
                    break;
                pick.add(card);
            }
            Collections.shuffle(pick, random);
            if (balanced(pick))
                return new BattleDeck(pick);
        }
        return null;
    }

    public static boolean balanced(List<BattleCard> cards) {
        if (DeckRules.problem(cards) != null)
            return false;
        boolean spell = false;
        boolean tank = false;
        boolean ranged = false;
        for (BattleCard card : cards) {
            var roles = CardRole.of(card);
            spell |= roles.contains(CardRole.SPELL);
            tank |= roles.contains(CardRole.TANK);
            ranged |= roles.contains(CardRole.RANGED);
        }
        double average = DeckRules.averageElixir(cards);
        return spell && tank && ranged && average >= MIN_AVERAGE - 1.0E-9D &&
                average <= MAX_AVERAGE + 1.0E-9D;
    }
}
