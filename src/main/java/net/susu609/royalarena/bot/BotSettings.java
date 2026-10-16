package net.susu609.royalarena.bot;

/** Bot difficulty knobs; see {@link BotBrain}. */
public record BotSettings(int thinkEveryTicks, int minGapTicks,
                          int attackMinTenths)
{
    public static final BotSettings DEFAULT = new BotSettings(
            BotBrain.THINK_EVERY_TICKS, BotBrain.MIN_GAP_TICKS,
            BotBrain.ATTACK_MIN_TENTHS);

    public BotSettings {
        thinkEveryTicks = Math.max(1, thinkEveryTicks);
        minGapTicks = Math.max(0, minGapTicks);
    }
}
