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
                         List<Attack> landed, List<Teleport> teleports,
                         List<TribeEvent> tribeEvents)
{
    /**
     * A tribe's special rule (docs/TRIBES.md) or a card's own (a Panda's
     * sneeze, a Camel's dash) fired on a unit this tick.
     */
    public record TribeEvent(UUID unit, Kind kind, double x, double z) {
        public enum Kind {
            REVIVE, TOTEM, LAST_FLAME, DODGE, VOLLEY, SNEEZE, DASH, LEVITATE,
            SONIC_BOOM, BLAST
        }
    }

    /**
     * A spell struck at (x, z) this tick: pulse {@code index} of
     * {@code card.spell().pulses()}, hitting (or healing) {@code hits} units
     * and towers, which stood at {@code strikes}.
     */
    public record SpellPulse(TeamSide side, BattleCard card, double x,
                             double z, int index, int hits,
                             List<Strike> strikes)
    {
    }

    /** Where a spell hit one unit or tower ({@code unit} null for a tower). */
    public record Strike(UUID unit, TowerId tower, double x, double z) {
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
     * the target, a melee hit with {@code knockback} pushes its victim, and
     * one with {@code slowTicks} slows it. A hit with {@code line} also
     * hurts every enemy on the line that wide from (fromX, fromZ), where the
     * attacker stood, through the target to {@code lineLength} blocks.
     */
    public record Attack(UUID attackerUnit, TowerId attackerTower,
                         UUID victimUnit, TowerId victimTower, double damage,
                         boolean ranged, int flightTicks, TeamSide side,
                         double x, double z, double splash, double knockback,
                         int slowTicks, double line, double lineLength,
                         double fromX, double fromZ)
    {
        /** A plain hit: no splash, no knockback; (x, z) is the target. */
        public Attack(UUID attackerUnit, TowerId attackerTower,
                UUID victimUnit, TowerId victimTower, double damage,
                boolean ranged, int flightTicks, TeamSide side, double x,
                double z)
        {
            this(attackerUnit, attackerTower, victimUnit, victimTower, damage,
                    ranged, flightTicks, side, x, z, 0.0D, 0.0D, 0, 0.0D, 0.0D,
                    x, z);
        }
    }

    /**
     * A unit that teleported this tick to (x, z); the engine already moved
     * it, the world only has to move the mob.
     */
    public record Teleport(UUID unit, double x, double z) {
    }

    /** A unit that died this tick, with the children it splits into. */
    public record Death(UUID unit, double x, double z,
                        List<ChildSpawn> children)
    {
    }
}
