package net.susu609.royalarena.bot;

import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.card.CardRole;
import net.susu609.royalarena.card.DeckRules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A random but balanced deck for the bot: 8 different cards with at least
 * one spell, one tank and one ranged troop, averaging
 * {@link #MIN_AVERAGE}..{@link #MAX_AVERAGE} elixir. The order is random too.
 */
public final class BotDeck {
    public static final double MIN_AVERAGE = 2.8D;
    public static final double MAX_AVERAGE = 3.6D;
    private static final int TRIES = 500;

    private BotDeck() {
    }

    public static BattleDeck random(Random random) {
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
