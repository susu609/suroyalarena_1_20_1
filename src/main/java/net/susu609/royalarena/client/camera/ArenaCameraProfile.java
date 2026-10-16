package net.susu609.royalarena.client.camera;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.match.TeamSide;

/**
 * Strategic camera behind the local side, looking down the length of the
 * arena at a steep downward angle. The view is straight along the arena axis
 * (no sideways swing); "diagonal" here means the downward tilt only. RED's
 * camera is BLUE's rotated 180 degrees about the arena centre, so both
 * players see their own half the same way.
 *
 * <p>The camera looks at a point {@code lookAhead} blocks from the arena
 * centre toward the local side, {@code distance} blocks away along a line
 * {@code pitch} degrees below the horizontal. Those numbers keep the whole
 * arena between the top of the screen and the HUD panel on a 16:9 window
 * (checked by DomainSmokeTest).</p>
 */
public final class ArenaCameraProfile {
    public static final ArenaCameraProfile DEFAULT =
            new ArenaCameraProfile(50.0F, 28.0D, 5.0D);

    private final float pitch;
    private final double distance;
    private final double lookAhead;

    public ArenaCameraProfile(float pitch, double distance, double lookAhead) {
        this.pitch = pitch;
        this.distance = distance;
        this.lookAhead = lookAhead;
    }

    public ArenaCameraTransform transform(ArenaLayout arena, TeamSide side) {
        ArenaPoint center = arena.center();
        double sign = ArenaLayout.sideSign(side);
        double radians = Math.toRadians(pitch);
        double z = center.z() + sign * (lookAhead + distance * Math.cos(radians));
        double y = center.y() + distance * Math.sin(radians);
        return new ArenaCameraTransform(
                center.x(), y, z, side == TeamSide.BLUE ? 180.0F : 0.0F,
                pitch);
    }
}
