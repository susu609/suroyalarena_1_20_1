package net.susu609.royalarena.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class RoyalArenaKeyMappings {
    public static final String CATEGORY = "key.categories.royalarena";

    public static final KeyMapping OPEN_MATCHING =
            key("key.royalarena.open_matching", GLFW.GLFW_KEY_M);
    public static final KeyMapping CARD_1 =
            key("key.royalarena.card_1", GLFW.GLFW_KEY_1);
    public static final KeyMapping CARD_2 =
            key("key.royalarena.card_2", GLFW.GLFW_KEY_2);
    public static final KeyMapping CARD_3 =
            key("key.royalarena.card_3", GLFW.GLFW_KEY_3);
    public static final KeyMapping CARD_4 =
            key("key.royalarena.card_4", GLFW.GLFW_KEY_4);

    private RoyalArenaKeyMappings() {
    }

    private static KeyMapping key(String translation, int key) {
        return new KeyMapping(
                translation, InputConstants.Type.KEYSYM, key, CATEGORY);
    }
}
