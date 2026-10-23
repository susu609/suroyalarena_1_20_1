package net.susu609.royalarena.sim;

import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.TeamSide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A side-by-side comparison of two cards (docs/COMPARE.md), measured with
 * the real combat engine through {@link Sim} and shown as a few rows plus
 * how each card does against the whole card pool. Pure: the console command
 * and the sandbox screen both call {@link #compare}.
 *
 * <p>Troop against troop: a duel at the bridge, an equal-elixir duel, each
 * attacking into the other defending, a lone run at a tower, a Witch behind
 * it, swarms, and the pool. A spell is measured by the share of a troop card
 * it takes off, alone and over the whole pool.</p>
 */
public final class CardCompare {
    /** Elixir ahead (see {@link Sim#push}) under which a matchup is a draw. */
    public static final double DRAW_BAND = 0.5D;
    /** Health share under which two duel results count as the same. */
    private static final double TIE_SHARE = 0.1D;
    private static final int DUEL_TICKS = 1200;
    private static final int SPELL_TICKS = 160;
    /** Cards listed as beaten and lost to. */
    private static final int LISTED = 3;
    private static final BattleCard[] SWARMS = {BattleCard.SKELETONS,
            BattleCard.WOLVES, BattleCard.SILVERFISH};

    private CardCompare() {
    }

    /** One measure: each card's value, and who leads ({@link #A}, {@link #B}, {@link #TIE}). */
    public record Row(String label, String a, String b, int lead) {
    }

    public static final int A = 1;
    public static final int B = -1;
    public static final int TIE = 0;

    /**
     * A card's standing against the pool: how many cards it beats, loses to
     * and draws with, and the {@link #LISTED} it beats and loses to most.
     */
    public record Pool(BattleCard card, int wins, int losses, int draws,
                       List<BattleCard> beats, List<BattleCard> losesTo)
    {
        public int total() {
            return wins + losses + draws;
        }
    }

    public record Report(BattleCard a, BattleCard b, List<Row> rows, Pool poolA,
                         Pool poolB, String verdict)
    {
    }

    /** Whether the card fights both ways: troops that attack and defend. */
    public static boolean fights(BattleCard card) {
        return !card.isSpell() && !card.stats().traits().buildingsOnly() &&
                !card.isBuilding() && !card.healsAround() &&
                !card.digsFlowers();
    }

    public static Report compare(BattleCard a, BattleCard b) {
        List<Row> rows = new ArrayList<>();
        if (a.isSpell() || b.isSpell())
            spellRows(a, b, rows);
        else
            troopRows(a, b, rows);
        Pool poolA = rated(a) ? pool(a) : null;
        Pool poolB = rated(b) ? pool(b) : null;
        return new Report(a, b, rows, poolA, poolB, verdict(a, b, rows));
    }

    // ------------------------------------------------------------ troops

    /** Whether the card meets troops: not a spell, a building, or a tower-only troop. */
    private static boolean rated(BattleCard card) {
        return !card.isSpell() && !card.isBuilding() &&
                !card.stats().traits().buildingsOnly();
    }

    private static void troopRows(BattleCard a, BattleCard b, List<Row> rows) {
        boolean meet = rated(a) && rated(b);
        if (!meet)
            rows.add(new Row("Duels and the pool skipped", rated(a) ? "-" :
                    "towers only", rated(b) ? "-" : "towers only", TIE));
        double[] duel = meet ? duel(a, 1, b, 1) : null;
        if (meet)
            rows.add(new Row("Duel at the bridge", percentLeft(duel[0]),
                    percentLeft(duel[1]), lead(duel[0] - duel[1], TIE_SHARE)));
        int budget = Math.max(a.cost(), b.cost());
        int na = copies(budget, a);
        int nb = copies(budget, b);
        if (meet && (na != 1 || nb != 1)) {
            double[] equal = duel(a, na, b, nb);
            rows.add(new Row("Same elixir (" + na + " " + a.displayName() +
                    " vs " + nb + " " + b.displayName() + ")",
                    percentLeft(equal[0]), percentLeft(equal[1]),
                    lead(equal[0] - equal[1], TIE_SHARE)));
        }
        double aAttacks = Sim.push(a, b);
        double bAttacks = Sim.push(b, a);
        rows.add(new Row("Attacking into the other (elixir ahead)",
                signed(aAttacks), signed(bAttacks),
                lead(aAttacks - bAttacks, DRAW_BAND)));
        Sim.Solo soloA = Sim.solo(a);
        Sim.Solo soloB = Sim.solo(b);
        double perA = soloA.towerDamage() / (double) a.cost();
        double perB = soloB.towerDamage() / (double) b.cost();
        rows.add(new Row("Alone at a tower (tower health per elixir)",
                solo(soloA, perA), solo(soloB, perB), lead(perA - perB, 1.0D)));
        BattleCard helper = a == BattleCard.WITCH || b == BattleCard.WITCH ?
                BattleCard.SKELETON_ARCHER : BattleCard.WITCH;
        double helpA = Sim.pushWith(a, helper, b) - aAttacks;
        double helpB = Sim.pushWith(b, helper, a) - bAttacks;
        rows.add(new Row(helper.displayName() + " behind it (gain)",
                signed(helpA), signed(helpB), lead(helpA - helpB, DRAW_BAND)));
        double swarmA = swarmAttack(a);
        double swarmB = swarmAttack(b);
        rows.add(new Row("Attacking into swarms", signed(swarmA),
                signed(swarmB), lead(swarmA - swarmB, DRAW_BAND)));
        double defA = swarmDefence(a);
        double defB = swarmDefence(b);
        rows.add(new Row("Defending against swarms", signed(defA),
                signed(defB), lead(defA - defB, DRAW_BAND)));
    }

    private static int copies(int budget, BattleCard card) {
        return Math.max(1, Math.min(5,
                (int) Math.round(budget / (double) card.cost())));
    }

    /**
     * {@code na} copies of {@code a} against {@code nb} of {@code b} at the
     * bridge, each side in turn on BLUE's half, averaged; the share of each
     * side's health left after {@link #DUEL_TICKS}.
     */
    private static double[] duel(BattleCard a, int na, BattleCard b, int nb) {
        double[] first = duelOnce(a, na, b, nb, TeamSide.BLUE);
        double[] second = duelOnce(b, nb, a, na, TeamSide.BLUE);
        return new double[] {(first[0] + second[1]) / 2.0D,
                (first[1] + second[0]) / 2.0D};
    }

    /** {@code x} on BLUE's side (z 2) against {@code y} on RED's (z -3). */
    private static double[] duelOnce(BattleCard x, int nx, BattleCard y,
            int ny, TeamSide xSide)
    {
        MatchInstance match = Sim.battle();
        CombatEngine engine = new CombatEngine(match);
        field(engine, match, x, nx, xSide, xSide == TeamSide.BLUE ? 2.0D : -3.0D);
        field(engine, match, y, ny, xSide.opponent(),
                xSide == TeamSide.BLUE ? -3.0D : 2.0D);
        for (int tick = 0; tick < DUEL_TICKS; tick++) {
            Sim.step(engine, match.arena());
            if (match.state() != MatchState.BATTLE ||
                    Sim.healthLeft(engine, TeamSide.BLUE) <= 0.0D ||
                    Sim.healthLeft(engine, TeamSide.RED) <= 0.0D)
                break;
        }
        return new double[] {kept(engine, xSide, x, nx),
                kept(engine, xSide.opponent(), y, ny)};
    }

    private static void field(CombatEngine engine, MatchInstance match,
            BattleCard card, int copies, TeamSide side, double z)
    {
        for (int i = 0; i < copies; i++)
            Sim.spawn(engine, match, card, side,
                    -6.0D + (i - (copies - 1) / 2.0D) * 1.6D, z);
    }

    private static double kept(CombatEngine engine, TeamSide side,
            BattleCard card, int copies)
    {
        return Math.min(1.0D, Sim.healthLeft(engine, side) /
                (Sim.cardHealth(card) * copies));
    }

    private static double swarmAttack(BattleCard card) {
        double sum = 0.0D;
        for (BattleCard swarm : SWARMS)
            sum += Sim.push(card, swarm);
        return sum / SWARMS.length;
    }

    private static double swarmDefence(BattleCard card) {
        double sum = 0.0D;
        for (BattleCard swarm : SWARMS)
            sum -= Sim.push(swarm, card);
        return sum / SWARMS.length;
    }

    // -------------------------------------------------------------- pool

    /**
     * Each fighting card against the pool: attacking into it and defending
     * against it, half the difference of the two. Over {@link #DRAW_BAND}
     * beats it, under minus that loses to it.
     */
    public static Pool pool(BattleCard card) {
        record Score(BattleCard other, double value) {
        }
        List<Score> scores = new ArrayList<>();
        for (BattleCard other : BattleCard.values()) {
            if (other == card || !fights(other))
                continue;
            scores.add(new Score(other,
                    (Sim.push(card, other) - Sim.push(other, card)) / 2.0D));
        }
        int wins = 0;
        int losses = 0;
        int draws = 0;
        for (Score score : scores) {
            if (score.value() > DRAW_BAND)
                wins++;
            else if (score.value() < -DRAW_BAND)
                losses++;
            else
                draws++;
        }
        scores.sort((x, y) -> Double.compare(y.value(), x.value()));
        List<BattleCard> beats = new ArrayList<>();
        List<BattleCard> losesTo = new ArrayList<>();
        for (int i = 0; i < Math.min(LISTED, scores.size()); i++) {
            Score best = scores.get(i);
            Score worst = scores.get(scores.size() - 1 - i);
            if (best.value() > DRAW_BAND)
                beats.add(best.other());
            if (worst.value() < -DRAW_BAND)
                losesTo.add(worst.other());
        }
        return new Pool(card, wins, losses, draws, beats, losesTo);
    }

    // ------------------------------------------------------------ spells

    private static void spellRows(BattleCard a, BattleCard b, List<Row> rows) {
        if (a.isSpell() && b.isSpell()) {
            double[] va = spellPool(a);
            double[] vb = spellPool(b);
            rows.add(new Row("Avg health taken off a card (all troops)",
                    percent(va[0]), percent(vb[0]), lead(va[0] - vb[0], 0.03D)));
            rows.add(new Row("... per elixir", percent(va[0] / a.cost()),
                    percent(vb[0] / b.cost()),
                    lead(va[0] / a.cost() - vb[0] / b.cost(), 0.01D)));
            rows.add(new Row("Cards it destroys outright",
                    String.valueOf((int) va[1]), String.valueOf((int) vb[1]),
                    lead(va[1] - vb[1], 0.5D)));
            rows.add(new Row("Avg stop on a troop (s)", seconds(va[2]),
                    seconds(vb[2]), lead(va[2] - vb[2], 0.2D)));
            return;
        }
        boolean spellIsA = a.isSpell();
        BattleCard spell = spellIsA ? a : b;
        BattleCard troop = spellIsA ? b : a;
        double[] hit = spellOn(spell, troop);
        double trade = hit[0] * troop.cost() - spell.cost();
        double[] pool = spellPool(spell);
        int spellLead = spellIsA ? A : B;
        rows.add(spellRow(spellIsA, spell.displayName() + " on " +
                troop.displayName() + " (health taken off)", percent(hit[0]),
                TIE));
        rows.add(spellRow(spellIsA, "Elixir traded (its share of the " +
                troop.displayName() + "'s cost minus the spell's)",
                signed(trade), trade > DRAW_BAND ? spellLead :
                        trade < -DRAW_BAND ? -spellLead : TIE));
        rows.add(spellRow(spellIsA, "Stops " + troop.displayName() + " (s)",
                seconds(hit[2]), TIE));
        rows.add(spellRow(spellIsA, spell.displayName() +
                " on the whole pool (avg health taken off)", percent(pool[0]),
                TIE));
    }

    /** A row with the spell's number in the spell's column, "-" in the other. */
    private static Row spellRow(boolean spellIsA, String label, String value,
            int lead)
    {
        return new Row(label, spellIsA ? value : "-", spellIsA ? "-" : value,
                lead);
    }

    /**
     * One cast of {@code spell} on a still {@code troop} card: the share of
     * its health taken off, whether it died, and the longest stop in seconds.
     */
    private static double[] spellOn(BattleCard spell, BattleCard troop) {
        MatchInstance match = Sim.battle();
        CombatEngine engine = new CombatEngine(match);
        Sim.spawn(engine, match, troop, TeamSide.RED, 0.0D, -5.0D);
        double x = 0.0D;
        double z = 0.0D;
        for (CombatUnit unit : engine.units()) {
            x += unit.x() / engine.units().size();
            z += unit.z() / engine.units().size();
        }
        engine.castSpell(TeamSide.BLUE, spell, x, z);
        double health = Sim.cardHealth(troop);
        int stopped = 0;
        for (int tick = 0; tick < SPELL_TICKS; tick++) {
            engine.tick();
            for (CombatUnit unit : engine.units())
                stopped = Math.max(stopped, unit.frozenTicks());
        }
        double left = Math.min(1.0D, Sim.healthLeft(engine, TeamSide.RED) /
                health);
        return new double[] {1.0D - left, left <= 0.0D ? 1.0D : 0.0D, stopped / 20.0D};
    }

    /** {@link #spellOn} averaged over the fighting troops: share, kills, stop. */
    private static double[] spellPool(BattleCard spell) {
        double share = 0.0D;
        double stop = 0.0D;
        int kills = 0;
        int n = 0;
        for (BattleCard troop : BattleCard.values()) {
            if (!fights(troop))
                continue;
            double[] hit = spellOn(spell, troop);
            share += hit[0];
            kills += (int) hit[1];
            stop += hit[2];
            n++;
        }
        return new double[] {share / n, kills, stop / n};
    }

    // ----------------------------------------------------------- verdict

    private static String verdict(BattleCard a, BattleCard b, List<Row> rows) {
        if (a.isSpell() != b.isSpell())
            return "A spell and a troop are not rivals: the rows show what the spell does to the troop.";
        int ahead = 0;
        int behind = 0;
        for (Row row : rows) {
            if (row.lead() == A)
                ahead++;
            else if (row.lead() == B)
                behind++;
        }
        return twoWay(a, b, ahead, behind);
    }

    private static String twoWay(BattleCard a, BattleCard b, int ahead,
            int behind)
    {
        int rated = ahead + behind;
        if (ahead == behind)
            return "Even: " + a.displayName() + " leads " + ahead +
                    ", " + b.displayName() + " leads " + behind + ".";
        BattleCard lead = ahead > behind ? a : b;
        return lead.displayName() + " leads " + Math.max(ahead, behind) +
                " of " + rated + " measures that have a leader.";
    }

    // -------------------------------------------------------------- text

    /** The report as plain lines, for chat and the console. */
    public static List<String> format(Report report) {
        List<String> out = new ArrayList<>();
        out.add(report.a().displayName() + " (" + report.a().cost() + ") vs " +
                report.b().displayName() + " (" + report.b().cost() + ")");
        for (Row row : report.rows()) {
            String mark = row.lead() == A ? " [A]" : row.lead() == B ? " [B]" : "";
            out.add(row.label() + ": " + row.a() + " | " + row.b() + mark);
        }
        for (Pool pool : new Pool[] {report.poolA(), report.poolB()}) {
            if (pool == null)
                continue;
            out.add(pool.card().displayName() + " vs " + pool.total() +
                    " cards: beats " + pool.wins() + ", loses to " +
                    pool.losses() + ", draws " + pool.draws());
            if (!pool.beats().isEmpty())
                out.add("  beats most: " + names(pool.beats()));
            if (!pool.losesTo().isEmpty())
                out.add("  loses most to: " + names(pool.losesTo()));
        }
        out.add(report.verdict());
        return out;
    }

    private static String names(List<BattleCard> cards) {
        return String.join(", ", cards.stream()
                .map(BattleCard::displayName).toList());
    }

    private static String percentLeft(double share) {
        return String.format(Locale.ROOT, "%.0f%% left", share * 100.0D);
    }

    private static String percent(double share) {
        return String.format(Locale.ROOT, "%.0f%%", share * 100.0D);
    }

    private static String seconds(double s) {
        return String.format(Locale.ROOT, "%.1f", s);
    }

    private static String signed(double value) {
        return String.format(Locale.ROOT, "%+.1f", value);
    }

    private static String solo(Sim.Solo solo, double perElixir) {
        String reach = solo.reachSeconds() < 0.0D ? "never arrives" :
                String.format(Locale.ROOT, "arrives %.0fs", solo.reachSeconds());
        return String.format(Locale.ROOT, "%.1f (%s)", perElixir, reach);
    }

    private static int lead(double difference, double band) {
        return difference > band ? A : difference < -band ? B : TIE;
    }

    /** Finds a card by id or display name (any case), or null. */
    public static BattleCard find(String text) {
        String t = text.trim().toLowerCase(Locale.ROOT).replace("'", "")
                .replace(' ', '_');
        for (BattleCard card : BattleCard.values()) {
            if (card.id().equals(t))
                return card;
        }
        return null;
    }
}
