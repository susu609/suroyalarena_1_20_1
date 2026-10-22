package net.susu609.royalarena.combat;

import net.susu609.royalarena.card.BattleCard;

/**
 * What the clients need to draw about a troop's state, as bits (sent by
 * {@code S2CTroopStatePacket}): the colour over its model, its particles, a
 * mirror image drawn see-through, a building's own decay kept off the damage
 * numbers.
 */
public final class TroopFlags {
    public static final int FROZEN = 1;
    public static final int STUNNED = 1 << 1;
    public static final int POISONED = 1 << 2;
    public static final int WITHERED = 1 << 3;
    public static final int HASTED = 1 << 4;
    public static final int WEBBED = 1 << 5;
    public static final int INVULNERABLE = 1 << 6;
    public static final int ILLUSION = 1 << 7;
    public static final int LEVITATING = 1 << 8;
    public static final int BUILDING = 1 << 9;

    private TroopFlags() {
    }

    /** The bits for a unit now. */
    public static int of(CombatUnit unit) {
        BattleCard card = BattleCard.forStats(unit.stats());
        int flags = 0;
        if (unit.stunned())
            flags |= STUNNED;
        else if (unit.levitating())
            flags |= LEVITATING;
        else if (unit.frozen())
            flags |= FROZEN;
        if (unit.poisoned())
            flags |= POISONED;
        if (unit.withered())
            flags |= WITHERED;
        if (unit.hasted())
            flags |= HASTED;
        if (unit.webbed())
            flags |= WEBBED;
        if (unit.invulnerable())
            flags |= INVULNERABLE;
        if (card != null && card.castsIllusions() && unit.generation() > 0)
            flags |= ILLUSION;
        if (card != null && card.isBuilding())
            flags |= BUILDING;
        return flags;
    }

    public static boolean has(int flags, int flag) {
        return (flags & flag) != 0;
    }
}
