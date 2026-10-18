package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.bot.BotDeck;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.card.Tribe;
import net.susu609.royalarena.card.TribeBonus;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.combat.TickResult;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.TeamSide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Tribes (docs/TRIBES.md): every troop belongs to one or two, a deck's
 * different cards of a tribe reach steps at 2, 4, 6 and 8, and the steps
 * change the troops of that tribe in combat: health, damage, armour
 * piercing, healing, the first-hit stun. Bot decks are built around tribes.
 */
public final class TribeSmokeTest {
    /** Eight spells: a deck that reaches no tribe step. */
    private static final List<BattleCard> SPELLS = List.of(
            BattleCard.ARROW_STORM, BattleCard.FIREBALL, BattleCard.LIGHTNING,
            BattleCard.HEALING_POTION, BattleCard.FREEZE, BattleCard.TNT,
            BattleCard.POISON_CLOUD, BattleCard.COBWEB);

    private TribeSmokeTest() {
    }

    public static void run() {
        everyTroopHasOneOrTwoTribes();
        stepsCountDifferentCards();
        tribesAreOffUnlessTurnedOn();
        zombiesGetHealthAndHeal();
        skeletonsPierceArmour();
        nightFirstHitStuns();
        botDecksAreBuiltAroundTribes();
        System.out.println("RoyalArena tribe smoke tests: PASS");
    }

    private static void everyTroopHasOneOrTwoTribes() {
        for (BattleCard card : BattleCard.values()) {
            int n = card.tribes().size();
            if (card.isSpell())
                check(n == 0, card.id() + ": spells have no tribe");
            else
                check(n == 1 || n == 2, card.id() + " has 1 or 2 tribes, not " + n);
        }
        for (Tribe tribe : Tribe.values()) {
            check(tribe.members().size() >= 2, tribe.id() + " has 2+ cards");
            check(tribe.steps().size() <= Tribe.THRESHOLDS.length,
                    tribe.id() + " steps");
        }
        check(Tribe.NIGHT.members().size() >= 8,
                "the Night tribe can fill a whole deck (step 8)");
    }

    private static void stepsCountDifferentCards() {
        List<BattleCard> deck = deck(BattleCard.ZOMBIE, BattleCard.HUSK,
                BattleCard.DROWNED, BattleCard.ZOMBIE_VILLAGER);
        Map<Tribe, Integer> counts = Tribe.counts(deck);
        check(counts.get(Tribe.ZOMBIE) == 4, "four Zombie-tribe cards");
        Map<Tribe, Integer> levels = Tribe.levels(deck);
        check(levels.get(Tribe.ZOMBIE) == 2, "4 cards reach step 2 (2 and 4)");
        check(!levels.containsKey(Tribe.AQUATIC) && !levels.containsKey(Tribe.NIGHT),
                "a tribe with one card reaches nothing");
        check(Tribe.ZOMBIE.level(1) == 0 && Tribe.ZOMBIE.level(2) == 1 &&
                Tribe.ZOMBIE.level(7) == 3 && Tribe.ZOMBIE.level(8) == 4,
                "steps at 2, 4, 6 and 8");
        check(Tribe.GOLEM.level(8) == 1, "a two-card tribe has only step 2");
        TribeBonus husk = Tribe.bonusFor(BattleCard.HUSK, levels);
        near(husk.health(), 1.05D, "step 2: +5% health");
        near(husk.regen(), 0.15D, "step 4: heals 0.15 a second");
        check(Tribe.bonusFor(BattleCard.FIREBALL, levels).none(),
                "a spell gets nothing");
        List<BattleCard> withGaps = new ArrayList<>(deck);
        withGaps.set(7, null);
        check(Tribe.summary(withGaps).equals("Tribes: Zombie 4/6"),
                "deck screen line: " + Tribe.summary(withGaps));
    }

    private static void tribesAreOffUnlessTurnedOn() {
        MatchInstance match = battle(deck(BattleCard.ZOMBIE, BattleCard.HUSK,
                BattleCard.DROWNED, BattleCard.ZOMBIE_VILLAGER));
        CombatEngine engine = new CombatEngine(match);
        CombatUnit plain = add(engine, BattleCard.HUSK, TeamSide.BLUE, 0.0D, 6.0D);
        near(plain.maxHealth(), BattleCard.HUSK.stats().health(),
                "tribes are off by default: the card's own health");
        engine.setTribes(true);
        CombatUnit tribal = add(engine, BattleCard.HUSK, TeamSide.BLUE, 2.0D, 6.0D);
        near(tribal.maxHealth(), BattleCard.HUSK.stats().health() * 1.05D,
                "with tribes on, four Zombie cards give +5% health");
    }

    private static void zombiesGetHealthAndHeal() {
        MatchInstance match = battle(deck(BattleCard.ZOMBIE, BattleCard.HUSK,
                BattleCard.DROWNED, BattleCard.ZOMBIE_VILLAGER));
        CombatEngine engine = new CombatEngine(match);
        engine.setTribes(true);
        // Out of every tower's range, on BLUE's bank next to the river.
        CombatUnit zombie = add(engine, BattleCard.ZOMBIE, TeamSide.BLUE, 0.0D, 3.2D);
        engine.castSpell(TeamSide.RED, BattleCard.FREEZE, 0.0D, 3.2D);
        double hurt = -1.0D;
        for (int i = 0; i < 20 && hurt < 0.0D; i++) {
            engine.tick();
            if (zombie.health() < zombie.maxHealth())
                hurt = zombie.health();
        }
        check(hurt > 0.0D, "Freeze hurt the zombie");
        for (int i = 0; i < 100; i++)
            engine.tick();
        near(zombie.health(), Math.min(zombie.maxHealth(), hurt + 0.75D),
                "step 4: 0.15 health a second back, 0.75 in 100 ticks");
    }

    private static void skeletonsPierceArmour() {
        MatchInstance match = battle(deck(BattleCard.SKELETON_ARCHER,
                BattleCard.SKELETONS, BattleCard.STRAY,
                BattleCard.SKELETON_RIDER));
        CombatEngine engine = new CombatEngine(match);
        engine.setTribes(true);
        add(engine, BattleCard.SKELETON_ARCHER, TeamSide.BLUE, 0.0D, 3.0D);
        CombatUnit zombie = add(engine, BattleCard.ZOMBIE, TeamSide.RED, 0.0D, -1.5D);
        for (int i = 0; i < 40 && zombie.health() == zombie.maxHealth(); i++)
            engine.tick();
        near(zombie.health(), BattleCard.ZOMBIE.stats().health() -
                        BattleCard.SKELETON_ARCHER.stats().damage() * 1.05D,
                "the archer's arrow does +5% and ignores the zombie's armour");
    }

    private static void nightFirstHitStuns() {
        MatchInstance match = battle(deck(BattleCard.ZOMBIE, BattleCard.CREEPER,
                BattleCard.ENDERMAN, BattleCard.SPIDER));
        CombatEngine engine = new CombatEngine(match);
        engine.setTribes(true);
        CombatUnit blue = add(engine, BattleCard.ZOMBIE, TeamSide.BLUE, 0.0D, 3.2D);
        CombatUnit red = add(engine, BattleCard.ZOMBIE, TeamSide.RED, 0.0D, 2.0D);
        TickResult first = engine.tick();
        check(first.attacks().size() == 2, "both zombies swing");
        check(red.frozen() && !blue.frozen(),
                "the Night zombie's first hit freezes; the other side has no tribe");
        for (int i = 0; i < 30; i++)
            engine.tick();
        check(!red.frozen() || red.frozenTicks() < 10,
                "only the first hit freezes");
    }

    private static void botDecksAreBuiltAroundTribes() {
        Random random = new Random(7L);
        for (Tribe tribe : Tribe.values()) {
            if (tribe.members().size() < 4)
                continue;
            BattleDeck deck = BotDeck.around(tribe, 4, random);
            check(deck != null, "a balanced deck around " + tribe.id());
            check(Tribe.counts(deck.cards()).get(tribe) == 4,
                    tribe.id() + ": exactly 4 of its cards");
            check(BotDeck.balanced(deck.cards()), tribe.id() + " deck is balanced");
        }
    }

    /** The given troops, filled up with spells to eight cards. */
    private static List<BattleCard> deck(BattleCard... troops) {
        List<BattleCard> cards = new ArrayList<>(List.of(troops));
        for (BattleCard spell : SPELLS) {
            if (cards.size() >= BattleDeck.DECK_SIZE)
                break;
            cards.add(spell);
        }
        return cards;
    }

    /** BLUE plays {@code blue}; RED plays eight spells (no tribe). */
    private static MatchInstance battle(List<BattleCard> blue) {
        MatchInstance match = MatchInstance.create(UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(),
                ArenaLayout.standard(new ArenaPoint(0, 80, 0)),
                new BattleDeck(blue), new BattleDeck(SPELLS));
        match.startBattle();
        return match;
    }

    private static CombatUnit add(CombatEngine engine, BattleCard card,
            TeamSide side, double x, double z)
    {
        return engine.add(UUID.randomUUID(), side, card.stats(), x, z);
    }

    private static void near(double actual, double expected, String name) {
        check(Math.abs(actual - expected) < 1.0E-6D,
                name + ": " + actual + " vs " + expected);
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
