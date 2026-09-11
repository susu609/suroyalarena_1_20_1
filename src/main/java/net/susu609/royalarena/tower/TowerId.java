package net.susu609.royalarena.tower;

import net.susu609.royalarena.match.TeamSide;

public enum TowerId {
    BLUE_LEFT(TeamSide.BLUE, TowerKind.PRINCESS),
    BLUE_RIGHT(TeamSide.BLUE, TowerKind.PRINCESS),
    BLUE_KING(TeamSide.BLUE, TowerKind.KING),
    RED_LEFT(TeamSide.RED, TowerKind.PRINCESS),
    RED_RIGHT(TeamSide.RED, TowerKind.PRINCESS),
    RED_KING(TeamSide.RED, TowerKind.KING);

    private final TeamSide side;
    private final TowerKind kind;

    TowerId(TeamSide side, TowerKind kind) {
        this.side = side;
        this.kind = kind;
    }

    public TowerKind kind() {
        return kind;
    }
    public TeamSide side() {
        return side;
    }

    public static TowerId king(TeamSide side) {
        return side == TeamSide.BLUE ? BLUE_KING : RED_KING;
    }

    public static TowerId left(TeamSide side) {
        return side == TeamSide.BLUE ? BLUE_LEFT : RED_LEFT;
    }

    public static TowerId right(TeamSide side) {
        return side == TeamSide.BLUE ? BLUE_RIGHT : RED_RIGHT;
    }
}
