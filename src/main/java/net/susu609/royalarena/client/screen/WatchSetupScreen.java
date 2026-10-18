package net.susu609.royalarena.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.susu609.royalarena.bot.BotPersonality;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.match.WatchSetup;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.c2s.C2SStartPracticePacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Before watching bots: each bot's deck (random, your saved deck or the
 * starter deck) and personality (random or one of the four), RED on the
 * left and BLUE on the right as in the match.
 */
public final class WatchSetupScreen extends Screen {
    private static final int BLUE = 0xFF5A8CFF;
    private static final int RED = 0xFFFF5A50;
    /** Kept between visits while the game runs. */
    private static WatchSetup last = WatchSetup.DEFAULT;

    private final Screen parent;
    private String blueDeck = last.blueDeck();
    private String redDeck = last.redDeck();
    private String blueStyle = last.blueStyle();
    private String redStyle = last.redStyle();

    public WatchSetupScreen(Screen parent) {
        super(Component.literal("Watch Bot vs Bot"));
        this.parent = parent;
    }

    @Override protected void init() {
        clearWidgets();
        int column = 130;
        int leftX = width / 2 - column - 10;
        int rightX = width / 2 + 10;
        int top = height / 2 - 30;
        addRenderableWidget(Button.builder(deckText(redDeck), b -> {
            redDeck = next(WatchSetup.DECKS, redDeck);
            init();
        }).bounds(leftX, top, column, 20).build());
        addRenderableWidget(Button.builder(styleText(redStyle), b -> {
            redStyle = next(styles(), redStyle);
            init();
        }).bounds(leftX, top + 24, column, 20).build());
        addRenderableWidget(Button.builder(deckText(blueDeck), b -> {
            blueDeck = next(WatchSetup.DECKS, blueDeck);
            init();
        }).bounds(rightX, top, column, 20).build());
        addRenderableWidget(Button.builder(styleText(blueStyle), b -> {
            blueStyle = next(styles(), blueStyle);
            init();
        }).bounds(rightX, top + 24, column, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Start"), b -> {
            last = new WatchSetup(blueDeck, redDeck, blueStyle, redStyle);
            ModNetwork.sendToServer(new C2SStartPracticePacket(last));
        }).bounds(width / 2 - 70, top + 62, 140, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"),
                b -> minecraft.setScreen(parent))
                .bounds(width / 2 - 70, top + 86, 140, 20).build());
    }

    @Override public void tick() {
        if (ClientMatchState.active())
            minecraft.setScreen(null);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick)
    {
        renderBackground(graphics);
        int column = 130;
        int top = height / 2 - 30;
        graphics.drawCenteredString(font, title, width / 2, top - 40,
                0xFFFFFFFF);
        graphics.drawCenteredString(font, "RED bot", width / 2 - column / 2 -
                10, top - 14, RED);
        graphics.drawCenteredString(font, "BLUE bot", width / 2 + column / 2 +
                10, top - 14, BLUE);
        graphics.drawCenteredString(font,
                "\"My deck\" is the deck you saved in the Deck screen",
                width / 2, top + 112, 0xFF999999);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() {
        return false;
    }

    private static List<String> styles() {
        List<String> ids = new ArrayList<>();
        ids.add(WatchSetup.RANDOM);
        for (BotPersonality p : BotPersonality.values())
            ids.add(p.id());
        return ids;
    }

    private static String next(List<String> options, String current) {
        int i = options.indexOf(current);
        return options.get((i + 1) % options.size());
    }

    private static Component deckText(String id) {
        return Component.literal("Deck: " + WatchSetup.deckLabel(id));
    }

    private static Component styleText(String id) {
        BotPersonality p = BotPersonality.byId(id);
        return Component.literal("Style: " +
                (p == null ? "Random" : p.displayName()));
    }
}
