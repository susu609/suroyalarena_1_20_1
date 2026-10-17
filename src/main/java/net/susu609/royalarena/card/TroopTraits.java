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
 */
public record TroopTraits(double splashRadius, boolean selfDestruct,
                          double knockback, boolean buildingsOnly)
{
    public static final TroopTraits NONE =
            new TroopTraits(0.0D, false, 0.0D, false);
    public static final TroopTraits BUILDINGS =
            new TroopTraits(0.0D, false, 0.0D, true);

    public TroopTraits(double splashRadius, boolean selfDestruct,
            double knockback)
    {
        this(splashRadius, selfDestruct, knockback, false);
    }

    public boolean splash() {
        return splashRadius > 0.0D;
    }
}
