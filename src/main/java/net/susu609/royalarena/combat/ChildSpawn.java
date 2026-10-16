package net.susu609.royalarena.combat;

import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

/** A unit to create after a split; register it with {@code engine.add}. */
public record ChildSpawn(TeamSide side, TroopStats stats, int generation,
                         double health, double damage, double radius,
                         double x, double z, int invulnerableTicks,
                         TowerId lockedTower)
{
}
