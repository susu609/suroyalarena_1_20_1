package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaGrid;
import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaScenery;
import net.susu609.royalarena.arena.ArenaWorldCheck;
import net.susu609.royalarena.arena.ArenaWorldCheck.Stamp;
import net.susu609.royalarena.arena.ArenaWorldCheck.Verdict;

/**
 * The arena world check: the mod only builds in a dimension it stamped
 * itself (or one that is completely empty), never in one with someone
 * else's blocks or another world's stamp; and the arena grid keeps every
 * arena, landscape included, apart from its neighbours.
 */
public final class ArenaWorldSmokeTest {
    private static final long SEED = 12345L;

    private ArenaWorldSmokeTest() {
    }

    public static void run() {
        verdicts();
        gridKeepsArenasApart();
    }

    private static void verdicts() {
        Stamp ours = new Stamp("a-code", SEED, "1.0", 3);
        check(ArenaWorldCheck.check(false, true, ours, SEED, true) ==
                Verdict.MISSING, "no dimension");
        check(ArenaWorldCheck.check(true, false, ours, SEED, true) ==
                Verdict.NOT_VOID, "not a void");
        check(ArenaWorldCheck.check(true, true, null, SEED, true) ==
                Verdict.NEW_STAMP, "empty and unstamped: stamp it");
        check(ArenaWorldCheck.check(true, true, null, SEED, false) ==
                Verdict.UNSTAMPED_BLOCKS, "foreign blocks: refuse");
        check(ArenaWorldCheck.check(true, true, ours, SEED, false) ==
                Verdict.READY, "our stamp, our blocks: ready");
        check(ArenaWorldCheck.check(true, true, ours, SEED + 1, true) ==
                Verdict.FOREIGN_WORLD, "another world's stamp: refuse");
        check(ArenaWorldCheck.check(true, true,
                new Stamp(" ", SEED, "1.0", -1), SEED, true) ==
                Verdict.UNREADABLE_STAMP, "blank code: refuse");
        for (Verdict v : Verdict.values()) {
            boolean usable = v == Verdict.READY || v == Verdict.NEW_STAMP;
            check(v.usable() == usable, v + " usable is " + usable);
            check(!ArenaWorldCheck.explain(v).isBlank(), v + " explained");
        }
    }

    private static void gridKeepsArenasApart() {
        check(ArenaGrid.x(0) == 0 && ArenaGrid.z(0) == 0, "slot 0 at origin");
        check(ArenaGrid.x(ArenaGrid.COLUMNS) == 0 &&
                ArenaGrid.z(ArenaGrid.COLUMNS) == ArenaGrid.SPACING,
                "a new row after " + ArenaGrid.COLUMNS);
        ArenaScenery a = new ArenaScenery(ArenaGrid.layout(0, 64));
        ArenaScenery right = new ArenaScenery(ArenaGrid.layout(1, 64));
        ArenaScenery below = new ArenaScenery(ArenaGrid.layout(
                ArenaGrid.COLUMNS, 64));
        check(a.maxX() < right.minX(), "landscapes apart in x: " +
                a.maxX() + " < " + right.minX());
        check(a.maxZ() < below.minZ(), "landscapes apart in z: " +
                a.maxZ() + " < " + below.minZ());
        ArenaLayout layout = ArenaGrid.layout(9, 64);
        check(layout.floorY() == 64, "floor kept");
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
