package net.susu609.royalarena;

import net.susu609.royalarena.bot.BotDeck;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.match.TeamSide;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Bot-versus-bot balance report ({@code gradlew balanceReport}): plays
 * random balanced bot decks against each other, every pairing twice with the
 * sides swapped, and writes how often each card's deck wins and how often
 * the bots play it to {@code docs/BALANCE_REPORT.md}. A card far above 50%
 * makes the decks that hold it win; far below, they lose.
 *
 * <p>Arguments: number of matches (default 200), output file.</p>
 */
public final class BalanceReport {
    private BalanceReport() {
    }

    private static final class Tally {
        int games;
        double wins;
        int plays;
    }

    public static void main(String[] args) throws IOException {
        int matches = args.length > 0 ? Integer.parseInt(args[0]) : 200;
        Path out = Path.of(args.length > 1 ? args[1] :
                "docs/BALANCE_REPORT.md");
        Map<BattleCard, Tally> cards = new EnumMap<>(BattleCard.class);
        for (BattleCard card : BattleCard.values())
            cards.put(card, new Tally());
        int blueWins = 0;
        int draws = 0;
        int overtimes = 0;
        int kings = 0;
        long ticks = 0L;
        long started = System.currentTimeMillis();
        for (int i = 0; i < matches; i++) {
            int pair = i / 2;
            BattleDeck a = BotDeck.random(new Random(pair * 2L + 1L));
            BattleDeck b = BotDeck.random(new Random(pair * 2L + 2L));
            // Each pairing twice, with the sides swapped.
            BattleDeck blue = i % 2 == 0 ? a : b;
            BattleDeck red = i % 2 == 0 ? b : a;
            Map<BattleCard, Integer> plays = new EnumMap<>(BattleCard.class);
            Sim.Outcome outcome = Sim.botMatch(blue, red, 1000L + i, plays);
            TeamSide winner = outcome.winner();
            ticks += outcome.ticks();
            overtimes += outcome.overtime() ? 1 : 0;
            kings += outcome.kingFell() ? 1 : 0;
            if (winner == TeamSide.BLUE)
                blueWins++;
            if (winner == null)
                draws++;
            count(cards, blue, winner == TeamSide.BLUE ? 1.0D :
                    winner == null ? 0.5D : 0.0D);
            count(cards, red, winner == TeamSide.RED ? 1.0D :
                    winner == null ? 0.5D : 0.0D);
            plays.forEach((card, n) -> cards.get(card).plays += n);
        }
        long seconds = (System.currentTimeMillis() - started) / 1000L;
        String report = report(cards, matches, blueWins, draws) +
                String.format(Locale.ROOT, "%nAverage match %.0f s; %d went " +
                        "to overtime; %d ended with a King tower down.%n",
                        ticks / 20.0D / matches, overtimes, kings);
        System.out.print(report);
        System.out.println("Took " + seconds + " s.");
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.writeString(out, report);
        System.out.println("Written to " + out.toAbsolutePath());
    }

    private static void count(Map<BattleCard, Tally> cards, BattleDeck deck,
            double result)
    {
        for (BattleCard card : deck.cards()) {
            Tally t = cards.get(card);
            t.games++;
            t.wins += result;
        }
    }

    private static String report(Map<BattleCard, Tally> cards, int matches,
            int blueWins, int draws)
    {
        StringBuilder s = new StringBuilder();
        s.append("# Balance report\n\n");
        s.append(String.format(Locale.ROOT, "%d bot-versus-bot matches with " +
                "random balanced decks (each pairing twice, sides swapped), " +
                "BLUE won %d, RED %d, %d draws. Unit ids are random, so " +
                "two runs differ by a match or two.%n%n", matches, blueWins,
                matches - blueWins - draws, draws));
        s.append("Win rate: share of the games the card's deck won (a draw " +
                "counts half). The margin is the 95% interval; a card is " +
                "flagged when the whole interval is past 40-60%.\n\n");
        s.append("| Card | Elixir | Decks | Win rate | Margin | Played per " +
                "game | |\n|---|---|---|---|---|---|---|\n");
        List<Map.Entry<BattleCard, Tally>> rows =
                new ArrayList<>(cards.entrySet());
        rows.removeIf(e -> e.getValue().games == 0);
        rows.sort((p, q) -> Double.compare(rate(q.getValue()),
                rate(p.getValue())));
        for (Map.Entry<BattleCard, Tally> row : rows) {
            Tally t = row.getValue();
            double rate = rate(t);
            double margin = 1.96D * Math.sqrt(rate * (1.0D - rate) / t.games);
            String flag = rate - margin > 0.6D ? "strong" :
                    rate + margin < 0.4D ? "weak" : "";
            s.append(String.format(Locale.ROOT,
                    "| %s | %d | %d | %.0f%% | +/-%.0f%% | %.1f | %s |%n",
                    row.getKey().displayName(), row.getKey().cost(), t.games,
                    rate * 100.0D, margin * 100.0D,
                    t.plays / (double) t.games, flag));
        }
        int missing = 0;
        for (Tally t : cards.values())
            missing += t.games == 0 ? 1 : 0;
        if (missing > 0)
            s.append(String.format(Locale.ROOT, "%n%d cards never made a bot " +
                    "deck.%n", missing));
        return s.toString();
    }

    private static double rate(Tally t) {
        return t.games == 0 ? 0.0D : t.wins / t.games;
    }
}
