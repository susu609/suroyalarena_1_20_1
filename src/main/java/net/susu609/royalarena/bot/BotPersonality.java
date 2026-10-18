package net.susu609.royalarena.bot;

import java.util.Locale;
import java.util.Random;

/**
 * How a bot plays its match; each match draws one at random from the seed
 * unless the config names one (docs/BOT.md).
 */
public enum BotPersonality {
    /**
     * Saves up to 9 elixir, drops a tank at the back of a lane and follows
     * it with support troops; defends with cheap cards to keep the tank.
     */
    TANK_PUSH("Tank Push", 20),
    /**
     * Cheap, fast troops at the bridge in pairs (a second right after the
     * first), lanes taking turns, without waiting for 7 elixir.
     */
    RUSH("Rush", -25),
    /**
     * Never attacks first (only at full elixir); after a defence it sends
     * attackers in behind its surviving defenders.
     */
    COUNTER("Counter", 30),
    /**
     * Plays like the default, uses spells a little more readily and finishes
     * a Princess tower under 30% health with a spell, keeping one spell back.
     */
    SPELL_SIEGE("Spell Siege", 0);

    private final String displayName;
    /** Added to the attack threshold, in tenths of elixir. */
    private final int attackOffset;

    BotPersonality(String displayName, int attackOffset) {
        this.displayName = displayName;
        this.attackOffset = attackOffset;
    }

    public String displayName() {
        return displayName;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The attack threshold for this personality, from the configured one. */
    public int attackMinTenths(int configured) {
        return Math.max(10, Math.min(100, configured + attackOffset));
    }

    /** The one this seed draws; independent of the bot's own random. */
    public static BotPersonality pick(long seed) {
        BotPersonality[] all = values();
        return all[new Random(seed * 0x9E3779B97F4A7C15L + 17L)
                .nextInt(all.length)];
    }

    /** The personality with this id, or null for "random" or an unknown id. */
    public static BotPersonality byId(String id) {
        if (id == null)
            return null;
        for (BotPersonality p : values()) {
            if (p.id().equals(id.trim().toLowerCase(Locale.ROOT)))
                return p;
        }
        return null;
    }
}
