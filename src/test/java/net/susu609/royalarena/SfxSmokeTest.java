package net.susu609.royalarena;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The shot, hit and spell sounds (scripts/sfx): every variant has an Ogg of a
 * sensible size, and sounds.json maps the event ArenaSfx registers to them.
 */
public final class SfxSmokeTest {
    private static final Path SOUNDS =
            Path.of("src/main/resources/assets/royalarena");
    /** Event name and how many variants it has (compose_sfx.SOUNDS). */
    private static final Object[][] SOUNDS_AND_VARIANTS = {
            {"bow_shot", 3}, {"tower_shot", 3}, {"volley", 2},
            {"crossbow_shot", 3}, {"arrow_hit", 4}, {"stone_hit", 3},
            {"spell_throw", 3}, {"zap", 2}, {"freeze_hit", 2},
            {"poison_hit", 2}, {"poison_bubble", 4}};

    private SfxSmokeTest() {
    }

    public static void run() {
        try {
            String json = Files.readString(SOUNDS.resolve("sounds.json"),
                    StandardCharsets.UTF_8);
            for (Object[] sound : SOUNDS_AND_VARIANTS) {
                String name = (String) sound[0];
                check(json.contains("\"" + name + "\""),
                        name + " is in sounds.json");
                for (int v = 1; v <= (int) sound[1]; v++) {
                    String file = name + v;
                    Path ogg = SOUNDS.resolve("sounds/sfx/" + file + ".ogg");
                    check(Files.exists(ogg), file + ".ogg exists");
                    byte[] bytes = Files.readAllBytes(ogg);
                    check(bytes.length > 2_000 && bytes.length < 100_000,
                            file + " is a short clip: " + bytes.length);
                    check(new String(bytes, 0, 4, StandardCharsets.US_ASCII)
                            .equals("OggS"), file + " is an Ogg file");
                    check(json.contains("\"royalarena:sfx/" + file + "\""),
                            file + " is mapped in sounds.json");
                }
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
