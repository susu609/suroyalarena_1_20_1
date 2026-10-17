package net.susu609.royalarena.combat;

import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.List;
import java.util.UUID;

/** Everything one combat tick decided, for the world layer to act out. */
public record TickResult(List<Move> moves, List<Attack> attacks,
                         List<Death> deaths, List<TowerId> destroyedTowers,
                         List<SpellPulse> spellPulses, List<Push> pushes)
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
     * A hit. Exactly one of {@code attackerUnit}/{@code attackerTower} and
     * one of {@code victimUnit}/{@code victimTower} is set.
     */
    public record Attack(UUID attackerUnit, TowerId attackerTower,
                         UUID victimUnit, TowerId victimTower, double damage,
                         boolean ranged)
    {
    }

    /** A unit that died this tick, with the children it splits into. */
    public record Death(UUID unit, double x, double z,
                        List<ChildSpawn> children)
    {
    }
}
