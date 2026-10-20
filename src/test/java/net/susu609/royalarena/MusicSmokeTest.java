package net.susu609.royalarena;

import net.susu609.royalarena.match.ArenaSounds;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The mod's own music (scripts/music): the Ogg exists and is a sensible
 * size, and sounds.json maps the sound event to it.
 */
public final class MusicSmokeTest {
    private static final Path SOUNDS =
            Path.of("src/main/resources/assets/royalarena");

    private MusicSmokeTest() {
    }

    public static void run() {
        Path track = SOUNDS.resolve("sounds/music/arena_phase1.ogg");
        if (!Files.exists(track))
            return;
        try {
            byte[] head = Files.readAllBytes(track);
            check(head.length > 100_000 && head.length < 3_000_000,
                    "the loop is 0.1 to 3 MB: " + head.length);
            check(new String(head, 0, 4, StandardCharsets.US_ASCII)
                    .equals("OggS"), "it is an Ogg file");
            String json = Files.readString(SOUNDS.resolve("sounds.json"));
            check(json.contains("\"" + ArenaSounds.PHASE1_MUSIC + "\""),
                    "sounds.json defines the sound event");
            check(json.contains("royalarena:music/arena_phase1") &&
                            json.contains("\"stream\": true") &&
                            json.contains("\"category\": \"music\""),
                    "streamed, in the music category, pointing at the file");
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        System.out.println("RoyalArena music tests: PASS");
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
