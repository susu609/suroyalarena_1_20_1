package net.susu609.royalarena.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.susu609.modlib.resource.RL;

public final class WConfig {

    private static final ForgeConfigSpec.Builder BUILDER =
            new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<String> EXAMPLE_RESOURCE =
            BUILDER.comment(
                            "Example ResourceLocation validated through " +
                                    "SuModLib.")
                    .define(
                            "exampleResource", "minecraft:stone",
                            WConfig::isValidResourceLocation);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private WConfig() {
    }

    private static boolean isValidResourceLocation(Object value) {
        return value instanceof String string && RL.parse(string) != null;
    }

    public static ResourceLocation exampleResource() {
        return RL.require(EXAMPLE_RESOURCE.get());
    }
}
