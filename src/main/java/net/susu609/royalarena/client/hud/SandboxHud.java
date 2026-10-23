package net.susu609.royalarena.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.client.camera.ArenaCameraController;
import net.susu609.royalarena.client.input.RoyalArenaKeyMappings;
import net.susu609.royalarena.client.screen.SandboxCardScreen;
import net.susu609.royalarena.client.screen.SandboxCompareScreen;
import net.susu609.royalarena.client.screen.SandboxTribeScreen;
import net.susu609.royalarena.client.state.ClientSandbox;
import net.susu609.royalarena.match.MatchSnapshot;
import net.susu609.royalarena.match.SandboxQuickBar;
import net.susu609.royalarena.match.SandboxSettings;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.c2s.C2SSandboxPacket;
import net.susu609.royalarena.network.c2s.C2SSandboxPacket.Action;

import java.util.ArrayList;
import java.util.List;

/**
 * The sandbox panel (docs/SANDBOX.md), down the left edge of the screen in
 * place of the hand: the side cards are played for (Tab), the card picker
 * (C), and the tools; and the quick bar along the bottom (keys 1-8), where
 * the hand is in a match. Clicks reach both through {@link #click} before
 * the arena gets them.
 */
public final class SandboxHud {
    private static final int LEFT = 6;
    private static final int TOP = 6;
    private static final int WIDTH = 120;
    /** Button height and gap on a tall screen; they shrink on a short one. */
    private static final int ROW_MAX = 16;
    private static final int ROW_MIN = 11;
    private static final int GAP_MAX = 2;
    /** Buttons in the panel, and the gaps between them in units of one gap. */
    private static final int BUTTONS = 11;
    private static final int GAP_UNITS = 14;
    /** The pick's name under the panel. */
    private static final int NAME_LINE = 18;
    private static final int BLUE = 0xFF1F4FD6;
    private static final int RED = 0xFFC81E1E;
    private static final int PLAIN = 0xFF3A3F4C;
    private static final int ON = 0xFF2E7D32;
    private static final int EXIT = 0xFF6A1B1B;
    private static final String[] SPEED_LABELS = {"||", "0.5x", "1x", "2x"};
    /** Quick bar cards: size, gap between them, gap to the screen bottom. */
    private static final int QUICK_W = 30;
    private static final int QUICK_H = 38;
    private static final int QUICK_GAP = 4;
    private static final int QUICK_BOTTOM = 6;
    private static final int PICKED = 0xFFFFD84A;
    private static final int PIN = 0xFF7FD4FF;

    private record Button(int x, int y, int w, int h, String label, int color,
                          Runnable action)
    {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private SandboxHud() {
    }

    /** Draws the panel; the arena HUD skips the hand in a sandbox. */
    public static void draw(GuiGraphics graphics, Font font) {
        MatchSnapshot.Sandbox box = ClientSandbox.settings();
        if (box == null)
            return;
        List<Button> buttons = buttons(box);
        int bottom = TOP;
        for (Button b : buttons)
            bottom = Math.max(bottom, b.y + b.h);
        graphics.fill(LEFT - 3, TOP - 3, LEFT + WIDTH + 3,
                bottom + NAME_LINE, 0xC0101010);
        double mx = ArenaCameraController.cursorGuiX();
        double my = ArenaCameraController.cursorGuiY();
        for (Button b : buttons) {
            graphics.fill(b.x, b.y, b.x + b.w, b.y + b.h, 0xFF000000);
            int fill = b.contains(mx, my) ? brighten(b.color) : b.color;
            graphics.fill(b.x + 1, b.y + 1, b.x + b.w - 1, b.y + b.h - 1, fill);
            graphics.drawCenteredString(font, b.label, b.x + b.w / 2,
                    b.y + (b.h - 8) / 2, 0xFFFFFFFF);
        }
        BattleCard card = ClientSandbox.card();
        String picked = card == null ? "No card picked" :
                card.displayName() + " (" + card.cost() + ")";
        graphics.drawString(font, picked, LEFT, bottom + 5,
                card == null ? 0xFF999999 : 0xFFFFE680, true);
        drawQuickBar(graphics, font, graphics.guiWidth(), graphics.guiHeight(),
                mx, my);
    }

    /** Left edge of quick bar slot {@code slot} on a screen this wide. */
    private static int quickX(int slot, int width) {
        int total = SandboxQuickBar.SIZE * QUICK_W +
                (SandboxQuickBar.SIZE - 1) * QUICK_GAP;
        // Centred, but right of the panel when the screen is narrow.
        int left = Math.max((width - total) / 2, LEFT + WIDTH + 8);
        return left + slot * (QUICK_W + QUICK_GAP);
    }

    private static int quickY(int height) {
        return height - QUICK_BOTTOM - QUICK_H;
    }

    /** The quick bar slot under (mx, my), or -1. */
    private static int quickSlotAt(double mx, double my, int width,
            int height)
    {
        int y = quickY(height);
        if (my < y || my >= y + QUICK_H)
            return -1;
        for (int slot = 0; slot < SandboxQuickBar.SIZE; slot++) {
            int x = quickX(slot, width);
            if (mx >= x && mx < x + QUICK_W)
                return slot;
        }
        return -1;
    }

    /**
     * Eight slots: pinned cards (blue corner) first, then the last picks;
     * the key number under each, the pick outlined in gold.
     */
    private static void drawQuickBar(GuiGraphics graphics, Font font,
            int width, int height, double mx, double my)
    {
        SandboxQuickBar bar = ClientSandbox.quickBar();
        int y = quickY(height);
        int hovered = quickSlotAt(mx, my, width, height);
        graphics.fill(quickX(0, width) - 3, y - 3,
                quickX(SandboxQuickBar.SIZE - 1, width) + QUICK_W + 3,
                y + QUICK_H + 3, 0x90101010);
        for (int slot = 0; slot < SandboxQuickBar.SIZE; slot++) {
            int x = quickX(slot, width);
            BattleCard card = bar.at(slot);
            if (card == null) {
                graphics.fill(x, y, x + QUICK_W, y + QUICK_H, 0x60303540);
            } else {
                int edge = card == ClientSandbox.card() ? PICKED :
                        slot == hovered ? 0xFFB8C8E8 : CardFace.EDGE;
                CardFace.draw(graphics, card, x, y, QUICK_W, QUICK_H, edge, 2);
                CardFace.costBadge(graphics, font, card.cost(), x, y, 10, 2,
                        true);
                if (bar.pinned(card))
                    graphics.fill(x + QUICK_W - 6, y + 1, x + QUICK_W - 1,
                            y + 6, PIN);
            }
            graphics.drawCenteredString(font, String.valueOf(slot + 1),
                    x + QUICK_W / 2, y + QUICK_H - 9, 0xFFFFFFFF);
        }
        if (hovered >= 0 && bar.at(hovered) != null)
            graphics.renderTooltip(font,
                    net.minecraft.network.chat.Component.literal(
                            bar.at(hovered).displayName() + " (" +
                                    bar.at(hovered).cost() + ")"),
                    (int) mx, (int) my);
    }

    /** Runs the button under the cursor; false when it missed the panel. */
    public static boolean click(double mx, double my) {
        MatchSnapshot.Sandbox box = ClientSandbox.settings();
        if (box == null)
            return false;
        var window = Minecraft.getInstance().getWindow();
        int slot = quickSlotAt(mx, my, window.getGuiScaledWidth(),
                window.getGuiScaledHeight());
        if (slot >= 0) {
            ClientSandbox.selectQuick(slot);
            return true;
        }
        for (Button b : buttons(box)) {
            if (b.contains(mx, my)) {
                b.action.run();
                return true;
            }
        }
        return false;
    }

    /**
     * {row height, gap}: the largest that lets the whole panel and the pick's
     * name fit on the screen, down to {@link #ROW_MIN}; a short screen never
     * needs scrolling.
     */
    private static int[] metrics() {
        int room = Minecraft.getInstance().getWindow().getGuiScaledHeight() -
                2 * TOP - NAME_LINE;
        for (int row = ROW_MAX; row > ROW_MIN; row--) {
            int gap = row >= ROW_MAX ? GAP_MAX : 1;
            if (BUTTONS * row + GAP_UNITS * gap <= room)
                return new int[] {row, gap};
        }
        return new int[] {ROW_MIN, 1};
    }

    private static List<Button> buttons(MatchSnapshot.Sandbox box) {
        List<Button> out = new ArrayList<>();
        int[] m = metrics();
        final int ROW = m[0];
        final int GAP = m[1];
        int y = TOP;
        TeamSide side = ClientSandbox.side();
        out.add(new Button(LEFT, y, WIDTH, ROW, "Side: " + side.name() + " [" +
                key(RoyalArenaKeyMappings.SANDBOX_SIDE) + "]",
                side == TeamSide.BLUE ? BLUE : RED, ClientSandbox::toggleSide));
        y += ROW + GAP;
        out.add(new Button(LEFT, y, WIDTH, ROW, "Cards... [" +
                key(RoyalArenaKeyMappings.SANDBOX_CARDS) + "]", PLAIN,
                SandboxHud::openCards));
        y += ROW + GAP * 3;
        out.add(new Button(LEFT, y, WIDTH, ROW, "Clear troops", PLAIN,
                () -> send(C2SSandboxPacket.of(Action.CLEAR))));
        y += ROW + GAP;
        out.add(new Button(LEFT, y, WIDTH, ROW, "Restore towers", PLAIN,
                () -> send(C2SSandboxPacket.of(Action.RESTORE_TOWERS))));
        y += ROW + GAP;
        boolean unbreakable = box.towersInvulnerable();
        out.add(new Button(LEFT, y, WIDTH, ROW, unbreakable ?
                "Towers: unbreakable" : "Towers: take damage",
                unbreakable ? ON : PLAIN,
                () -> send(C2SSandboxPacket.of(Action.TOWERS_UNBREAKABLE,
                        unbreakable ? 0 : 1))));
        y += ROW + GAP;
        boolean endless = box.infiniteElixir();
        out.add(new Button(LEFT, y, WIDTH, ROW, endless ? "Elixir: endless" :
                "Elixir: B " + box.blueElixir() / 10 + " / R " +
                        box.redElixir() / 10, endless ? ON : PLAIN,
                () -> send(C2SSandboxPacket.of(Action.INFINITE_ELIXIR,
                        endless ? 0 : 1))));
        y += ROW + GAP;
        int w = (WIDTH - GAP * 3) / 4;
        for (int i = 0; i < SandboxSettings.SPEEDS.length; i++) {
            int speed = SandboxSettings.SPEEDS[i];
            out.add(new Button(LEFT + i * (w + GAP), y, w, ROW,
                    SPEED_LABELS[i], box.speed() == speed ? ON : PLAIN,
                    () -> send(C2SSandboxPacket.of(Action.SPEED, speed))));
        }
        y += ROW + GAP;
        out.add(new Button(LEFT, y, WIDTH, ROW, "Tribes...", PLAIN,
                () -> Minecraft.getInstance()
                        .setScreen(new SandboxTribeScreen())));
        y += ROW + GAP;
        out.add(new Button(LEFT, y, WIDTH, ROW, "Compare cards...", PLAIN,
                () -> Minecraft.getInstance()
                        .setScreen(new SandboxCompareScreen())));
        y += ROW + GAP;
        out.add(new Button(LEFT, y, WIDTH, ROW, ClientSandbox.stats() ?
                "Stats: on" : "Stats: off", ClientSandbox.stats() ? ON : PLAIN,
                ClientSandbox::toggleStats));
        y += ROW + GAP * 3;
        out.add(new Button(LEFT, y, WIDTH, ROW, "Exit sandbox", EXIT,
                () -> send(C2SSandboxPacket.of(Action.EXIT))));
        return out;
    }

    /** Opens the card picker (button or key). */
    public static void openCards() {
        Minecraft.getInstance().setScreen(new SandboxCardScreen());
    }

    private static void send(C2SSandboxPacket packet) {
        ModNetwork.sendToServer(packet);
    }

    private static String key(net.minecraft.client.KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage().getString();
    }

    private static int brighten(int argb) {
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 40);
        int g = Math.min(255, ((argb >> 8) & 0xFF) + 40);
        int b = Math.min(255, (argb & 0xFF) + 40);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
