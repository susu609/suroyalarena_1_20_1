package net.susu609.royalarena.arena;

public record ArenaPoint(int x, int y, int z) {
    public ArenaPoint offset(int dx, int dy, int dz) {
        return new ArenaPoint(x + dx, y + dy, z + dz);
    }
}
