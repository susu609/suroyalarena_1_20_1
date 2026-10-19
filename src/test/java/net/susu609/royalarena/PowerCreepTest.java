package net.susu609.royalarena;

import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.CostFormula;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Keeps the cards inside a power band, so a new card or a stat change that
 * is too strong (or useless) fails the build instead of slipping into the
 * game. Measurements come from {@link Sim}:
 * <ul>
 * <li>troops that fight: the push score, the elixir a card comes out
 * ahead on average when it attacks across the bridge into each other card
 * defending under its tower, and when it defends against each;</li>
 * <li>troops that only hit towers: tower health taken per elixir when they
 * walk in alone (they need support against defenders, so pushes alone would
 * call them weak);</li>
 * <li>spells: cost formula value per elixir paid.</li>
 * </ul>
 */
public final class PowerCreepTest {
    /** Strongest a fighting troop may be (elixir ahead per fight). */
    public static final double MAX_SCORE = 1.1D;
    /** Weakest before a card counts as dead weight. */
    public static final double MIN_SCORE = -1.2D;
    /** Tower health per elixir for troops that only hit towers. */
    public static final double MIN_SIEGE = 12.0D;
    public static final double MAX_SIEGE = 25.0D;
    /** Spell value per elixir paid. */
    public static final double MIN_SPELL = 0.9D;
    public static final double MAX_SPELL = 1.12D;

    private PowerCreepTest() {
    }

    public static void run() {
        List<String> problems = new ArrayList<>();
        StringBuilder table = new StringBuilder();
        for (BattleCard card : BattleCard.values()) {
            String line;
            if (card.isSpell()) {
                double value = CostFormula.raw(card.spell()) / card.cost();
                line = String.format(Locale.ROOT, "%-16s spell value/elixir %.2f",
                        card.id(), value);
                if (value < MIN_SPELL || value > MAX_SPELL)
                    problems.add(line);
            } else if (card.isBuilding()) {
                // A building cannot attack across the bridge: it is
                // measured only on defence.
                double score = defendScore(card);
                line = String.format(Locale.ROOT, "%-16s defend score %+.2f",
                        card.id(), score);
                if (score < MIN_SCORE || score > MAX_SCORE)
                    problems.add(line);
            } else if (card.stats().traits().buildingsOnly()) {
                double siege = Sim.soloTowerDamage(card) / (double) card.cost();
                line = String.format(Locale.ROOT, "%-16s tower damage/elixir %.1f",
                        card.id(), siege);
                if (siege < MIN_SIEGE || siege > MAX_SIEGE)
                    problems.add(line);
            } else {
                double score = pushScore(card);
                line = String.format(Locale.ROOT, "%-16s push score %+.2f",
                        card.id(), score);
                if (score < MIN_SCORE || score > MAX_SCORE)
                    problems.add(line);
            }
            table.append("  ").append(line).append('\n');
        }
        if (!problems.isEmpty())
            throw new AssertionError("cards outside the power band:\n  " +
                    String.join("\n  ", problems) + "\nall cards:\n" + table);
        System.out.println("RoyalArena power creep tests: PASS");
    }

    /** Elixir a building comes out ahead defending against each fighting troop. */
    static double defendScore(BattleCard card) {
        double defend = 0.0D;
        int fights = 0;
        for (BattleCard other : BattleCard.values()) {
            if (other.isSpell() || other.stats().traits().buildingsOnly() ||
                    other.isBuilding())
                continue;
            defend -= Sim.push(other, card);
            fights++;
        }
        return defend / fights;
    }

    /** Average of attacking into and defending against every fighting troop. */
    static double pushScore(BattleCard card) {
        double attack = 0.0D;
        double defend = 0.0D;
        int fights = 0;
        for (BattleCard other : BattleCard.values()) {
            if (other.isSpell() || other.stats().traits().buildingsOnly() ||
                    other.isBuilding())
                continue;
            attack += Sim.push(card, other);
            defend -= Sim.push(other, card);
            fights++;
        }
        return (attack + defend) / (2.0D * fights);
    }
}
