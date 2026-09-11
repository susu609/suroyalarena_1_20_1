package net.susu609.royalarena.arena;

public record ArenaBox(int minX, int maxX, int minZ, int maxZ) {
    public ArenaBox {
        if (minX > maxX || minZ > maxZ) {
            throw new IllegalArgumentException("Invalid arena box");
        }
    }

    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }
}
