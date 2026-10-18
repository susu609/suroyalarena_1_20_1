package net.susu609.royalarena;

import net.susu609.royalarena.match.ArenaSounds;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.SoundCues;
import net.susu609.royalarena.match.SoundCues.Cue;
import net.susu609.royalarena.match.SoundCues.Frame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The match-rhythm sounds fire once each, at the right moment, and every
 * chosen sound id exists in 1.20.1.
 */
public final class SoundCuesSmokeTest {
    private static final UUID MATCH = UUID.randomUUID();

    private SoundCuesSmokeTest() {
    }

    public static void run() {
        idsExist();
        aWholeMatch();
        nothingWhenIdle();
        System.out.println("RoyalArena sound cue tests: PASS");
    }

    private static Frame frame(MatchState state, int ticks, int rate,
            boolean overtime, int tenths)
    {
        return new Frame(true, MATCH, state, ticks, rate, overtime, tenths);
    }

    /** Plays a match tick by tick and collects every cue. */
    private static void aWholeMatch() {
        List<Frame> frames = new ArrayList<>();
        for (int t = 60; t > 0; t--)
            frames.add(frame(MatchState.COUNTDOWN, t, 1, false, 50));
        // 180 s of regulation: x2 at 60 s left, elixir fills at some point.
        for (int t = 3600; t >= 0; t--) {
            int rate = t <= 1200 ? 2 : 1;
            int tenths = t > 3000 ? 50 + (3600 - t) / 10 : 100;
            frames.add(frame(MatchState.BATTLE, t, rate, false,
                    Math.min(100, tenths)));
        }
        // Overtime starts with 120 s; x3 after the first minute.
        for (int t = 2400; t >= 0; t--)
            frames.add(frame(MatchState.BATTLE, t, t <= 1200 ? 3 : 2, true,
                    100));
        frames.add(frame(MatchState.FINISHED, 0, 3, true, 100));

        List<SoundCues.Fired> all = new ArrayList<>();
        Frame before = Frame.NONE;
        for (Frame now : frames) {
            all.addAll(SoundCues.between(before, now));
            before = now;
        }
        check(count(all, Cue.MATCH_FOUND) == 1, "match found once: " + all);
        check(count(all, Cue.COUNTDOWN) == 3, "3, 2, 1: " + count(all,
                Cue.COUNTDOWN));
        check(count(all, Cue.FIGHT) == 1, "one fight horn");
        check(count(all, Cue.ELIXIR_RATE) == 2, "x2 and x3");
        check(count(all, Cue.OVERTIME) == 1, "one overtime bell");
        check(count(all, Cue.FINAL_SECOND) == 20,
                "the last 10 s of regulation and of overtime: " +
                        count(all, Cue.FINAL_SECOND));
        check(count(all, Cue.ELIXIR_FULL) == 1, "elixir full once");
        int i = indexOf(all, Cue.FIGHT);
        check(i > indexOf(all, Cue.COUNTDOWN), "the horn after the countdown");
    }

    private static void nothingWhenIdle() {
        check(SoundCues.between(Frame.NONE, Frame.NONE).isEmpty(),
                "no match, no sound");
        Frame a = frame(MatchState.BATTLE, 2000, 1, false, 40);
        check(SoundCues.between(a, a).isEmpty(), "nothing changed, silence");
    }

    private static void idsExist() {
        Path file = Path.of("src/test/resources/vanilla_sound_events_1_20_1.txt");
        if (!Files.exists(file))
            return;
        Set<String> events = new HashSet<>();
        try {
            for (String line : Files.readAllLines(file)) {
                if (!line.isBlank() && !line.startsWith("#"))
                    events.add(line.trim());
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        for (String id : ArenaSounds.ALL)
            check(events.contains(id), "no 1.20.1 sound " + id);
    }

    private static int count(List<SoundCues.Fired> all, Cue cue) {
        return (int) all.stream().filter(f -> f.cue() == cue).count();
    }

    private static int indexOf(List<SoundCues.Fired> all, Cue cue) {
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).cue() == cue)
                return i;
        }
        return -1;
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
