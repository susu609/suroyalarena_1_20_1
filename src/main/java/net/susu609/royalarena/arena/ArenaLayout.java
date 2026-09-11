package net.susu609.royalarena.arena;

import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

public final class ArenaLayout {
    public static final int HALF_WIDTH = 9;
    public static final int HALF_LENGTH = 15;

    private final ArenaPoint center;
    private final ArenaBox blueDeploy;
    private final ArenaBox redDeploy;

    private ArenaLayout(ArenaPoint center) {
        this.center = center;
        this.blueDeploy = new ArenaBox(
                center.x() - HALF_WIDTH + 1, center.x() + HALF_WIDTH - 1,
                center.z() + 2, center.z() + HALF_LENGTH - 1);
        this.redDeploy = new ArenaBox(
                center.x() - HALF_WIDTH + 1, center.x() + HALF_WIDTH - 1,
                center.z() - HALF_LENGTH + 1, center.z() - 2);
    }

    public static ArenaLayout standard(ArenaPoint center) {
        return new ArenaLayout(center);
    }

    public ArenaPoint bridgeLeft() {
        return center.offset(-5, 1, 0);
    }
    public ArenaPoint bridgeRight() {
        return center.offset(5, 1, 0);
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

    public ArenaPoint towerPoint(TowerId id) {
        return switch (id) {
            case BLUE_LEFT -> center.offset(-6, 1, 9);
            case BLUE_RIGHT -> center.offset(6, 1, 9);
            case BLUE_KING -> center.offset(0, 1, 12);
            case RED_LEFT -> center.offset(6, 1, -9);
            case RED_RIGHT -> center.offset(-6, 1, -9);
            case RED_KING -> center.offset(0, 1, -12);
        };
    }
}
