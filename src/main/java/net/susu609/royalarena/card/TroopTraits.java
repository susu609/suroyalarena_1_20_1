package net.susu609.royalarena.card;

/**
 * Special abilities of a troop on top of its plain stats.
 *
 * @param splashRadius        every enemy within this many blocks of the target
 *                            takes the hit too (0 = single target)
 * @param selfDestruct        the unit dies when it strikes (Creeper, Vex)
 * @param knockback           blocks a melee hit pushes the victim away (Ravager)
 * @param buildingsOnly       the unit ignores enemy troops and walks straight to
 *                            the towers (Iron Golem, Hoglin)
 * @param slowTicks           a troop it hits walks at {@link #SLOW_SPEED} of its
 *                            speed for this long (Stray's frost arrows)
 * @param poisonTicks         a troop it hits is poisoned for this long
 *                            (Cave Spider); 0 = no poison
 * @param poisonPerSecond     poison damage per second
 * @param teleportCooldownTicks the unit teleports away from a ranged shot or a
 *                            spell that would hit it, once per cooldown
 *                            (Enderman); 0 = never
 * @param hover               the unit floats this many blocks above the floor
 *                            (Blaze, Ghast); 0 = walks on the ground
 * @param crossesRiver        the unit crosses the river anywhere, not only on the
 *                            bridges (Blaze)
 * @param deployOnRiver       the card may also be played on the river (Drowned)
 * @param frostWeak           spells that freeze deal double damage and freeze for
 *                            twice as long (Blaze)
 * @param waterWeak           the unit takes 1.5x damage while on the river (Blaze)
 */
public record TroopTraits(double splashRadius, boolean selfDestruct,
                          double knockback, boolean buildingsOnly,
                          int slowTicks, int poisonTicks,
                          double poisonPerSecond, int teleportCooldownTicks,
                          double hover, boolean crossesRiver,
                          boolean deployOnRiver, boolean frostWeak,
                          boolean waterWeak)
{
    /** Walking speed left to a slowed troop. */
    public static final double SLOW_SPEED = 0.6D;
    /** Damage taken on the river by a water-weak troop. */
    public static final double WATER_WEAKNESS = 1.5D;
    /** Damage taken from a freezing spell by a frost-weak troop. */
    public static final double FROST_WEAKNESS = 2.0D;

    public static final TroopTraits NONE = new TroopTraits(0.0D, false, 0.0D,
            false, 0, 0, 0.0D, 0, 0.0D, false, false, false, false);
    public static final TroopTraits BUILDINGS = new TroopTraits(0.0D, false,
            0.0D, true, 0, 0, 0.0D, 0, 0.0D, false, false, false, false);

    public TroopTraits(double splashRadius, boolean selfDestruct,
            double knockback)
    {
        this(splashRadius, selfDestruct, knockback, false, 0, 0, 0.0D, 0,
                0.0D, false, false, false, false);
    }

    /** Only slows what it hits. */
    public static TroopTraits slowing(int ticks) {
        return new TroopTraits(0.0D, false, 0.0D, false, ticks, 0, 0.0D, 0,
                0.0D, false, false, false, false);
    }

    /** Poisons what it hits: {@code perSecond} damage a second for {@code ticks}. */
    public static TroopTraits poisoning(int ticks, double perSecond) {
        return new TroopTraits(0.0D, false, 0.0D, false, 0, ticks, perSecond,
                0, 0.0D, false, false, false, false);
    }

    /** Teleports away from ranged shots and spells, once per cooldown. */
    public static TroopTraits teleporting(int cooldownTicks) {
        return new TroopTraits(0.0D, false, 0.0D, false, 0, 0, 0.0D,
                cooldownTicks, 0.0D, false, false, false, false);
    }

    /** Floats {@code height} blocks up; the three flags as documented. */
    public static TroopTraits hovering(double height, boolean crossesRiver,
            boolean frostWeak, boolean waterWeak)
    {
        return new TroopTraits(0.0D, false, 0.0D, false, 0, 0, 0.0D, 0,
                height, crossesRiver, false, frostWeak, waterWeak);
    }

    /** Can be played on the river as well as on its own half. */
    public static TroopTraits riverDeploy() {
        return new TroopTraits(0.0D, false, 0.0D, false, 0, 0, 0.0D, 0, 0.0D,
                false, true, false, false);
    }

    public boolean splash() {
        return splashRadius > 0.0D;
    }

    public boolean slows() {
        return slowTicks > 0;
    }

    public boolean poisons() {
        return poisonTicks > 0 && poisonPerSecond > 0.0D;
    }

    /** Poison damage one hit leaves behind, in total. */
    public double poisonTotal() {
        return poisons() ? poisonPerSecond * poisonTicks / 20.0D : 0.0D;
    }

    public boolean teleports() {
        return teleportCooldownTicks > 0;
    }

    public boolean hovers() {
        return hover > 0.0D;
    }
}
