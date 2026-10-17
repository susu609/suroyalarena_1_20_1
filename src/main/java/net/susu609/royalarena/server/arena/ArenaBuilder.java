package net.susu609.royalarena.server.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;

/**
 * Builds a fresh arena for every match and removes it afterwards. Blocks are
 * placed with UPDATE_CLIENTS only, so river water never schedules flow ticks
 * and nothing next to the arena reacts while it is built or cleared.
 */
public final class ArenaBuilder {
    private static final int FLAGS = Block.UPDATE_CLIENTS;
    private static final int AIR_HEIGHT = 5;

    private ArenaBuilder() {
    }

    public static void build(ServerLevel level, ArenaLayout arena) {
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

    public static void markTowerDestroyed(ServerLevel level, ArenaPoint point) {
        set(level, point.x(), point.y() - 1, point.z(),
                Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
    }

    public static void clear(ServerLevel level, ArenaLayout arena) {
        int minY = arena.floorY() - 1;
        int maxY = arena.floorY() + AIR_HEIGHT;
        for (int x = arena.minX() - 1; x <= arena.maxX() + 1; x++) {
            for (int z = arena.minZ() - 1; z <= arena.maxZ() + 1; z++) {
                for (int y = minY; y <= maxY; y++)
                    set(level, x, y, z, Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static void set(ServerLevel level, int x, int y, int z,
            BlockState state)
    {
        level.setBlock(new BlockPos(x, y, z), state, FLAGS);
    }
}
