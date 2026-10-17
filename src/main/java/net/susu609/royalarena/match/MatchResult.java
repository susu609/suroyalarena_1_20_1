package net.susu609.royalarena.match;

import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.combat.CombatStats;

import java.util.Map;

/**
 * What the result screen shows once a match is over: the winner (null on a
 * draw), each side's crowns, why it ended, how long the battle ran, and each
 * side's MVP: the card that dealt the most damage to troops and towers.
 *
 * @param blueMvp   null when BLUE dealt no damage
 * @param vsBot     the RED side was the bot (the screen offers a rematch)
 */
public record MatchResult(TeamSide winner, int blueCrowns, int redCrowns,
                          MatchRecord.EndReason endReason, int battleTicks,
                          BattleCard blueMvp, double blueMvpDamage,
                          BattleCard redMvp, double redMvpDamage,
                          boolean vsBot)
{
    public static MatchResult of(MatchInstance match, CombatStats stats,
            boolean vsBot)
    {
        BattleCard[] mvp = new BattleCard[2];
        double[] damage = new double[2];
        for (TeamSide side : TeamSide.values()) {
            for (Map.Entry<BattleCard, CombatStats.CardStats> e :
                    stats.cards(side).entrySet()) {
                double dealt = e.getValue().damage() +
                        e.getValue().towerDamage();
                if (dealt > damage[side.ordinal()]) {
                    damage[side.ordinal()] = dealt;
                    mvp[side.ordinal()] = e.getKey();
                }
            }
        }
        MatchRecord.EndReason reason = match.record().endReason();
        return new MatchResult(match.winner(), match.crowns(TeamSide.BLUE),
                match.crowns(TeamSide.RED),
                reason == null ? MatchRecord.EndReason.ABANDONED : reason,
                match.record().battleTicks(),
                mvp[0], damage[0], mvp[1], damage[1], vsBot);
    }

    public int crowns(TeamSide side) {
        return side == TeamSide.BLUE ? blueCrowns : redCrowns;
    }

    public BattleCard mvp(TeamSide side) {
        return side == TeamSide.BLUE ? blueMvp : redMvp;
    }

    public double mvpDamage(TeamSide side) {
        return side == TeamSide.BLUE ? blueMvpDamage : redMvpDamage;
    }
}
