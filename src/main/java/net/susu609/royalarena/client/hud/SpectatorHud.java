package net.susu609.royalarena.client.hud;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.match.TeamSide;

import java.util.List;

/**
 * While watching bots: one panel per bot at the edges of the wide screen,
 * RED on the left and BLUE on the right (the side-on camera puts their halves
 * there). Each shows the team and personality, crowns, the four cards in hand
 * (greyed when unaffordable), the next card and the elixir bar.
 */
final class SpectatorHud {
    /** Share of the screen width each panel takes (the camera keeps it free). */
    static final float PANEL_SHARE = 0.17F;
    private static final int BLUE = 0xFF2F6FD6;
    private static final int RED = 0xFFC8352E;
    private static final int PANEL = 0xB0141418;
    private static final int CARD_EDGE = 0xFF1E2433;
    private static final int CARD_DISABLED = 0x99606060;
    private static final int ELIXIR = 0xFFD05CFF;
    private static final int ELIXIR_DARK = 0xFF3A2140;
    private static final int ELIXIR_EDGE = 0xFF1A0E1F;
    private static final int ELIXIR_FULL = 0xFFF4C8FF;
    private static final int GOLD = 0xFFFFD84A;

    private SpectatorHud() {
    }

    static void draw(GuiGraphics graphics, Font font, int width, int height) {
        int margin = Math.max(4, width / 120);
        int panelW = Math.round(width * PANEL_SHARE) - 2 * margin;
        panel(graphics, font, TeamSide.RED, margin, panelW, height);
        panel(graphics, font, TeamSide.BLUE, width - margin - panelW, panelW,
                height);
    }

    private static void panel(GuiGraphics graphics, Font font, TeamSide side,
            int x, int w, int screenHeight)
    {
        int pad = Math.max(3, w / 24);
        int gap = Math.max(2, w / 30);
        int cardW = (w - 2 * pad - gap) / 2;
        int cardH = Math.round(cardW * 39.0F / 31.0F);
        int nextW = Math.round(cardW * 0.58F);
        int nextH = Math.round(cardH * 0.58F);
        int line = font.lineHeight + 2;
        int barH = Math.max(6, cardW / 6);
        int h = pad + 2 * line + 2 * cardH + gap + line + nextH + gap +
                barH + pad + line;
        int y = Math.max(4, (screenHeight - h) / 2);
        int color = side == TeamSide.BLUE ? BLUE : RED;

        graphics.fill(x, y, x + w, y + h, PANEL);
        graphics.fill(x, y, x + w, y + 2, color);
        int cy = y + pad;
        String team = side == TeamSide.BLUE ? "BLUE" : "RED";
        graphics.drawString(font, team, x + pad, cy, color | 0xFF000000, true);
        String crowns = BattleHudOverlay.crowns(side) + " ♛";
        graphics.drawString(font, crowns, x + w - pad - font.width(crowns), cy,
                GOLD, true);
        cy += line;
        String style = ClientMatchState.style(side);
        if (font.width(style) > w - 2 * pad)
            style = font.plainSubstrByWidth(style, w - 2 * pad);
        graphics.drawString(font, style, x + pad, cy, 0xFFDDDDDD, true);
        cy += line;

        int elixir = ClientMatchState.elixirTenths(side);
        List<BattleCard> hand = ClientMatchState.hand(side);
        for (int slot = 0; slot < 4 && slot < hand.size(); slot++) {
            BattleCard card = hand.get(slot);
            int cx = x + pad + (slot % 2) * (cardW + gap);
            int top = cy + (slot / 2) * (cardH + gap);
            CardFace.draw(graphics, card, cx, top, cardW, cardH, CARD_EDGE,
                    Math.max(1, cardW / 20));
            boolean affordable = card.cost() * 10 <= elixir;
            if (!affordable)
                graphics.fill(cx, top, cx + cardW, top + cardH, CARD_DISABLED);
            int badge = Math.max(8, cardW / 3);
            CardFace.costBadge(graphics, font, card.cost(), cx, top, badge, 1,
                    affordable);
        }
        cy += 2 * cardH + gap;

        graphics.drawString(font, "Next:", x + pad, cy + 1, 0xFFFFFFFF, true);
        BattleCard next = ClientMatchState.nextCard(side);
        if (next != null)
            CardFace.draw(graphics, next, x + pad + font.width("Next: ") + 2,
                    cy, nextW, nextH, CARD_EDGE, 1);
        cy += Math.max(line, nextH) + gap;

        int barX = x + pad;
        int barW = w - 2 * pad;
        graphics.fill(barX - 1, cy - 1, barX + barW + 1, cy + barH + 1,
                ELIXIR_EDGE);
        graphics.fill(barX, cy, barX + barW, cy + barH, ELIXIR_DARK);
        boolean full = elixir >= 100;
        float throb = full ? 0.5F + 0.5F * Mth.sin(Util.getMillis() / 140.0F)
                : 0.0F;
        int fill = full ? lerp(ELIXIR, ELIXIR_FULL, throb) : ELIXIR;
        graphics.fill(barX, cy, barX + Math.round(barW * Math.min(100, elixir)
                / 100.0F), cy + barH, fill);
        for (int i = 1; i < 10; i++) {
            int tx = barX + Math.round(barW * i / 10.0F);
            graphics.fill(tx, cy, tx + 1, cy + barH, ELIXIR_EDGE);
        }
        cy += barH + 2;
        String amount = (elixir / 10) + " elixir" +
                (ClientMatchState.elixirRate() > 1 ?
                        "  x" + ClientMatchState.elixirRate() : "");
        graphics.drawString(font, amount, x + pad, cy, full ? ELIXIR_FULL :
                0xFFE0B0FF, true);
    }

    private static int lerp(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
