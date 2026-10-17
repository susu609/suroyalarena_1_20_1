package net.susu609.royalarena.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.susu609.royalarena.bot.BotDeck;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.card.CardRole;
import net.susu609.royalarena.card.DeckRules;
import net.susu609.royalarena.card.SpellStats;
import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.client.hud.CardFace;
import net.susu609.royalarena.client.state.ClientDeckState;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.c2s.C2SDeckRequestPacket;
import net.susu609.royalarena.network.c2s.C2SSaveDeckPacket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Deck editor: the 8 deck slots on top, the card library below.
 * <ul>
 * <li>Click a library card to add it to the first empty slot; with a full
 * deck it is picked up, then click the deck card it should replace.</li>
 * <li>Click a deck card (or its library card) to take it out.</li>
 * <li>Right-click drops a picked-up card.</li>
 * </ul>
 * Save sends the deck to the server, which checks it and stores it with the
 * world; the next match uses it (shuffled).
 */
public final class DeckScreen extends Screen {
    private static final int GAP = 6;
    private static final int TOP = 22;
    /** Summary, status and library label between deck and library. */
    private static final int TEXT_BAND = 44;
    private static final int GOLD = 0xFFFFD84A;
    private static final int HOVER = 0xFFB8C8E8;
    private static final int EMPTY = 0x80101620;
    private static final int IN_DECK = 0xA0303030;

    private final Screen parent;
    private final BattleCard[] slots = new BattleCard[BattleDeck.DECK_SIZE];
    private BattleCard held;
    private boolean dirty;
    private int seenVersion = -1;
    private String status = "Loading deck...";
    private int statusColor = 0xFFAAAAAA;

    private int cardW;
    private int cardH;
    private Button saveButton;

    public DeckScreen(Screen parent) {
        super(Component.literal("Deck"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Three rows of cards (deck + two library rows), the text between
        // them and the buttons must fit the height.
        int byHeight = (height - deckY() - TEXT_BAND - GAP - 32) / 3;
        int byWidth = (Math.min(width, 420) - 20) / 8 - GAP;
        cardW = Math.max(16, Math.min(byWidth, byHeight * 31 / 39));
        cardH = Math.round(cardW * 39 / 31.0F);

        int y = height - 26;
        int w = 70;
        int x = width / 2 - (4 * w + 3 * 4) / 2;
        saveButton = addRenderableWidget(Button.builder(
                Component.literal("Save"), b -> save())
                .bounds(x, y, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Random"),
                b -> fill(BotDeck.random(new Random()).cards()))
                .bounds(x + (w + 4), y, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Starter"),
                b -> fill(BattleDeck.starter().cards()))
                .bounds(x + 2 * (w + 4), y, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"),
                b -> onClose())
                .bounds(x + 3 * (w + 4), y, w, 20).build());

        if (seenVersion < 0) {
            ModNetwork.sendToServer(new C2SDeckRequestPacket());
            if (!ClientDeckState.deck().isEmpty())
                load(ClientDeckState.deck());
        }
        updateButtons();
    }

    @Override
    public void tick() {
        if (seenVersion != ClientDeckState.version()) {
            seenVersion = ClientDeckState.version();
            if (!ClientDeckState.deck().isEmpty())
                load(ClientDeckState.deck());
            if (!ClientDeckState.message().isEmpty()) {
                status = ClientDeckState.message();
                statusColor = ClientDeckState.ok() ? 0xFF7CFC7C : 0xFFFF7070;
            } else {
                status = "";
            }
        }
        updateButtons();
    }

    private void load(List<BattleCard> cards) {
        Arrays.fill(slots, null);
        for (int i = 0; i < cards.size() && i < slots.length; i++)
            slots[i] = cards.get(i);
        held = null;
        dirty = false;
    }

    private void fill(List<BattleCard> cards) {
        load(cards);
        dirty = true;
        status = "";
    }

    private void save() {
        List<BattleCard> cards = deck();
        if (DeckRules.problem(cards) != null)
            return;
        ModNetwork.sendToServer(new C2SSaveDeckPacket(DeckRules.ids(cards)));
        status = "Saving...";
        statusColor = 0xFFAAAAAA;
    }

    private List<BattleCard> deck() {
        return new ArrayList<>(Arrays.asList(slots));
    }

    private void updateButtons() {
        if (saveButton != null)
            saveButton.active = dirty && DeckRules.problem(deck()) == null;
    }

    // ------------------------------------------------------------ layout

    private int rowX(int count) {
        return width / 2 - (count * cardW + (count - 1) * GAP) / 2;
    }

    private int deckX(int slot) {
        return rowX(BattleDeck.DECK_SIZE) + slot * (cardW + GAP);
    }

    private int deckY() {
        return TOP + 4;
    }

    private int libraryTop() {
        return deckY() + cardH + TEXT_BAND;
    }

    private int libraryX(int index) {
        int perRow = BattleDeck.DECK_SIZE;
        int row = index / perRow;
        int inRow = Math.min(perRow, BattleCard.values().length - row * perRow);
        return rowX(inRow) + (index % perRow) * (cardW + GAP);
    }

    private int libraryY(int index) {
        return libraryTop() + (index / BattleDeck.DECK_SIZE) * (cardH + GAP);
    }

    private int deckSlotAt(double mx, double my) {
        for (int i = 0; i < slots.length; i++) {
            if (inside(mx, my, deckX(i), deckY()))
                return i;
        }
        return -1;
    }

    private BattleCard libraryCardAt(double mx, double my) {
        BattleCard[] all = BattleCard.values();
        for (int i = 0; i < all.length; i++) {
            if (inside(mx, my, libraryX(i), libraryY(i)))
                return all[i];
        }
        return null;
    }

    private boolean inside(double mx, double my, int x, int y) {
        return mx >= x && mx < x + cardW && my >= y && my < y + cardH;
    }

    private int indexInDeck(BattleCard card) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == card)
                return i;
        }
        return -1;
    }

    // ------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button))
            return true;
        if (button == 1) {
            held = null;
            return true;
        }
        if (button != 0)
            return false;
        int slot = deckSlotAt(mx, my);
        if (slot >= 0) {
            if (held != null) {
                slots[slot] = held;
                held = null;
            } else {
                slots[slot] = null;
            }
            changed();
            return true;
        }
        BattleCard card = libraryCardAt(mx, my);
        if (card == null)
            return false;
        int present = indexInDeck(card);
        if (present >= 0) {
            slots[present] = null;
            held = null;
            changed();
            return true;
        }
        int empty = indexInDeck(null);
        if (empty >= 0) {
            slots[empty] = card;
            held = null;
            changed();
        } else {
            held = held == card ? null : card;
            status = held == null ? "" : "Click the deck card to replace";
            statusColor = 0xFFFFD84A;
        }
        return true;
    }

    private void changed() {
        dirty = true;
        status = "";
        updateButtons();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick)
    {
        renderBackground(graphics);
        graphics.drawCenteredString(font, "Deck", width / 2, 8, 0xFFFFFFFF);

        BattleCard hovered = null;
        for (int i = 0; i < slots.length; i++) {
            int x = deckX(i);
            int y = deckY();
            boolean over = inside(mouseX, mouseY, x, y);
            if (slots[i] == null) {
                graphics.fill(x, y, x + cardW, y + cardH,
                        over && held != null ? 0x80FFD84A : EMPTY);
                graphics.renderOutline(x, y, cardW, cardH, 0xFF5A6478);
                graphics.drawCenteredString(font, "+", x + cardW / 2,
                        y + cardH / 2 - 4, 0xFF8A94A8);
                continue;
            }
            drawCard(graphics, slots[i], x, y,
                    over ? (held != null ? GOLD : HOVER) : CardFace.EDGE);
            if (over)
                hovered = slots[i];
        }

        List<BattleCard> cards = deck();
        long count = cards.stream().filter(c -> c != null).count();
        String summary = String.format(Locale.ROOT,
                "%d/8 cards   Average elixir: %.1f", count,
                DeckRules.averageElixir(cards));
        graphics.drawCenteredString(font, summary, width / 2,
                deckY() + cardH + 6, 0xFFDDDDDD);
        if (!status.isEmpty())
            graphics.drawCenteredString(font, status, width / 2,
                    deckY() + cardH + 18, statusColor);

        graphics.drawString(font, "Card library", rowX(BattleDeck.DECK_SIZE),
                libraryTop() - 11, 0xFFCCCCCC);
        BattleCard[] all = BattleCard.values();
        for (int i = 0; i < all.length; i++) {
            int x = libraryX(i);
            int y = libraryY(i);
            boolean over = inside(mouseX, mouseY, x, y);
            drawCard(graphics, all[i], x, y,
                    all[i] == held ? GOLD : over ? HOVER : CardFace.EDGE);
            if (indexInDeck(all[i]) >= 0) {
                graphics.pose().pushPose();
                graphics.pose().translate(0.0F, 0.0F, 200.0F);
                graphics.fill(x, y, x + cardW, y + cardH, IN_DECK);
                graphics.drawCenteredString(font, "In deck", x + cardW / 2,
                        y + cardH - 11, 0xFFFFFFFF);
                graphics.pose().popPose();
            }
            if (over)
                hovered = all[i];
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        if (hovered != null)
            graphics.renderComponentTooltip(font, describe(hovered), mouseX,
                    mouseY);
    }

    private void drawCard(GuiGraphics graphics, BattleCard card, int x, int y,
            int edgeColor)
    {
        int edge = Math.max(1, cardW / 20);
        CardFace.draw(graphics, card, x, y, cardW, cardH, edgeColor, edge);
        int badge = Math.max(9, Math.round(cardW * 11 / 31.0F));
        int overhang = Math.round(cardW * 2.5F / 31.0F);
        CardFace.costBadge(graphics, font, card.cost(), x, y, badge, overhang,
                true);
    }

    /** Tooltip lines: name, cost, roles and the numbers that matter. */
    private static List<Component> describe(BattleCard card) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(card.displayName())
                .withStyle(ChatFormatting.YELLOW));
        String roles = CardRole.of(card).stream().map(CardRole::label)
                .collect(Collectors.joining(", "));
        lines.add(Component.literal(card.cost() + " elixir - " + roles)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        if (card.isSpell()) {
            SpellStats s = card.spell();
            lines.add(line("Radius %.1f blocks, lands after %.1f s",
                    s.radius(), s.castTicks() / 20.0D));
            String effect = s.heal() ? "healing" : "damage";
            if (s.pulses() > 1)
                lines.add(line("%.0f " + effect + " over %.1f s (%.0f every " +
                                "%.2f s)", s.damagePerPulse() * s.pulses(),
                        s.pulses() * s.pulseInterval() / 20.0D,
                        s.damagePerPulse(), s.pulseInterval() / 20.0D));
            else
                lines.add(line("%.0f " + effect, s.damagePerPulse()));
            if (s.heal())
                lines.add(line("Heals your own troops, never towers"));
            if (s.maxTargets() > 0)
                lines.add(line("Strikes the %d targets with the most health",
                        s.maxTargets()));
            if (s.stunTicks() > 0)
                lines.add(line("Stuns for %.1f s", s.stunTicks() / 20.0D));
            if (s.freezeTicks() > 0)
                lines.add(line("Freezes troops and towers for %.0f s",
                        s.freezeTicks() / 20.0D));
            if (s.knockback() > 0)
                lines.add(line("Knocks back %.1f block", s.knockback()));
            if (s.burnTicks() > 0)
                lines.add(line("Burns %.0f/s for %.0f s", s.burnPerSecond(),
                        s.burnTicks() / 20.0D));
            if (!s.heal())
                lines.add(line("%.0f%% damage to towers",
                        s.towerMultiplier() * 100));
            return lines;
        }
        TroopStats s = card.stats();
        lines.add(line(s.count() > 1 ? "%.0f health x%d" : "%.0f health",
                s.health(), s.count()));
        lines.add(line("%.0f damage every %.1f s", s.damage(),
                s.attackTicks() / 20.0D));
        lines.add(line("Range %.1f, speed %.2f blocks/s", s.range(),
                s.speed()));
        if (s.split() != null)
            lines.add(line(s.split().generations() == 1 ?
                            "Splits in %d when it dies" :
                            "Splits in %d when it dies, %d times",
                    s.split().children(), s.split().generations()));
        if (s.traits().splash())
            lines.add(line("Hits everything within %.1f blocks of its target",
                    s.traits().splashRadius()));
        if (s.traits().selfDestruct())
            lines.add(line("Explodes on its first hit and is gone"));
        if (s.traits().knockback() > 0)
            lines.add(line("Knocks its target back %.1f block",
                    s.traits().knockback()));
        if (s.traits().buildingsOnly())
            lines.add(line("Ignores troops, walks straight to the towers"));
        return lines;
    }

    private static Component line(String format, Object... args) {
        return Component.literal(String.format(Locale.ROOT, format, args))
                .withStyle(ChatFormatting.GRAY);
    }
}
