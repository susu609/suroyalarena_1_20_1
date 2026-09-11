package net.susu609.royalarena.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.List;

public final class BattleHudOverlay implements IGuiOverlay {
    public static final BattleHudOverlay INSTANCE = new BattleHudOverlay();
    private static final int CARD_W = 58;
    private static final int CARD_H = 34;
    private static final int CARD_GAP = 4;

    private BattleHudOverlay() {
    }

    public static int cardSlotAt(double mouseX, double mouseY, int screenWidth,
            int screenHeight)
    {
        int totalW = 4 * CARD_W + 3 * CARD_GAP;
        int startX = (screenWidth - totalW) / 2;
        int y = screenHeight - CARD_H - 14;
        for (int slot = 0; slot < 4; slot++) {
            int x = startX + slot * (CARD_W + CARD_GAP);
            if (mouseX >= x && mouseX < x + CARD_W && mouseY >= y &&
                    mouseY < y + CARD_H)
            {
                return slot;
            }
        }
        return -1;
    }
    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick,
            int screenWidth, int screenHeight)
    {
        if (!ClientMatchState.active())
            return;
        Minecraft mc = Minecraft.getInstance();
        List<BattleCard> hand = ClientMatchState.hand();

        int totalW = 4 * CARD_W + 3 * CARD_GAP;
        int startX = (screenWidth - totalW) / 2;
        int y = screenHeight - CARD_H - 14;
        for (int i = 0; i < hand.size(); i++) {
            BattleCard card = hand.get(i);
            int x = startX + i * (CARD_W + CARD_GAP);
            boolean selected = ClientMatchState.selectedSlot() == i;
            graphics.fill(
                    x, y, x + CARD_W, y + CARD_H,
                    selected ? 0xE09A63FF : 0xD0222222);
            graphics.fill(
                    x + 2, y + 2, x + CARD_W - 2, y + CARD_H - 2, 0xD03A3A3A);
            graphics.drawString(
                    mc.font, (i + 1) + "  " + card.displayName(), x + 5, y + 6,
                    0xFFFFFFFF, false);
            graphics.drawString(
                    mc.font, "Elixir " + card.cost(), x + 5, y + 19, 0xFFDFA7FF,
                    false);
        }

        BattleCard next = ClientMatchState.nextCard();
        if (next != null) {
            int x = startX + totalW + 8;
            graphics.fill(x, y + 7, x + 52, y + CARD_H, 0xC0202020);
            graphics.drawString(
                    mc.font, "NEXT", x + 5, y + 10, 0xFFAAAAAA, false);
            graphics.drawString(
                    mc.font, next.displayName(), x + 5, y + 21, 0xFFFFFFFF,
                    false);
        }

        drawElixir(graphics, mc, screenWidth, y - 16);
        drawMatchInfo(graphics, mc, screenWidth);
    }

    private static void drawElixir(GuiGraphics graphics, Minecraft mc,
            int screenWidth, int y)
    {
        int tenths = ClientMatchState.elixirTenths();
        int whole = tenths / 10;
        int barW = 12 * 10;
        int x = (screenWidth - barW) / 2;
        for (int i = 0; i < 10; i++) {
            int bx = x + i * 12;
            graphics.fill(
                    bx, y, bx + 10, y + 8, i < whole ? 0xFFE05CFF : 0xFF3A2140);
        }
        graphics.drawString(
                mc.font, String.format("%.1f / 10", tenths / 10.0F),
                x + barW + 6, y, 0xFFFFFFFF, false);
    }
    private static void drawMatchInfo(GuiGraphics graphics, Minecraft mc,
            int screenWidth)
    {
        int seconds = Math.max(0, ClientMatchState.ticksRemaining() / 20);
        String timer = String.format("%d:%02d", seconds / 60, seconds % 60);
        graphics.drawCenteredString(
                mc.font, timer, screenWidth / 2, 10, 0xFFFFFFFF);

        int blueKing = ClientMatchState.towerHealth(TowerId.BLUE_KING);
        int redKing = ClientMatchState.towerHealth(TowerId.RED_KING);
        graphics.drawString(
                mc.font, "BLUE King " + blueKing, 10, 10, 0xFF66AAFF, true);
        String red = "RED King " + redKing;
        graphics.drawString(
                mc.font, red, screenWidth - mc.font.width(red) - 10, 10,
                0xFFFF7777, true);

        if (ClientMatchState.state() == MatchState.COUNTDOWN) {
            int count =
                    Math.max(1, (ClientMatchState.ticksRemaining() + 19) / 20);
            graphics.drawCenteredString(
                    mc.font, Integer.toString(count), screenWidth / 2, 34,
                    0xFFFFFF55);
        } else if (ClientMatchState.state() == MatchState.FINISHED) {
            TeamSide winner = ClientMatchState.winner();
            String text = winner == null ? "DRAW" :
                    (winner == ClientMatchState.side() ? "VICTORY" : "DEFEAT");
            graphics.drawCenteredString(
                    mc.font, text, screenWidth / 2, 34,
                    winner == ClientMatchState.side() ? 0xFF55FF55 :
                            0xFFFF6666);
        }
    }
}
