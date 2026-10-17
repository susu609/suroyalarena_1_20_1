package net.susu609.royalarena.combat;

import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.List;
import java.util.UUID;

/**
 * Everything one combat tick decided, for the world layer to act out:
 * {@code attacks} are the blows struck and shots fired this tick,
 * {@code landed} the ones whose damage was applied this tick (melee blows of
 * this tick and arrows arriving now).
 */
public record TickResult(List<Move> moves, List<Attack> attacks,
                         List<Death> deaths, List<TowerId> destroyedTowers,
                         List<SpellPulse> spellPulses, List<Push> pushes,
                         List<Attack> landed)
{
    /**
     * A spell struck at (x, z) this tick: pulse {@code index} of
     * {@code card.spell().pulses()}, hitting {@code hits} units and towers.
     */
    public record SpellPulse(TeamSide side, BattleCard card, double x,
                             double z, int index, int hits)
    {
    }

    /** A unit knocked back by (dx, dz); the engine already moved it. */
    public record Push(UUID unit, double dx, double dz) {
    }

    /** A unit should walk toward (x, z) this tick at {@code speed} blocks. */
    public record Move(UUID unit, double x, double z, double speed) {
    }

    /**
     * A blow or a shot. Exactly one of {@code attackerUnit}/{@code
     * attackerTower} and one of {@code victimUnit}/{@code victimTower} is
     * set. A melee blow lands at once ({@code flightTicks} 0); a ranged shot
     * flies for {@code flightTicks} and its damage lands then (see
     * {@link TickResult#landed}). {@code side} is the attacker's team,
     * (x, z) where the target stood when it was struck or shot at; a hit
     * with {@code splash} also hits every enemy within that many blocks of
     * the target, and a melee hit with {@code knockback} pushes its victim.
     */
    public record Attack(UUID attackerUnit, TowerId attackerTower,
                         UUID victimUnit, TowerId victimTower, double damage,
                         boolean ranged, int flightTicks, TeamSide side,
                         double x, double z, double splash, double knockback)
    {
        /** A plain hit: no splash, no knockback; (x, z) is the target. */
        public Attack(UUID attackerUnit, TowerId attackerTower,
                UUID victimUnit, TowerId victimTower, double damage,
                boolean ranged, int flightTicks, TeamSide side, double x,
                double z)
        {
            this(attackerUnit, attackerTower, victimUnit, victimTower, damage,
                    ranged, flightTicks, side, x, z, 0.0D, 0.0D);
        }
    }

    /** A unit that died this tick, with the children it splits into. */
    public record Death(UUID unit, double x, double z,
                        List<ChildSpawn> children)
    {
    }
}
