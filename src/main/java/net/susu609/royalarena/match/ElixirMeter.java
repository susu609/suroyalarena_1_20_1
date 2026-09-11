package net.susu609.royalarena.match;

public final class ElixirMeter {
    public static final int MAX_TENTHS = 100;
    public static final int START_TENTHS = 50;
    private static final int TICKS_PER_TENTH = 6;

    private int tenths = START_TENTHS;
    private int regenTicks;
    public boolean spend(int wholeElixir) {
        if (!canSpend(wholeElixir))
            return false;
        tenths -= wholeElixir * 10;
        return true;
    }
    public boolean canSpend(int wholeElixir) {
        return wholeElixir >= 0 && tenths >= wholeElixir * 10;
    }
    public int tenths() {
        return tenths;
    }
    public void tick() {
        if (tenths >= MAX_TENTHS) {
            regenTicks = 0;
            return;
        }
        regenTicks++;
        if (regenTicks >= TICKS_PER_TENTH) {
            regenTicks = 0;
            tenths = Math.min(MAX_TENTHS, tenths + 1);
        }
    }
}
