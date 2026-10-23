package net.susu609.royalarena;

import net.susu609.royalarena.sim.Sim;
import net.susu609.royalarena.bot.BotDeck;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.card.Tribe;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.stats.Json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * What the tribe steps are worth (docs/TRIBES.md): for every tribe, bot
 * decks built around 2 to 6 of its cards (as many as it has) play plain random
 * decks, each match twice with the sides swapped, once with tribe bonuses
 * and once without. The gap is what the bonus adds; the "on" rate is how
 * the tribe deck does in the game.
 *
 * <p>Arguments: matches per tribe and size (default 200), JSON output file
 * (optional, for the card pages).</p>
 */
public final class TribeReport {
    private TribeReport() {
    }

    public static void main(String[] args) throws IOException {
        int matches = args.length > 0 ? Integer.parseInt(args[0]) : 200;
        List<Object> rows = new ArrayList<>();
        System.out.println("tribe      cards  off    on   gain");
        for (Tribe tribe : Tribe.values()) {
            int size = tribe.members().size();
            for (int core : new int[] {2, 3, 4, 5, 6}) {
                // A deck needs a spell and a tank, so a full tribe deck of 8
                // is for players, not the bot's balanced decks.
                if (size < core || BotDeck.around(tribe, core,
                        new Random(1L)) == null)
                    continue;
                double off = rate(tribe, core, matches, false);
                double on = rate(tribe, core, matches, true);
                System.out.printf(Locale.ROOT, "%-10s %4d  %4.0f%% %4.0f%% %+5.0f%n",
                        tribe.id(), core, off * 100, on * 100, (on - off) * 100);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("tribe", tribe.id());
                row.put("cards", core);
                row.put("off", Math.round(off * 100));
                row.put("on", Math.round(on * 100));
                rows.add(row);
            }
        }
        if (args.length > 1) {
            List<Object> tribes = new ArrayList<>();
            for (Tribe tribe : Tribe.values()) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("id", tribe.id());
                t.put("label", tribe.label());
                t.put("labelVi", tribe.labelVi());
                List<Object> members = new ArrayList<>();
                for (BattleCard card : tribe.members())
                    members.add(card.id());
                t.put("members", members);
                List<Object> steps = new ArrayList<>();
                for (Tribe.Step step : tribe.steps())
                    steps.add(step.text());
                t.put("steps", steps);
                tribes.add(t);
            }
            Map<String, Object> all = new LinkedHashMap<>();
            all.put("matches", matches);
            all.put("tribes", tribes);
            all.put("rows", rows);
            Path out = Path.of(args[1]);
            Files.createDirectories(out.toAbsolutePath().getParent());
            Files.writeString(out, Json.write(all));
        }
    }

    /** Win rate of decks around {@code tribe} against plain decks. */
    static double rate(Tribe tribe, int core, int matches, boolean tribes) {
        double score = 0.0D;
        Map<BattleCard, Integer> plays = new HashMap<>();
        for (int i = 0; i < matches; i++) {
            Random random = new Random(tribe.ordinal() * 100003L + core * 7919L + i / 2);
            BattleDeck mine = BotDeck.around(tribe, core, random);
            BattleDeck theirs = BotDeck.plain(random);
            if (mine == null)
                continue;
            boolean blue = i % 2 == 0;
            Sim.Outcome o = Sim.botMatch(blue ? mine : theirs,
                    blue ? theirs : mine, 5000L + i, plays, tribes);
            TeamSide me = blue ? TeamSide.BLUE : TeamSide.RED;
            score += o.winner() == null ? 0.5D : o.winner() == me ? 1.0D : 0.0D;
        }
        return score / matches;
    }
}
