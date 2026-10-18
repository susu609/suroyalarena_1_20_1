package net.susu609.royalarena.card;

/**
 * What a troop's tribes give it in a match (docs/TRIBES.md). Multipliers are
 * 1 when nothing changes; the others add.
 *
 * @param health          max health multiplier
 * @param damage          damage multiplier (every hit, towers too)
 * @param speed           walking speed multiplier
 * @param attackSpeed     attack rate multiplier: 1.25 attacks 25% more often
 * @param armor           extra armour points ({@link TroopStats#armor()})
 * @param regen           health healed a second while alive
 * @param lifesteal       share of the damage it deals that it heals back
 * @param towerDamage     extra multiplier on damage to towers
 * @param riverDamage     extra multiplier on damage dealt from the river
 * @param poisonTicks     its hits poison for this long (0 = no)
 * @param poisonPerSecond that poison's damage a second
 * @param burnTicks       its hits set the victim burning for this long
 * @param burnPerSecond   that burn's damage a second
 * @param pierce          its hits and blasts ignore the victim's armour
 * @param firstHitStun    its first hit freezes the victim this long (its
 *                        swing timer waits, it does not restart)
 */
public record TribeBonus(double health, double damage, double speed,
                         double attackSpeed, double armor, double regen,
                         double lifesteal, double towerDamage,
                         double riverDamage, int poisonTicks,
                         double poisonPerSecond, int burnTicks,
                         double burnPerSecond, boolean pierce,
                         int firstHitStun)
{
    public static final TribeBonus NONE = new TribeBonus(1.0D, 1.0D, 1.0D,
            1.0D, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0, 0.0D, 0, 0.0D, false, 0);

    public static TribeBonus health(double multiplier) {
        return with(NONE, multiplier, 1.0D);
    }

    public static TribeBonus damage(double multiplier) {
        return with(NONE, 1.0D, multiplier);
    }

    public static TribeBonus healthAndDamage(double multiplier) {
        return with(NONE, multiplier, multiplier);
    }

    private static TribeBonus with(TribeBonus b, double health, double damage) {
        return new TribeBonus(health, damage, b.speed, b.attackSpeed, b.armor,
                b.regen, b.lifesteal, b.towerDamage, b.riverDamage,
                b.poisonTicks, b.poisonPerSecond, b.burnTicks, b.burnPerSecond,
                b.pierce, b.firstHitStun);
    }

    public static TribeBonus speed(double multiplier) {
        return new TribeBonus(1.0D, 1.0D, multiplier, 1.0D, 0.0D, 0.0D, 0.0D,
                1.0D, 1.0D, 0, 0.0D, 0, 0.0D, false, 0);
    }

    public static TribeBonus attackSpeed(double multiplier) {
        return new TribeBonus(1.0D, 1.0D, 1.0D, multiplier, 0.0D, 0.0D, 0.0D,
                1.0D, 1.0D, 0, 0.0D, 0, 0.0D, false, 0);
    }

    public static TribeBonus armor(double points) {
        return new TribeBonus(1.0D, 1.0D, 1.0D, 1.0D, points, 0.0D, 0.0D,
                1.0D, 1.0D, 0, 0.0D, 0, 0.0D, false, 0);
    }

    public static TribeBonus regen(double perSecond) {
        return new TribeBonus(1.0D, 1.0D, 1.0D, 1.0D, 0.0D, perSecond, 0.0D,
                1.0D, 1.0D, 0, 0.0D, 0, 0.0D, false, 0);
    }

    public static TribeBonus lifesteal(double share) {
        return new TribeBonus(1.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D, share,
                1.0D, 1.0D, 0, 0.0D, 0, 0.0D, false, 0);
    }

    public static TribeBonus towerDamage(double multiplier) {
        return new TribeBonus(1.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D,
                multiplier, 1.0D, 0, 0.0D, 0, 0.0D, false, 0);
    }

    public static TribeBonus riverDamage(double multiplier) {
        return new TribeBonus(1.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D,
                1.0D, multiplier, 0, 0.0D, 0, 0.0D, false, 0);
    }

    public static TribeBonus poison(int ticks, double perSecond) {
        return new TribeBonus(1.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D,
                1.0D, 1.0D, ticks, perSecond, 0, 0.0D, false, 0);
    }

    public static TribeBonus burn(int ticks, double perSecond) {
        return new TribeBonus(1.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D,
                1.0D, 1.0D, 0, 0.0D, ticks, perSecond, false, 0);
    }

    public static TribeBonus piercing() {
        return new TribeBonus(1.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D,
                1.0D, 1.0D, 0, 0.0D, 0, 0.0D, true, 0);
    }

    public static TribeBonus firstHitStun(int ticks) {
        return new TribeBonus(1.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D,
                1.0D, 1.0D, 0, 0.0D, 0, 0.0D, false, ticks);
    }

    /** Both bonuses together: multipliers multiply, the rest add up. */
    public TribeBonus plus(TribeBonus o) {
        return new TribeBonus(health * o.health, damage * o.damage,
                speed * o.speed, attackSpeed * o.attackSpeed, armor + o.armor,
                regen + o.regen, lifesteal + o.lifesteal,
                towerDamage * o.towerDamage, riverDamage * o.riverDamage,
                Math.max(poisonTicks, o.poisonTicks),
                poisonPerSecond + o.poisonPerSecond,
                Math.max(burnTicks, o.burnTicks), burnPerSecond + o.burnPerSecond,
                pierce || o.pierce, Math.max(firstHitStun, o.firstHitStun));
    }

    public boolean none() {
        return equals(NONE);
    }
}
