package net.susu609.royalarena.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.registries.ForgeRegistries;
import net.susu609.modlib.resource.RL;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.client.camera.ArenaCameraController;
import net.susu609.royalarena.client.hud.HudLayout.Rect;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Battle HUD laid out in {@link HudLayout} reference units: a wooden panel
 * with four portrait cards, a smaller next card on the left and a continuous
 * elixir bar under the cards; a timer box top right and crown counters on the
 * right edge. Unaffordable cards are greyed out.
 */
public final class BattleHudOverlay implements IGuiOverlay {
    public static final BattleHudOverlay INSTANCE = new BattleHudOverlay();

    private static final int PANEL_EDGE = 0xFF3A2614;
    private static final int PANEL_FILL = 0xFF6E4B2A;
    private static final int CARD_EDGE = 0xFF1E2433;
    private static final int CARD_SELECTED = 0xFFFFD84A;
    private static final int CARD_GLOW = 0x80FFE680;
    private static final int CARD_HOVER = 0xFFB8C8E8;
    private static final int CARD_HOVER_TINT = 0x20FFFFFF;
    private static final int CARD_FILL = 0xFF3B5B8C;
    private static final int CARD_DISABLED = 0x99606060;
    private static final int ELIXIR = 0xFFD05CFF;
    private static final int ELIXIR_DARK = 0xFF3A2140;
    /** Cost badge of a card the player cannot afford yet. */
    private static final int ELIXIR_DIM = 0xFF7A4A88;
    private static final int ELIXIR_EDGE = 0xFF1A0E1F;
    private static final int BLUE = 0xFF2F6FD6;
    private static final int RED = 0xFFC8352E;

    private static final Map<BattleCard, ItemStack> ICONS =
            new EnumMap<>(BattleCard.class);
    /** Animated lift of each hand slot, in reference units. */
    private static final float[] LIFTS = new float[4];
    private static long lastFrameMillis;

    private BattleHudOverlay() {
    }

    public static int cardSlotAt(double mouseX, double mouseY, int screenWidth,
            int screenHeight)
    {
        return HudLayout.of(screenWidth, screenHeight)
                .cardSlotAt(mouseX, mouseY);
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick,
            int screenWidth, int screenHeight)
    {
        if (!ClientMatchState.active())
            return;
        Minecraft mc = Minecraft.getInstance();
        HudLayout layout = HudLayout.of(screenWidth, screenHeight);

        drawPanel(graphics, layout);
        drawHand(graphics, mc.font, layout);
        drawNext(graphics, mc.font, layout);
        drawElixir(graphics, mc.font, layout);
        drawTimer(graphics, mc.font, layout);
        drawCrowns(graphics, mc.font, layout);
        drawBanner(graphics, mc.font, layout);
    }

    private static void drawPanel(GuiGraphics graphics, HudLayout layout) {
        Rect panel = layout.panel();
        graphics.fill(panel.x(), panel.y(), panel.right(), panel.bottom(),
                PANEL_EDGE);
        int edge = Math.max(1, Math.round(layout.u() * 2.0F));
        graphics.fill(panel.x() + edge, panel.y() + edge, panel.right() - edge,
                panel.bottom(), PANEL_FILL);
    }

    private static void drawHand(GuiGraphics graphics, Font font,
            HudLayout layout)
    {
        List<BattleCard> hand = ClientMatchState.hand();
        int elixir = ClientMatchState.elixirTenths();
        int hovered = layout.cardSlotAt(
                ArenaCameraController.cursorGuiX(),
                ArenaCameraController.cursorGuiY());
        long now = Util.getMillis();
        float elapsed = lastFrameMillis == 0L ? 0.0F :
                Math.min(100.0F, now - lastFrameMillis);
        lastFrameMillis = now;
        for (int slot = 0; slot < hand.size() && slot < 4; slot++) {
            BattleCard card = hand.get(slot);
            boolean selected = ClientMatchState.selectedSlot() == slot;
            boolean hover = !selected && hovered == slot;
            boolean affordable = card.cost() * 10 <= elixir;
            float target = selected ? HudLayout.SELECT_LIFT :
                    hover ? HudLayout.HOVER_LIFT : 0.0F;
            // Everything on the card (frame, art, tint, cost badge) follows
            // this one animated lift, so it all rises and falls together.
            float lift = LIFTS[slot] =
                    HudLayout.approachLift(LIFTS[slot], target, elapsed);
            Rect r = layout.card(slot, lift);
            if (selected) {
                int glow = Math.max(1, Math.round(layout.u() * 2.0F));
                graphics.fill(r.x() - glow, r.y() - glow, r.right() + glow,
                        r.bottom() + glow, CARD_GLOW);
            }
            drawCardFace(graphics, layout, card, r,
                    selected ? CARD_SELECTED : hover ? CARD_HOVER : CARD_EDGE);
            if (!affordable)
                overlay(graphics, r, CARD_DISABLED);
            else if (hover || selected)
                overlay(graphics, r, CARD_HOVER_TINT);

            Rect cost = layout.cardCost(slot, lift);
            drawDrop(graphics, cost, affordable ? ELIXIR : ELIXIR_DIM);
            text(graphics, font, Integer.toString(card.cost()),
                    cost.centerX(), cost.centerY(), textScale(layout, 8.0F),
                    0xFFFFFFFF);
        }
    }

    private static void drawNext(GuiGraphics graphics, Font font,
            HudLayout layout)
    {
        BattleCard next = ClientMatchState.nextCard();
        if (next == null)
            return;
        Rect label = layout.nextLabel();
        text(graphics, font, "Next:", label.centerX(), label.centerY(),
                textScale(layout, 7.0F), 0xFFFFFFFF);
        drawCardFace(graphics, layout, next, layout.nextCard(), CARD_EDGE);
    }

    private static void drawCardFace(GuiGraphics graphics, HudLayout layout,
            BattleCard card, Rect r, int edgeColor)
    {
        int edge = Math.max(1, Math.round(layout.u() * 1.5F));
        graphics.fill(r.x(), r.y(), r.right(), r.bottom(), edgeColor);
        graphics.fill(r.x() + edge, r.y() + edge, r.right() - edge,
                r.bottom() - edge, CARD_FILL);
        ResourceLocation art = CardArt.textureOf(card);
        if (art != null) {
            graphics.blit(art, r.x() + edge, r.y() + edge,
                    r.w() - 2 * edge, r.h() - 2 * edge, 0.0F, 0.0F, 1, 1, 1,
                    1);
            return;
        }
        float size = Math.min(r.w(), r.h()) * 0.75F;
        item(graphics, icon(card), r.centerX(),
                r.y() + r.h() * 0.45F, size);
    }

    private static void drawElixir(GuiGraphics graphics, Font font,
            HudLayout layout)
    {
        int tenths = ClientMatchState.elixirTenths();
        Rect bar = layout.elixirBar();
        graphics.fill(bar.x() - 1, bar.y() - 1, bar.right() + 1,
                bar.bottom() + 1, ELIXIR_EDGE);
        graphics.fill(bar.x(), bar.y(), bar.right(), bar.bottom(),
                ELIXIR_DARK);
        int filled = Math.round(bar.w() * Math.min(100, tenths) / 100.0F);
        graphics.fill(bar.x(), bar.y(), bar.x() + filled, bar.bottom(),
                ELIXIR);
        for (int i = 1; i < 10; i++) {
            int x = bar.x() + Math.round(bar.w() * i / 10.0F);
            graphics.fill(x, bar.y(), x + 1, bar.bottom(), ELIXIR_EDGE);
        }
        Rect drop = layout.elixirDrop();
        drawDrop(graphics, drop);
        text(graphics, font, Integer.toString(tenths / 10), drop.centerX(),
                drop.centerY(), textScale(layout, 11.0F), 0xFFFFFFFF);
    }

    private static void drawTimer(GuiGraphics graphics, Font font,
            HudLayout layout)
    {
        Rect box = layout.timerBox();
        graphics.fill(box.x(), box.y(), box.right(), box.bottom(), 0xB0101010);
        int seconds = Math.max(0, ClientMatchState.ticksRemaining() / 20);
        if (ClientMatchState.state() != MatchState.BATTLE)
            seconds = 0;
        float u = layout.u();
        text(graphics, font, "Time left:", box.centerX(), box.y() + 4.0F * u,
                textScale(layout, 5.0F), 0xFFDDDDDD);
        text(graphics, font,
                String.format("%d:%02d", seconds / 60, seconds % 60),
                box.centerX(), box.y() + 10.5F * u, textScale(layout, 8.0F),
                0xFFFFFFFF);
    }

    private static void drawCrowns(GuiGraphics graphics, Font font,
            HudLayout layout)
    {
        TeamSide local = ClientMatchState.side();
        TeamSide opponent = local.opponent();
        crownBox(graphics, font, layout, layout.topCrowns(), opponent);
        crownBox(graphics, font, layout, layout.bottomCrowns(), local);
    }

    private static void crownBox(GuiGraphics graphics, Font font,
            HudLayout layout, Rect r, TeamSide side)
    {
        graphics.fill(r.x(), r.y(), r.right(), r.bottom(),
                side == TeamSide.BLUE ? BLUE : RED);
        text(graphics, font, Integer.toString(crowns(side)), r.centerX(),
                r.centerY(), textScale(layout, 9.0F), 0xFFFFFFFF);
    }

    /** Crowns of {@code side}: destroyed towers of the opponent. */
    private static int crowns(TeamSide side) {
        int count = 0;
        for (TowerId id : TowerId.values()) {
            if (id.side() != side &&
                    ClientMatchState.towerHealth(id) <= 0)
                count++;
        }
        return count;
    }

    private static void drawBanner(GuiGraphics graphics, Font font,
            HudLayout layout)
    {
        String text;
        int color;
        if (ClientMatchState.state() == MatchState.COUNTDOWN) {
            text = Integer.toString(Math.max(
                    1, (ClientMatchState.ticksRemaining() + 19) / 20));
            color = 0xFFFFFF55;
        } else if (ClientMatchState.state() == MatchState.FINISHED) {
            TeamSide winner = ClientMatchState.winner();
            boolean won = winner == ClientMatchState.side();
            text = winner == null ? "DRAW" : won ? "VICTORY" : "DEFEAT";
            color = winner == null ? 0xFFFFFFFF :
                    won ? 0xFF55FF55 : 0xFFFF6666;
        } else {
            return;
        }
        Rect r = layout.banner();
        text(graphics, font, text, r.centerX(), r.centerY(),
                textScale(layout, 24.0F), color);
    }

    private static void drawDrop(GuiGraphics graphics, Rect r) {
        drawDrop(graphics, r, ELIXIR);
    }

    /** Elixir drop above items and card overlays, so it never greys out. */
    private static void drawDrop(GuiGraphics graphics, Rect r, int fill) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 220.0F);
        graphics.fill(r.x(), r.y(), r.right(), r.bottom(), ELIXIR_EDGE);
        graphics.fill(r.x() + 1, r.y() + 1, r.right() - 1, r.bottom() - 1,
                fill);
        pose.popPose();
    }

    /** Draws over items, which GuiGraphics renders at z = 150. */
    private static void overlay(GuiGraphics graphics, Rect r, int color) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 200.0F);
        graphics.fill(r.x(), r.y(), r.right(), r.bottom(), color);
        pose.popPose();
    }

    /** Scale that makes the 9-pixel font {@code refHeight} units tall. */
    private static float textScale(HudLayout layout, float refHeight) {
        return refHeight * layout.u() / 9.0F;
    }

    /** Centred text above items and overlays. */
    private static void text(GuiGraphics graphics, Font font, String s,
            float centerX, float centerY, float scale, int color)
    {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 250.0F);
        pose.scale(scale, scale, 1.0F);
        graphics.drawString(font, s, -font.width(s) / 2, -4, color, true);
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
    private static ItemStack icon(BattleCard card) {
        return ICONS.computeIfAbsent(card, c -> {
            if (c.isSpell()) {
                Item item = ForgeRegistries.ITEMS.getValue(
                        RL.require(c.iconItemId()));
                return new ItemStack(item == null ? Items.PAPER : item);
            }
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(
                    RL.require(c.entityId()));
            SpawnEggItem egg = type == null ? null : SpawnEggItem.byId(type);
            return new ItemStack(egg == null ? Items.PAPER : egg);
        });
    }
}
