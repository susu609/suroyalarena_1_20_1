package net.susu609.royalarena.server.arena;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.susu609.modlib.world.VoidWorlds;
import net.susu609.royalarena.RoyalArena;

/**
 * The empty dimension matches are played in
 * (data/royalarena/dimension/arena.json, a sumodlib void dimension), so the
 * arena never lands on terrain, whatever world type the save uses.
 */
public final class ArenaDimension {
    public static final ResourceKey<Level> KEY =
            VoidWorlds.dimension(RoyalArena.MOD_ID, "arena");
    /** Floor height of every arena in the arena dimension. */
    public static final int FLOOR_Y = 64;

    private ArenaDimension() {
    }

    /**
     * The arena dimension once the mod has checked and stamped it (see
     * {@link ArenaWorld}), or null: never the overworld.
     */
    public static ServerLevel level(MinecraftServer server) {
        return ArenaWorld.level(server);
    }

    public static boolean is(Level level) {
        return level.dimension() == KEY;
    }
}
