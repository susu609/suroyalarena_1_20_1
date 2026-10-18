package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.bot.BotDeck;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.card.CardRole;
import net.susu609.royalarena.card.DeckRules;
import net.susu609.royalarena.match.MatchInstance;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Pure-Java checks for deck building and the bot's random decks. */
public final class DeckSmokeTest {
    private DeckSmokeTest() {
    }

    public static void run() {
        deckRulesNeedEightDifferentCards();
        rolesDescribeTheCards();
        botDecksAreRandomButBalanced();
        matchesUseTheGivenDecks();
        System.out.println("RoyalArena deck smoke tests: PASS");
    }

    private static void deckRulesNeedEightDifferentCards() {
        List<BattleCard> starter = BattleDeck.starter().cards();
        check(DeckRules.problem(starter) == null, "the starter deck is valid");
        check(DeckRules.problem(starter.subList(0, 7)) != null,
                "seven cards are not a deck");
        List<BattleCard> twice = new ArrayList<>(starter);
        twice.set(7, twice.get(0));
        check(DeckRules.problem(twice) != null, "no card twice");
        List<BattleCard> hole = new ArrayList<>(starter);
        hole.set(3, null);
        check(DeckRules.problem(hole) != null, "no empty slot");
        check(DeckRules.parse(List.of("zombie", "nope")) == null,
                "unknown ids are rejected");
        check(DeckRules.parse(DeckRules.ids(starter)).equals(starter),
                "ids round-trip");
        // The starter deck's costs follow the cards (25 with the vanilla
        // health costs; it was 26), so the total is summed from them.
        int total = 0;
        for (BattleCard card : starter)
            total += card.cost();
        check(Math.abs(DeckRules.averageElixir(starter) - total / 8.0D) < 1e-9,
                "average elixir of the starter deck: " +
                        DeckRules.averageElixir(starter) + " vs " + total + "/8");
        check(Math.abs(DeckRules.averageElixir(hole) -
                        (total - starter.get(3).cost()) / 7.0D) < 1e-9,
                "average skips empty slots");
        boolean threw = false;
        try {
            new BattleDeck(twice);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check(threw, "a BattleDeck refuses an invalid list");
    }

    private static void rolesDescribeTheCards() {
        check(CardRole.of(BattleCard.IRON_GOLEM).contains(CardRole.TANK),
                "Iron Golem is a tank");
        check(CardRole.of(BattleCard.BIG_SLIME).contains(CardRole.TANK),
                "Big Slime counts its children: a tank");
        check(Math.abs(CardRole.bodyHealth(BattleCard.BIG_SLIME.stats()) -
                108.0D) < 1e-9, "Big Slime body health 36 + 2x18 + 4x9 (36 since the slime is drawn 4x)");
        check(!CardRole.of(BattleCard.ZOMBIE).contains(CardRole.TANK),
                "two Zombies are not a tank");
        check(CardRole.of(BattleCard.SKELETON_ARCHER).contains(CardRole.RANGED)
                && CardRole.of(BattleCard.STRAY).contains(CardRole.RANGED),
                "archers are ranged");
        // Skeletons are now two (vanilla health 20 each), so three Cave
        // Spiders stand in as the three-body swarm.
        check(CardRole.of(BattleCard.CAVE_SPIDER).contains(CardRole.SWARM),
                "three Cave Spiders are a swarm");
        check(CardRole.of(BattleCard.FIREBALL).equals(
                EnumSet.of(CardRole.SPELL)), "a spell is only a spell");
    }

    private static void botDecksAreRandomButBalanced() {
        Set<Set<BattleCard>> decks = new HashSet<>();
        Set<List<BattleCard>> orders = new HashSet<>();
        for (long seed = 0; seed < 300; seed++) {
            List<BattleCard> cards = BotDeck.random(new Random(seed)).cards();
            check(BotDeck.balanced(cards), "seed " + seed + " " + cards);
            double average = DeckRules.averageElixir(cards);
            check(average >= BotDeck.MIN_AVERAGE &&
                    average <= BotDeck.MAX_AVERAGE, "average " + average);
            decks.add(EnumSet.copyOf(cards));
            orders.add(cards);
        }
        check(decks.size() >= 4, "bots pick different card sets: " +
                decks.size());
        check(orders.size() >= 200, "and shuffle them: " + orders.size());
        check(BotDeck.random(new Random(42)).cards().equals(
                BotDeck.random(new Random(42)).cards()),
                "the same seed gives the same deck");
        List<BattleCard> noSpell = new ArrayList<>(List.of(
                BattleCard.ZOMBIE, BattleCard.SKELETON_ARCHER,
                BattleCard.SPIDER, BattleCard.SKELETONS, BattleCard.BIG_SLIME,
                BattleCard.STRAY, BattleCard.VINDICATOR,
                BattleCard.IRON_GOLEM));
        check(!BotDeck.balanced(noSpell), "a deck without a spell is not " +
                "balanced");
    }

    private static void matchesUseTheGivenDecks() {
        BattleDeck blue = BotDeck.random(new Random(7));
        BattleDeck red = BattleDeck.starter().shuffled(new Random(8));
        List<BattleCard> blueCards = blue.cards();
        List<BattleCard> redCards = red.cards();
        check(EnumSet.copyOf(redCards).equals(
                EnumSet.copyOf(BattleDeck.starter().cards())),
                "shuffling keeps the cards");
        MatchInstance match = MatchInstance.create(UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(),
                ArenaLayout.standard(new ArenaPoint(0, 80, 0)), blue, red);
        check(match.blue().deck().hand().equals(blueCards.subList(0, 4)) &&
                match.red().deck().hand().equals(redCards.subList(0, 4)),
                "each side plays its own deck");
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
