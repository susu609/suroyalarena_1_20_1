package net.susu609.royalarena.client.screen;

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
 * name search, a troops / spells filter and a tribe filter. Clicking a card
 * picks it for the side shown on the panel and closes the picker.
 */
public final class SandboxCardScreen extends Screen {
    private static final int CARD_W = 30;
    private static final int CARD_H = 38;
    private static final int PITCH_X = 36;
    private static final int PITCH_Y = 46;
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
        columns = Math.max(1, Math.min(12, (width - 40) / PITCH_X));
        gridLeft = (width - columns * PITCH_X) / 2 + (PITCH_X - CARD_W) / 2;
        gridTop = 60;
    }

    /** The card under the mouse, or null. */
    private BattleCard cardAt(double mx, double my) {
        for (int i = 0; i < shown.size(); i++) {
            int x = gridLeft + (i % columns) * PITCH_X;
            int y = gridTop + (i / columns) * PITCH_Y;
            if (mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H)
                return shown.get(i);
        }
        return null;
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        BattleCard card = cardAt(mx, my);
        if (card != null && button == 0) {
            ClientSandbox.select(card);
            onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick)
    {
        renderBackground(graphics);
        TeamSide side = ClientSandbox.side();
        graphics.drawCenteredString(font, "Pick a card for " + side.name() +
                        " (Tab on the panel switches side)", width / 2, 10,
                side == TeamSide.BLUE ? 0xFF6FA0FF : 0xFFFF7070);
        BattleCard hovered = cardAt(mouseX, mouseY);
        for (int i = 0; i < shown.size(); i++) {
            BattleCard card = shown.get(i);
            int x = gridLeft + (i % columns) * PITCH_X;
            int y = gridTop + (i / columns) * PITCH_Y;
            boolean picked = card == ClientSandbox.card();
            int edge = picked ? 0xFFFFD84A : card == hovered ? 0xFFB8C8E8 :
                    CardFace.EDGE;
            CardFace.draw(graphics, card, x, y, CARD_W, CARD_H, edge, 2);
            CardFace.costBadge(graphics, font, card.cost(), x, y, 10, 2, true);
        }
        if (shown.isEmpty())
            graphics.drawCenteredString(font, "No card matches", width / 2,
                    gridTop + 10, 0xFF999999);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hovered != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(hovered.displayName() + " - " +
                    hovered.cost() + " elixir"));
            if (!hovered.tribes().isEmpty())
                lines.add(Component.literal(hovered.tribes().stream()
                        .map(Tribe::label).reduce((a, b) -> a + ", " + b)
                        .orElse("")));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    @Override public boolean isPauseScreen() {
        return false;
    }
}
