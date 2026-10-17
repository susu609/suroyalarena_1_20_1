package net.susu609.royalarena.card;

/**
 * Special abilities of a troop on top of its plain stats.
 *
 * @param splashRadius  every enemy within this many blocks of the target
 *                      takes the hit too (0 = single target)
 * @param selfDestruct  the unit dies when it strikes (Creeper)
 * @param knockback     blocks a melee hit pushes the victim away (Ravager)
 * @param buildingsOnly the unit ignores enemy troops and walks straight to
 *                      the towers (Iron Golem, Hoglin)
 * @param slowTicks     a troop it hits walks at {@link #SLOW_SPEED} of its
 *                      speed for this long (Stray's frost arrows)
 */
public record TroopTraits(double splashRadius, boolean selfDestruct,
                          double knockback, boolean buildingsOnly,
                          int slowTicks)
{
    /** Walking speed left to a slowed troop. */
    public static final double SLOW_SPEED = 0.6D;

    public static final TroopTraits NONE =
            new TroopTraits(0.0D, false, 0.0D, false, 0);
    public static final TroopTraits BUILDINGS =
            new TroopTraits(0.0D, false, 0.0D, true, 0);

    public TroopTraits(double splashRadius, boolean selfDestruct,
            double knockback)
    {
        this(splashRadius, selfDestruct, knockback, false, 0);
    }

    /** Only slows what it hits. */
    public static TroopTraits slowing(int ticks) {
        return new TroopTraits(0.0D, false, 0.0D, false, ticks);
    }

    public boolean splash() {
        return splashRadius > 0.0D;
    }

    public boolean slows() {
        return slowTicks > 0;
    }
}
