package net.susu609.royalarena.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.susu609.royalarena.client.camera.ArenaCameraProfile;

/**
 * Client settings, file {@code config/royalarena-client.toml} in the game
 * folder (in the dev environment: {@code run-client1/config/}). Forge reloads
 * the file when it is saved, and the camera reads it every tick, so changes
 * show up while a match is running.
 */
public final class ArenaClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER =
            new ForgeConfigSpec.Builder();

    static {
        BUILDER.comment("Strategic camera during a match.").push("camera");
    }

    public static final ForgeConfigSpec.DoubleValue PITCH = BUILDER
            .comment("Degrees below the horizontal. 90 = straight down, " +
                    "about 65-70 = almost top-down with a slight diagonal.")
            .defineInRange("pitch", ArenaCameraProfile.DEFAULT.pitch(), 20.0D,
                    89.0D);

    public static final ForgeConfigSpec.DoubleValue DISTANCE = BUILDER
            .comment("Blocks from the camera to the point it looks at. " +
                    "Larger = whole arena smaller on screen.")
            .defineInRange("distance", ArenaCameraProfile.DEFAULT.distance(),
                    5.0D, 80.0D);

    public static final ForgeConfigSpec.DoubleValue LOOK_AHEAD = BUILDER
            .comment("Blocks from the arena centre toward your own side that " +
                    "the camera looks at. Larger = arena drawn higher on " +
                    "screen, clear of the card panel.")
            .defineInRange("lookAhead", ArenaCameraProfile.DEFAULT.lookAhead(),
                    -16.0D, 16.0D);

    public static final ForgeConfigSpec.DoubleValue FOV = BUILDER
            .comment("Field of view during a match. 0 = keep your normal " +
                    "game FOV. Lower = flatter, less perspective.")
            .defineInRange("fov", 0.0D, 0.0D, 110.0D);

    static {
        BUILDER.pop();
        BUILDER.comment("Battle effects.").push("effects");
    }

    public static final ForgeConfigSpec.DoubleValue CAMERA_SHAKE = BUILDER
            .comment("How hard the camera shakes on TNT, Lightning, Golem " +
                    "blows and falling towers. 0 = never, 1 = normal.")
            .defineInRange("cameraShake", 1.0D, 0.0D, 2.0D);

    public static final ForgeConfigSpec.BooleanValue DAMAGE_NUMBERS = BUILDER
            .comment("Damage and healing numbers floating up from troops " +
                    "and towers.")
            .define("damageNumbers", true);

    public static final ForgeConfigSpec.DoubleValue WORLD_TEXT_SCALE = BUILDER
            .comment("Size of damage numbers and health bars over troops " +
                    "and towers. 1 = normal.")
            .defineInRange("worldTextScale", 1.0D, 0.5D, 2.0D);

    public static final ForgeConfigSpec.BooleanValue TEAM_RINGS = BUILDER
            .comment("A blue or red ring on the floor under every troop.")
            .define("teamRings", true);

    public static final ForgeConfigSpec.BooleanValue BATTLE_MUSIC = BUILDER
            .comment("Battle music (the Pigstep disc, on the Music volume) " +
                    "while a battle runs.")
            .define("battleMusic", true);

    public static final ForgeConfigSpec.BooleanValue ARENA_MUSIC = BUILDER
            .comment("Play the mod's own tune, Arena March, in the first " +
                    "phase of a battle (from the countdown until the elixir " +
                    "speeds up); false keeps Pigstep all battle. Needs " +
                    "battleMusic.")
            .define("arenaMusic", true);

    public static final ForgeConfigSpec.BooleanValue MENU_HINT = BUILDER
            .comment("A reminder in the top left corner, outside a match, " +
                    "of the key that opens the Royal Arena screen.")
            .define("menuHint", true);

    static {
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ArenaClientConfig() {
    }

    /** Camera built from the current file values. */
    public static ArenaCameraProfile cameraProfile() {
        return new ArenaCameraProfile(PITCH.get().floatValue(), DISTANCE.get(),
                LOOK_AHEAD.get());
    }

    /** The FOV the match camera renders with. */
    public static double effectiveFov(double gameFov) {
        double fov = FOV.get();
        return fov > 0.0D ? fov : gameFov;
    }
}
