package net.susu609.royalarena.arena;

/**
 * Where each arena sits in the arena dimension: a fixed grid from the
 * origin, {@link #SPACING} blocks apart, {@link #COLUMNS} to a row. Slot 0
 * is the first arena; a slot is reused once its match is cleared away.
 */
public final class ArenaGrid {
    public static final int SPACING = 64;
    public static final int COLUMNS = 8;

    private ArenaGrid() {
    }

    /** X of the centre of the arena in {@code slot}. */
    public static int x(int slot) {
        return (slot % COLUMNS) * SPACING;
    }

    /** Z of the centre of the arena in {@code slot}. */
    public static int z(int slot) {
        return (slot / COLUMNS) * SPACING;
    }

    /** The arena of {@code slot} on a floor at {@code floorY}. */
    public static ArenaLayout layout(int slot, int floorY) {
        return ArenaLayout.standard(new ArenaPoint(x(slot), floorY, z(slot)));
    }
}
