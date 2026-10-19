package net.susu609.royalarena.match;

import net.susu609.royalarena.card.Tribe;

import java.util.EnumMap;
import java.util.Map;

/**
 * The switches of a sandbox match (docs/SANDBOX.md): one player plays any
 * card for either side, with no clock and no bot. Towers can be made
 * unbreakable, elixir endless, time paused or slowed, and each side given
 * any tribe step.
 */
public final class SandboxSettings {
    /** Playback speeds in half match ticks per server tick: paused, 0.5x, 1x, 2x. */
    public static final int[] SPEEDS = {0, 1, 2, 4};
    public static final int NORMAL_SPEED = 2;

    private boolean towersInvulnerable = true;
    private boolean infiniteElixir = true;
    private int speed = NORMAL_SPEED;
    private final Map<TeamSide, Map<Tribe, Integer>> tribes =
            new EnumMap<>(TeamSide.class);

    public SandboxSettings() {
        for (TeamSide side : TeamSide.values())
            tribes.put(side, new EnumMap<>(Tribe.class));
    }

    public boolean towersInvulnerable() {
        return towersInvulnerable;
    }

    public void setTowersInvulnerable(boolean on) {
        towersInvulnerable = on;
    }

    public boolean infiniteElixir() {
        return infiniteElixir;
    }

    public void setInfiniteElixir(boolean on) {
        infiniteElixir = on;
    }

    /** Half match ticks per server tick: 0 paused, 1 half speed, 2, 4. */
    public int speed() {
        return speed;
    }

    /** Sets the speed; anything not in {@link #SPEEDS} is ignored. */
    public void setSpeed(int halfTicks) {
        for (int s : SPEEDS) {
            if (s == halfTicks)
                speed = halfTicks;
        }
    }

    /** The tribe steps a side plays with, chosen by hand (empty: none). */
    public Map<Tribe, Integer> tribeLevels(TeamSide side) {
        return tribes.get(side);
    }

    /** Sets a side's step of a tribe, clamped to the steps it has. */
    public void setTribeLevel(TeamSide side, Tribe tribe, int level) {
        int clamped = Math.max(0, Math.min(tribe.steps().size(), level));
        if (clamped == 0)
            tribes.get(side).remove(tribe);
        else
            tribes.get(side).put(tribe, clamped);
    }

    /** A side's tribe steps by tribe ordinal, for the snapshot. */
    public int[] tribeArray(TeamSide side) {
        int[] out = new int[Tribe.values().length];
        tribes.get(side).forEach((tribe, level) -> out[tribe.ordinal()] = level);
        return out;
    }
}
