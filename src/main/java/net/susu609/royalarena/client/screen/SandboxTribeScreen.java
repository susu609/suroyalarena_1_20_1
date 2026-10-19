package net.susu609.royalarena.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.susu609.royalarena.card.Tribe;
import net.susu609.royalarena.client.state.ClientSandbox;
import net.susu609.royalarena.match.MatchSnapshot;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.c2s.C2SSandboxPacket;

/**
 * The sandbox tribe steps (docs/SANDBOX.md): for each tribe, the step BLUE
 * and RED play with; a click goes one step up, past the last back to 0.
 * Troops get their bonus when they appear, so it changes the next ones.
 */
public final class SandboxTribeScreen extends Screen {
    private static final int ROW = 22;

    public SandboxTribeScreen() {
        super(Component.literal("Sandbox tribes"));
    }

    @Override protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        MatchSnapshot.Sandbox box = ClientSandbox.settings();
        if (box == null) {
            onClose();
            return;
        }
        Tribe[] tribes = Tribe.values();
        int top = 40;
        for (int i = 0; i < tribes.length; i++) {
            Tribe tribe = tribes[i];
            int y = top + i * ROW;
            for (TeamSide side : TeamSide.values()) {
                int level = box.tribes(side)[i];
                int max = tribe.steps().size();
                int x = width / 2 + (side == TeamSide.BLUE ? 0 : 64);
                int index = i;
                addRenderableWidget(Button.builder(
                        Component.literal(side.name().charAt(0) + " " +
                                level + "/" + max), b -> {
                            ModNetwork.sendToServer(C2SSandboxPacket.tribe(side,
                                    index, level >= max ? 0 : level + 1));
                        }).bounds(x, y, 60, 20).build());
            }
        }
        addRenderableWidget(Button.builder(Component.literal("Done"),
                b -> onClose()).bounds(width / 2 - 40,
                top + tribes.length * ROW + 6, 80, 20).build());
    }

    private int lastHash;

    @Override public void tick() {
        // Rebuild when the server's answer changes the steps.
        MatchSnapshot.Sandbox box = ClientSandbox.settings();
        if (box == null) {
            onClose();
            return;
        }
        int hash = java.util.Arrays.hashCode(box.blueTribes()) * 31 +
                java.util.Arrays.hashCode(box.redTribes());
        if (hash != lastHash) {
            lastHash = hash;
            rebuild();
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick)
    {
        renderBackground(graphics);
        graphics.drawCenteredString(font,
                "Tribe steps (new troops get them)", width / 2, 14,
                0xFFFFFFFF);
        Tribe[] tribes = Tribe.values();
        for (int i = 0; i < tribes.length; i++) {
            graphics.drawString(font, tribes[i].label(), width / 2 - 110,
                    40 + i * ROW + 6, 0xFFE0E0E0, true);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() {
        return false;
    }
}
