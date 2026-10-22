package net.susu609.royalarena.server.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.ModList;
import net.susu609.modlib.world.VoidWorlds;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.arena.ArenaGrid;
import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaScenery;
import net.susu609.royalarena.arena.ArenaWorldCheck;
import net.susu609.royalarena.arena.ArenaWorldCheck.Verdict;
import net.susu609.royalarena.config.ArenaCommonConfig;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.UUID;

/**
 * The arena dimension as a world the mod can vouch for. When the server
 * has started, {@link #prepare} checks it ({@link ArenaWorldCheck}): it must
 * exist, be an all-air void, and carry this world's stamp (a random code,
 * the seed, the mod version and the highest arena slot ever used, saved in
 * the dimension's own data). A dimension with no stamp that is completely
 * empty is stamped; one with blocks the mod did not build, or stamped by
 * another world, is refused: matches do not start (there is no fallback into
 * the overworld) and the log says why and what to do.
 *
 * <p>A vouched dimension is refreshed before the first match: every arena
 * slot used before (a match cut off by a crash or a stop leaves its blocks
 * and mobs behind) is cleared, and every entity in it removed. With the
 * {@code purgeArenaWorldOnStart} setting the dimension's whole save folder is
 * deleted as well, at start and at stop.</p>
 */
public final class ArenaWorld {
    private static final String DATA_NAME = "royalarena_arena_world";
    /** Arena slots whose floor centre is looked at to call it empty. */
    private static final int EMPTY_CHECK_SLOTS = 16;

    private static Verdict verdict;

    private ArenaWorld() {
    }

    /** What the stamp says: saved with the arena dimension. */
    static final class Data extends SavedData {
        String code;
        long worldSeed;
        String modVersion;
        int maxSlot = -1;

        Data(String code, long worldSeed, String modVersion) {
            this.code = code;
            this.worldSeed = worldSeed;
            this.modVersion = modVersion;
        }

        static Data load(CompoundTag tag) {
            Data data = new Data(tag.getString("code"), tag.getLong("seed"),
                    tag.getString("modVersion"));
            data.maxSlot = tag.contains("maxSlot") ? tag.getInt("maxSlot") : -1;
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putString("code", code);
            tag.putLong("seed", worldSeed);
            tag.putString("modVersion", modVersion);
            tag.putInt("maxSlot", maxSlot);
            return tag;
        }
    }

    /** The arena dimension if the mod vouches for it, else null. */
    public static ServerLevel level(MinecraftServer server) {
        if (verdict == null || !verdict.usable())
            return null;
        return server.getLevel(ArenaDimension.KEY);
    }

    /** The last check's verdict, or null before the server started. */
    public static Verdict verdict() {
        return verdict;
    }

    /** Why matches cannot start, in plain words (null when they can). */
    public static String refusal() {
        return verdict == null ? "the arena world is not checked yet" :
                verdict.usable() ? null : ArenaWorldCheck.explain(verdict);
    }

    private static String modVersion() {
        return ModList.get().getModContainerById(RoyalArena.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    /**
     * Checks, stamps and refreshes the arena dimension; call once the server
     * has started.
     */
    public static void prepare(MinecraftServer server) {
        verdict = null;
        ServerLevel level = server.getLevel(ArenaDimension.KEY);
        boolean exists = level != null;
        boolean isVoid = exists && VoidWorlds.isVoid(level);
        Data data = exists && isVoid ?
                level.getDataStorage().get(Data::load, DATA_NAME) : null;
        ArenaWorldCheck.Stamp stamp = data == null ? null :
                new ArenaWorldCheck.Stamp(data.code, data.worldSeed,
                        data.modVersion, data.maxSlot);
        long seed = server.overworld().getSeed();
        boolean gridEmpty = !(exists && isVoid && stamp == null) ||
                gridEmpty(level);
        Verdict result = ArenaWorldCheck.check(exists, isVoid, stamp, seed,
                gridEmpty);
        if (!result.usable()) {
            verdict = result;
            RoyalArena.LOGGER.error(
                    "Royal Arena: matches will not start, {}. The mod does " +
                            "not build in a world it cannot vouch for. To " +
                            "start clean, delete {} while the server is " +
                            "stopped, or set purgeArenaWorldOnStart = true " +
                            "in royalarena-common.toml.",
                    ArenaWorldCheck.explain(result), folder(server));
            return;
        }
        if (result == Verdict.NEW_STAMP) {
            data = new Data(UUID.randomUUID().toString(), seed, modVersion());
            data.setDirty();
            level.getDataStorage().set(DATA_NAME, data);
            RoyalArena.LOGGER.info(
                    "Royal Arena: stamped the arena world with code {}",
                    data.code);
        } else if (!modVersion().equals(data.modVersion)) {
            RoyalArena.LOGGER.info(
                    "Royal Arena: the arena world was made by mod version " +
                            "{}, this is {}; refreshing it", data.modVersion,
                    modVersion());
            data.modVersion = modVersion();
            data.setDirty();
        }
        verdict = result;
        refresh(level, data);
    }

    /** True when no block stands at any arena slot's floor centre. */
    private static boolean gridEmpty(ServerLevel level) {
        for (int slot = 0; slot < EMPTY_CHECK_SLOTS; slot++) {
            for (int dy = -1; dy <= 0; dy++) {
                BlockPos pos = new BlockPos(ArenaGrid.x(slot),
                        ArenaDimension.FLOOR_Y + dy, ArenaGrid.z(slot));
                if (!level.getBlockState(pos).isAir())
                    return false;
            }
        }
        return true;
    }

    /**
     * Clears every arena slot used so far and removes every non-player
     * entity in the landscape boxes around them.
     */
    private static void refresh(ServerLevel level, Data data) {
        if (data.maxSlot < 0)
            return;
        int removed = 0;
        for (int slot = 0; slot <= data.maxSlot; slot++) {
            ArenaLayout layout = ArenaGrid.layout(slot,
                    ArenaDimension.FLOOR_Y);
            ArenaBuilder.clear(level, layout);
            ArenaScenery scenery = new ArenaScenery(layout);
            int floor = layout.floorY();
            AABB box = new AABB(scenery.minX() - 2,
                    floor - ArenaScenery.DEPTH - 1, scenery.minZ() - 2,
                    scenery.maxX() + 3, floor + ArenaScenery.MAX_HEIGHT + 2,
                    scenery.maxZ() + 3);
            List<Entity> leftovers = level.getEntities((Entity) null, box,
                    e -> !(e instanceof Player));
            for (Entity entity : leftovers) {
                entity.discard();
                removed++;
            }
        }
        RoyalArena.LOGGER.info(
                "Royal Arena: refreshed {} arena slot(s) left from earlier " +
                        "matches, removed {} entit(ies)", data.maxSlot + 1,
                removed);
        data.maxSlot = -1;
        data.setDirty();
    }

    /** An arena slot was used: remember it so the next start can clear it. */
    static void noteSlot(ServerLevel level, int slot) {
        Data data = level.getDataStorage().get(Data::load, DATA_NAME);
        if (data != null && slot > data.maxSlot) {
            data.maxSlot = slot;
            data.setDirty();
        }
    }

    /** The arena dimension's folder inside the world. */
    private static Path folder(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath()
                .normalize();
        return DimensionType.getStorageFolder(ArenaDimension.KEY, root)
                .normalize();
    }

    /**
     * Deletes the arena dimension's whole save folder (regions, entities and
     * the stamp). {@code closed}: the server is shut, the dimension no longer
     * in use; otherwise it is only deleted if the server has not loaded it
     * yet. Only ever the folder {@code dimensions/royalarena/arena} inside
     * this world, and symbolic links are deleted, never followed.
     */
    public static void purgeFiles(MinecraftServer server, boolean closed) {
        Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath()
                .normalize();
        Path dir = folder(server);
        Path parent = dir.getParent();
        if (!dir.startsWith(root) || parent == null ||
                !dir.getFileName().toString().equals("arena") ||
                !parent.getFileName().toString().equals(RoyalArena.MOD_ID)) {
            RoyalArena.LOGGER.warn(
                    "Royal Arena: not purging {}: it is not this world's " +
                            "royalarena arena folder", dir);
            return;
        }
        if (!closed && server.getLevel(ArenaDimension.KEY) != null) {
            RoyalArena.LOGGER.info(
                    "Royal Arena: the arena world is already loaded; its " +
                            "folder is deleted when the server stops");
            return;
        }
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS))
            return;
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) throws IOException
                {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path d,
                        IOException e) throws IOException
                {
                    if (e != null)
                        throw e;
                    Files.delete(d);
                    return FileVisitResult.CONTINUE;
                }
            });
            RoyalArena.LOGGER.info(
                    "Royal Arena: deleted the arena world folder {}", dir);
        } catch (IOException e) {
            RoyalArena.LOGGER.warn(
                    "Royal Arena: could not delete the arena world folder " +
                            "{}: {}", dir, e.toString());
        }
    }

    /** Forget the check (the server is shutting down). */
    public static void forget() {
        verdict = null;
    }

    /** Whether the settings ask for the dimension to be wiped. */
    public static boolean purgeRequested() {
        return ArenaCommonConfig.PURGE_ARENA_WORLD_ON_START.get();
    }
}
