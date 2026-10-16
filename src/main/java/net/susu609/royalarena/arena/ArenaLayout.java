package net.susu609.royalarena.arena;

import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

/**
 * Even 18x32 arena. Every size is even, so both mirror axes lie on block
 * boundaries: world X = center.x() and world Z = center.z(). Blocks span
 * [center - HALF, center + HALF - 1] on each axis. BLUE owns +Z, RED owns -Z.
 */
public final class ArenaLayout {
    public static final int HALF_WIDTH = 9;
    public static final int HALF_LENGTH = 16;
    /** River rows on each side of the Z axis (2 rows total). */
    public static final int RIVER_HALF = 1;
    /** Empty rows between the river and the front of a Princess tower. */
    public static final int RIVER_TO_PRINCESS = 8;
    public static final int PRINCESS_SIZE = 4;
    public static final int KING_SIZE = 4;
    /** Bridge centre and Princess tower centre share this X offset. */
    public static final int LANE_OFFSET = 6;
    public static final int BRIDGE_WIDTH = 2;

    private static final int PRINCESS_CENTER_Z =
            RIVER_HALF + RIVER_TO_PRINCESS + PRINCESS_SIZE / 2;
    private static final int KING_CENTER_Z = HALF_LENGTH - 1 - KING_SIZE / 2;

    private final ArenaPoint center;
    private final ArenaBox blueDeploy;
    private final ArenaBox redDeploy;

    private ArenaLayout(ArenaPoint center) {
        this.center = center;
        this.blueDeploy = new ArenaBox(
                minX(), maxX(), center.z() + RIVER_HALF, maxZ());
        this.redDeploy = new ArenaBox(
                minX(), maxX(), minZ(), center.z() - RIVER_HALF - 1);
    }

    public static ArenaLayout standard(ArenaPoint center) {
        return new ArenaLayout(center);
    }

    public int minX() {
        return center.x() - HALF_WIDTH;
    }
    public int maxX() {
        return center.x() + HALF_WIDTH - 1;
    }
    public int minZ() {
        return center.z() - HALF_LENGTH;
    }
    public int maxZ() {
        return center.z() + HALF_LENGTH - 1;
    }

    public boolean isRiver(int worldZ) {
        int rel = worldZ - center.z();
        return rel >= -RIVER_HALF && rel < RIVER_HALF;
    }

    public boolean isBridge(int worldX) {
        int rel = worldX - center.x();
        int mirrored = rel >= 0 ? rel : -1 - rel;
        int half = BRIDGE_WIDTH / 2;
        return mirrored >= LANE_OFFSET - half && mirrored < LANE_OFFSET + half;
    }

    /** Centre of the left bridge (world -X), on the river axis. */
    public ArenaPoint bridgeLeft() {
        return center.offset(-LANE_OFFSET, 1, 0);
    }
    public ArenaPoint bridgeRight() {
        return center.offset(LANE_OFFSET, 1, 0);
    }

    /** Bridge on the same world side as {@code worldX}. */
    public ArenaPoint bridgeFor(double worldX) {
        return worldX < center.x() ? bridgeLeft() : bridgeRight();
    }

    /**
     * Where a troop of {@code side} should walk before heading for its tower:
     * the near end of the bridge on its own side of the arena until it is
     * lined up with it, then one block past the far end. Returns null once
     * the troop has crossed the river.
     */
    public ArenaWaypoint riverWaypoint(TeamSide side, double x, double z) {
        double dir = side == TeamSide.BLUE ? -1.0D : 1.0D;
        double forward = (z - center.z()) * dir;
        if (forward >= RIVER_HALF + 0.9D)
            return null;
        double bridgeX = bridgeFor(x).x();
        boolean linedUp = Math.abs(x - bridgeX) <= 0.6D;
        double targetForward = linedUp ? RIVER_HALF + 1.0D : -RIVER_HALF - 0.5D;
        return new ArenaWaypoint(bridgeX, center.z() + targetForward * dir);
    }

    public boolean canDeploy(TeamSide side, int worldX, int worldZ) {
        return deployArea(side).contains(worldX, worldZ);
    }
    public ArenaBox deployArea(TeamSide side) {
        return side == TeamSide.BLUE ? blueDeploy : redDeploy;
    }

    public ArenaPoint cameraAnchor(TeamSide side) {
        int z = side == TeamSide.BLUE ? center.z() + 24 : center.z() - 24;
        return new ArenaPoint(center.x(), center.y() + 18, z);
    }

    public ArenaPoint controllerAnchor(TeamSide side) {
        int z = side == TeamSide.BLUE ? center.z() + 20 : center.z() - 20;
        return new ArenaPoint(center.x(), center.y() + 4, z);
    }
    public ArenaPoint center() {
        return center;
    }
    public int floorY() {
        return center.y();
    }

    /**
     * Tower centre. All towers are even-sized, so the centre lies on a block
     * corner: the tower covers [x - size/2, x + size/2 - 1] on each axis.
     */
    public ArenaPoint towerPoint(TowerId id) {
        return switch (id) {
            case BLUE_LEFT -> center.offset(-LANE_OFFSET, 1, PRINCESS_CENTER_Z);
            case BLUE_RIGHT -> center.offset(LANE_OFFSET, 1, PRINCESS_CENTER_Z);
            case BLUE_KING -> center.offset(0, 1, KING_CENTER_Z);
            case RED_LEFT -> center.offset(LANE_OFFSET, 1, -PRINCESS_CENTER_Z);
            case RED_RIGHT -> center.offset(-LANE_OFFSET, 1, -PRINCESS_CENTER_Z);
            case RED_KING -> center.offset(0, 1, -KING_CENTER_Z);
        };
    }
}
