package net.susu609.royalarena.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.Tribe;
import net.susu609.royalarena.client.hud.CardFace;
import net.susu609.royalarena.client.state.ClientSandbox;
import net.susu609.royalarena.match.TeamSide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The sandbox card picker (docs/SANDBOX.md): every card in a grid, with a
 * name search, a troops / spells filter and a tribe filter. The grid
 * scrolls (mouse wheel) and draws smaller cards on a short screen. A click
 * picks the card for the side shown on the panel and closes the picker
 * (Shift keeps it open); a right click pins the card to the quick bar. The
 * tooltip has the deck screen's numbers.
 */
public final class SandboxCardScreen extends Screen {
    /** Card sizes: normal, and small for a short screen. */
    private static final int[] CARD_W = {30, 24};
    private static final int[] CARD_H = {38, 30};
    private static final int[] PITCH_X = {36, 28};
    private static final int[] PITCH_Y = {46, 36};
    private static final int[] MAX_COLUMNS = {12, 16};
    private static final int GRID_TOP = 60;
    private static final int GRID_BOTTOM_GAP = 8;
    private static final int SCROLL_STEP = 24;
    private static final String[] KINDS = {"All", "Troops", "Spells"};

    /** Kept between openings, like the search text. */
    private static int kind;
    private static int tribe = -1;
    private static String query = "";

    private EditBox search;
    private Button kindButton;
    private Button tribeButton;
    private final List<BattleCard> shown = new ArrayList<>();
    private int gridLeft;
    private int gridTop;
    private int columns;
    /** 0 normal cards, 1 small. */
    private int size;
    private int scroll;

    public SandboxCardScreen() {
        super(Component.literal("Sandbox cards"));
    }

    @Override protected void init() {
        int top = 28;
        search = new EditBox(font, width / 2 - 150, top, 140, 18,
                Component.literal("Search"));
        search.setValue(query);
        search.setResponder(text -> {
            query = text;
            refresh();
        });
        addRenderableWidget(search);
        setInitialFocus(search);
        kindButton = addRenderableWidget(Button.builder(
                Component.literal(KINDS[kind]), b -> {
                    kind = (kind + 1) % KINDS.length;
                    refresh();
                }).bounds(width / 2 - 4, top - 1, 70, 20).build());
        tribeButton = addRenderableWidget(Button.builder(
                Component.literal(tribeLabel()), b -> {
                    tribe = tribe + 1 >= Tribe.values().length ? -1 : tribe + 1;
                    refresh();
                }).bounds(width / 2 + 72, top - 1, 80, 20).build());
        refresh();
    }

    private static String tribeLabel() {
        return tribe < 0 ? "All tribes" : Tribe.values()[tribe].label();
    }

    private void refresh() {
        shown.clear();
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (BattleCard card : BattleCard.values()) {
            if (kind == 1 && card.isSpell() || kind == 2 && !card.isSpell())
                continue;
            if (tribe >= 0 && !card.tribes().contains(Tribe.values()[tribe]))
                continue;
            if (!q.isEmpty() && !card.displayName().toLowerCase(Locale.ROOT)
                    .contains(q) && !card.id().contains(q))
                continue;
            shown.add(card);
        }
        kindButton.setMessage(Component.literal(KINDS[kind]));
        tribeButton.setMessage(Component.literal(tribeLabel()));
        gridTop = GRID_TOP;
        // Small cards when the normal ones would need more than a screen
        // and a half of scrolling.
        size = 0;
        if (contentHeight() > viewHeight() * 3 / 2)
            size = 1;
        columns = Math.max(1, Math.min(MAX_COLUMNS[size],
                (width - 40) / PITCH_X[size]));
        gridLeft = (width - columns * PITCH_X[size]) / 2 +
                (PITCH_X[size] - CARD_W[size]) / 2;
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int viewHeight() {
        return Math.max(PITCH_Y[1], height - gridTop - GRID_BOTTOM_GAP);
    }

    private int contentHeight() {
        int cols = Math.max(1, Math.min(MAX_COLUMNS[size],
                (width - 40) / PITCH_X[size]));
        int rows = (shown.size() + cols - 1) / cols;
        return rows * PITCH_Y[size];
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - viewHeight());
    }

    private int cardX(int i) {
        return gridLeft + (i % columns) * PITCH_X[size];
    }

    private int cardY(int i) {
        return gridTop + (i / columns) * PITCH_Y[size] - scroll;
    }

    /** The card under the mouse (only inside the visible grid), or null. */
    private BattleCard cardAt(double mx, double my) {
        if (my < gridTop || my >= gridTop + viewHeight())
            return null;
        for (int i = 0; i < shown.size(); i++) {
            int x = cardX(i);
            int y = cardY(i);
            if (mx >= x && mx < x + CARD_W[size] && my >= y &&
                    my < y + CARD_H[size])
                return shown.get(i);
        }
        return null;
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        BattleCard card = cardAt(mx, my);
        if (card != null && button == 0) {
            ClientSandbox.select(card);
            // Shift: pick and keep looking.
            if (!hasShiftDown())
                onClose();
            return true;
        }
        if (card != null && button == 1) {
            ClientSandbox.togglePin(card);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        int before = scroll;
        scroll = Math.max(0, Math.min(maxScroll(),
                scroll - (int) Math.round(delta * SCROLL_STEP)));
        return scroll != before || super.mouseScrolled(mx, my, delta);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick)
    {
        renderBackground(graphics);
        TeamSide side = ClientSandbox.side();
        graphics.drawCenteredString(font, "Pick a card for " + side.name() +
                        " (Tab on the panel switches side)", width / 2, 10,
                side == TeamSide.BLUE ? 0xFF6FA0FF : 0xFFFF7070);
        graphics.drawCenteredString(font, "Click: pick  Shift: stay open  " +
                        "Right click: pin (keys 1-8)  Wheel: scroll", width / 2,
                48, 0xFF9AA4B8);
        BattleCard hovered = cardAt(mouseX, mouseY);
        int view = viewHeight();
        graphics.enableScissor(0, gridTop, width, gridTop + view);
        for (int i = 0; i < shown.size(); i++) {
            BattleCard card = shown.get(i);
            int x = cardX(i);
            int y = cardY(i);
            if (y + CARD_H[size] < gridTop || y > gridTop + view)
                continue;
            boolean picked = card == ClientSandbox.card();
            int edge = picked ? 0xFFFFD84A : card == hovered ? 0xFFB8C8E8 :
                    CardFace.EDGE;
            CardFace.draw(graphics, card, x, y, CARD_W[size], CARD_H[size],
                    edge, 2);
            CardFace.costBadge(graphics, font, card.cost(), x, y,
                    size == 0 ? 10 : 8, 2, true);
            // A pinned card has a blue corner, as on the quick bar.
            if (ClientSandbox.quickBar().pinned(card))
                graphics.fill(x + CARD_W[size] - 6, y + 1, x + CARD_W[size] - 1,
                        y + 6, 0xFF7FD4FF);
        }
        graphics.disableScissor();
        if (maxScroll() > 0) {
            // The scroll bar right of the grid.
            int barX = gridLeft + columns * PITCH_X[size] + 2;
            int thumb = Math.max(12, view * view / contentHeight());
            int thumbY = gridTop + (view - thumb) * scroll / maxScroll();
            graphics.fill(barX, gridTop, barX + 3, gridTop + view, 0x60FFFFFF);
            graphics.fill(barX, thumbY, barX + 3, thumbY + thumb, 0xFFD0D8E8);
        }
        if (shown.isEmpty())
            graphics.drawCenteredString(font, "No card matches", width / 2,
                    gridTop + 10, 0xFF999999);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hovered != null) {
            // The deck screen's numbers, abilities and tribes.
            List<Component> lines = new ArrayList<>(DeckScreen.describe(hovered));
            lines.add(Component.literal(ClientSandbox.quickBar().pinned(hovered) ?
                    "Pinned (right click to unpin)" :
                    "Right click: pin to the quick bar")
                    .withStyle(ChatFormatting.DARK_GRAY));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    @Override public boolean isPauseScreen() {
        return false;
    }
}
