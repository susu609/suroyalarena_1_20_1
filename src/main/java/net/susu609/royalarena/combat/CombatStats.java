package net.susu609.royalarena.combat;

import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.match.TeamSide;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * What each card did in one match, counted by {@link CombatEngine} as the
 * damage lands: health really taken off enemy troops and towers (a blow
 * bigger than what is left counts only what was left), troops finished off,
 * health given back by healing. Towers shooting troops count apart.
 */
public final class CombatStats {
    /** One card of one side. */
    public static final class CardStats {
        private double damage;
        private double towerDamage;
        private int kills;
        private double healing;

        /** Health taken off enemy troops. */
        public double damage() {
            return damage;
        }
        /** Health taken off enemy towers. */
        public double towerDamage() {
            return towerDamage;
        }
        /** Enemy troops (and split children) it finished off. */
        public int kills() {
            return kills;
        }
        /** Health given back to its own troops. */
        public double healing() {
            return healing;
        }
    }

    private final Map<TeamSide, Map<BattleCard, CardStats>> cards =
            new EnumMap<>(TeamSide.class);
    private final Map<TeamSide, double[]> towerShots =
            new EnumMap<>(TeamSide.class);

    public CombatStats() {
        for (TeamSide side : TeamSide.values()) {
            cards.put(side, new EnumMap<>(BattleCard.class));
            towerShots.put(side, new double[1]);
        }
    }

    /** Every card of {@code side} that did something, by card. */
    public Map<BattleCard, CardStats> cards(TeamSide side) {
        return Collections.unmodifiableMap(cards.get(side));
    }

    /** What {@code card} of {@code side} did (all zero if nothing). */
    public CardStats card(TeamSide side, BattleCard card) {
        CardStats stats = cards.get(side).get(card);
        return stats == null ? new CardStats() : stats;
    }

    /** Health the towers of {@code side} took off enemy troops. */
    public double towerShots(TeamSide side) {
        return towerShots.get(side)[0];
    }

    private CardStats of(TeamSide side, BattleCard card) {
        return cards.get(side).computeIfAbsent(card, c -> new CardStats());
    }

    void troopDamage(TeamSide side, BattleCard source, double amount,
            boolean killed)
    {
        if (amount <= 0.0D && !killed)
            return;
        if (source == null) {
            towerShots.get(side)[0] += Math.max(0.0D, amount);
            return;
        }
        CardStats stats = of(side, source);
        stats.damage += Math.max(0.0D, amount);
        if (killed)
            stats.kills++;
    }

    void towerDamage(TeamSide side, BattleCard source, double amount) {
        if (amount > 0.0D && source != null)
            of(side, source).towerDamage += amount;
    }

    void healing(TeamSide side, BattleCard source, double amount) {
        if (amount > 0.0D && source != null)
            of(side, source).healing += amount;
    }
}
