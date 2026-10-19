package net.susu609.royalarena.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.susu609.royalarena.bot.BotDeck;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.card.Body;
import net.susu609.royalarena.card.CardFilter;
import net.susu609.royalarena.card.CardRole;
import net.susu609.royalarena.card.CardTactic;
import net.susu609.royalarena.card.DeckRules;
import net.susu609.royalarena.card.SpellStats;
import net.susu609.royalarena.card.Tribe;
import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.client.hud.CardFace;
import net.susu609.royalarena.client.sound.ClientSounds;
import net.susu609.royalarena.match.ArenaSounds;
import net.susu609.royalarena.client.state.ClientDeckState;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.c2s.C2SDeckRequestPacket;
import net.susu609.royalarena.network.c2s.C2SSaveDeckPacket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * <li>Tabs (All, Troops, Spells, Buildings) and filter buttons (Attack,
 * Defense, Support from the card JSON; Tank, Swarm, Ranged, Area from the
 * stats; all pressed ones must match) choose what the library shows, 16 cards
 * a page: the arrows or the mouse wheel turn the page.</li>
 * </ul>
 * Save sends the deck to the server, which checks it and stores it with the
 * world; the next match uses it (shuffled).
 */
public final class DeckScreen extends Screen {
    private static final int GAP = 6;
    private static final int TOP = 22;
    /** Summary and status under the deck. */
    private static final int TEXT_BAND = 30;
    /** Two rows of tab and filter buttons. */
    private static final int CONTROLS = 30;
    /** The "Card library" line with the page arrows. */
    private static final int LABEL = 13;
    private static final int CONTROL_H = 13;
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

    private CardFilter.Tab tab = CardFilter.Tab.ALL;
    private final Set<CardFilter.Chip> chips =
            EnumSet.noneOf(CardFilter.Chip.class);
    private boolean byElixir;
    private int page;
    private final Map<CardFilter.Tab, Button> tabButtons =
            new EnumMap<>(CardFilter.Tab.class);
    private final Map<CardFilter.Chip, Button> chipButtons =
            new EnumMap<>(CardFilter.Chip.class);
    private Button sortButton;

    public DeckScreen(Screen parent) {
        super(Component.literal("Deck"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Three rows of cards (deck + two library rows), the text and
        // filter buttons between them and the buttons must fit the height.
        int byHeight = (height - deckY() - TEXT_BAND - CONTROLS - LABEL -
                GAP - 32) / 3;
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
        addFilterButtons();

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
                ClientSounds.ui(ClientDeckState.ok() ? ArenaSounds.DECK_SAVED :
                        ArenaSounds.CARD_REFUSED);
            } else {
                status = "";
            }
        }
        updateButtons();
    }

    /** Tabs and sort on the first row, the filter chips on the second. */
    private void addFilterButtons() {
        tabButtons.clear();
        chipButtons.clear();
        int top = controlsTop();
        int tabW = 50;
        int sortW = 72;
        CardFilter.Tab[] tabs = CardFilter.Tab.values();
        int rowW = tabs.length * (tabW + 3) + sortW;
        int x = width / 2 - rowW / 2;
        for (CardFilter.Tab t : tabs) {
            tabButtons.put(t, addRenderableWidget(Button.builder(
                    Component.literal(t.label()), b -> {
                        tab = t;
                        page = 0;
                        refreshFilterButtons();
                    }).bounds(x, top, tabW, CONTROL_H).build()));
            x += tabW + 3;
        }
        sortButton = addRenderableWidget(Button.builder(Component.empty(),
                b -> {
                    byElixir = !byElixir;
                    page = 0;
                    refreshFilterButtons();
                }).bounds(x, top, sortW, CONTROL_H).build());

        CardFilter.Chip[] all = CardFilter.Chip.values();
        int chipW = 42;
        x = width / 2 - (all.length * (chipW + 3) - 3) / 2;
        for (CardFilter.Chip c : all) {
            chipButtons.put(c, addRenderableWidget(Button.builder(
                    Component.literal(c.label()), b -> {
                        if (!chips.remove(c))
                            chips.add(c);
                        page = 0;
                        refreshFilterButtons();
                    }).bounds(x, top + CONTROL_H + 3, chipW, CONTROL_H)
                    .build()));
            x += chipW + 3;
        }
        int arrows = rowX(BattleDeck.DECK_SIZE) +
                BattleDeck.DECK_SIZE * (cardW + GAP) - GAP;
        addRenderableWidget(Button.builder(Component.literal(">"),
                b -> turnPage(1)).bounds(arrows - 14, libraryTop() - LABEL,
                14, CONTROL_H - 1).build());
        addRenderableWidget(Button.builder(Component.literal("<"),
                b -> turnPage(-1)).bounds(arrows - 70, libraryTop() - LABEL,
                14, CONTROL_H - 1).build());
        refreshFilterButtons();
    }

    /** Pressed tabs and chips are shown in yellow. */
    private void refreshFilterButtons() {
        tabButtons.forEach((t, b) -> b.setMessage(pressed(t.label(),
                t == tab)));
        chipButtons.forEach((c, b) -> b.setMessage(pressed(c.label(),
                chips.contains(c))));
        if (sortButton != null)
            sortButton.setMessage(Component.literal(byElixir ?
                    "Sort: elixir" : "Sort: default"));
    }

    private static Component pressed(String label, boolean on) {
        return on ? Component.literal(label).withStyle(ChatFormatting.YELLOW)
                : Component.literal(label);
    }

    private List<BattleCard> shown() {
        return CardFilter.shown(tab, chips, byElixir);
    }

    private List<BattleCard> pageCards() {
        return CardFilter.page(shown(), page);
    }

    private void turnPage(int by) {
        int pages = CardFilter.pages(shown().size());
        page = Math.max(0, Math.min(pages - 1, page + by));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (my >= libraryTop() - LABEL) {
            turnPage(delta < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
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

    private int controlsTop() {
        return deckY() + cardH + TEXT_BAND;
    }

    private int libraryTop() {
        return controlsTop() + CONTROLS + LABEL;
    }

    /** Where card {@code index} of the page goes; rows are left-aligned. */
    private int libraryX(int index) {
        return rowX(BattleDeck.DECK_SIZE) +
                (index % BattleDeck.DECK_SIZE) * (cardW + GAP);
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
        List<BattleCard> cards = pageCards();
        for (int i = 0; i < cards.size(); i++) {
            if (inside(mx, my, libraryX(i), libraryY(i)))
                return cards.get(i);
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
                ClientSounds.ui(ArenaSounds.DECK_PLACE);
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
            ClientSounds.ui(ArenaSounds.DECK_PLACE);
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
        String tribes = Tribe.summary(cards);
        if (!status.isEmpty())
            graphics.drawCenteredString(font, status, width / 2,
                    deckY() + cardH + 18, statusColor);
        else if (!tribes.isEmpty())
            graphics.drawCenteredString(font, tribes, width / 2,
                    deckY() + cardH + 18, 0xFFB8E07A);

        List<BattleCard> shown = shown();
        int pages = CardFilter.pages(shown.size());
        page = Math.min(page, pages - 1);
        graphics.drawString(font, String.format(Locale.ROOT,
                        "Card library  %d/%d cards", shown.size(),
                        BattleCard.values().length),
                rowX(BattleDeck.DECK_SIZE), libraryTop() - 11, 0xFFCCCCCC);
        int arrows = rowX(BattleDeck.DECK_SIZE) +
                BattleDeck.DECK_SIZE * (cardW + GAP) - GAP;
        graphics.drawCenteredString(font, (page + 1) + "/" + pages,
                arrows - 35, libraryTop() - 10, 0xFFCCCCCC);
        List<BattleCard> all = CardFilter.page(shown, page);
        if (all.isEmpty())
            graphics.drawCenteredString(font, tab == CardFilter.Tab.BUILDINGS ?
                            "No building cards yet" : "No card matches",
                    width / 2, libraryTop() + cardH / 2, 0xFF8A94A8);
        for (int i = 0; i < all.size(); i++) {
            BattleCard card = all.get(i);
            int x = libraryX(i);
            int y = libraryY(i);
            boolean over = inside(mouseX, mouseY, x, y);
            drawCard(graphics, card, x, y,
                    card == held ? GOLD : over ? HOVER : CardFace.EDGE);
            if (indexInDeck(card) >= 0) {
                graphics.pose().pushPose();
                graphics.pose().translate(0.0F, 0.0F, 200.0F);
                graphics.fill(x, y, x + cardW, y + cardH, IN_DECK);
                graphics.drawCenteredString(font, "In deck", x + cardW / 2,
                        y + cardH - 11, 0xFFFFFFFF);
                graphics.pose().popPose();
            }
            if (over)
                hovered = card;
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
        lines.add(Component.literal(card.tactics().stream()
                .map(CardTactic::label).collect(Collectors.joining(", ")))
                .withStyle(ChatFormatting.AQUA));
        if (card.isSpell()) {
            SpellStats s = card.spell();
            lines.add(line("Radius %.1f blocks, lands after %.1f s",
                    s.radius(), s.castTicks() / 20.0D));
            String effect = s.heal() ? "healing" : s.poison() ? "poison" :
                    "damage";
            if (s.damagePerPulse() <= 0.0D) {
                // Cobweb stays a while; Swiftness has no damage line.
                if (s.pulses() > 1)
                    lines.add(line("Stays %.0f s", s.pulses() *
                            s.pulseInterval() / 20.0D));
            } else if (s.pulses() > 1) {
                lines.add(line("%.0f " + effect + " over %.1f s (%.0f every " +
                                "%.2f s)", s.damagePerPulse() * s.pulses(),
                        s.pulses() * s.pulseInterval() / 20.0D,
                        s.damagePerPulse(), s.pulseInterval() / 20.0D));
            } else {
                lines.add(line("%.0f " + effect, s.damagePerPulse()));
            }
            if (s.heal())
                lines.add(line("Heals your own troops, never towers"));
            if (s.poison())
                lines.add(line("Poison: 1.5x on the living, none on golems"));
            if (s.slowTicks() > 0)
                lines.add(line("Slows enemies' walk 60%% and attack 50%% for %.1f s",
                        (s.durationTicks() + s.slowTicks()) / 20.0D));
            if (s.hasteTicks() > 0)
                lines.add(line("Your troops walk 50%% and attack 75%% faster " +
                        "for %.0f s", s.hasteTicks() / 20.0D));
            if (s.maxTargets() > 0)
                lines.add(line("Strikes the %d targets with the most health",
                        s.maxTargets()));
            if (s.stunTicks() > 0)
                lines.add(line("Stuns for %.1f s", s.stunTicks() / 20.0D));
            if (s.freezeTicks() > 0)
                lines.add(line("Freezes troops and towers for %.0f s",
                        s.freezeTicks() / 20.0D));
            if (s.middleReach())
                lines.add(line("Reaches only to the middle of the bridge"));
            if (s.knockback() > 0)
                lines.add(line("Knocks back %.1f block", s.knockback()));
            if (s.burnTicks() > 0)
                lines.add(line("Burns %.0f/s for %.0f s", s.burnPerSecond(),
                        s.burnTicks() / 20.0D));
            if (!s.friendly() && !s.poison() && s.damagePerPulse() > 0.0D)
                lines.add(line("%.0f%% damage to towers",
                        s.towerMultiplier() * 100));
            else if (s.poison())
                lines.add(line("Never hurts towers"));
            return lines;
        }
        TroopStats s = card.stats();
        lines.add(line("Tribe: %s", String.join(", ", card.tribes().stream()
                .map(Tribe::label).toList())));
        lines.add(line(s.count() > 1 ? "%.0f health x%d" : "%.0f health",
                s.health(), s.count()));
        lines.add(line("%.0f damage every %.1f s", s.damage(),
                s.attackTicks() / 20.0D));
        lines.add(line("Range %.1f, speed %.2f blocks/s", s.range(),
                s.speed()));
        if (s.split() != null && s.split().into() != null)
            lines.add(line("Leaves a Skeleton Archer when it dies"));
        else if (s.split() != null)
            lines.add(line(s.split().generations() == 1 ?
                            "Splits in %d when it dies" :
                            "Splits in %d when it dies, %d times",
                    s.split().children(), s.split().generations()));
        if (s.armor() > 0)
            lines.add(line("Armour %.0f: takes %.0f%% less from hits and spells " +
                            "(not poison or fire)",
                    s.armor(), (1.0D - s.armorFactor()) * 100.0D));
        if (s.traits().splash())
            lines.add(line("Hits everything within %.1f blocks of its target",
                    s.traits().splashRadius()));
        if (s.traits().selfDestruct())
            lines.add(line("Explodes on its first hit and is gone"));
        if (s.traits().knockback() > 0)
            lines.add(line("Knocks its target back %.1f block",
                    s.traits().knockback()));
        if (s.traits().slows()) {
            lines.add(line("Slows what it hits for %.0f s",
                    s.traits().slowTicks() / 20.0D));
            lines.add(line("Frost: double damage to frost-weak troops"));
        }
        if (s.traits().buildingsOnly())
            lines.add(line("Ignores troops, walks straight to the towers"));
        if (s.traits().poisons())
            lines.add(line("Poisons what it hits: %.1f a second for %.0f s " +
                            "(1.5x on the living, none on golems)",
                    s.traits().poisonPerSecond(), s.traits().poisonTicks() / 20.0D));
        if (card.body() != Body.LIVING)
            lines.add(line("%s: %s", card.body().label(),
                    card.body().poisonable() ? "poison does normal damage" :
                            "poison cannot touch it"));
        if (s.traits().teleports())
            lines.add(line("Teleports out of a ranged shot or a spell, every %.0f s",
                    s.traits().teleportCooldownTicks() / 20.0D));
        if (s.traits().hovers())
            lines.add(line("Floats above the ground"));
        if (s.traits().crossesRiver())
            lines.add(line("Crosses the river anywhere, not only the bridges"));
        if (s.traits().deployOnRiver())
            lines.add(line("Can also be played on the river"));
        if (s.traits().frostWeak())
            lines.add(line("Freezing spells hit it twice as hard and freeze it twice as long"));
        if (s.traits().waterWeak())
            lines.add(line("Takes 1.5 times the damage while on the river"));
        if (card.fireImmune())
            lines.add(line("Nether mob: never burns (Fireball still hurts)"));
        if (s.traits().frostImmune())
            lines.add(line("Never frozen or slowed; frost only hurts it"));
        if (s.traits().lines())
            lines.add(line("Fangs bite every enemy on a line toward its target"));
        if (s.traits().charges())
            lines.add(line("Locks on for %.0f s before hitting a new target",
                    s.traits().chargeTicks() / 20.0D));
        if (s.traits().riverStrong())
            lines.add(line("Hits 1.5 times as hard from the river"));
        if (s.traits().shots() > 1)
            lines.add(line("Fires %d quick shots, then reloads", s.traits().shots()));
        if (s.traits().leavesFireTrail())
            lines.add(line("Leaves a burning trail for %.0f s: enemies on it burn",
                    s.traits().fireTrailTicks() / 20.0D));
        return lines;
    }

    private static Component line(String format, Object... args) {
        return Component.literal(String.format(Locale.ROOT, format, args))
                .withStyle(ChatFormatting.GRAY);
    }
}
