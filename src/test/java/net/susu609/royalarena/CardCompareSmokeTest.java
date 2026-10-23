package net.susu609.royalarena;

import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.sim.CardCompare;
import net.susu609.royalarena.sim.CardCompare.Report;
import net.susu609.royalarena.sim.CardCompare.Row;

import java.util.List;

/**
 * The two-card comparison (docs/COMPARE.md): a card ties with itself, the
 * rows mirror when the cards swap, a spell is measured on a troop, a
 * tower-only troop skips the duels, and the pool lists stay short.
 */
public final class CardCompareSmokeTest {
    private CardCompareSmokeTest() {
    }

    public static void run() {
        findsCards();
        aCardTiesWithItself();
        swappingMirrorsTheRows();
        spellsAreMeasuredOnTroops();
        towerOnlyTroopsSkipTheDuels();
        poolListsAreShort();
        System.out.println("RoyalArena card compare tests: PASS");
    }

    private static void findsCards() {
        check(CardCompare.find("Dragon's Breath") == BattleCard.DRAGONS_BREATH,
                "a display name finds the card");
        check(CardCompare.find("iron_golem") == BattleCard.IRON_GOLEM,
                "an id finds the card");
        check(CardCompare.find("no such card") == null, "unknown: null");
    }

    private static void aCardTiesWithItself() {
        Report report = CardCompare.compare(BattleCard.PILLAGER,
                BattleCard.PILLAGER);
        for (Row row : report.rows())
            check(row.lead() == CardCompare.TIE, "same card, tie: " + row.label());
    }

    private static void swappingMirrorsTheRows() {
        Report ab = CardCompare.compare(BattleCard.GIANT, BattleCard.IRON_GOLEM);
        Report ba = CardCompare.compare(BattleCard.IRON_GOLEM, BattleCard.GIANT);
        check(ab.rows().size() == ba.rows().size(), "same rows both ways");
        for (int i = 0; i < ab.rows().size(); i++) {
            check(ab.rows().get(i).lead() == -ba.rows().get(i).lead(),
                    "mirrored lead: " + ab.rows().get(i).label());
            check(ab.rows().get(i).a().equals(ba.rows().get(i).b()),
                    "mirrored value: " + ab.rows().get(i).label());
        }
    }

    private static void spellsAreMeasuredOnTroops() {
        Report report = CardCompare.compare(BattleCard.FIREBALL,
                BattleCard.WITCH);
        check(report.poolA() == null && report.poolB() != null,
                "only the troop has a pool");
        check(report.rows().get(0).a().endsWith("%") &&
                report.rows().get(0).b().equals("-"),
                "the spell's number sits in the spell's column");
        Report swapped = CardCompare.compare(BattleCard.WITCH,
                BattleCard.FIREBALL);
        check(swapped.rows().get(0).b().equals(report.rows().get(0).a()),
                "and in the other column when swapped");
        Report two = CardCompare.compare(BattleCard.ZAP, BattleCard.FIREBALL);
        check(two.rows().stream().anyMatch(r ->
                r.label().contains("per elixir")), "spell against spell");
    }

    private static void towerOnlyTroopsSkipTheDuels() {
        Report report = CardCompare.compare(BattleCard.HOGLIN,
                BattleCard.ZOMBIE);
        check(report.poolA() == null && report.poolB() != null,
                "a tower-only troop has no pool");
        check(report.rows().stream().noneMatch(r ->
                r.label().startsWith("Duel at")), "and no duel");
    }

    private static void poolListsAreShort() {
        Report report = CardCompare.compare(BattleCard.ZOMBIE,
                BattleCard.SKELETONS);
        for (CardCompare.Pool pool : List.of(report.poolA(), report.poolB())) {
            check(pool.beats().size() <= 3 && pool.losesTo().size() <= 3,
                    "at most three cards listed each way");
            check(pool.wins() + pool.losses() + pool.draws() > 20,
                    "against the whole pool");
        }
        List<String> lines = CardCompare.format(report);
        check(lines.size() < 20, "the report stays short: " + lines.size());
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
