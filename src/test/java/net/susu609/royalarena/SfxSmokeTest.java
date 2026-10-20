package net.susu609.royalarena;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The shot and spell-cast sounds (scripts/sfx): each has an Ogg of a sensible
 * size and a sounds.json entry under the id ArenaSfx registers.
 */
public final class SfxSmokeTest {
    private static final Path SOUNDS =
            Path.of("src/main/resources/assets/royalarena");
    private static final String[] NAMES = {
            "bow_shot", "tower_shot", "volley", "zap_cast", "freeze_cast",
            "poison_cast"};

    private SfxSmokeTest() {
    }

    public static void run() {
        try {
            String json = Files.readString(SOUNDS.resolve("sounds.json"),
                    StandardCharsets.UTF_8);
            for (String name : NAMES) {
                Path ogg = SOUNDS.resolve("sounds/sfx/" + name + ".ogg");
                check(Files.exists(ogg), name + ".ogg exists");
                byte[] bytes = Files.readAllBytes(ogg);
                check(bytes.length > 2_000 && bytes.length < 200_000,
                        name + " is a short clip: " + bytes.length);
                check(new String(bytes, 0, 4, StandardCharsets.US_ASCII)
                        .equals("OggS"), name + " is an Ogg file");
                check(json.contains("\"" + name + "\""),
                        name + " is in sounds.json");
                check(json.contains("royalarena:sfx/" + name),
                        name + " points at sounds/sfx");
            }
        } catch (IOException e) {
            throw new AssertionError("sfx files unreadable", e);
        }
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
