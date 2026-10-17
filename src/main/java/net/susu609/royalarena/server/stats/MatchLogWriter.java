package net.susu609.royalarena.server.stats;

import net.minecraftforge.fml.loading.FMLPaths;
import net.susu609.royalarena.RoyalArena;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Appends match log lines ({@code MatchLog}) to {@value #FILE} in the game
 * folder (where the server or the singleplayer client runs). A failed write
 * is logged and skipped; it never stops the game.
 */
public final class MatchLogWriter {
    public static final String FILE = "royalarena/matches.jsonl";
    private static boolean announced;

    private MatchLogWriter() {
    }

    public static synchronized void append(String line) {
        Path file = FMLPaths.GAMEDIR.get().resolve(FILE);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, line + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            if (!announced) {
                announced = true;
                RoyalArena.LOGGER.info("[royalarena] Match log: {}",
                        file.toAbsolutePath());
            }
        } catch (IOException e) {
            RoyalArena.LOGGER.warn("[royalarena] Could not write the match " +
                    "log {}: {}", file.toAbsolutePath(), e.toString());
        }
    }
}
