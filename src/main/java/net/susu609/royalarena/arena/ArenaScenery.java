package net.susu609.royalarena.arena;

/**
 * Hills, mountains and forest around an arena, so it does not float in the
 * void. Pure and deterministic: the same arena centre always gives the same
 * landscape, and {@code ArenaBuilder} places and removes the blocks.
 *
 * <p>The ground starts level with the arena wall and rises with the distance
 * from it, into grassy hills and a few rocky, snow-capped peaks at the
 * outer edge. Trees grow on the lower slopes. Everything stays within
 * {@link #MARGIN} blocks of the wall, inside the arena's slot of the grid.</p>
 */
public final class ArenaScenery {
    /** Blocks of landscape outside the wall on every side. */
    public static final int MARGIN = 12;
    /** The landscape is this deep under the arena floor. */
    public static final int DEPTH = 4;
    /** Highest block a column (or a tree on it) can reach above the floor. */
    public static final int MAX_HEIGHT = 22;
    /** Ground at or above this height is bare rock. */
    public static final int ROCK_HEIGHT = 9;
    /** Ground at or above this height has snow on top. */
    public static final int SNOW_HEIGHT = 13;

    public enum Surface {
        GRASS, ROCK, SNOW
    }

    public enum Tree {
        OAK, BIRCH, SPRUCE
    }

    private final ArenaLayout arena;
    private final long seed;

    public ArenaScenery(ArenaLayout arena) {
        this.arena = arena;
        ArenaPoint c = arena.center();
        this.seed = c.x() * 341873128712L + c.z() * 132897987541L + 0x5DEECE66DL;
    }

    public int minX() {
        return arena.minX() - 1 - MARGIN;
    }
    public int maxX() {
        return arena.maxX() + 1 + MARGIN;
    }
    public int minZ() {
        return arena.minZ() - 1 - MARGIN;
    }
    public int maxZ() {
        return arena.maxZ() + 1 + MARGIN;
    }

    /**
     * Blocks between (x, z) and the arena wall ring; 0 on the floor or the
     * wall itself.
     */
    public int distance(int x, int z) {
        int dx = Math.max(Math.max(arena.minX() - 1 - x, x - arena.maxX() - 1),
                0);
        int dz = Math.max(Math.max(arena.minZ() - 1 - z, z - arena.maxZ() - 1),
                0);
        return Math.max(dx, dz);
    }

    /** Whether (x, z) gets landscape (outside the wall, inside the margin). */
    public boolean contains(int x, int z) {
        return x >= minX() && x <= maxX() && z >= minZ() && z <= maxZ() &&
                distance(x, z) > 0;
    }

    /**
     * Height of the ground above the arena floor at (x, z): 1 next to the
     * wall (level with its top), rising to hills and peaks further out.
     */
    public int height(int x, int z) {
        int d = distance(x, z);
        if (d <= 0)
            return 0;
        double slope = d * 0.3D;
        double hills = 3.0D * noise(x / 6.0D, z / 6.0D, 1);
        // Peaks only rise far from the wall, where they frame the view.
        double far = Math.max(0.0D, (d - 5) / (double) (MARGIN - 5));
        double peaks = Math.max(0.0D, noise(x / 9.0D, z / 9.0D, 2) - 0.45D) *
                24.0D * far;
        double h = 1.0D + slope + hills * Math.min(1.0D, d / 4.0D) + peaks;
        return (int) Math.max(1, Math.min(MAX_HEIGHT - 8, Math.round(h)));
    }

    public Surface surface(int x, int z) {
        int h = height(x, z);
        if (h >= SNOW_HEIGHT)
            return Surface.SNOW;
        // Rock shows through on the steep, high ground.
        if (h >= ROCK_HEIGHT || h >= ROCK_HEIGHT - 2 &&
                hash(x, z, 3) < 0.4D)
            return Surface.ROCK;
        return Surface.GRASS;
    }

    /**
     * The tree growing at (x, z), or null. At most one tree per 3x3 cell, on
     * grass at least three blocks from the wall (its leaves never reach it).
     */
    public Tree tree(int x, int z) {
        if (distance(x, z) < 3 || surface(x, z) != Surface.GRASS)
            return null;
        int cellX = Math.floorDiv(x, 3);
        int cellZ = Math.floorDiv(z, 3);
        int pick = (int) (hash(cellX, cellZ, 4) * 9.0D);
        if (Math.floorMod(x, 3) + Math.floorMod(z, 3) * 3 != pick)
            return null;
        if (hash(cellX, cellZ, 5) > 0.95D)
            return null;
        int h = height(x, z);
        double kind = hash(cellX, cellZ, 6);
        if (h >= ROCK_HEIGHT - 3 || kind < 0.3D)
            return Tree.SPRUCE;
        return kind < 0.55D ? Tree.BIRCH : Tree.OAK;
    }

    /** Trunk height of a tree at (x, z): 4 to 6 blocks. */
    public int trunkHeight(int x, int z) {
        return 4 + (int) (hash(x, z, 7) * 3.0D);
    }

    /** A flower or a tuft of grass on open grass, 0 none, 1 grass, 2 flower. */
    public int plant(int x, int z) {
        if (surface(x, z) != Surface.GRASS || tree(x, z) != null)
            return 0;
        double r = hash(x, z, 8);
        return r < 0.06D ? 2 : r < 0.3D ? 1 : 0;
    }

    /** 0..1, the same for the same cell and channel. */
    double hash(int x, int z, int channel) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^
                (channel * 0x165667B19E3779F9L);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    /** Smooth value noise, 0..1. */
    private double noise(double x, double z, int channel) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double fx = smooth(x - x0);
        double fz = smooth(z - z0);
        double a = hash(x0, z0, channel);
        double b = hash(x0 + 1, z0, channel);
        double c = hash(x0, z0 + 1, channel);
        double d = hash(x0 + 1, z0 + 1, channel);
        return lerp(lerp(a, b, fx), lerp(c, d, fx), fz);
    }

    private static double smooth(double t) {
        return t * t * (3.0D - 2.0D * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
