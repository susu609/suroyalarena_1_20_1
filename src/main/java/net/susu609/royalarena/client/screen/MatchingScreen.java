package net.susu609.royalarena.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.client.state.ClientQueueState;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.c2s.C2SJoinQueuePacket;
import net.susu609.royalarena.network.c2s.C2SLeaveQueuePacket;
import net.susu609.royalarena.network.c2s.C2SSandboxPacket;
import net.susu609.royalarena.network.c2s.C2SStartPracticePacket;

public final class MatchingScreen extends Screen {
    private boolean previousSearching;

    public MatchingScreen() {
        super(Component.literal("Su Royal Arena"));
    }

    @Override protected void init() {
        previousSearching = ClientQueueState.searching();
        rebuildButtons();
    }

    @Override public void tick() {
        if (ClientMatchState.active()) {
            minecraft.setScreen(null);
            return;
        }
        if (previousSearching != ClientQueueState.searching()) {
            previousSearching = ClientQueueState.searching();
            rebuildButtons();
        }
    }

    private static final int BUTTON_H = 20;
    private static final int BIG_W = 164;
    private static final int HALF_W = 80;
    private static final int GAP = 4;
    /** Title, status and the hint under the buttons. */
    private static final int TEXT_ABOVE = 30;
    private static final int TEXT_BELOW = 14;

    /** Top of the whole block (title to hint), centred but never off screen. */
    private int blockTop() {
        int total = TEXT_ABOVE + BUTTON_H + GAP + 2 * (BUTTON_H + GAP) +
                TEXT_BELOW;
        return Math.max(4, (height - total) / 2);
    }

    /**
     * Find Match on its own line, then Play vs Bot / Watch and Sandbox / Deck
     * in two columns: the block is 5 lines high, so it fits any screen the
     * game can be played on and nothing scrolls.
     */
    private void rebuildButtons() {
        clearWidgets();
        boolean searching = ClientQueueState.searching();
        int top = blockTop() + TEXT_ABOVE;
        int left = width / 2 - BIG_W / 2;
        addRenderableWidget(Button.builder(
                Component.literal(searching ? "Cancel Search" : "Find Match"),
                button -> {
                    if (ClientQueueState.searching())
                        ModNetwork.sendToServer(new C2SLeaveQueuePacket());
                    else
                        ModNetwork.sendToServer(new C2SJoinQueuePacket());
                }).bounds(left, top, BIG_W, BUTTON_H).build());
        int row2 = top + BUTTON_H + GAP;
        int row3 = row2 + BUTTON_H + GAP;
        int right = left + HALF_W + GAP;
        addRenderableWidget(Button.builder(
                Component.literal("Play vs Bot"),
                button -> ModNetwork.sendToServer(new C2SStartPracticePacket()))
                .bounds(left, row2, HALF_W, BUTTON_H).build());
        addRenderableWidget(Button.builder(
                Component.literal("Watch Bots"),
                button -> minecraft.setScreen(new WatchSetupScreen(this)))
                .bounds(right, row2, HALF_W, BUTTON_H).build());
        addRenderableWidget(Button.builder(
                Component.literal("Sandbox"),
                button -> ModNetwork.sendToServer(
                        C2SSandboxPacket.of(C2SSandboxPacket.Action.START)))
                .bounds(left, row3, HALF_W, BUTTON_H).build());
        addRenderableWidget(Button.builder(
                Component.literal("Deck"),
                button -> minecraft.setScreen(new DeckScreen(this)))
                .bounds(right, row3, HALF_W, BUTTON_H).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick)
    {
        renderBackground(graphics);
        int top = blockTop();
        graphics.drawCenteredString(font, title, width / 2, top, 0xFFFFFFFF);
        String status = ClientQueueState.message();
        if (ClientQueueState.searching())
            status += "  #" + ClientQueueState.position();
        graphics.drawCenteredString(font, status, width / 2, top + 14,
                0xFFCCCCCC);
        int below = top + TEXT_ABOVE + 3 * (BUTTON_H + GAP) + 2;
        graphics.drawCenteredString(font, "Solo: you play BLUE against a bot",
                width / 2, below, 0xFF999999);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() {
        return false;
    }
}
