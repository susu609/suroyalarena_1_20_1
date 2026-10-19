package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.CardRole;
import net.susu609.royalarena.card.TroopTraits;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.combat.TickResult;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;
import net.susu609.royalarena.tower.TowerKind;

import java.util.UUID;

/**
 * Pure-Java checks for troop traits: Creeper self-destruct splash, Witch
 * splash, Ravager knockback, tower-only targeting. Units stand around the river, out of tower
 * range, and the harness never moves them.
 */
public final class TraitSmokeTest {
    private TraitSmokeTest() {
    }

    public static void run() {
        newCardsHaveTheirRoles();
        creeperExplodesOnce();
        creeperHurtsTheTowerItReaches();
        witchSplashesAroundItsTarget();
        ravagerKnocksItsVictimBack();
        goatRamsFarther();
        buildingTargetersIgnoreTroops();
        strayArrowsSlow();
        System.out.println("RoyalArena trait smoke tests: PASS");
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

    private static void newCardsHaveTheirRoles() {
        check(CardRole.of(BattleCard.RAVAGER).contains(CardRole.TANK),
                "Ravager is a tank");
        check(CardRole.of(BattleCard.WITCH).contains(CardRole.RANGED) &&
                CardRole.of(BattleCard.WITCH).contains(CardRole.AREA),
                "Witch is ranged area damage");
        check(CardRole.of(BattleCard.CREEPER).contains(CardRole.AREA),
                "Creeper is area damage");
        check(CardRole.of(BattleCard.WOLVES).contains(CardRole.SWARM),
                "Wolves are a swarm");
        check(!CardRole.of(BattleCard.IRON_GOLEM).contains(CardRole.BUILDINGS) &&
                CardRole.of(BattleCard.IRON_GOLEM).contains(CardRole.TANK) &&
                CardRole.of(BattleCard.HOGLIN).contains(CardRole.BUILDINGS) &&
                !CardRole.of(BattleCard.RAVAGER).contains(CardRole.BUILDINGS),
                "the Hoglin targets towers; the Iron Golem and Ravager fight");
        check(CardRole.of(BattleCard.PILLAGER).contains(CardRole.RANGED),
                "the Pillager is ranged");
        check(CardRole.of(BattleCard.MAGMA_CUBE).contains(CardRole.TANK) &&
                BattleCard.MAGMA_CUBE.stats().split().generations() == 1,
                "the Magma Cube is a tank that splits once");
        for (BattleCard card : BattleCard.values())
            check(card.isSpell() || card.stats().range() <
                    TowerKind.PRINCESS.range(), card.id() +
                    " is outranged by a Princess tower");
        check(BattleCard.SKELETONS.modelScale() < 1.0F &&
                BattleCard.SPIDER.modelScale() < 1.0F &&
                BattleCard.ZOMBIE.modelScale() == 1.0F,
                "the swarms are drawn smaller");
    }

    private static void creeperExplodesOnce() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit creeper = unit(engine, BattleCard.CREEPER, TeamSide.BLUE,
                0.5D, 1.0D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                0.5D, 0.0D);
        CombatUnit spider = unit(engine, BattleCard.SPIDER, TeamSide.RED,
                2.0D, 0.0D);
        CombatUnit far = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                -4.5D, 0.0D);
        // Inside the blast, but out of reach of the enemies' own attacks.
        CombatUnit friend = unit(engine, BattleCard.ZOMBIE, TeamSide.BLUE,
                -1.5D, 1.0D);
        TickResult result = engine.tick();
        check(result.attacks().size() >= 1 &&
                result.attacks().stream().anyMatch(
                        a -> creeper.id().equals(a.attackerUnit()) &&
                                a.splash() > 0.0D),
                "the Creeper strikes with splash");
        check(!creeper.alive() && result.deaths().stream().anyMatch(
                d -> d.unit().equals(creeper.id())),
                "the Creeper dies when it explodes");
        check(!zombie.alive(), "its target takes 30 and dies");
        check(!spider.alive(), "a Spider 1.5 blocks away is caught too");
        near(far.health(), 20.0D, "enemies outside the blast are safe");
        near(friend.health(), 20.0D, "the blast spares its own side");
        for (int i = 0; i < 40; i++)
            engine.tick();
        check(engine.unit(creeper.id()) == null, "it explodes only once");
    }

    private static void creeperHurtsTheTowerItReaches() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        ArenaPoint tower = match.arena().towerPoint(TowerId.RED_LEFT);
        // Just in front of the tower, so it hits it at once.
        unit(engine, BattleCard.CREEPER, TeamSide.BLUE, tower.x(),
                tower.z() + 2.6D);
        engine.tick();
        check(match.tower(TowerId.RED_LEFT).health() == 110 - 30,
                "a Creeper deals full damage to the tower: " +
                        match.tower(TowerId.RED_LEFT).health());
    }

    private static void witchSplashesAroundItsTarget() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        unit(engine, BattleCard.WITCH, TeamSide.BLUE, 0.5D, 4.0D);
        CombatUnit target = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                0.5D, -0.5D);
        CombatUnit beside = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                1.7D, -0.5D);
        CombatUnit away = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                5.0D, -0.5D);
        TickResult shot = engine.tick();
        int flight = shot.attacks().stream()
                .filter(a -> a.ranged() && a.splash() > 0.0D)
                .findFirst().orElseThrow().flightTicks();
        for (int i = 0; i < flight; i++)
            engine.tick();
        double hit = 20.0D - 8.0D * BattleCard.ZOMBIE.stats().armorFactor();
        near(target.health(), hit,
                "the potion hits its target for 8, less the zombie's armour");
        near(beside.health(), hit, "and a zombie beside it");
        near(away.health(), 20.0D, "but not one 4.5 blocks away");
    }

    private static void ravagerKnocksItsVictimBack() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit ravager = unit(engine, BattleCard.RAVAGER, TeamSide.BLUE,
                0.5D, 2.5D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                0.5D, 0.0D);
        TickResult result = engine.tick();
        check(result.pushes().stream().anyMatch(
                p -> p.unit().equals(zombie.id())),
                "the Ravager's hit pushes the zombie");
        near(zombie.health(),
                20.0D - 8.0D * BattleCard.ZOMBIE.stats().armorFactor(),
                "the hit deals 8, less the zombie's armour");
        // 1 block away lands in the river band, so it is put on RED's bank.
        near(zombie.z(), -1.5D, "pushed straight away, kept on land");
        near(zombie.x(), 0.5D, "straight away from the Ravager");
        near(ravager.z(), 2.5D, "the Ravager stays put");
    }

    private static void goatRamsFarther() {
        var goat = BattleCard.GOAT.stats();
        check(goat.traits().knockback() >= 3.0D *
                        BattleCard.RAVAGER.stats().traits().knockback() &&
                        goat.health() < BattleCard.ZOMBIE.stats().health() &&
                        goat.attackTicks() >= 40 && BattleCard.GOAT.cost() == 1,
                "Goat: a 1-elixir ram, 3x the Ravager's push, fragile, slow swing");
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit ram = unit(engine, BattleCard.GOAT, TeamSide.BLUE, 0.5D, 6.0D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                0.5D, 4.8D);
        TickResult result = engine.tick();
        check(result.pushes().stream().anyMatch(
                p -> p.unit().equals(zombie.id())), "the ram pushes the zombie");
        check(zombie.z() <= 4.8D - 2.5D, "about 3 blocks back: " + zombie.z());
        near(ram.z(), 6.0D, "the Goat stays put");
    }

    private static void buildingTargetersIgnoreTroops() {
        for (BattleCard card : new BattleCard[] {BattleCard.HOGLIN})
        {
            MatchInstance match = battle();
            CombatEngine engine = new CombatEngine(match);
            CombatUnit tank = unit(engine, card, TeamSide.BLUE, -6.0D, -2.0D);
            CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                    -6.0D, -3.5D);
            TickResult result = engine.tick();
            check(tank.target() == null, card.id() + " ignores the Zombie");
            check(result.moves().stream().anyMatch(
                    m -> m.unit().equals(tank.id())),
                    card.id() + " keeps walking to its tower");
            check(result.attacks().stream().noneMatch(
                    a -> tank.id().equals(a.attackerUnit())),
                    card.id() + " does not hit the Zombie");
            check(tank.id().equals(zombie.target()),
                    "the Zombie still fights the " + card.id());
        }
    }

    private static void strayArrowsSlow() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit stray = unit(engine, BattleCard.STRAY, TeamSide.BLUE,
                0.5D, 3.0D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                0.5D, -3.0D);
        double normal = BattleCard.ZOMBIE.stats().speedPerTick();
        boolean slowedWalk = false;
        for (int tick = 0; tick < 40 && !slowedWalk; tick++) {
            TickResult r = engine.tick();
            for (TickResult.Move m : r.moves()) {
                if (m.unit().equals(zombie.id()) && zombie.slowed())
                    slowedWalk = Math.abs(m.speed() - normal *
                            TroopTraits.SLOW_SPEED) < 1.0E-9D;
            }
        }
        check(slowedWalk, "a Zombie hit by a Stray walks at 60%");
        check(zombie.slowTicks() <= 20, "for a second at most");
        check(stray.alive(), "the Stray is fine");
    }

    private static void near(double actual, double expected, String name) {
        check(Math.abs(actual - expected) < 1.0E-6D,
                name + ": " + actual + " vs " + expected);
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
