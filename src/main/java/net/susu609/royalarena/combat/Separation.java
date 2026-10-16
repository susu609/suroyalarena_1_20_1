package net.susu609.royalarena.combat;

import net.susu609.royalarena.arena.ArenaLayout;

import java.util.List;

/**
 * Soft push that keeps overlapping units from standing on the same spot.
 * Troop mobs have no vanilla AI, so vanilla entity pushing never moves them
 * and this takes its place. Units inside the river band (on a bridge) are
 * left alone, so nobody is shoved into the water.
 */
public final class Separation {
    private static final double STRENGTH = 0.25D;
    /**
     * The push never exceeds this share of the unit's own walking speed, so
     * two units that want the same spot (the mouth of a bridge, say) keep
     * making progress instead of cancelling each other out.
     */
    private static final double MAX_SHARE_OF_SPEED = 0.6D;

    private Separation() {
    }

    /** Extra (dx, dz) to add to this unit's move for one tick. */
    public static double[] offset(CombatUnit unit, List<CombatUnit> all,
            ArenaLayout arena)
    {
        if (arena.sideOf(unit.z()) == 0)
            return new double[2];
        double pushX = 0.0D;
        double pushZ = 0.0D;
        for (CombatUnit other : all) {
            if (other == unit)
                continue;
            double dx = unit.x() - other.x();
            double dz = unit.z() - other.z();
            double distance = Math.hypot(dx, dz);
            double reach = unit.radius() + other.radius();
            if (distance >= reach)
                continue;
            double overlap = reach - distance;
            if (distance < 1.0E-6D) {
                dx = unit.id().compareTo(other.id()) > 0 ? 1.0D : -1.0D;
                dz = 0.0D;
                distance = 1.0D;
            }
            pushX += dx / distance * overlap * STRENGTH;
            pushZ += dz / distance * overlap * STRENGTH;
        }
        double cap = unit.stats().speedPerTick() * MAX_SHARE_OF_SPEED;
        double length = Math.hypot(pushX, pushZ);
        if (length > cap) {
            pushX *= cap / length;
            pushZ *= cap / length;
        }
        if (arena.sideOf(unit.z() + pushZ) == 0)
            pushZ = 0.0D;
        return new double[] {pushX, pushZ};
    }
}
