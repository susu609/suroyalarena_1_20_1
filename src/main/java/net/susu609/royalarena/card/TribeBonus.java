package net.susu609.royalarena.card;

/**
 * What a troop's tribes give it in a match (docs/TRIBES.md). Only troops of
 * the tribe get it. Multipliers are 1 when nothing changes; the others add
 * (or are off at 0 / false).
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
 * @param slowOnHit       its hits slow the victim's walk this long
 * @param revive          when it dies it gets up again with this share of
 *                        its max health; once per unit, and once per
 *                        {@link #REVIVE_COOLDOWN} for the whole side (0 = never)
 * @param totem           a blow that would kill it leaves it at 1 health and
 *                        unhurt for {@link #TOTEM_TICKS}; once per
 *                        {@link #TOTEM_COOLDOWN} for the whole side
 * @param deathBurnTicks  when it dies, enemies within
 *                        {@link #DEATH_BURN_RADIUS} burn this long
 * @param deathBurnPerSecond that burn's damage a second
 * @param packDamage      extra damage share for each other troop of a pack
 *                        tribe within {@link #PACK_RADIUS} (up to
 *                        {@link #PACK_MAX})
 * @param extraShotEvery  every n-th ranged shot also hits a second enemy in
 *                        range for {@link #EXTRA_SHOT_SHARE} of the damage
 * @param dodgeFirstShot  a ranged shot that would hit it misses, once per
 *                        {@link #DODGE_COOLDOWN} for the whole side
 */
public record TribeBonus(double health, double damage, double speed,
                         double attackSpeed, double armor, double regen,
                         double lifesteal, double towerDamage,
                         double riverDamage, int poisonTicks,
                         double poisonPerSecond, int burnTicks,
                         double burnPerSecond, boolean pierce,
                         int firstHitStun, int slowOnHit, double revive,
                         boolean totem, int deathBurnTicks,
                         double deathBurnPerSecond, double packDamage,
                         int extraShotEvery, boolean dodgeFirstShot)
{
    /** A totem-saved unit cannot be hurt for this long (0.5 s). */
    public static final int TOTEM_TICKS = 10;
    /**
     * Saving rules work for one troop of the side at a time, then wait
     * (ticks): a side has one revive every 20 s, one totem every 20 s, one
     * dodge every 16 s. Per troop they would give a swarm a second life each.
     */
    public static final int REVIVE_COOLDOWN = 400;
    public static final int TOTEM_COOLDOWN = 400;
    public static final int DODGE_COOLDOWN = 320;
    public static final double DEATH_BURN_RADIUS = 2.0D;
    public static final double PACK_RADIUS = 4.0D;
    public static final int PACK_MAX = 3;
    public static final double EXTRA_SHOT_SHARE = 0.5D;

    public static final TribeBonus NONE = new TribeBonus(1.0D, 1.0D, 1.0D,
            1.0D, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0, 0.0D, 0, 0.0D, false, 0,
            0, 0.0D, false, 0, 0.0D, 0.0D, 0, false);

    public static TribeBonus health(double multiplier) {
        return NONE.with(multiplier, 1.0D, 1.0D, 1.0D);
    }

    public static TribeBonus damage(double multiplier) {
        return NONE.with(1.0D, multiplier, 1.0D, 1.0D);
    }

    public static TribeBonus healthAndDamage(double multiplier) {
        return NONE.with(multiplier, multiplier, 1.0D, 1.0D);
    }

    public static TribeBonus speed(double multiplier) {
        return NONE.with(1.0D, 1.0D, multiplier, 1.0D);
    }

    public static TribeBonus attackSpeed(double multiplier) {
        return NONE.with(1.0D, 1.0D, 1.0D, multiplier);
    }

    private TribeBonus with(double health, double damage, double speed,
            double attackSpeed)
    {
        return new TribeBonus(health, damage, speed, attackSpeed, armor, regen,
                lifesteal, towerDamage, riverDamage, poisonTicks,
                poisonPerSecond, burnTicks, burnPerSecond, pierce, firstHitStun,
                slowOnHit, revive, totem, deathBurnTicks, deathBurnPerSecond,
                packDamage, extraShotEvery, dodgeFirstShot);
    }

    public static TribeBonus armor(double points) {
        return new Builder().armor(points).build();
    }

    public static TribeBonus regen(double perSecond) {
        return new Builder().regen(perSecond).build();
    }

    public static TribeBonus lifesteal(double share) {
        return new Builder().lifesteal(share).build();
    }

    public static TribeBonus towerDamage(double multiplier) {
        return new Builder().towerDamage(multiplier).build();
    }

    public static TribeBonus riverDamage(double multiplier) {
        return new Builder().riverDamage(multiplier).build();
    }

    public static TribeBonus poison(int ticks, double perSecond) {
        return new Builder().poison(ticks, perSecond).build();
    }

    public static TribeBonus burn(int ticks, double perSecond) {
        return new Builder().burn(ticks, perSecond).build();
    }

    public static TribeBonus piercing() {
        return new Builder().pierce().build();
    }

    public static TribeBonus firstHitStun(int ticks) {
        return new Builder().firstHitStun(ticks).build();
    }

    public static TribeBonus slowOnHit(int ticks) {
        return new Builder().slowOnHit(ticks).build();
    }

    public static TribeBonus revive(double share) {
        return new Builder().revive(share).build();
    }

    public static TribeBonus undying() {
        return new Builder().totem().build();
    }

    public static TribeBonus deathBurn(int ticks, double perSecond) {
        return new Builder().deathBurn(ticks, perSecond).build();
    }

    public static TribeBonus pack(double perAlly) {
        return new Builder().pack(perAlly).build();
    }

    public static TribeBonus extraShot(int every) {
        return new Builder().extraShot(every).build();
    }

    public static TribeBonus dodge() {
        return new Builder().dodge().build();
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
                pierce || o.pierce, Math.max(firstHitStun, o.firstHitStun),
                Math.max(slowOnHit, o.slowOnHit), Math.max(revive, o.revive),
                totem || o.totem, Math.max(deathBurnTicks, o.deathBurnTicks),
                deathBurnPerSecond + o.deathBurnPerSecond,
                packDamage + o.packDamage,
                extraShotEvery == 0 ? o.extraShotEvery : o.extraShotEvery == 0 ?
                        extraShotEvery : Math.min(extraShotEvery, o.extraShotEvery),
                dodgeFirstShot || o.dodgeFirstShot);
    }

    public boolean none() {
        return equals(NONE);
    }

    /** Builds one-effect bonuses without spelling out every field. */
    private static final class Builder {
        private double armor;
        private double regen;
        private double lifesteal;
        private double towerDamage = 1.0D;
        private double riverDamage = 1.0D;
        private int poisonTicks;
        private double poisonPerSecond;
        private int burnTicks;
        private double burnPerSecond;
        private boolean pierce;
        private int firstHitStun;
        private int slowOnHit;
        private double revive;
        private boolean totem;
        private int deathBurnTicks;
        private double deathBurnPerSecond;
        private double packDamage;
        private int extraShotEvery;
        private boolean dodge;

        Builder armor(double v) { armor = v; return this; }
        Builder regen(double v) { regen = v; return this; }
        Builder lifesteal(double v) { lifesteal = v; return this; }
        Builder towerDamage(double v) { towerDamage = v; return this; }
        Builder riverDamage(double v) { riverDamage = v; return this; }
        Builder poison(int t, double v) { poisonTicks = t; poisonPerSecond = v; return this; }
        Builder burn(int t, double v) { burnTicks = t; burnPerSecond = v; return this; }
        Builder pierce() { pierce = true; return this; }
        Builder firstHitStun(int t) { firstHitStun = t; return this; }
        Builder slowOnHit(int t) { slowOnHit = t; return this; }
        Builder revive(double v) { revive = v; return this; }
        Builder totem() { totem = true; return this; }
        Builder deathBurn(int t, double v) { deathBurnTicks = t; deathBurnPerSecond = v; return this; }
        Builder pack(double v) { packDamage = v; return this; }
        Builder extraShot(int n) { extraShotEvery = n; return this; }
        Builder dodge() { dodge = true; return this; }

        TribeBonus build() {
            return new TribeBonus(1.0D, 1.0D, 1.0D, 1.0D, armor, regen,
                    lifesteal, towerDamage, riverDamage, poisonTicks,
                    poisonPerSecond, burnTicks, burnPerSecond, pierce,
                    firstHitStun, slowOnHit, revive, totem, deathBurnTicks,
                    deathBurnPerSecond, packDamage, extraShotEvery, dodge);
        }
    }
}
