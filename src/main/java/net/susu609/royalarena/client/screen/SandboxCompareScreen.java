package net.susu609.royalarena.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.c2s.C2SSandboxPacket;
import net.susu609.royalarena.sim.CardCompare;
import net.susu609.royalarena.sim.CardCompare.Pool;
import net.susu609.royalarena.sim.CardCompare.Report;
import net.susu609.royalarena.sim.CardCompare.Row;

import java.util.List;

/**
 * The sandbox two-card comparison (docs/COMPARE.md): choose card A and card
 * B, then Compare measures them with the combat engine (the same code as
 * {@code /royalarena compare}, run on this client; about half a second) and
 * lists a few rows and, per troop, how many cards it beats and loses to,
 * with the three it beats and loses to most. Show on arena plays the two
 * cards at the left bridge for the sandbox to watch.
 */
public final class SandboxCompareScreen extends Screen {
    private static final int LABEL_W = 210;
    private static final int VALUE_W = 78;
    private static final int LINE = 11;
    private static final int LEAD = 0xFFFFE680;
    private static final int PLAIN = 0xFFB8C0CC;

    /** Kept between openings. */
    private static BattleCard cardA = BattleCard.GIANT;
    private static BattleCard cardB = BattleCard.IRON_GOLEM;
    private static Report report;

    public SandboxCompareScreen() {
        super(Component.literal("Compare cards"));
    }

    @Override protected void init() {
        int top = 24;
        addRenderableWidget(Button.builder(Component.literal("A: " +
                cardA.displayName()), b -> pick(true))
                .bounds(width / 2 - 170, top, 110, 20).build());
        addRenderableWidget(Button.builder(Component.literal("B: " +
                cardB.displayName()), b -> pick(false))
                .bounds(width / 2 - 55, top, 110, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Swap"), b -> {
            BattleCard t = cardA;
            cardA = cardB;
            cardB = t;
            report = null;
            rebuildWidgets();
        }).bounds(width / 2 + 60, top, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Compare"),
                b -> report = CardCompare.compare(cardA, cardB))
                .bounds(width / 2 + 105, top, 65, 20).build());
        int bottom = height - 26;
        addRenderableWidget(Button.builder(Component.literal("Show on arena"),
                b -> showOnArena()).bounds(width / 2 - 110, bottom, 100, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Done"),
                b -> onClose()).bounds(width / 2 + 10, bottom, 100, 20)
                .build());
        if (report == null || report.a() != cardA || report.b() != cardB)
            report = null;
    }

    /** Chooses a card in the picker, then comes back here. */
    private void pick(boolean first) {
        Minecraft.getInstance().setScreen(new SandboxCardScreen(card -> {
            if (first)
                cardA = card;
            else
                cardB = card;
            report = null;
            Minecraft.getInstance().setScreen(new SandboxCompareScreen());
        }, "Pick card " + (first ? "A" : "B")));
    }

    /**
     * BLUE's card at the left bridge on BLUE's side, RED's on the other,
     * as in the bridge duel; a spell lands on the other card's spot.
     */
    private void showOnArena() {
        var arena = ClientMatchState.arena();
        if (arena == null)
            return;
        ArenaPoint bridge = arena.bridgeLeft();
        BlockPos blue = new BlockPos(bridge.x(), bridge.y(), bridge.z() + 3);
        BlockPos red = new BlockPos(bridge.x(), bridge.y(), bridge.z() - 4);
        ModNetwork.sendToServer(C2SSandboxPacket.deploy(cardA.id(),
                TeamSide.BLUE, cardA.isSpell() ? red : blue));
        ModNetwork.sendToServer(C2SSandboxPacket.deploy(cardB.id(),
                TeamSide.RED, cardB.isSpell() ? blue : red));
        onClose();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick)
    {
        renderBackground(graphics);
        graphics.drawCenteredString(font, "Compare two cards", width / 2, 8,
                0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (report == null) {
            graphics.drawCenteredString(font, "Pick A and B, then Compare",
                    width / 2, 60, 0xFF999999);
            return;
        }
        int left = width / 2 - (LABEL_W + 2 * VALUE_W) / 2;
        int y = 54;
        graphics.drawString(font, report.a().displayName() + " (" +
                report.a().cost() + ")", left + LABEL_W, y, 0xFF6FA0FF, true);
        graphics.drawString(font, report.b().displayName() + " (" +
                report.b().cost() + ")", left + LABEL_W + VALUE_W, y,
                0xFFFF7070, true);
        y += LINE + 2;
        for (Row row : report.rows()) {
            graphics.drawString(font, font.plainSubstrByWidth(row.label(),
                    LABEL_W - 4), left, y, PLAIN, true);
            graphics.drawString(font, row.a(), left + LABEL_W, y,
                    row.lead() == CardCompare.A ? LEAD : PLAIN, true);
            graphics.drawString(font, row.b(), left + LABEL_W + VALUE_W, y,
                    row.lead() == CardCompare.B ? LEAD : PLAIN, true);
            y += LINE;
        }
        y += 4;
        for (Pool pool : new Pool[] {report.poolA(), report.poolB()}) {
            if (pool == null)
                continue;
            graphics.drawString(font, pool.card().displayName() + " vs " +
                    pool.total() + " cards: beats " + pool.wins() +
                    ", loses to " + pool.losses() + ", draws " + pool.draws(),
                    left, y, pool.card() == report.a() ? 0xFF6FA0FF :
                            0xFFFF7070, true);
            y += LINE;
            y = names(graphics, left + 8, y, "beats most", pool.beats());
            y = names(graphics, left + 8, y, "loses most to", pool.losesTo());
        }
        y += 4;
        graphics.drawString(font, report.verdict(), left, y, 0xFF9CE29C, true);
    }

    private int names(GuiGraphics graphics, int x, int y, String label,
            List<BattleCard> cards)
    {
        if (cards.isEmpty())
            return y;
        graphics.drawString(font, label + ": " + String.join(", ",
                cards.stream().map(BattleCard::displayName).toList()), x, y,
                PLAIN, true);
        return y + LINE;
    }

    @Override public boolean isPauseScreen() {
        return false;
    }
}
