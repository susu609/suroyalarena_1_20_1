package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.CostFormula;
import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.combat.ChildSpawn;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.combat.TickResult;
import net.susu609.royalarena.combat.TickResult.TribeEvent.Kind;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.TeamSide;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Batch 4: Illusioner, Axolotl, Endermite, Allay, the new Magma Cube, and
 * the Wither Skull, Ender Pearl and Water Bucket spells. Units stand still
 * (only {@code engine.tick()}), BLUE's at z 2.2 and RED's at z -2.2: out of
 * every tower's reach, the river between them.
 */
public final class MobsWave6SmokeTest {
    private MobsWave6SmokeTest() {
    }

    public static void run() {
        illusionerBlindsAndSplitsOffImages();
        axolotlPlaysDeadOnce();
        endermitesLureEndermenAndVanish();
        allayFollowsAndHeals();
        witherSkullWithersAndBlocksHealing();
        enderPearlMovesOneTroop();
        waterBucketDousesPushesAndHurts();
        magmaCubeLeapsOntoTheArcher();
        abilitiesCountInTheCost();
        jockeysHaveTwoLives();
        System.out.println("RoyalArena wave 6 mobs smoke tests: PASS");
    }

    private static MatchInstance battle() {
        MatchInstance match = MatchInstance.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ArenaLayout.standard(new ArenaPoint(0, 80, 0)));
        match.startBattle();
        return match;
    }

    private static CombatUnit unit(CombatEngine engine, BattleCard card,
            TeamSide side, double x, double z)
    {
        return engine.add(UUID.randomUUID(), side, card.stats(), x, z);
    }

    /**
     * A still shooter of no card: it hits for {@code damage} once every
     * {@code interval} ticks from 6 blocks.
     */
    private static CombatUnit shooter(CombatEngine engine, TeamSide side,
            double damage, int interval, double x, double z)
    {
        return engine.add(UUID.randomUUID(), side,
                new TroopStats(1, 200, damage, interval, 6.0D, 0.0D, 0.3D),
                x, z);
    }

    private static boolean has(TickResult r, Kind kind) {
        return r.tribeEvents().stream().anyMatch(e -> e.kind() == kind);
    }

    private static void illusionerBlindsAndSplitsOffImages() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit illusioner = unit(engine, BattleCard.ILLUSIONER,
                TeamSide.BLUE, 0.0D, 2.2D);
        CombatUnit dummy = shooter(engine, TeamSide.RED, 13.0D, 400, 0.0D,
                -2.2D);
        List<ChildSpawn> images = new ArrayList<>();
        boolean blinded = false;
        for (int t = 0; t < 70 && !blinded; t++) {
            TickResult r = engine.tick();
            images.addAll(r.spawns());
            blinded = has(r, Kind.BLIND);
        }
        check(images.size() == BattleCard.ILLUSION_COUNT,
                "under half health the Illusioner splits off 3 images: " +
                        images.size());
        check(images.stream().allMatch(i -> i.health() == 1.0D &&
                i.generation() == 1), "images have 1 health");
        check(blinded && dummy.blinded(), "the 3rd arrow blinds");
        check(Math.abs(dummy.range() - 6.0D * BattleCard.BLIND_RANGE) < 1.0E-9D,
                "a blinded troop's range is cut to 60%");

        List<UUID> ids = new ArrayList<>();
        for (ChildSpawn image : images)
            ids.add(engine.add(UUID.randomUUID(), image).id());
        int imageShots = 0;
        int vanished = 0;
        for (int t = 0; t <= BattleCard.ILLUSION_TICKS; t++) {
            TickResult r = engine.tick();
            imageShots += r.attacks().stream().filter(a ->
                    ids.contains(a.attackerUnit())).count();
            vanished += r.tribeEvents().stream().filter(e ->
                    e.kind() == Kind.VANISH && ids.contains(e.unit())).count();
        }
        check(imageShots == 0, "images never shoot");
        check(vanished == BattleCard.ILLUSION_COUNT,
                "images vanish after 4 s: " + vanished);
        check(illusioner.alive(), "the Illusioner itself stays");
    }

    private static void axolotlPlaysDeadOnce() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit axolotl = unit(engine, BattleCard.AXOLOTL, TeamSide.BLUE,
                0.0D, 2.2D);
        shooter(engine, TeamSide.RED, 13.0D, 180, 0.0D, -2.2D);
        boolean down = false;
        for (int t = 0; t < 40 && !down; t++)
            down = has(engine.tick(), Kind.PLAY_DEAD);
        check(down && axolotl.playingDead(), "hurt under 30%, it plays dead");
        CombatUnit second = shooter(engine, TeamSide.RED, 1.0D, 20, 0.5D,
                -2.2D);
        engine.tick();
        check(second.target() == null, "nobody picks a troop playing dead");
        double low = axolotl.health();
        boolean up = false;
        for (int t = 0; t < BattleCard.PLAY_DEAD_TICKS + 2 && !up; t++)
            up = has(engine.tick(), Kind.WAKE);
        check(up && !axolotl.playingDead(), "it gets up after 3 s");
        check(axolotl.health() >= low + axolotl.maxHealth() * 0.45D,
                "and has half its health back: " + low + " -> " +
                        axolotl.health());
        boolean again = false;
        for (int t = 0; t < 200 && axolotl.alive(); t++)
            again |= has(engine.tick(), Kind.PLAY_DEAD);
        check(!again, "it plays dead only once");
    }

    private static void endermitesLureEndermenAndVanish() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit enderman = unit(engine, BattleCard.ENDERMAN, TeamSide.RED,
                0.0D, 2.2D);
        unit(engine, BattleCard.ZOMBIE, TeamSide.BLUE, 0.9D, 2.2D);
        List<CombatUnit> mites = new ArrayList<>();
        for (int i = 0; i < 4; i++)
            mites.add(unit(engine, BattleCard.ENDERMITE, TeamSide.BLUE,
                    -3.0D - 0.3D * i, 2.2D));
        engine.tick();
        check(mites.stream().anyMatch(m -> m.id().equals(enderman.target())),
                "the Enderman leaves the closer Zombie for an Endermite");
        for (int t = 0; t < BattleCard.ENDERMITE_TICKS; t++)
            engine.tick();
        check(mites.stream().noneMatch(CombatUnit::alive),
                "Endermites vanish after 10 s");
    }

    private static void allayFollowsAndHeals() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit allay = unit(engine, BattleCard.ALLAY, TeamSide.BLUE,
                1.6D, 2.2D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.BLUE,
                0.0D, 2.2D);
        shooter(engine, TeamSide.RED, 8.0D, 1000, 0.0D, -2.2D);
        TickResult first = engine.tick();
        check(first.moves().stream().anyMatch(m -> m.unit().equals(allay.id())),
                "the Allay flies to its troop");
        check(first.attacks().stream().noneMatch(a ->
                allay.id().equals(a.attackerUnit())), "the Allay never attacks");
        for (int t = 0; t < 8; t++)
            engine.tick();
        double hurt = zombie.health();
        check(hurt < zombie.maxHealth(), "the Zombie was shot");
        boolean aura = false;
        for (int t = 0; t < 25; t++)
            aura |= has(engine.tick(), Kind.HEAL_AURA);
        check(aura && zombie.health() > hurt, "the Allay heals it: " + hurt +
                " -> " + zombie.health());
    }

    private static void witherSkullWithersAndBlocksHealing() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                0.0D, -2.2D);
        CombatUnit wither = unit(engine, BattleCard.WITHER_SKELETON,
                TeamSide.RED, 1.0D, -2.2D);
        unit(engine, BattleCard.ALLAY, TeamSide.RED, 0.5D, -2.6D);
        engine.castSpell(TeamSide.BLUE, BattleCard.WITHER_SKULL, 0.5D, -2.2D);
        engine.tick();
        check(zombie.withered() && !wither.withered(),
                "the skull withers the Zombie, not the Wither Skeleton");
        // A Wither Skeleton's own blow withers too (it is immune itself).
        CombatUnit victim = unit(engine, BattleCard.ZOMBIE, TeamSide.BLUE,
                1.0D, 2.2D);
        CombatUnit skeleton = unit(engine, BattleCard.WITHER_SKELETON,
                TeamSide.RED, 1.0D, 3.0D);
        check(skeleton.alive() && victim.alive(), "a brawl under way");
        for (int t = 0; t < 60 && !victim.withered(); t++)
            engine.tick();
        check(victim.withered() || !victim.alive(),
                "a Wither Skeleton's blow withers what it hits");
        double after = zombie.health();
        double lowest = after;
        for (int t = 0; t < BattleCard.WITHER_TICKS - 5; t++) {
            engine.tick();
            check(zombie.health() <= lowest + 1.0E-9D,
                    "a withered troop heals nothing (the Allay is next to it)");
            lowest = zombie.health();
        }
        check(after - zombie.health() > 4.0D,
                "about 1 a second through armour: " + (after - zombie.health()));
    }

    private static void enderPearlMovesOneTroop() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit own = unit(engine, BattleCard.ZOMBIE, TeamSide.BLUE, 0.0D,
                6.0D);
        engine.castSpell(TeamSide.BLUE, BattleCard.ENDER_PEARL, 0.0D, 6.0D);
        TickResult r = engine.tick();
        check(r.teleports().size() == 1, "one troop moves");
        check(own.z() < 2.0D && own.z() > 1.0D,
                "an own troop goes forward up to the river, not across: " +
                        own.z());
        CombatUnit enemy = unit(engine, BattleCard.ZOMBIE, TeamSide.RED, 4.0D,
                4.0D);
        engine.castSpell(TeamSide.BLUE, BattleCard.ENDER_PEARL, 4.0D, 4.0D);
        engine.tick();
        check(enemy.z() < -1.0D, "an enemy is sent back to its own half: " +
                enemy.z());
    }

    private static void waterBucketDousesPushesAndHurts() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit blaze = unit(engine, BattleCard.BLAZE, TeamSide.RED, -0.6D,
                -2.2D);
        // A Ravager survives the Fireball that sets it burning.
        CombatUnit zombie = unit(engine, BattleCard.RAVAGER, TeamSide.RED,
                1.6D, -2.2D);
        CombatUnit strider = unit(engine, BattleCard.STRIDER, TeamSide.RED,
                -1.9D, -2.6D);
        engine.castSpell(TeamSide.BLUE, BattleCard.FIREBALL, 2.6D, -2.2D);
        for (int t = 0; t < 12; t++)
            engine.tick();
        check(zombie.burnTicks() > 0 && !engine.firePatches().isEmpty(),
                "the Ravager burns and the Strider left a fire trail");
        double blazeBefore = blaze.health();
        double zombieX = zombie.x();
        engine.castSpell(TeamSide.BLUE, BattleCard.WATER_BUCKET, 0.0D, -2.2D);
        engine.tick();
        check(zombie.burnTicks() == 0, "water puts the Ravager out");
        check(engine.firePatches().isEmpty(), "and the fire trail");
        check(blazeBefore - blaze.health() >= BattleCard.WATER_DAMAGE - 0.5D,
                "water hurts the Blaze: " + (blazeBefore - blaze.health()));
        check(zombie.x() > zombieX + 0.5D,
                "enemies are pushed away from the splash: " + zombie.x());
        check(strider.health() < strider.maxHealth(), "and the Strider");
    }

    private static void magmaCubeLeapsOntoTheArcher() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit cube = unit(engine, BattleCard.MAGMA_CUBE, TeamSide.BLUE,
                0.0D, 4.5D);
        unit(engine, BattleCard.ZOMBIE, TeamSide.RED, -2.2D, 4.5D);
        CombatUnit archer = unit(engine, BattleCard.SKELETON_ARCHER,
                TeamSide.RED, 1.0D, 2.0D);
        TickResult r = engine.tick();
        check(has(r, Kind.LEAP) && r.teleports().size() == 1,
                "the cube leaps");
        check(archer.id().equals(cube.target()),
                "onto the archer, not the nearer Zombie");
        check(r.attacks().stream().anyMatch(a ->
                        cube.id().equals(a.attackerUnit()) &&
                                a.splash() == BattleCard.LEAP_SPLASH),
                "and splashes where it lands");
        check(!has(engine.tick(), Kind.LEAP), "then waits before leaping again");
    }

    private static void abilitiesCountInTheCost() {
        for (BattleCard card : new BattleCard[] {BattleCard.ILLUSIONER,
                BattleCard.AXOLOTL, BattleCard.ENDERMITE, BattleCard.ALLAY,
                BattleCard.MAGMA_CUBE})
            check(CostFormula.abilityModifier(card) != 1.0D,
                    card.id() + "'s ability is in its cost");
        for (BattleCard card : new BattleCard[] {BattleCard.WITHER_SKULL,
                BattleCard.ENDER_PEARL, BattleCard.WATER_BUCKET})
            check(CostFormula.spellAbility(card) > 0.0D &&
                            CostFormula.elixir(card) == card.cost(),
                    card.id() + "'s rule is in its cost");
    }

    /**
     * A jockey carries its rider from the start: the rider shoots from the
     * saddle and never walks; whichever dies first, the other goes on.
     */
    private static void jockeysHaveTwoLives() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit chicken = unit(engine, BattleCard.CHICKEN_JOCKEY,
                TeamSide.BLUE, 0.0D, 2.2D);
        CombatUnit baby = engine.addRider(UUID.randomUUID(), chicken);
        TickResult first = engine.tick();
        check(baby.mount() != null && first.moves().stream().noneMatch(m ->
                        m.unit().equals(baby.id())),
                "the rider sits on its mount, it does not walk");
        engine.castSpell(TeamSide.RED, BattleCard.ZAP, 0.0D, 2.2D);
        TickResult zapped = engine.tick();
        check(!chicken.alive() && baby.alive(),
                "a Zap downs the chicken, not the baby zombie");
        check(zapped.deaths().stream().allMatch(d -> d.children().isEmpty()),
                "no second zombie appears: the rider was already there");
        // (once the Zap's stun wears off)
        boolean walks = false;
        for (int t = 0; t < 15 && !walks; t++)
            walks = engine.tick().moves().stream().anyMatch(m ->
                    m.unit().equals(baby.id()));
        check(baby.mount() == null && walks, "the baby zombie walks on alone");

        CombatEngine other = new CombatEngine(battle());
        CombatUnit camel = unit(other, BattleCard.CAMEL, TeamSide.BLUE, 0.0D,
                2.2D);
        CombatUnit husk = other.addRider(UUID.randomUUID(), camel);
        for (int i = 0; i < 8 && husk.alive(); i++) {
            other.castSpell(TeamSide.RED, BattleCard.ZAP, 0.0D, 2.2D);
            for (int t = 0; t < 10; t++)
                other.tick();
        }
        check(!husk.alive() && camel.alive(),
                "the Husk falls first; the Camel lives on");
        check(other.tick().moves().stream().anyMatch(m ->
                        m.unit().equals(camel.id())),
                "and keeps walking to the towers");
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
