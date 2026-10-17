package net.susu609.royalarena.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.susu609.modlib.resource.RL;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.card.BattleCard;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Optional hand-made card portraits. A card uses the PNG
 * {@code assets/royalarena/textures/gui/card/<card id>.png} from any loaded
 * resource pack (or this mod's own resources) when it exists; otherwise the
 * HUD falls back to the spawn egg of the card's mob. The file may have any
 * size, it is stretched over the card face (the card is 31:39, so 124x156 or
 * 248x312 is a good size). Lookups are cached and cleared on resource reload
 * (F3+T or changing resource packs).
 */
public final class CardArt {
    private static final Map<BattleCard, Optional<ResourceLocation>> CACHE =
            new EnumMap<>(BattleCard.class);

    private CardArt() {
    }

    /** Resource path of a card's portrait, e.g. {@code textures/gui/card/zombie.png}. */
    public static String path(BattleCard card) {
        return "textures/gui/card/" + card.id() + ".png";
    }

    /** The portrait texture, or {@code null} when no pack provides one. */
    public static ResourceLocation textureOf(BattleCard card) {
        return CACHE.computeIfAbsent(card, c -> {
            ResourceLocation location = RL.of(RoyalArena.MOD_ID, path(c));
            boolean present = Minecraft.getInstance().getResourceManager()
                    .getResource(location).isPresent();
            return present ? Optional.of(location) : Optional.empty();
        }).orElse(null);
    }

    public static void clear() {
        CACHE.clear();
    }
}
