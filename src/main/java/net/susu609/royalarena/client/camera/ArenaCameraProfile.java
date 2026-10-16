package net.susu609.royalarena.client.camera;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.match.TeamSide;

public final class ArenaCameraProfile {
    public static final ArenaCameraProfile DEFAULT =
            new ArenaCameraProfile(0.0D, 0.5D, 0.5D, 38.0F);

    private final double xOffset;
    private final double yOffset;
    private final double zOffset;
    private final float pitch;

    public ArenaCameraProfile(double xOffset, double yOffset, double zOffset,
            float pitch)
    {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
        this.pitch = pitch;
    }

    public ArenaCameraTransform transform(ArenaLayout arena, TeamSide side) {
        ArenaPoint anchor = arena.cameraAnchor(side);
        return new ArenaCameraTransform(
                anchor.x() + xOffset, anchor.y() + yOffset,
                anchor.z() + (side == TeamSide.BLUE ? zOffset : -zOffset),
                side == TeamSide.BLUE ? 180.0F : 0.0F, pitch);
    }
}
