package net.susu609.royalarena.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.registries.ForgeRegistries;
import net.susu609.modlib.resource.RL;
import net.susu609.royalarena.card.BattleCard;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws a card the same way everywhere (battle hand, next card, deck
 * screen): an edge, the card blue face and the art, which is the card's
 * portrait PNG ({@link CardArt}) or, without one, the mob's spawn egg / the
 * spell's icon item. The elixir cost badge sits on the top-left corner.
 */
public final class CardFace {
    public static final int FILL = 0xFF3B5B8C;
    public static final int EDGE = 0xFF1E2433;
    public static final int ELIXIR = 0xFFD05CFF;
    public static final int ELIXIR_DIM = 0xFF7A4A88;
    public static final int ELIXIR_EDGE = 0xFF1A0E1F;

    private static final Map<BattleCard, ItemStack> ICONS =
            new EnumMap<>(BattleCard.class);

    private CardFace() {
    }

    /** Edge, face and art inside the rectangle (x, y, w, h). */
    public static void draw(GuiGraphics graphics, BattleCard card, int x,
            int y, int w, int h, int edgeColor, int edge)
    {
        graphics.fill(x, y, x + w, y + h, edgeColor);
        graphics.fill(x + edge, y + edge, x + w - edge, y + h - edge, FILL);
        ResourceLocation art = CardArt.textureOf(card);
        if (art != null) {
            graphics.blit(art, x + edge, y + edge, w - 2 * edge,
                    h - 2 * edge, 0.0F, 0.0F, 1, 1, 1, 1);
            return;
        }
        float size = Math.min(w, h) * 0.75F;
        item(graphics, icon(card), x + w / 2.0F, y + h * 0.45F, size);
    }

    /**
     * Cost drop of {@code size} pixels on the top-left corner of a card at
     * (cardX, cardY), sticking out by {@code overhang}; drawn above items
     * and overlays.
     */
    public static void costBadge(GuiGraphics graphics, Font font, int cost,
            int cardX, int cardY, int size, int overhang, boolean affordable)
    {
        int x = cardX - overhang;
        int y = cardY - overhang;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 220.0F);
        graphics.fill(x, y, x + size, y + size, ELIXIR_EDGE);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1,
                affordable ? ELIXIR : ELIXIR_DIM);
        String text = Integer.toString(cost);
        float scale = size * 0.7F / 9.0F;
        pose.translate(x + size / 2.0F, y + size / 2.0F, 30.0F);
        pose.scale(scale, scale, 1.0F);
        graphics.drawString(font, text, -font.width(text) / 2, -4, 0xFFFFFFFF,
                true);
        pose.popPose();
    }

    private static void item(GuiGraphics graphics, ItemStack stack,
            float centerX, float centerY, float size)
    {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX - size / 2.0F, centerY - size / 2.0F, 0.0F);
        float scale = size / 16.0F;
        pose.scale(scale, scale, 1.0F);
        graphics.renderItem(stack, 0, 0);
        pose.popPose();
    }

    /**
     * Fallback card art: the spawn egg of a troop's mob, or a spell's icon
     * item.
     */
    public static ItemStack icon(BattleCard card) {
        return ICONS.computeIfAbsent(card, c -> {
            if (c.isSpell()) {
                Item item = ForgeRegistries.ITEMS.getValue(
                        RL.require(c.iconItemId()));
                ItemStack stack = new ItemStack(item == null ? Items.PAPER :
                        item);
                // A plain splash potion is "uncraftable"; show healing.
                if (c == BattleCard.HEALING_POTION)
                    PotionUtils.setPotion(stack, Potions.HEALING);
                return stack;
            }
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(
                    RL.require(c.entityId()));
            SpawnEggItem egg = type == null ? null : SpawnEggItem.byId(type);
            return new ItemStack(egg == null ? Items.PAPER : egg);
        });
    }
}
