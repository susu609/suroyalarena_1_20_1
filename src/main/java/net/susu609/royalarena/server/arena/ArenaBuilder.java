package net.susu609.royalarena.server.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;
import net.susu609.royalarena.tower.TowerKind;

public final class ArenaBuilder {
    private ArenaBuilder() {
    }

    public static void build(ServerLevel level, ArenaLayout arena) {
        int y = arena.floorY();
        for (int x = arena.center().x() - ArenaLayout.HALF_WIDTH;
             x <= arena.center().x() + ArenaLayout.HALF_WIDTH; x++) {
            for (int z = arena.center().z() - ArenaLayout.HALF_LENGTH;
                 z <= arena.center().z() + ArenaLayout.HALF_LENGTH; z++) {
                Block floor = Math.abs(z - arena.center().z()) <= 1 ?
                        Blocks.BLUE_CONCRETE : Blocks.GRASS_BLOCK;
                if (Math.abs(z - arena.center().z()) <= 1 &&
                        isBridgeX(arena, x))
                    floor = Blocks.STONE_BRICKS;
                level.setBlockAndUpdate(
                        new BlockPos(x, y, z), floor.defaultBlockState());
                for (int dy = 1; dy <= 4; dy++) {
                    level.setBlockAndUpdate(
                            new BlockPos(x, y + dy, z),
                            Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int x = arena.center().x() - ArenaLayout.HALF_WIDTH;
             x <= arena.center().x() + ArenaLayout.HALF_WIDTH; x++) {
            level.setBlockAndUpdate(
                    new BlockPos(
                            x, y + 1,
                            arena.center().z() - ArenaLayout.HALF_LENGTH),
                    Blocks.STONE_BRICKS.defaultBlockState());
            level.setBlockAndUpdate(
                    new BlockPos(
                            x, y + 1,
                            arena.center().z() + ArenaLayout.HALF_LENGTH),
                    Blocks.STONE_BRICKS.defaultBlockState());
        }
        for (int z = arena.center().z() - ArenaLayout.HALF_LENGTH;
             z <= arena.center().z() + ArenaLayout.HALF_LENGTH; z++) {
            level.setBlockAndUpdate(
                    new BlockPos(
                            arena.center().x() - ArenaLayout.HALF_WIDTH,
                            y + 1, z), Blocks.STONE_BRICKS.defaultBlockState());
            level.setBlockAndUpdate(
                    new BlockPos(
                            arena.center().x() + ArenaLayout.HALF_WIDTH,
                            y + 1, z), Blocks.STONE_BRICKS.defaultBlockState());
        }
        for (TowerId id : TowerId.values())
            buildTower(
                    level, arena.towerPoint(id), id.side(),
                    id.kind() == TowerKind.KING);
    }
    private static boolean isBridgeX(ArenaLayout arena, int x) {
        int rel = x - arena.center().x();
        return (rel >= -6 && rel <= -4) || (rel >= 4 && rel <= 6);
    }
    private static void buildTower(ServerLevel level, ArenaPoint point,
            TeamSide side, boolean king)
    {
        Block body = side == TeamSide.BLUE ? Blocks.BLUE_WOOL : Blocks.RED_WOOL;
        int height = king ? 4 : 3;
        for (int dy = 0; dy < height; dy++) {
            level.setBlockAndUpdate(
                    new BlockPos(point.x(), point.y() + dy, point.z()),
                    body.defaultBlockState());
        }
        level.setBlockAndUpdate(
                new BlockPos(point.x(), point.y() + height, point.z()),
                Blocks.GOLD_BLOCK.defaultBlockState());
    }
    public static void markTowerDestroyed(ServerLevel level, ArenaPoint point) {
        for (int dy = 0; dy < 5; dy++) {
            level.setBlockAndUpdate(
                    new BlockPos(point.x(), point.y() + dy, point.z()),
                    Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(
                new BlockPos(point.x(), point.y() - 1, point.z()),
                Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
    }
    public static void clear(ServerLevel level, ArenaLayout arena) {
        int minY = arena.floorY();
        int maxY = minY + 5;
        for (int x = arena.center().x() - ArenaLayout.HALF_WIDTH - 1;
             x <= arena.center().x() + ArenaLayout.HALF_WIDTH + 1; x++) {
            for (int z = arena.center().z() - ArenaLayout.HALF_LENGTH - 1;
                 z <= arena.center().z() + ArenaLayout.HALF_LENGTH + 1; z++) {
                for (int y = minY; y <= maxY; y++) {
                    level.setBlockAndUpdate(
                            new BlockPos(x, y, z),
                            Blocks.AIR.defaultBlockState());
                }
            }
        }
    }
}
