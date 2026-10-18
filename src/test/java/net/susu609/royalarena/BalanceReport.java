package net.susu609.royalarena;

import net.susu609.royalarena.bot.BotDeck;
import net.susu609.royalarena.bot.BotPersonality;
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

    /** One card over all the matches its deck played. */
    static final class Tally {
        int games;
        double wins;
        int plays;

        double rate() {
            return games == 0 ? 0.0D : wins / games;
        }

        /** Half the 95% interval of {@link #rate()}. */
        double margin() {
            double r = rate();
            return games == 0 ? 0.0D :
                    1.96D * Math.sqrt(r * (1.0D - r) / games);
        }
    }

    /** Everything a run of bot matches found. */
    static final class Result {
        final Map<BattleCard, Tally> cards = new EnumMap<>(BattleCard.class);
        final Map<BotPersonality, Tally> personalities =
                new EnumMap<>(BotPersonality.class);
        int matches;
        int blueWins;
        int draws;
        int overtimes;
        int kings;
        long ticks;

        int redWins() {
            return matches - blueWins - draws;
        }

        double averageSeconds() {
            return matches == 0 ? 0.0D : ticks / 20.0D / matches;
        }
    }

    public static void main(String[] args) throws IOException {
        int matches = args.length > 0 ? Integer.parseInt(args[0]) : 200;
        Path out = Path.of(args.length > 1 ? args[1] :
                "docs/BALANCE_REPORT.md");
        long started = System.currentTimeMillis();
        Result r = play(matches);
        long seconds = (System.currentTimeMillis() - started) / 1000L;
        StringBuilder styles = new StringBuilder("\nBot personalities " +
                "(wins over games, draws count half):");
        r.personalities.forEach((p, t) -> styles.append(String.format(
                Locale.ROOT, " %s %.0f%%;", p.displayName(),
                t.games == 0 ? 0.0D : 100.0D * t.wins / t.games)));
        String report = report(r.cards, matches, r.blueWins, r.draws) +
                styles + "\n" +
                String.format(Locale.ROOT, "%nAverage match %.0f s; %d went " +
                        "to overtime; %d ended with a King tower down.%n",
                        r.averageSeconds(), r.overtimes, r.kings);
        System.out.print(report);
        System.out.println("Took " + seconds + " s.");
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.writeString(out, report);
        System.out.println("Written to " + out.toAbsolutePath());
    }

    /**
     * Plays {@code matches} bot-versus-bot matches with random balanced
     * decks, every pairing twice with the sides swapped.
     */
    static Result play(int matches) {
        Result result = new Result();
        result.matches = matches;
        Map<BattleCard, Tally> cards = result.cards;
        for (BattleCard card : BattleCard.values())
            cards.put(card, new Tally());
        for (int i = 0; i < matches; i++) {
            int pair = i / 2;
            // Plain decks, so each card is measured on its own; tribes are
            // measured by TribeReport.
            BattleDeck a = BotDeck.plain(new Random(pair * 2L + 1L));
            BattleDeck b = BotDeck.plain(new Random(pair * 2L + 2L));
            // Each pairing twice, with the sides swapped.
            BattleDeck blue = i % 2 == 0 ? a : b;
            BattleDeck red = i % 2 == 0 ? b : a;
            Map<BattleCard, Integer> plays = new EnumMap<>(BattleCard.class);
            Sim.Outcome outcome = Sim.botMatch(blue, red, 1000L + i, plays);
            TeamSide winner = outcome.winner();
            result.ticks += outcome.ticks();
            result.overtimes += outcome.overtime() ? 1 : 0;
            result.kings += outcome.kingFell() ? 1 : 0;
            if (winner == TeamSide.BLUE)
                result.blueWins++;
            if (winner == null)
                result.draws++;
            count(cards, blue, winner == TeamSide.BLUE ? 1.0D :
                    winner == null ? 0.5D : 0.0D);
            count(cards, red, winner == TeamSide.RED ? 1.0D :
                    winner == null ? 0.5D : 0.0D);
            plays.forEach((card, n) -> cards.get(card).plays += n);
            tallyStyle(result, outcome.blue(), winner == TeamSide.BLUE ? 1.0D :
                    winner == null ? 0.5D : 0.0D);
            tallyStyle(result, outcome.red(), winner == TeamSide.RED ? 1.0D :
                    winner == null ? 0.5D : 0.0D);
        }
        return result;
    }

    private static void tallyStyle(Result result, BotPersonality style,
            double score)
    {
        Tally t = result.personalities.computeIfAbsent(style,
                k -> new Tally());
        t.games++;
        t.wins += score;
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
        rows.sort((p, q) -> Double.compare(q.getValue().rate(),
                p.getValue().rate()));
        for (Map.Entry<BattleCard, Tally> row : rows) {
            Tally t = row.getValue();
            double rate = t.rate();
            double margin = t.margin();
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
}
