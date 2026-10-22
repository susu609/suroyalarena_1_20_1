package net.susu609.royalarena.server.arena;

import net.minecraft.server.level.ServerLevel;
import net.susu609.royalarena.arena.ArenaGrid;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Hands out the arena slots of the arena dimension (see {@link ArenaGrid}).
 * The arena dimension is the only place arenas are built: there is no
 * fallback into the overworld any more (a world the mod cannot vouch for is
 * not built in, see ArenaWorld).
 */
public final class ArenaAllocator {
    public static final ArenaAllocator INSTANCE = new ArenaAllocator();

    private final ArrayDeque<Integer> reusable = new ArrayDeque<>();
    private final Set<Integer> active = new HashSet<>();
    private int nextSlot;

    private ArenaAllocator() {
    }

    public ArenaReservation allocate(ServerLevel level) {
        if (!ArenaDimension.is(level))
            throw new IllegalStateException(
                    "arenas are only built in the arena dimension");
        int slot = reusable.isEmpty() ? nextSlot++ : reusable.removeFirst();
        active.add(slot);
        ArenaWorld.noteSlot(level, slot);
        return new ArenaReservation(
                slot, ArenaGrid.layout(slot, ArenaDimension.FLOOR_Y));
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
