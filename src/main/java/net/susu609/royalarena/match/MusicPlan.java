package net.susu609.royalarena.match;

/**
 * Which battle music plays when (docs/MUSIC.md). A full match is 3:00 of
 * regulation and 2:00 of sudden death; each piece is exactly as long as its
 * phase and starts when the phase does:
 *
 * <pre>
 *  0:00 - 2:00  PHASE1      Arena March, looped (x1 elixir)
 *  2:00 - 3:00  RUSH        Arena Rush (x2 elixir; 30 s left at 2:30, 10 s at 2:50)
 *  3:00 - 5:00  LAST_STAND  Last Stand (sudden death; 60 s left at 4:00, 10 s at 4:50)
 * </pre>
 *
 * The countdown before the battle plays PHASE1. Pure Java, so the smoke
 * test can check it.
 */
public final class MusicPlan {
    /** The last minute of regulation: elixir is double and Arena Rush plays. */
    public static final int RUSH_TICKS_LEFT = 20 * 60;

    public enum Track {
        PHASE1(true), RUSH(false), LAST_STAND(false);

        private final boolean loops;

        Track(boolean loops) {
            this.loops = loops;
        }

        /** Whether it repeats (the others play once, as long as their phase). */
        public boolean loops() {
            return loops;
        }
    }

    private MusicPlan() {
    }

    /** The track for this moment of a match, or null when none should play. */
    public static Track trackFor(MatchState state, int ticksRemaining,
            boolean overtime)
    {
        if (state == MatchState.COUNTDOWN)
            return Track.PHASE1;
        if (state != MatchState.BATTLE)
            return null;
        if (overtime)
            return Track.LAST_STAND;
        return ticksRemaining <= RUSH_TICKS_LEFT ? Track.RUSH : Track.PHASE1;
    }
}
