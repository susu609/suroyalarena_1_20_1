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

    private void rebuildButtons() {
        clearWidgets();
        boolean searching = ClientQueueState.searching();
        Component label =
                Component.literal(searching ? "Cancel Search" : "Find Match");
        addRenderableWidget(Button.builder(
                label, button -> {
                    if (ClientQueueState.searching())
                        ModNetwork.sendToServer(new C2SLeaveQueuePacket());
                    else
                        ModNetwork.sendToServer(new C2SJoinQueuePacket());
                }).bounds(width / 2 - 70, height / 2 + 28, 140, 20).build());
        addRenderableWidget(Button.builder(
                Component.literal("Practice Match"),
                button -> ModNetwork.sendToServer(new C2SStartPracticePacket()))
                .bounds(width / 2 - 70, height / 2 + 52, 140, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick)
    {
        renderBackground(graphics);
        graphics.drawCenteredString(
                font, title, width / 2, height / 2 - 55, 0xFFFFFFFF);
        String status = ClientQueueState.message();
        if (ClientQueueState.searching())
            status += "  #" + ClientQueueState.position();
        graphics.drawCenteredString(
                font, status, width / 2, height / 2 - 18, 0xFFCCCCCC);
        graphics.drawCenteredString(
                font, "Practice: play BLUE against an empty RED side",
                width / 2, height / 2 + 78, 0xFF999999);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() {
        return false;
    }
}
