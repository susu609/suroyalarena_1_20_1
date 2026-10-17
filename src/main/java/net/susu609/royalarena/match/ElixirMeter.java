package net.susu609.royalarena.match;

public final class ElixirMeter {
    public static final int MAX_TENTHS = 100;
    public static final int START_TENTHS = 50;
    /** Ticks per elixir at the normal rate (2.8 s, balance.json phase 1). */
    public static final int TICKS_PER_ELIXIR = 56;

    private int tenths = START_TENTHS;
    /** Progress toward the next tenth, in tenths of a tick. */
    private int regen;

    public boolean canSpend(int wholeElixir) {
        return wholeElixir >= 0 && tenths >= wholeElixir * 10;
    }
    public int tenths() {
        return tenths;
    }

    public boolean spend(int wholeElixir) {
        if (!canSpend(wholeElixir))
            return false;
        tenths -= wholeElixir * 10;
        return true;
    }

    /** One tick at the normal rate. */
    public void tick() {
        tick(TICKS_PER_ELIXIR);
    }

    /** One tick, gaining an elixir every {@code ticksPerElixir} ticks. */
    public void tick(int ticksPerElixir) {
        if (tenths >= MAX_TENTHS) {
            regen = 0;
            return;
        }
        regen += 10;
        while (regen >= ticksPerElixir && tenths < MAX_TENTHS) {
            regen -= ticksPerElixir;
            tenths++;
        }
    }
}
