package net.susu609.royalarena.match;

public enum TeamSide {
    BLUE, RED;

    public TeamSide opponent() {
        return this == BLUE ? RED : BLUE;
    }
}
