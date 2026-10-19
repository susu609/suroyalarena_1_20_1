package net.susu609.royalarena.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.client.camera.ArenaCameraController;
import net.susu609.royalarena.client.input.RoyalArenaKeyMappings;
import net.susu609.royalarena.client.screen.SandboxCardScreen;
import net.susu609.royalarena.client.screen.SandboxTribeScreen;
import net.susu609.royalarena.client.state.ClientSandbox;
import net.susu609.royalarena.match.MatchSnapshot;
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
 * (C), and the tools. Clicks reach it through {@link #click} before the
 * arena gets them.
 */
public final class SandboxHud {
    private static final int LEFT = 6;
    private static final int TOP = 6;
    private static final int WIDTH = 120;
    private static final int ROW = 16;
    private static final int GAP = 2;
    private static final int BLUE = 0xFF1F4FD6;
    private static final int RED = 0xFFC81E1E;
    private static final int PLAIN = 0xFF3A3F4C;
    private static final int ON = 0xFF2E7D32;
    private static final int EXIT = 0xFF6A1B1B;
    private static final String[] SPEED_LABELS = {"||", "0.5x", "1x", "2x"};

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
        graphics.fill(LEFT - 3, TOP - 3, LEFT + WIDTH + 3, bottom + 18,
                0xC0101010);
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
    }

    /** Runs the button under the cursor; false when it missed the panel. */
    public static boolean click(double mx, double my) {
        MatchSnapshot.Sandbox box = ClientSandbox.settings();
        if (box == null)
            return false;
        for (Button b : buttons(box)) {
            if (b.contains(mx, my)) {
                b.action.run();
                return true;
            }
        }
        return false;
    }

    private static List<Button> buttons(MatchSnapshot.Sandbox box) {
        List<Button> out = new ArrayList<>();
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
