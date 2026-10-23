package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.BattleCard.BeaconEffect;
import net.susu609.royalarena.card.CostFormula;
import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.combat.ChildSpawn;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.combat.TickResult;
import net.susu609.royalarena.combat.TickResult.Attack;
import net.susu609.royalarena.combat.TickResult.TribeEvent.Kind;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.TeamSide;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Batch 6: Pillager Captain, Wither, Zombie Horse, Bat, Mooshroom, Parrot,
 * Glow Squid, Sheep, Monster Spawner, Campfire, Beacon, and the Lava Bucket,
 * Potion of Harming and Firework Rocket spells. Units stand still (only
 * {@code engine.tick()}), out of every tower's reach.
 */
public final class MobsWave8SmokeTest {
    private MobsWave8SmokeTest() {
    }

    public static void run() {
        captainBannerBoostsIllagers();
        witherHasThreeHeadsAndArmour();
        jockeysAndSwarms();
        mooshroomSharesStew();
        parrotDancesFaster();
        glowSquidInks();
        sheepWoolTakesTwoHits();
        spawnerMakesVanillaMobs();
        campfireBurnsAndWarms();
        beaconTurnsItsEffects();
        lavaBurnsTheFireproofNot();
        harmingHurtsAndHealsOwnUndead();
        fireworkDoublesOnFlyers();
        abilitiesCountInTheCost();
        System.out.println("RoyalArena wave 8 mobs smoke tests: PASS");
    }

    private static CombatEngine engine() {
        MatchInstance match = MatchInstance.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ArenaLayout.standard(new ArenaPoint(0, 80, 0)));
        match.startBattle();
        return new CombatEngine(match);
    }

    private static CombatUnit unit(CombatEngine engine, BattleCard card,
            TeamSide side, double x, double z)
    {
        return engine.add(UUID.randomUUID(), side, card.stats(), x, z);
    }

    /** A still target of no card: lots of health, hits once in a long while. */
    private static CombatUnit dummy(CombatEngine engine, TeamSide side,
            double damage, int interval, double range, double x, double z)
    {
        return engine.add(UUID.randomUUID(), side,
                new TroopStats(1, 200, damage, interval, range, 0.0D, 0.3D),
                x, z);
    }

    private static boolean has(TickResult r, Kind kind) {
        return r.tribeEvents().stream().anyMatch(e -> e.kind() == kind);
    }

    /** The first attack {@code attacker} makes in up to 200 ticks. */
    private static Attack firstAttack(CombatEngine engine, CombatUnit attacker) {
        for (int t = 0; t < 200; t++) {
            for (Attack a : engine.tick().attacks()) {
                if (attacker.id().equals(a.attackerUnit()))
                    return a;
            }
        }
        return null;
    }

    private static void captainBannerBoostsIllagers() {
        CombatEngine plain = engine();
        CombatUnit alone = unit(plain, BattleCard.VINDICATOR, TeamSide.BLUE,
                0.0D, -2.2D);
        dummy(plain, TeamSide.RED, 0.0D, 400, 1.0D, 0.0D, -3.0D);
        double base = firstAttack(plain, alone).damage();

        CombatEngine banner = engine();
        CombatUnit boosted = unit(banner, BattleCard.VINDICATOR, TeamSide.BLUE,
                0.0D, -2.2D);
        CombatUnit captain = unit(banner, BattleCard.PILLAGER_CAPTAIN,
                TeamSide.BLUE, 2.5D, -2.2D);
        dummy(banner, TeamSide.RED, 0.0D, 400, 1.0D, 0.0D, -3.0D);
        double withBanner = firstAttack(banner, boosted).damage();
        check(captain.alive() && Math.abs(withBanner - base *
                        (1.0D + BattleCard.BANNER_DAMAGE)) < 0.01D,
                "the banner adds 15% to an Illager's blow: " + base + " -> " +
                        withBanner);

        banner.castSpell(TeamSide.RED, BattleCard.ANVIL, 2.5D, -2.2D);
        boolean dropped = false;
        for (int t = 0; t < 5 && !dropped; t++)
            dropped = has(banner.tick(), Kind.BANNER);
        check(!captain.alive() && dropped, "a fallen Captain drops its banner");
    }

    private static void witherHasThreeHeadsAndArmour() {
        CombatEngine engine = engine();
        CombatUnit wither = unit(engine, BattleCard.WITHER, TeamSide.BLUE, 0.0D,
                2.2D);
        List<CombatUnit> foes = new ArrayList<>();
        for (int i = -1; i <= 1; i++)
            foes.add(unit(engine, BattleCard.ZOMBIE, TeamSide.RED, i * 1.2D,
                    -2.2D));
        int shots = 0;
        for (int t = 0; t < 40 && shots == 0; t++) {
            for (Attack a : engine.tick().attacks()) {
                if (wither.id().equals(a.attackerUnit()))
                    shots++;
            }
        }
        check(shots == BattleCard.WITHER_HEADS, "three heads, three skulls: " +
                shots);
        for (int t = 0; t < 20; t++)
            engine.tick();
        check(foes.stream().allMatch(CombatUnit::withered),
                "every skull withers what it hits");

        CombatEngine shield = engine();
        CombatUnit w = unit(shield, BattleCard.WITHER, TeamSide.BLUE, 0.0D,
                2.2D);
        // Lightning, not a Fireball: no knockback moving it out of reach.
        for (int i = 0; i < 2; i++) {
            shield.castSpell(TeamSide.RED, BattleCard.LIGHTNING, 0.0D, 2.2D);
            shield.tick();
        }
        check(w.health() < w.maxHealth() / 2.0D, "Lightning: under half");
        dummy(shield, TeamSide.RED, 10.0D, 400, 6.0D, 0.0D, -2.2D);
        double before = w.health();
        for (int t = 0; t < 30; t++)
            shield.tick();
        check(Math.abs(before - w.health() - 5.0D) < 0.01D,
                "under half health a shot does half: " + (before - w.health()));
    }

    private static void jockeysAndSwarms() {
        check(BattleCard.ZOMBIE_HORSE.stats().split().into() ==
                        BattleCard.ZOMBIE.stats(),
                "a Zombie rides the Zombie Horse");
        check(BattleCard.BAT.stats().count() == 5 &&
                        BattleCard.BAT.stats().traits().hover() > 0.0D,
                "five flying Bats");
    }

    private static void mooshroomSharesStew() {
        CombatEngine engine = engine();
        CombatUnit golem = unit(engine, BattleCard.IRON_GOLEM, TeamSide.BLUE,
                3.0D, 2.2D);
        unit(engine, BattleCard.MOOSHROOM, TeamSide.BLUE, 4.8D, 2.2D);
        engine.castSpell(TeamSide.RED, BattleCard.ANVIL, 3.0D, 2.2D);
        engine.tick();
        double hurt = golem.health();
        boolean stew = false;
        for (int t = 1; t < BattleCard.STEW_TICKS; t++)
            stew |= has(engine.tick(), Kind.STEW);
        check(stew && Math.abs(golem.health() - hurt - BattleCard.STEW_HEAL) <
                0.01D, "the Mooshroom's stew heals 3: " + golem.health());
    }

    /** Ticks between an archer's first two shots, with or without a Parrot. */
    private static int gap(boolean parrot) {
        CombatEngine engine = engine();
        CombatUnit archer = unit(engine, BattleCard.SKELETON_ARCHER,
                TeamSide.BLUE, 0.0D, 2.2D);
        if (parrot)
            unit(engine, BattleCard.PARROT, TeamSide.BLUE, 1.0D, 2.6D);
        dummy(engine, TeamSide.RED, 0.0D, 400, 1.0D, 0.0D, -2.0D);
        List<Integer> at = new ArrayList<>();
        for (int t = 0; t < 120 && at.size() < 2; t++) {
            for (Attack a : engine.tick().attacks()) {
                if (archer.id().equals(a.attackerUnit()))
                    at.add(t);
            }
        }
        return at.get(1) - at.get(0);
    }

    private static void parrotDancesFaster() {
        int plain = gap(false);
        int danced = gap(true);
        check(danced < plain, "a dancing Parrot speeds up the archer: " +
                plain + " -> " + danced);
    }

    private static void glowSquidInks() {
        CombatEngine engine = engine();
        unit(engine, BattleCard.GLOW_SQUID, TeamSide.BLUE, 0.0D, -2.2D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED, 0.8D,
                -2.2D);
        boolean ink = false;
        for (int t = 0; t < 60 && !ink; t++)
            ink = has(engine.tick(), Kind.INK);
        check(ink && zombie.blinded(), "hurt, the Glow Squid inks and blinds");
    }

    private static void sheepWoolTakesTwoHits() {
        CombatEngine engine = engine();
        CombatUnit sheep = unit(engine, BattleCard.SHEEP, TeamSide.BLUE, 0.0D,
                2.2D);
        dummy(engine, TeamSide.RED, 4.0D, 20, 6.0D, 0.0D, -2.2D);
        boolean sheared = false;
        for (int t = 0; t < 45; t++)
            sheared |= has(engine.tick(), Kind.SHEARED);
        check(sheared && sheep.health() == sheep.maxHealth(),
                "the wool takes the first two hits whole");
        for (int t = 0; t < 25; t++)
            engine.tick();
        check(sheep.health() < sheep.maxHealth(), "the third hit gets through");
    }

    private static void spawnerMakesVanillaMobs() {
        CombatEngine engine = engine();
        CombatUnit spawner = unit(engine, BattleCard.MONSTER_SPAWNER,
                TeamSide.BLUE, 0.0D, 5.0D);
        List<ChildSpawn> made = new ArrayList<>();
        for (int t = 0; t < BattleCard.MONSTER_SPAWNER.stats().attackTicks() + 1;
                t++)
            made.addAll(engine.tick().spawns());
        check(made.size() == 1, "one mob every 5 s: " + made.size());
        ChildSpawn mob = made.get(0);
        BattleCard of = BattleCard.forStats(mob.stats());
        check(BattleCard.SPAWNER_MOBS.contains(of) && Math.abs(mob.health() -
                        of.stats().health() * BattleCard.SPAWNED_HEALTH) < 0.01D,
                "a vanilla spawner mob at half health: " + of);
        engine.castSpell(TeamSide.RED, BattleCard.TNT, 0.0D, 5.0D);
        engine.castSpell(TeamSide.RED, BattleCard.TNT, 0.0D, 5.0D);
        int broken = 0;
        for (int t = 0; t < 3; t++)
            broken += engine.tick().spawns().size();
        check(!spawner.alive() && broken == BattleCard.SPAWNER_BREAK_MOBS,
                "a broken spawner lets out two more: " + broken);
    }

    private static void campfireBurnsAndWarms() {
        CombatEngine engine = engine();
        unit(engine, BattleCard.CAMPFIRE, TeamSide.BLUE, 0.0D, -2.6D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED, 0.0D,
                -4.0D);
        CombatUnit blaze = unit(engine, BattleCard.BLAZE, TeamSide.RED, 1.0D,
                -4.0D);
        for (int t = 0; t < 22; t++)
            engine.tick();
        check(zombie.burnTicks() > 0 && blaze.burnTicks() == 0,
                "the Campfire burns enemies close by, not a Blaze");
    }

    private static void beaconTurnsItsEffects() {
        int p = BattleCard.BEACON_PHASE_TICKS;
        check(BeaconEffect.at(0) == BeaconEffect.SPEED &&
                BeaconEffect.at(p) == BeaconEffect.STRENGTH &&
                BeaconEffect.at(2 * p) == BeaconEffect.REGENERATION &&
                BeaconEffect.at(4 * p - 1) == BeaconEffect.RESISTANCE &&
                BeaconEffect.at(4 * p) == BeaconEffect.SPEED,
                "Speed, Strength, Regeneration, Resistance, every 10 s");
        CombatEngine engine = engine();
        CombatUnit vindicator = unit(engine, BattleCard.VINDICATOR,
                TeamSide.BLUE, 0.0D, -2.2D);
        unit(engine, BattleCard.BEACON, TeamSide.BLUE, 2.0D, 2.4D);
        dummy(engine, TeamSide.RED, 0.0D, 400, 1.0D, 0.0D, -3.0D);
        for (int t = 0; t < p; t++)
            engine.tick();
        double strong = -1.0D;
        for (int t = 0; t < 60 && strong < 0.0D; t++) {
            for (Attack a : engine.tick().attacks()) {
                if (vindicator.id().equals(a.attackerUnit()))
                    strong = a.damage();
            }
        }
        check(Math.abs(strong - 12.0D * BattleCard.BEACON_STRENGTH) < 0.01D,
                "Strength: +20% damage: " + strong);
    }

    private static void lavaBurnsTheFireproofNot() {
        CombatEngine engine = engine();
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED, 0.0D,
                -4.0D);
        CombatUnit blaze = unit(engine, BattleCard.BLAZE, TeamSide.RED, 1.0D,
                -4.0D);
        engine.castSpell(TeamSide.BLUE, BattleCard.LAVA_BUCKET, 0.5D, -4.0D);
        engine.tick();
        check(zombie.burnTicks() > 0 && zombie.slowed() &&
                        blaze.burnTicks() == 0 && !blaze.slowed(),
                "lava burns and slows a Zombie, not a Blaze");
    }

    private static void harmingHurtsAndHealsOwnUndead() {
        CombatEngine engine = engine();
        CombatUnit own = unit(engine, BattleCard.ZOMBIE, TeamSide.BLUE, 0.0D,
                2.2D);
        engine.castSpell(TeamSide.RED, BattleCard.FIREWORK_ROCKET, 0.0D, 2.2D);
        engine.tick();
        CombatUnit enemy = unit(engine, BattleCard.ZOMBIE, TeamSide.RED, 2.0D,
                2.2D);
        double hurt = own.health();
        engine.castSpell(TeamSide.BLUE, BattleCard.HARMING_POTION, 1.0D, 2.2D);
        engine.tick();
        check(own.health() > hurt, "an own Zombie in it heals");
        check(Math.abs(enemy.health() - (20.0D - 12.0D)) < 0.01D,
                "an enemy takes 12 through its armour: " + enemy.health());
    }

    private static void fireworkDoublesOnFlyers() {
        CombatEngine engine = engine();
        CombatUnit phantom = unit(engine, BattleCard.PHANTOM, TeamSide.RED,
                0.0D, -4.0D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED, 0.8D,
                -4.0D);
        engine.castSpell(TeamSide.BLUE, BattleCard.FIREWORK_ROCKET, 0.4D, -4.0D);
        engine.tick();
        double air = phantom.maxHealth() - phantom.health();
        double ground = zombie.maxHealth() - zombie.health();
        check(Math.abs(air - 16.0D * phantom.armorFactor()) < 0.01D &&
                        Math.abs(ground - 8.0D * zombie.armorFactor()) < 0.01D,
                "16 on a flyer, 8 on the ground: " + air + ", " + ground);
    }

    private static void abilitiesCountInTheCost() {
        for (BattleCard card : new BattleCard[] {BattleCard.PILLAGER_CAPTAIN,
                BattleCard.WITHER, BattleCard.MOOSHROOM, BattleCard.GLOW_SQUID,
                BattleCard.CAMPFIRE})
            check(CostFormula.raw(card) > CostFormula.raw(card.stats()),
                    card.id() + "'s ability counts in its cost");
        for (BattleCard card : new BattleCard[] {BattleCard.PARROT,
                BattleCard.SHEEP, BattleCard.BEACON, BattleCard.MONSTER_SPAWNER})
            check(CostFormula.raw(card) > 0.5D,
                    card.id() + " deals no damage but has a cost");
        for (BattleCard spell : new BattleCard[] {BattleCard.LAVA_BUCKET,
                BattleCard.HARMING_POTION, BattleCard.FIREWORK_ROCKET})
            check(CostFormula.raw(spell) > CostFormula.raw(spell.spell()),
                    spell.id() + "'s rule counts in its cost");
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
