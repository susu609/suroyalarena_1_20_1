package net.susu609.royalarena.arena;

import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;
import net.susu609.royalarena.tower.TowerKind;

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

    /**
     * Whether a floor block is part of the road, laid out like Clash
     * Royale's: on each half a "U" through the three towers. A 2-wide lane
     * runs from each bridge, straight through its Princess tower (the bridge,
     * the lane and the tower share one axis), back to the row level with the
     * middle of the King tower, where a 2-row road turns in and meets the
     * King's sides. No road in front of the towers, none in the river and
     * none under a tower. Mirrored on both axes.
     */
    public boolean isPath(int worldX, int worldZ) {
        if (isRiver(worldZ) || underTower(worldX, worldZ))
            return false;
        int rx = worldX - center.x();
        int rz = worldZ - center.z();
        int depth = rz >= 0 ? rz : -1 - rz;
        int mirroredX = rx >= 0 ? rx : -1 - rx;
        int half = BRIDGE_WIDTH / 2;
        boolean onLaneAxis = mirroredX >= LANE_OFFSET - half &&
                mirroredX < LANE_OFFSET + half;
        // Rows level with the middle of the King tower.
        int backRoad = KING_CENTER_Z - 1;
        boolean lane = onLaneAxis && depth >= RIVER_HALF &&
                depth <= backRoad + 1;
        boolean road = depth >= backRoad && depth <= backRoad + 1 &&
                mirroredX < LANE_OFFSET + half;
        return lane || road;
    }

    /** Whether the block is part of a tower's footprint. */
    public boolean underTower(int worldX, int worldZ) {
        for (TowerId id : TowerId.values()) {
            ArenaPoint p = towerPoint(id);
            int size = (int) towerHalfSize(id.kind());
            if (worldX >= p.x() - size && worldX < p.x() + size &&
                    worldZ >= p.z() - size && worldZ < p.z() + size)
                return true;
        }
        return false;
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

    /** +1 for BLUE (owns +Z), -1 for RED. */
    public static int sideSign(TeamSide side) {
        return side == TeamSide.BLUE ? 1 : -1;
    }

    /**
     * Which bank a world Z is on: +1 BLUE's, -1 RED's, 0 inside the river
     * band (the river rows and the bridges across them).
     */
    public int sideOf(double worldZ) {
        double rel = worldZ - center.z();
        if (rel > RIVER_HALF)
            return 1;
        if (rel < -RIVER_HALF)
            return -1;
        return 0;
    }

    /**
     * Next point for a walker at (fx, fz) heading for (tx, tz): the target
     * itself, or a bridge waypoint when the river is in the way. A walker
     * anywhere over the width of the bridge on its own X side crosses it; one
     * farther out first goes to the near end of the bridge, or, when it is
     * already at the bank, slides along the bank (never backwards) until it
     * is over the bridge. The wide tolerance matters: a narrow one makes
     * crowded units flip between "line up" and "cross" every tick and stall.
     */
    public ArenaWaypoint route(double fx, double fz, double tx, double tz) {
        int from = sideOf(fz);
        int to = sideOf(tz);
        if (from == to || to == 0)
            return new ArenaWaypoint(tx, tz);
        double bridgeX = bridgeFor(fx).x();
        double across = center.z() + to * (RIVER_HALF + 1.0D);
        if (Math.abs(fx - bridgeX) <= BRIDGE_WIDTH / 2.0D)
            return new ArenaWaypoint(bridgeX, across);
        if (from == 0)
            return new ArenaWaypoint(bridgeX, fz);
        double nearEnd = center.z() + from * (RIVER_HALF + 0.5D);
        boolean atTheBank = (fz - nearEnd) * from < 0.0D;
        return new ArenaWaypoint(bridgeX, atTheBank ? fz : nearEnd);
    }

    /**
     * Keeps a spawn point on the floor and out of the water: clamps X/Z to
     * the arena, and moves points inside the river band that are not on a
     * bridge back to the bank of {@code preferredSide}.
     */
    public ArenaWaypoint clampSpawn(double x, double z, int preferredSide) {
        double cx = Math.max(minX() + 0.5D, Math.min(maxX() + 0.5D, x));
        double cz = Math.max(minZ() + 0.5D, Math.min(maxZ() + 0.5D, z));
        if (sideOf(cz) == 0 && !isBridge((int) Math.floor(cx))) {
            cz = center.z() + preferredSide * (RIVER_HALF + 0.5D);
        }
        return new ArenaWaypoint(cx, cz);
    }

    /** Half the footprint of a tower of this kind, in blocks. */
    public static double towerHalfSize(TowerKind kind) {
        return (kind == TowerKind.KING ? KING_SIZE : PRINCESS_SIZE) / 2.0D;
    }

    public boolean canDeploy(TeamSide side, int worldX, int worldZ) {
        return deployArea(side).contains(worldX, worldZ);
    }
    /** Whether the block (x, z) is part of the arena floor or river. */
    public boolean contains(int worldX, int worldZ) {
        return worldX >= minX() && worldX <= maxX() && worldZ >= minZ() &&
                worldZ <= maxZ();
    }

    /**
     * Where a card may be played: a troop on the player's own deploy area, a
     * spell anywhere on the arena.
     */
    public boolean canPlay(TeamSide side, BattleCard card, int worldX,
            int worldZ)
    {
        return card.isSpell() ? contains(worldX, worldZ) :
                canDeploy(side, worldX, worldZ);
    }

    public ArenaBox deployArea(TeamSide side) {
        return side == TeamSide.BLUE ? blueDeploy : redDeploy;
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

    /** Tower whose centre is within half a block of (x, z), or null. */
    public TowerId towerAt(double x, double z) {
        for (TowerId id : TowerId.values()) {
            ArenaPoint p = towerPoint(id);
            if (Math.abs(p.x() - x) <= 0.5D && Math.abs(p.z() - z) <= 0.5D)
                return id;
        }
        return null;
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
