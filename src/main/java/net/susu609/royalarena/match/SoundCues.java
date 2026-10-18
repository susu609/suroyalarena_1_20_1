package net.susu609.royalarena.match;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Which match-rhythm sounds to play between two looks at the match (the
 * client compares the snapshot it had with the one it has now): a match
 * found, each second of the countdown, the fight horn, faster elixir,
 * overtime, the last ten seconds and the elixir bar filling up. Pure Java so
 * the rules can be tested.
 */
public final class SoundCues {
    private SoundCues() {
    }

    public enum Cue {
        MATCH_FOUND, COUNTDOWN, FIGHT, ELIXIR_RATE, OVERTIME, FINAL_SECOND,
        ELIXIR_FULL
    }

    /** What the cues look at in a match snapshot. */
    public record Frame(boolean active, UUID matchId, MatchState state,
                        int ticksRemaining, int elixirRate, boolean overtime,
                        int elixirTenths)
    {
        public static final Frame NONE = new Frame(false, null,
                MatchState.WAITING, 0, 1, false, 0);

        /** Whole seconds left, counting a started second as a whole one. */
        public int seconds() {
            return (Math.max(0, ticksRemaining) + 19) / 20;
        }
    }

    /** One cue, with the seconds left for the ticking ones. */
    public record Fired(Cue cue, int seconds) {
    }

    public static List<Fired> between(Frame before, Frame now) {
        List<Fired> out = new ArrayList<>();
        if (!now.active())
            return out;
        boolean same = before.active() && now.matchId() != null &&
                now.matchId().equals(before.matchId());
        if (!same) {
            out.add(new Fired(Cue.MATCH_FOUND, 0));
            before = Frame.NONE;
        }
        if (now.state() == MatchState.COUNTDOWN && now.seconds() > 0 &&
                (before.state() != MatchState.COUNTDOWN ||
                        now.seconds() != before.seconds()))
            out.add(new Fired(Cue.COUNTDOWN, now.seconds()));
        if (before.state() == MatchState.COUNTDOWN &&
                now.state() == MatchState.BATTLE)
            out.add(new Fired(Cue.FIGHT, 0));
        if (now.state() != MatchState.BATTLE ||
                before.state() != MatchState.BATTLE)
            return out;
        if (now.overtime() && !before.overtime())
            out.add(new Fired(Cue.OVERTIME, 0));
        else if (now.elixirRate() > before.elixirRate())
            out.add(new Fired(Cue.ELIXIR_RATE, now.elixirRate()));
        int s = now.seconds();
        if (s > 0 && s <= ArenaSounds.FINAL_SECONDS_FROM &&
                s != before.seconds() && now.overtime() == before.overtime())
            out.add(new Fired(Cue.FINAL_SECOND, s));
        if (now.elixirTenths() >= ElixirMeter.MAX_TENTHS &&
                before.elixirTenths() < ElixirMeter.MAX_TENTHS)
            out.add(new Fired(Cue.ELIXIR_FULL, 0));
        return out;
    }
}
