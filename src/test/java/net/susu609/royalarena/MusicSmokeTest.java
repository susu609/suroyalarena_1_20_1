package net.susu609.royalarena;

import net.susu609.royalarena.match.ArenaSounds;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.MusicPlan;
import net.susu609.royalarena.match.MusicPlan.Track;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The mod's own music (scripts/music): the Ogg exists and is a sensible
 * size, and sounds.json maps the sound event to it.
 */
public final class MusicSmokeTest {
    private static final Path SOUNDS =
            Path.of("src/main/resources/assets/royalarena");

    private MusicSmokeTest() {
    }

    public static void run() {
        planFollowsTheMatch();
        Path first = SOUNDS.resolve("sounds/music/arena_phase1.ogg");
        if (!Files.exists(first))
            return;
        try {
            String json = Files.readString(SOUNDS.resolve("sounds.json"));
            for (String[] piece : new String[][] {
                    {ArenaSounds.PHASE1_MUSIC, "arena_phase1"},
                    {ArenaSounds.RUSH_MUSIC, "arena_rush"},
                    {ArenaSounds.LAST_STAND_MUSIC, "last_stand"}}) {
                Path track = SOUNDS.resolve("sounds/music/" + piece[1] + ".ogg");
                check(Files.exists(track), piece[1] + ".ogg exists");
                byte[] bytes = Files.readAllBytes(track);
                check(bytes.length > 100_000 && bytes.length < 3_000_000,
                        piece[1] + " is 0.1 to 3 MB: " + bytes.length);
                check(new String(bytes, 0, 4, StandardCharsets.US_ASCII)
                        .equals("OggS"), piece[1] + " is an Ogg file");
                check(json.contains("\"" + piece[0] + "\""),
                        "sounds.json defines " + piece[0]);
                check(json.contains("royalarena:music/" + piece[1]),
                        "and points at " + piece[1]);
            }
            check(json.contains("\"stream\": true") &&
                            json.contains("\"category\": \"music\""),
                    "streamed, in the music category");
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        System.out.println("RoyalArena music tests: PASS");
    }

    /** The timeline of a full match: 3:00 regulation, then 2:00 sudden death. */
    private static void planFollowsTheMatch() {
        int regulation = MatchInstance.DEFAULT_BATTLE_TICKS;      // 3:00
        int overtime = MatchInstance.OVERTIME_TICKS;              // 2:00
        check(regulation == 20 * 180 && overtime == 20 * 120,
                "a match is 3:00 and 2:00");
        check(MusicPlan.trackFor(MatchState.COUNTDOWN, regulation, false)
                == Track.PHASE1, "the countdown plays Arena March");
        // Elapsed time -> remaining ticks in regulation.
        check(plan(0) == Track.PHASE1, "0:00");
        check(plan(1 * 60 + 59) == Track.PHASE1, "1:59");
        check(plan(2 * 60) == Track.RUSH, "2:00, 60 seconds left");
        check(plan(2 * 60 + 30) == Track.RUSH, "2:30, 30 seconds left");
        check(plan(2 * 60 + 50) == Track.RUSH, "2:50, the 10 second countdown");
        check(plan(2 * 60 + 59) == Track.RUSH, "2:59");
        // Sudden death: remaining counts down from 2:00.
        for (int second : new int[] {0, 60, 110, 119})
            check(MusicPlan.trackFor(MatchState.BATTLE,
                            overtime - second * 20, true) == Track.LAST_STAND,
                    "sudden death at +" + second + " s");
        check(MusicPlan.trackFor(MatchState.FINISHED, 0, true) == null,
                "nothing after the match");
        check(Track.PHASE1.loops() && !Track.RUSH.loops() &&
                        !Track.LAST_STAND.loops(),
                "only the first piece loops");
    }

    private static Track plan(int elapsedSeconds) {
        return MusicPlan.trackFor(MatchState.BATTLE,
                MatchInstance.DEFAULT_BATTLE_TICKS - elapsedSeconds * 20,
                false);
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
