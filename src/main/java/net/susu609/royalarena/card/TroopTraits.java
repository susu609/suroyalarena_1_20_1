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
 * @param frostImmune         freezing spells and frost arrows only hurt it: it
 *                            is never frozen or slowed (Polar Bear)
 * @param lineWidth           the hit runs in a line from the unit to its
 *                            target and on to its full range, this wide, and
 *                            hurts every enemy on it (Evoker fangs); 0 = no line
 * @param chargeTicks         a new target first takes this long to lock on
 *                            before the first hit (Guardian laser); 0 = none
 * @param riverStrong         hits {@link #RIVER_STRENGTH} times as hard while
 *                            standing on the river (Guardian)
 * @param fireTrailTicks      while it walks it leaves burning patches that
 *                            last this long and set enemies standing on them
 *                            burning (Strider); 0 = none
 */
public record TroopTraits(double splashRadius, boolean selfDestruct,
                          double knockback, boolean buildingsOnly,
                          int slowTicks, int poisonTicks,
                          double poisonPerSecond, int teleportCooldownTicks,
                          double hover, boolean crossesRiver,
                          boolean deployOnRiver, boolean frostWeak,
                          boolean waterWeak, boolean frostImmune,
                          double lineWidth, int chargeTicks,
                          boolean riverStrong, int fireTrailTicks)
{
    /** A fire trail patch: its radius, how often one is dropped, its burn. */
    public static final double TRAIL_RADIUS = 0.7D;
    public static final int TRAIL_EVERY_TICKS = 10;
    public static final double TRAIL_BURN_PER_SECOND = 1.0D;
    public static final int TRAIL_BURN_TICKS = 20;

    public TroopTraits(double splashRadius, boolean selfDestruct,
            double knockback, boolean buildingsOnly, int slowTicks,
            int poisonTicks, double poisonPerSecond, int teleportCooldownTicks,
            double hover, boolean crossesRiver, boolean deployOnRiver,
            boolean frostWeak, boolean waterWeak, boolean frostImmune,
            double lineWidth, int chargeTicks, boolean riverStrong)
    {
        this(splashRadius, selfDestruct, knockback, buildingsOnly, slowTicks,
                poisonTicks, poisonPerSecond, teleportCooldownTicks, hover,
                crossesRiver, deployOnRiver, frostWeak, waterWeak, frostImmune,
                lineWidth, chargeTicks, riverStrong, 0);
    }

    /** Leaves a fire trail whose patches last {@code ticks} (Strider). */
    public static TroopTraits fireTrail(int ticks) {
        return new TroopTraits(0.0D, false, 0.0D, false, 0, 0, 0.0D, 0, 0.0D,
                false, false, false, false, false, 0.0D, 0, false, ticks);
    }

    public boolean leavesFireTrail() {
        return fireTrailTicks > 0;
    }

    /** Walking speed left to a slowed troop. */
    public static final double SLOW_SPEED = 0.6D;
    /** Damage taken on the river by a water-weak troop. */
    public static final double WATER_WEAKNESS = 1.5D;
    /** Damage taken from a freezing spell by a frost-weak troop. */
    public static final double FROST_WEAKNESS = 2.0D;
    /** Damage dealt from the river by a river-strong troop. */
    public static final double RIVER_STRENGTH = 1.5D;

    public static final TroopTraits NONE = new TroopTraits(0.0D, false, 0.0D,
            false, 0, 0, 0.0D, 0, 0.0D, false, false, false, false, false,
            0.0D, 0, false);
    public static final TroopTraits BUILDINGS = new TroopTraits(0.0D, false,
            0.0D, true, 0, 0, 0.0D, 0, 0.0D, false, false, false, false, false,
            0.0D, 0, false);

    public TroopTraits(double splashRadius, boolean selfDestruct,
            double knockback)
    {
        this(splashRadius, selfDestruct, knockback, false, 0, 0, 0.0D, 0,
                0.0D, false, false, false, false, false, 0.0D, 0, false);
    }

    /** Only slows what it hits. */
    public static TroopTraits slowing(int ticks) {
        return new TroopTraits(0.0D, false, 0.0D, false, ticks, 0, 0.0D, 0,
                0.0D, false, false, false, false, false, 0.0D, 0, false);
    }

    /** Poisons what it hits: {@code perSecond} damage a second for {@code ticks}. */
    public static TroopTraits poisoning(int ticks, double perSecond) {
        return new TroopTraits(0.0D, false, 0.0D, false, 0, ticks, perSecond,
                0, 0.0D, false, false, false, false, false, 0.0D, 0, false);
    }

    /** Teleports away from ranged shots and spells, once per cooldown. */
    public static TroopTraits teleporting(int cooldownTicks) {
        return new TroopTraits(0.0D, false, 0.0D, false, 0, 0, 0.0D,
                cooldownTicks, 0.0D, false, false, false, false, false, 0.0D,
                0, false);
    }

    /** Floats {@code height} blocks up; the three flags as documented. */
    public static TroopTraits hovering(double height, boolean crossesRiver,
            boolean frostWeak, boolean waterWeak)
    {
        return new TroopTraits(0.0D, false, 0.0D, false, 0, 0, 0.0D, 0,
                height, crossesRiver, false, frostWeak, waterWeak, false, 0.0D,
                0, false);
    }

    /**
     * Flies {@code height} blocks up straight over the river and only goes
     * for the towers (Phantom).
     */
    public static TroopTraits swooping(double height) {
        return new TroopTraits(0.0D, false, 0.0D, true, 0, 0, 0.0D, 0, height,
                true, false, false, false, false, 0.0D, 0, false);
    }

    /** Can be played on the river as well as on its own half. */
    public static TroopTraits riverDeploy() {
        return new TroopTraits(0.0D, false, 0.0D, false, 0, 0, 0.0D, 0, 0.0D,
                false, true, false, false, false, 0.0D, 0, false);
    }

    /**
     * Stings again and again, each sting poisoning, and floats
     * {@code hover} blocks up (Bee).
     */
    public static TroopTraits stinging(int poisonTicks, double perSecond,
            double hover)
    {
        return new TroopTraits(0.0D, false, 0.0D, false, 0, poisonTicks,
                perSecond, 0, hover, false, false, false, false, false, 0.0D,
                0, false);
    }

    /** Never frozen or slowed (Polar Bear). */
    public static TroopTraits frostProof() {
        return new TroopTraits(0.0D, false, 0.0D, false, 0, 0, 0.0D, 0, 0.0D,
                false, false, false, false, true, 0.0D, 0, false);
    }

    /** Hits everything on a line this wide toward its target (Evoker). */
    public static TroopTraits line(double width) {
        return new TroopTraits(0.0D, false, 0.0D, false, 0, 0, 0.0D, 0, 0.0D,
                false, false, false, false, false, width, 0, false);
    }

    /**
     * Locks on for {@code chargeTicks} before hitting a new target, may be
     * played on the river and hits harder from there (Guardian). It swims
     * {@code hover} blocks up: a vanilla Guardian flops about on the ground.
     */
    public static TroopTraits laser(int chargeTicks, double hover) {
        return new TroopTraits(0.0D, false, 0.0D, false, 0, 0, 0.0D, 0, hover,
                false, true, false, false, false, 0.0D, chargeTicks, true);
    }

    public boolean lines() {
        return lineWidth > 0.0D;
    }

    public boolean charges() {
        return chargeTicks > 0;
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
