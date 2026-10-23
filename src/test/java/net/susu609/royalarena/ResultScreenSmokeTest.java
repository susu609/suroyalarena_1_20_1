package net.susu609.royalarena;

import net.susu609.royalarena.sim.Sim;
import net.susu609.royalarena.bot.BotDeck;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.combat.CombatStats;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.MatchResult;
import net.susu609.royalarena.match.ResultTimeline;
import net.susu609.royalarena.match.TeamSide;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Pure-Java checks for the result screen: the order its animation plays in
 * and the numbers a finished match hands it.
 */
public final class ResultScreenSmokeTest {
    private ResultScreenSmokeTest() {
    }

    public static void run() {
        crownsPopInOrder();
        cuesFollowTheAnimation();
        aBotMatchGivesItsResult();
        System.out.println("RoyalArena result screen tests: PASS");
    }

    private static void crownsPopInOrder() {
        // You won 3-1: the opponent's crown first, then yours left, right,
        // King.
        ResultTimeline t = new ResultTimeline(3, 1);
        int theirLeft = t.crownTick(false, 0);
        check(theirLeft == ResultTimeline.FIRST_CROWN, "the opponent's first");
        check(t.crownTick(false, 1) < 0 && t.crownTick(false, 2) < 0,
                "the opponent earned only the left crown");
        int left = t.crownTick(true, 0);
        int right = t.crownTick(true, 2);
        int king = t.crownTick(true, 1);
        check(theirLeft < left && left < right && right < king,
                "then yours: left, right, King");
        check(t.wordTick() > king, "the word after the last crown");
        check(t.finalTick() > t.wordTick(), "the buttons last");
        check(!t.crownShown(true, 1, king - 0.5F) &&
                t.crownShown(true, 1, king), "a crown shows from its tick");
        near(t.crownScale(true, 1, king), ResultTimeline.POP_SCALE,
                "it pops big");
        near(t.crownScale(true, 1, king + ResultTimeline.POP_TICKS), 1.0D,
                "and settles");
        near(t.wordScale(t.wordTick()), ResultTimeline.WORD_SCALE,
                "the word zooms in");
        near(t.dim(0.0F), ResultTimeline.DIM_START, "light at first");
        near(t.dim(100.0F), ResultTimeline.DIM_END, "then dark");

        ResultTimeline none = new ResultTimeline(0, 0);
        check(none.wordTick() == ResultTimeline.FIRST_CROWN +
                ResultTimeline.WORD_DELAY, "no crowns: the word comes early");
        for (int slot = 0; slot < 3; slot++)
            check(!none.crownShown(true, slot, 1000.0F), "no crown shows");
    }

    private static void cuesFollowTheAnimation() {
        ResultTimeline t = new ResultTimeline(2, 1);
        List<ResultTimeline.Cue> all = t.cues(-1.0F, t.finalTick());
        check(all.size() == 1 + 3 + 1 + 1, "ribbons, 3 crowns, word, final: "
                + all);
        check(all.get(0).kind() == ResultTimeline.CueKind.RIBBONS, "first");
        for (int i = 0; i < 3; i++)
            check(all.get(1 + i).index() == i, "crown pitches rise");
        // Frame by frame gives the same cues, once each.
        int count = 0;
        float prev = -1.0F;
        for (float now = 0.0F; now <= t.finalTick(); now += 0.37F) {
            count += t.cues(prev, now).size();
            prev = now;
        }
        count += t.cues(prev, t.finalTick()).size();
        check(count == all.size(), "every cue once: " + count);
    }

    private static void aBotMatchGivesItsResult() {
        Map<BattleCard, Integer> plays = new EnumMap<>(BattleCard.class);
        Sim.Outcome outcome = Sim.botMatch(BotDeck.random(new Random(5)),
                BotDeck.random(new Random(6)), 41L, plays);
        MatchInstance match = outcome.match();
        CombatStats stats = outcome.engine().stats();
        MatchResult result = MatchResult.of(match, stats, true);
        check(result.winner() == match.winner(), "winner");
        for (TeamSide side : TeamSide.values()) {
            check(result.crowns(side) == match.crowns(side), side + " crowns");
            BattleCard mvp = result.mvp(side);
            check(mvp != null, side + " has an MVP");
            for (Map.Entry<BattleCard, CombatStats.CardStats> e :
                    stats.cards(side).entrySet())
                check(e.getValue().damage() + e.getValue().towerDamage() <=
                        result.mvpDamage(side) + 1.0E-9D,
                        e.getKey().id() + " did not beat the MVP");
        }
        check(result.endReason() == match.record().endReason(), "reason");
        check(result.battleTicks() == match.record().battleTicks(), "length");
    }

    private static void near(double actual, double expected, String name) {
        check(Math.abs(actual - expected) < 1.0E-5D,
                name + ": " + actual + " vs " + expected);
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
