package net.susu609.royalarena.client.camera;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.match.TeamSide;

/**
 * Strategic camera behind the local side, swung around the arena centre by
 * {@code yawOffset} degrees so the view is slightly diagonal. RED's camera is
 * BLUE's rotated 180 degrees about the centre, so both players get the same
 * view of their own half.
 */
public final class ArenaCameraProfile {
    public static final ArenaCameraProfile DEFAULT =
            new ArenaCameraProfile(0.5D, 0.5D, 38.0F, 10.0F);

    private final double yOffset;
    private final double distanceOffset;
    private final float pitch;
    private final float yawOffset;

    public ArenaCameraProfile(double yOffset, double distanceOffset,
            float pitch, float yawOffset)
    {
        this.yOffset = yOffset;
        this.distanceOffset = distanceOffset;
        this.pitch = pitch;
        this.yawOffset = yawOffset;
    }

    public ArenaCameraTransform transform(ArenaLayout arena, TeamSide side) {
        ArenaPoint center = arena.center();
        ArenaPoint anchor = arena.cameraAnchor(side);
        double distance =
                Math.abs(anchor.z() - center.z()) + distanceOffset;
        double sign = side == TeamSide.BLUE ? 1.0D : -1.0D;
        double radians = Math.toRadians(yawOffset);
        double x = center.x() - sign * distance * Math.sin(radians);
        double z = center.z() + sign * distance * Math.cos(radians);
        float baseYaw = side == TeamSide.BLUE ? 180.0F : 0.0F;
        return new ArenaCameraTransform(
                x, anchor.y() + yOffset, z, baseYaw + yawOffset, pitch);
    }
}
