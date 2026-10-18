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
        onlyTheTribeGetsIt();
        zombiesRiseAgainOnce();
        illagersHaveATotem();
        netherLastFlame();
        nightShadowDodgesOneShot();
        skeletonVolley();
        beastPack();
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
        check(levels.get(Tribe.ZOMBIE) == 3, "4 cards reach 3 steps (2, 3 and 4)");
        check(!levels.containsKey(Tribe.AQUATIC) && !levels.containsKey(Tribe.NIGHT),
                "a tribe with one card reaches nothing");
        check(Tribe.ZOMBIE.level(1) == 0 && Tribe.ZOMBIE.level(2) == 1 &&
                Tribe.ZOMBIE.level(3) == 2 && Tribe.ZOMBIE.level(6) == 5 &&
                Tribe.ZOMBIE.level(8) == 5, "five steps, at 2, 3, 4, 5 and 6");
        check(Tribe.GOLEM.level(8) == 1, "a two-card tribe has only step 2");
        for (Tribe tribe : Tribe.values()) {
            if (tribe.steps().size() == Tribe.THRESHOLDS.length)
                check(tribe.bonus(4).plus(TribeBonus.NONE).equals(tribe.bonus(4)) &&
                        !tribe.bonus(4).equals(tribe.bonus(3)),
                        tribe.id() + ": step 5 adds a special rule");
        }
        TribeBonus husk = Tribe.bonusFor(BattleCard.HUSK, levels);
        near(husk.health(), 1.05D, "step 2: +5% health");
        near(husk.regen(), 0.15D, "step 3: heals 0.15 a second");
        near(husk.lifesteal(), 0.05D, "step 4: lifesteal 5%");
        near(husk.revive(), 0.0D, "rising again needs 5 cards");
        check(Tribe.bonusFor(BattleCard.FIREBALL, levels).none(),
                "a spell gets nothing");
        List<BattleCard> withGaps = new ArrayList<>(deck);
        withGaps.set(7, null);
        check(Tribe.summary(withGaps).equals("Tribes: Zombie 4/5"),
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
                "step 3: 0.15 health a second back, 0.75 in 100 ticks");
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

    private static void onlyTheTribeGetsIt() {
        List<BattleCard> deck = deck(BattleCard.ZOMBIE, BattleCard.HUSK,
                BattleCard.DROWNED, BattleCard.ZOMBIE_VILLAGER,
                BattleCard.ZOMBIFIED_PIGLIN, BattleCard.VINDICATOR);
        Map<Tribe, Integer> levels = Tribe.levels(deck);
        check(Tribe.bonusFor(BattleCard.VINDICATOR, levels).none(),
                "a Vindicator in a Zombie deck gets nothing from the Zombie tribe");
        check(Tribe.bonusFor(BattleCard.HUSK, levels).revive() > 0.0D,
                "the Zombie tribe's own troops do");
    }

    private static void zombiesRiseAgainOnce() {
        MatchInstance match = battle(deck(BattleCard.ZOMBIE, BattleCard.HUSK,
                BattleCard.DROWNED, BattleCard.ZOMBIE_VILLAGER,
                BattleCard.ZOMBIFIED_PIGLIN));
        CombatEngine engine = new CombatEngine(match);
        engine.setTribes(true);
        CombatUnit husk = add(engine, BattleCard.HUSK, TeamSide.BLUE, 0.0D, 3.2D);
        engine.castSpell(TeamSide.RED, BattleCard.FIREBALL, 0.0D, 3.2D);
        boolean rose = false;
        for (int i = 0; i < 30 && !rose; i++) {
            TickResult r = engine.tick();
            rose = r.tribeEvents().stream().anyMatch(e ->
                    e.kind() == TickResult.TribeEvent.Kind.REVIVE);
        }
        check(rose && husk.alive(), "the Fireball kills the Husk and it rises again");
        near(husk.health(), husk.maxHealth() * 0.25D, "with 25% health");
        check(!husk.invulnerable(), "with no moment of safety");
        CombatUnit other = add(engine, BattleCard.HUSK, TeamSide.BLUE, 6.0D, 3.2D);
        engine.castSpell(TeamSide.RED, BattleCard.FIREBALL, husk.x(), husk.z());
        for (int i = 0; i < 40; i++)
            engine.tick();
        check(!husk.alive() || engine.unit(husk.id()) == null,
                "only once: the second death is for good");
        engine.castSpell(TeamSide.RED, BattleCard.FIREBALL, other.x(), other.z());
        boolean again = false;
        for (int i = 0; i < 40; i++)
            again |= engine.tick().tribeEvents().stream().anyMatch(e ->
                    e.kind() == TickResult.TribeEvent.Kind.REVIVE);
        check(!again, "one rise every 20 s for the side: another Husk falls for good");
    }

    private static void illagersHaveATotem() {
        MatchInstance match = battle(deck(BattleCard.VINDICATOR,
                BattleCard.PILLAGER, BattleCard.EVOKER, BattleCard.VEX,
                BattleCard.WITCH));
        CombatEngine engine = new CombatEngine(match);
        engine.setTribes(true);
        CombatUnit evoker = add(engine, BattleCard.EVOKER, TeamSide.BLUE, 0.0D, 3.2D);
        engine.castSpell(TeamSide.RED, BattleCard.LIGHTNING, 0.0D, 3.2D);
        boolean totem = false;
        for (int i = 0; i < 30 && !totem; i++)
            totem = engine.tick().tribeEvents().stream().anyMatch(e ->
                    e.kind() == TickResult.TribeEvent.Kind.TOTEM);
        check(totem && evoker.alive(), "Lightning would kill the Evoker; the totem saves it");
        near(evoker.health(), 1.0D, "at 1 health");
    }

    private static void netherLastFlame() {
        MatchInstance match = battle(deck(BattleCard.PIGLIN_BRUTE,
                BattleCard.BLAZE, BattleCard.GHAST, BattleCard.HOGLIN,
                BattleCard.MAGMA_CUBE));
        CombatEngine engine = new CombatEngine(match);
        engine.setTribes(true);
        CombatUnit ghast = add(engine, BattleCard.GHAST, TeamSide.BLUE, 0.0D, 3.2D);
        // A sturdy target that does not hit back, so the Ghast cannot kill it.
        CombatUnit zombie = engine.add(UUID.randomUUID(), TeamSide.RED,
                new net.susu609.royalarena.card.TroopStats(1, 500, 0.0D, 100000,
                        1.2D, 0.0D, 0.3D), 1.0D, 3.2D);
        engine.castSpell(TeamSide.RED, BattleCard.ARROW_STORM, 0.0D, 4.5D);
        boolean flame = false;
        for (int i = 0; i < 120 && !flame; i++)
            flame = engine.tick().tribeEvents().stream().anyMatch(e ->
                    e.kind() == TickResult.TribeEvent.Kind.LAST_FLAME);
        check(flame && !ghast.alive(), "the Ghast dies and leaves its last flame");
        check(zombie.burnTicks() > 0, "the enemy next to it burns");
    }

    private static void nightShadowDodgesOneShot() {
        MatchInstance match = battle(deck(BattleCard.ZOMBIE, BattleCard.CREEPER,
                BattleCard.ENDERMAN, BattleCard.SPIDER, BattleCard.WITCH));
        CombatEngine engine = new CombatEngine(match);
        engine.setTribes(true);
        CombatUnit zombie = add(engine, BattleCard.ZOMBIE, TeamSide.BLUE, 0.0D, -1.5D);
        add(engine, BattleCard.SKELETON_ARCHER, TeamSide.RED, 0.0D, 3.0D);
        int dodges = 0;
        double before = zombie.health();
        for (int i = 0; i < 60; i++) {
            dodges += (int) engine.tick().tribeEvents().stream().filter(e ->
                    e.kind() == TickResult.TribeEvent.Kind.DODGE).count();
        }
        check(dodges == 1, "one arrow misses, not more: " + dodges);
        check(zombie.health() < before, "the next arrows land");
    }

    private static void skeletonVolley() {
        MatchInstance match = battle(deck(BattleCard.SKELETON_ARCHER,
                BattleCard.SKELETONS, BattleCard.STRAY,
                BattleCard.SKELETON_RIDER, BattleCard.WITHER_SKELETON));
        CombatEngine engine = new CombatEngine(match);
        engine.setTribes(true);
        add(engine, BattleCard.SKELETON_ARCHER, TeamSide.BLUE, 0.0D, 3.0D);
        CombatUnit a = add(engine, BattleCard.GUARDIAN, TeamSide.RED, 0.0D, -1.5D);
        CombatUnit b = add(engine, BattleCard.GUARDIAN, TeamSide.RED, 1.5D, -1.5D);
        int volleys = 0;
        for (int i = 0; i < 62; i++)
            volleys += (int) engine.tick().tribeEvents().stream().filter(e ->
                    e.kind() == TickResult.TribeEvent.Kind.VOLLEY).count();
        check(volleys == 1, "every 3rd shot is a volley: one in 3 shots, " + volleys);
        check(a.health() < a.maxHealth() && b.health() < b.maxHealth(),
                "both targets were hit");
    }

    private static void beastPack() {
        List<BattleCard> wolves = deck(BattleCard.WOLVES, BattleCard.FOX,
                BattleCard.POLAR_BEAR, BattleCard.HOGLIN, BattleCard.BEE);
        TribeBonus bonus = Tribe.bonusFor(BattleCard.WOLVES, Tribe.levels(wolves));
        near(bonus.packDamage(), 0.05D, "five Beast cards: the pack bonus");
        MatchInstance match = battle(wolves);
        CombatEngine engine = new CombatEngine(match);
        engine.setTribes(true);
        for (int i = 0; i < 4; i++)
            add(engine, BattleCard.WOLVES, TeamSide.BLUE, i * 0.6D, 3.2D);
        CombatUnit dummy = engine.add(UUID.randomUUID(), TeamSide.RED,
                new net.susu609.royalarena.card.TroopStats(1, 500, 0.0D, 100000,
                        1.2D, 0.0D, 0.3D), 0.9D, 2.2D);
        TickResult first = engine.tick();
        double hit = first.attacks().stream().mapToDouble(TickResult.Attack::damage).max().orElse(0);
        near(hit, BattleCard.WOLVES.stats().damage() * 1.05D * 1.15D,
                "+5% damage, and +5% for each of 3 packmates");
        check(dummy.health() < 500.0D, "the dummy is bitten");
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
