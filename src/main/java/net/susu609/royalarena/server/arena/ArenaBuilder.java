package net.susu609.royalarena.server.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.arena.ArenaScenery;

/**
 * Builds a fresh arena for every match and removes it afterwards. Blocks are
 * placed with UPDATE_CLIENTS only, so river water never schedules flow ticks
 * and nothing next to the arena reacts while it is built or cleared. In the
 * arena dimension the arena also gets hills and forest around it
 * ({@link ArenaScenery}); in the overworld fallback it does not, so nothing of
 * the world is overwritten.
 */
public final class ArenaBuilder {
    private static final int FLAGS = Block.UPDATE_CLIENTS;
    private static final int AIR_HEIGHT = 5;

    private ArenaBuilder() {
    }

    public static void build(ServerLevel level, ArenaLayout arena) {
        if (ArenaDimension.is(level))
            buildScenery(level, new ArenaScenery(arena), arena.floorY());
        int y = arena.floorY();
        for (int x = arena.minX(); x <= arena.maxX(); x++) {
            for (int z = arena.minZ(); z <= arena.maxZ(); z++) {
                set(level, x, y - 1, z, Blocks.STONE.defaultBlockState());
                set(level, x, y, z, floorBlock(arena, x, z));
                for (int dy = 1; dy <= AIR_HEIGHT; dy++)
                    set(level, x, y + dy, z, Blocks.AIR.defaultBlockState());
            }
        }
        buildBorder(level, arena, y);
    }

    private static BlockState floorBlock(ArenaLayout arena, int x, int z) {
        if (arena.isRiver(z)) {
            return arena.isBridge(x) ? Blocks.SPRUCE_PLANKS.defaultBlockState()
                    : Blocks.WATER.defaultBlockState();
        }
        if (arena.isPath(x, z))
            return Blocks.DIRT_PATH.defaultBlockState();
        boolean dark = ((x - arena.minX()) + (z - arena.minZ())) % 2 == 0;
        return dark ? Blocks.MOSS_BLOCK.defaultBlockState() :
                Blocks.GRASS_BLOCK.defaultBlockState();
    }

    /** Wall ring one block outside the floor, from river bed to y + 1. */
    private static void buildBorder(ServerLevel level, ArenaLayout arena,
            int y)
    {
        for (int x = arena.minX() - 1; x <= arena.maxX() + 1; x++) {
            for (int z = arena.minZ() - 1; z <= arena.maxZ() + 1; z++) {
                boolean edge = x < arena.minX() || x > arena.maxX() ||
                        z < arena.minZ() || z > arena.maxZ();
                if (!edge)
                    continue;
                BlockState wall = z > arena.maxZ() ?
                        Blocks.BLUE_CONCRETE.defaultBlockState() :
                        z < arena.minZ() ?
                                Blocks.RED_CONCRETE.defaultBlockState() :
                                Blocks.STONE_BRICKS.defaultBlockState();
                for (int dy = -1; dy <= 1; dy++)
                    set(level, x, y + dy, z, wall);
            }
        }
    }

    private static void buildScenery(ServerLevel level, ArenaScenery scenery,
            int floor)
    {
        for (int x = scenery.minX(); x <= scenery.maxX(); x++) {
            for (int z = scenery.minZ(); z <= scenery.maxZ(); z++) {
                if (!scenery.contains(x, z))
                    continue;
                int top = floor + scenery.height(x, z);
                ArenaScenery.Surface surface = scenery.surface(x, z);
                for (int y = floor - ArenaScenery.DEPTH; y <= top; y++) {
                    BlockState block;
                    if (y == top) {
                        block = switch (surface) {
                            case GRASS -> Blocks.GRASS_BLOCK.defaultBlockState();
                            case ROCK -> Blocks.STONE.defaultBlockState();
                            case SNOW -> Blocks.SNOW_BLOCK.defaultBlockState();
                        };
                    } else if (y >= top - 2 && surface == ArenaScenery.Surface.GRASS) {
                        block = Blocks.DIRT.defaultBlockState();
                    } else {
                        block = Blocks.STONE.defaultBlockState();
                    }
                    set(level, x, y, z, block);
                }
                ArenaScenery.Tree tree = scenery.tree(x, z);
                if (tree != null) {
                    growTree(level, tree, x, top + 1, z,
                            scenery.trunkHeight(x, z));
                    continue;
                }
                int plant = scenery.plant(x, z);
                if (plant == 1)
                    set(level, x, top + 1, z, Blocks.GRASS.defaultBlockState());
                else if (plant == 2)
                    set(level, x, top + 1, z, (x + z) % 2 == 0 ?
                            Blocks.POPPY.defaultBlockState() :
                            Blocks.DANDELION.defaultBlockState());
            }
        }
    }

    /**
     * A simple tree: a trunk and a blob of persistent leaves (they never
     * decay, and nothing ticks them).
     */
    private static void growTree(ServerLevel level, ArenaScenery.Tree tree,
            int x, int y, int z, int trunk)
    {
        BlockState log = switch (tree) {
            case OAK -> Blocks.OAK_LOG.defaultBlockState();
            case BIRCH -> Blocks.BIRCH_LOG.defaultBlockState();
            case SPRUCE -> Blocks.SPRUCE_LOG.defaultBlockState();
        };
        BlockState leaves = (switch (tree) {
            case OAK -> Blocks.OAK_LEAVES;
            case BIRCH -> Blocks.BIRCH_LEAVES;
            case SPRUCE -> Blocks.SPRUCE_LEAVES;
        }).defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        int topY = y + trunk;
        if (tree == ArenaScenery.Tree.SPRUCE) {
            // A cone: wide at the bottom, a point on top.
            for (int dy = 2; dy <= trunk + 1; dy++) {
                int r = dy >= trunk ? (dy == trunk + 1 ? 0 : 1) :
                        (trunk - dy) % 2 == 0 ? 1 : 2;
                leafLayer(level, leaves, x, y + dy, z, r);
            }
        } else {
            leafLayer(level, leaves, x, topY - 2, z, 2);
            leafLayer(level, leaves, x, topY - 1, z, 2);
            leafLayer(level, leaves, x, topY, z, 1);
            leafLayer(level, leaves, x, topY + 1, z, 1);
        }
        for (int dy = 0; dy < trunk; dy++)
            set(level, x, y + dy, z, log);
    }

    private static void leafLayer(ServerLevel level, BlockState leaves, int x,
            int y, int z, int radius)
    {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // Round the corners off the bigger layers.
                if (radius >= 2 && Math.abs(dx) == radius &&
                        Math.abs(dz) == radius)
                    continue;
                set(level, x + dx, y, z + dz, leaves);
            }
        }
    }

    /**
     * Broken stone where a tower fell: cobblestone, mossy cobblestone and
     * stone brick slabs scattered over its footprint ({@code half} blocks
     * from its centre), always the same pile for the same spot. Only slabs,
     * so troops still step over it on their way to the King. The arena's
     * clear removes it with the rest.
     */
    public static void rubble(ServerLevel level, ArenaPoint point, int half) {
        BlockState[] pieces = {
                Blocks.COBBLESTONE_SLAB.defaultBlockState(),
                Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState(),
                Blocks.STONE_BRICK_SLAB.defaultBlockState(),
                Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState()};
        for (int x = point.x() - half; x < point.x() + half; x++) {
            for (int z = point.z() - half; z < point.z() + half; z++) {
                int h = Math.floorMod(x * 734287 + z * 912931, 97);
                if (h >= 35)
                    set(level, x, point.y(), z, pieces[h % pieces.length]);
            }
        }
    }

    public static void markTowerDestroyed(ServerLevel level, ArenaPoint point) {
        set(level, point.x(), point.y() - 1, point.z(),
                Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
    }

    public static void clear(ServerLevel level, ArenaLayout arena) {
        if (ArenaDimension.is(level))
            clearScenery(level, new ArenaScenery(arena), arena.floorY());
        int minY = arena.floorY() - 1;
        int maxY = arena.floorY() + AIR_HEIGHT;
        for (int x = arena.minX() - 1; x <= arena.maxX() + 1; x++) {
            for (int z = arena.minZ() - 1; z <= arena.maxZ() + 1; z++) {
                for (int y = minY; y <= maxY; y++)
                    set(level, x, y, z, Blocks.AIR.defaultBlockState());
            }
        }
    }

    /** Air over the whole landscape box, trees and leaves included. */
    private static void clearScenery(ServerLevel level, ArenaScenery scenery,
            int floor)
    {
        BlockState air = Blocks.AIR.defaultBlockState();
        // Leaves stick out up to two blocks past the outermost trunk.
        for (int x = scenery.minX() - 2; x <= scenery.maxX() + 2; x++) {
            for (int z = scenery.minZ() - 2; z <= scenery.maxZ() + 2; z++) {
                if (!scenery.contains(x, z) && scenery.distance(x, z) == 0)
                    continue;
                for (int y = floor - ArenaScenery.DEPTH;
                        y <= floor + ArenaScenery.MAX_HEIGHT; y++) {
                    if (!level.getBlockState(new BlockPos(x, y, z)).isAir())
                        set(level, x, y, z, air);
                }
            }
        }
    }

    private static void set(ServerLevel level, int x, int y, int z,
            BlockState state)
    {
        level.setBlock(new BlockPos(x, y, z), state, FLAGS);
    }
}
