package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.bot.BotBrain;
import net.susu609.royalarena.bot.BotPersonality;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.CardRole;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Pure-Java checks for the bot personalities and the skills they share:
 * each match draws a personality from its seed, Tank Push saves for a tank
 * at the back, Rush plays early at the bridge, Counter does not open, a low
 * opponent elixir is punished, and no spell wakes a sleeping King.
 */
public final class BotPersonalitySmokeTest {
    private BotPersonalitySmokeTest() {
    }

    public static void run() {
        personalitiesComeFromTheSeed();
        tankPushSavesForATankAtTheBack();
        rushPlaysEarlyAtTheBridge();
        counterDoesNotOpen();
        lowOpponentElixirIsPunished();
        noSpellWakesTheKing();
        System.out.println("RoyalArena bot personality tests: PASS");
    }

    private static MatchInstance battle() {
        MatchInstance match = MatchInstance.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ArenaLayout.standard(new ArenaPoint(0, 80, 0)));
        match.startBattle();
        return match;
    }

    /** First play of a RED bot alone against an idle BLUE: elixir and spot. */
    private record First(BattleCard card, int elixirBefore, int x, int z) {
    }

    private static First firstPlay(MatchInstance match, BotBrain bot,
            int maxTicks)
    {
        CombatEngine engine = new CombatEngine(match);
        for (int i = 0; i < maxTicks; i++) {
            match.tick();
            BotBrain.Play play = bot.tick(match, engine);
            if (play == null)
                continue;
            BattleCard card = match.red().deck().cardAt(play.slot());
            int before = match.red().elixirTenths();
            check(match.tryDeploy(TeamSide.RED, play.slot(), play.x(),
                    play.z()).accepted(), "the server accepts it: " + play);
            return new First(card, before, play.x(), play.z());
        }
        return null;
    }

    private static void personalitiesComeFromTheSeed() {
        Set<BotPersonality> seen = EnumSet.noneOf(BotPersonality.class);
        for (long seed = 1; seed <= 200; seed++) {
            check(BotPersonality.pick(seed) == BotPersonality.pick(seed),
                    "the same seed draws the same personality");
            seen.add(new BotBrain(TeamSide.RED, seed).personality());
        }
        check(seen.size() == BotPersonality.values().length,
                "every personality turns up: " + seen);
        check(BotPersonality.byId("tank_push") == BotPersonality.TANK_PUSH &&
                BotPersonality.byId(" Spell_Siege ") ==
                        BotPersonality.SPELL_SIEGE &&
                BotPersonality.byId("random") == null,
                "config ids, and random means draw one");
        BotBrain bot = new BotBrain(TeamSide.RED, 3L);
        bot.setPersonality(BotPersonality.RUSH);
        check(bot.personality() == BotPersonality.RUSH,
                "the config overrides the drawn one");
    }

    private static void tankPushSavesForATankAtTheBack() {
        MatchInstance match = battle();
        boolean tankInHand = match.red().deck().hand().stream().anyMatch(
                c -> !c.isSpell() && CardRole.of(c).contains(CardRole.TANK));
        First first = firstPlay(match, new BotBrain(TeamSide.RED, 1L,
                BotPersonality.TANK_PUSH), 1200);
        check(first != null, "a Tank Push bot attacks in the end");
        check(first.elixirBefore() >= 90,
                "it saves up to 9 elixir first: " + first.elixirBefore());
        if (tankInHand) {
            check(CardRole.of(first.card()).contains(CardRole.TANK),
                    "and opens with its tank: " + first.card());
            check(first.z() <= match.arena().center().z() -
                            ArenaLayout.RIVER_HALF - 9,
                    "from the back of its half: z=" + first.z());
        }
    }

    private static void rushPlaysEarlyAtTheBridge() {
        MatchInstance match = battle();
        First first = firstPlay(match, new BotBrain(TeamSide.RED, 1L,
                BotPersonality.RUSH), 400);
        check(first != null && first.elixirBefore() < 70,
                "a Rush bot does not wait for 7 elixir");
        check(first.z() == match.arena().center().z() -
                        ArenaLayout.RIVER_HALF - 1,
                "it plays right at the bridge: z=" + first.z());
        check(first.card().cost() <= 3, "with a cheap card: " + first.card());
    }

    private static void counterDoesNotOpen() {
        MatchInstance match = battle();
        First first = firstPlay(match, new BotBrain(TeamSide.RED, 1L,
                BotPersonality.COUNTER), 1200);
        check(first != null && first.elixirBefore() >= 100,
                "a Counter bot only opens rather than waste full elixir");
    }

    private static void lowOpponentElixirIsPunished() {
        MatchInstance calm = battle();
        First waits = firstPlay(calm, new BotBrain(TeamSide.RED, 1L,
                BotPersonality.SPELL_SIEGE), BotBrain.THINK_EVERY_TICKS);
        check(waits == null, "with 5 elixir it usually waits for 7");

        MatchInstance match = battle();
        // BLUE has just spent 4 of its 5 elixir.
        check(match.blue().elixir().spend(4), "BLUE spends");
        First first = firstPlay(match, new BotBrain(TeamSide.RED, 1L,
                BotPersonality.SPELL_SIEGE), BotBrain.THINK_EVERY_TICKS);
        check(first != null, "it attacks at once while BLUE has no elixir");
    }

    private static void noSpellWakesTheKing() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        // Hand: Big Slime, Golem, Arrow Storm, Fireball (see SpellSmokeTest).
        for (int i = 0; i < 4; i++)
            match.red().deck().play(0);
        ArenaPoint king = match.arena().towerPoint(TowerId.BLUE_KING);
        for (int i = 0; i < 4; i++)
            engine.add(UUID.randomUUID(), TeamSide.BLUE,
                    BattleCard.SKELETONS.stats(), king.x() - 1.5D + i,
                    king.z() - 2.5D);
        BotBrain bot = new BotBrain(TeamSide.RED, 5L,
                BotPersonality.SPELL_SIEGE);
        for (int i = 0; i < 100; i++) {
            BotBrain.Play play = bot.tick(match, engine);
            if (play == null)
                continue;
            BattleCard card = match.red().deck().cardAt(play.slot());
            if (!card.isSpell())
                continue;
            check(match.arena().distanceToTower(play.x() + 0.5D,
                            play.z() + 0.5D, TowerId.BLUE_KING) >
                            card.spell().radius(),
                    "no spell touches the sleeping King: " + card + " " +
                            play);
        }
        check(!engine.kingAwake(TeamSide.BLUE), "the BLUE King sleeps on");
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
