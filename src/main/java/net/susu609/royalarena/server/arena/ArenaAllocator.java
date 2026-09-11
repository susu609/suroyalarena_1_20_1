package net.susu609.royalarena.server.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class ArenaAllocator {
    public static final ArenaAllocator INSTANCE = new ArenaAllocator();
    private static final int SPACING = 64;
    private static final int COLUMNS = 8;

    private final ArrayDeque<Integer> reusable = new ArrayDeque<>();
    private final Set<Integer> active = new HashSet<>();
    private int nextSlot;

    private ArenaAllocator() {
    }

    public ArenaReservation allocate(ServerLevel level) {
        int slot = reusable.isEmpty() ? nextSlot++ : reusable.removeFirst();
        active.add(slot);
        BlockPos spawn = level.getSharedSpawnPos();
        int gridX = slot % COLUMNS;
        int gridZ = slot / COLUMNS;
        int x = spawn.getX() + gridX * SPACING;
        int z = spawn.getZ() + gridZ * SPACING;
        int y = Mth.clamp(spawn.getY() + 48, 96, 220);
        return new ArenaReservation(
                slot, ArenaLayout.standard(new ArenaPoint(x, y, z)));
    }

    public void release(ArenaReservation reservation) {
        if (active.remove(reservation.slot()))
            reusable.addLast(reservation.slot());
    }

    public void reset() {
        reusable.clear();
        active.clear();
        nextSlot = 0;
    }
}
