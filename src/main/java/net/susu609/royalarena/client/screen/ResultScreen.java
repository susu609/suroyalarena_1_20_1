package net.susu609.royalarena.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.susu609.modlib.resource.RL;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.client.hud.CardFace;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.match.MatchResult;
import net.susu609.royalarena.match.ResultTimeline;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.c2s.C2SResultDonePacket;
import net.susu609.royalarena.network.s2c.S2CMatchResultPacket;

import java.util.Locale;
import java.util.UUID;

/**
 * The end of a match (docs/UI.md): over the dimmed arena, the opponent's
 * ribbon and crowns on top, yours below, VICTORY / DEFEAT / DRAW in between,
 * then your MVP card and the buttons. The crowns pop one by one
 * ({@link ResultTimeline}); a click or a key skips to the end.
 *
 * <p>Laid out on a 427 x 240 GUI-pixel block (854 x 480 at GUI scale 2),
 * centred. Vanilla textures: the Advancement challenge frame for the crown
 * slots, the golden helmet as the crown, the toast for the MVP line, the
 * player's face from their skin. Ours: the ribbons and the light rays.</p>
 */
public final class ResultScreen extends Screen {
    private static final int BLOCK_W = 427;
    private static final int BLOCK_H = 240;
    private static final ResourceLocation ADVANCEMENT_WIDGETS =
            RL.mc("textures/gui/advancements/widgets.png");
    private static final ResourceLocation TOASTS = RL.mc("textures/gui/toasts.png");
    private static final ResourceLocation ZOMBIE_SKIN =
            RL.mc("textures/entity/zombie/zombie.png");
    private static final ResourceLocation RIBBON_RED =
            RL.of(RoyalArena.MOD_ID, "textures/gui/result/ribbon_red.png");
    private static final ResourceLocation RIBBON_BLUE =
            RL.of(RoyalArena.MOD_ID, "textures/gui/result/ribbon_blue.png");
    private static final ResourceLocation RAYS =
            RL.of(RoyalArena.MOD_ID, "textures/gui/result/rays.png");
    private static final int RIBBON_W = 232;
    private static final int RIBBON_H = 30;
    private static final int FRAME = 26;
    private static final int FRAME_U = 26;
    private static final int FRAME_GOLD_V = 128;
    private static final int FRAME_GREY_V = 154;
    /** How far the ribbons slide in from. */
    private static final int SLIDE = 45;
    private static final ItemStack CROWN = new ItemStack(Items.GOLDEN_HELMET);

    private final MatchResult result;
    private final TeamSide local;
    private final String[] names;
    private final UUID[] players;
    private final ResultTimeline timeline;
    private int ticks;
    private boolean skipped;
    private float cueTime = -1.0F;
    private boolean sent;
    private Button againButton;
    private Button okButton;

    private ResultScreen(S2CMatchResultPacket packet) {
        super(Component.literal("Result"));
        this.result = packet.result();
        this.local = packet.localSide();
        this.names = new String[] {packet.blueName(), packet.redName()};
        this.players = new UUID[] {packet.bluePlayer(), packet.redPlayer()};
        this.timeline = new ResultTimeline(result.crowns(local),
                result.crowns(local.opponent()));
    }

    /** Opens the screen for a result from the server. */
    public static void open(S2CMatchResultPacket packet) {
        Minecraft.getInstance().setScreen(new ResultScreen(packet));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = top() + 218;
        boolean again = result.vsBot();
        int w = 98;
        againButton = addRenderableWidget(Button.builder(
                Component.literal("Play again"), b -> close(true))
                .bounds(cx - w - 3, y, w, 20).build());
        okButton = addRenderableWidget(Button.builder(
                Component.literal("OK"), b -> onClose())
                .bounds(again ? cx + 3 : cx - w / 2, y, w, 20).build());
        againButton.visible = false;
        okButton.visible = false;
    }

    @Override
    public void tick() {
        ticks++;
        boolean ended = timeline.finalShown(time(0.0F));
        againButton.visible = ended && result.vsBot();
        okButton.visible = ended;
        // Sent home by the server (time out): nothing left to show.
        if (!ClientMatchState.active() && ticks > 2) {
            sent = true;
            minecraft.setScreen(null);
        }
    }

    private float time(float partialTick) {
        return skipped ? timeline.finalTick() : ticks + partialTick;
    }

    private int top() {
        return Math.max(0, (height - BLOCK_H) / 2);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!timeline.finalShown(time(0.0F))) {
            skip();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key != 256 && !timeline.finalShown(time(0.0F))) {
            skip();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    /** Jumps to the end without the sounds it skips. */
    private void skip() {
        skipped = true;
        cueTime = timeline.finalTick();
    }

    @Override
    public void onClose() {
        close(false);
    }

    private void close(boolean again) {
        if (!sent) {
            sent = true;
            ModNetwork.sendToServer(new C2SResultDonePacket(again));
        }
        minecraft.setScreen(null);
    }

    // ------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick)
    {
        float t = time(partialTick);
        playCues(t);
        int cx = width / 2;
        int top = top();
        int dim = Math.round(timeline.dim(t) * 255.0F);
        graphics.fill(0, 0, width, height, (dim << 24) | 0x0A0A10);

        boolean won = result.winner() == local;
        boolean draw = result.winner() == null;
        if (timeline.wordShown(t)) {
            int raysY = draw ? top + 90 : won ? top + 136 : top + 26;
            drawRays(graphics, cx, raysY, draw ? 0.35F : won ? 0.95F : 0.7F, t);
        }

        drawCrowns(graphics, cx, top + 6, false, t);
        drawCrowns(graphics, cx, top + 116, true, t);
        float slide = 1.0F - timeline.ribbons(t);
        drawRibbon(graphics, local.opponent(), RIBBON_RED,
                Math.round(cx - RIBBON_W / 2.0F - SLIDE * slide), top + 49,
                0xFFAAAA, slide, t);
        drawRibbon(graphics, local, RIBBON_BLUE,
                Math.round(cx - RIBBON_W / 2.0F + SLIDE * slide), top + 158,
                0xAAD4FF, slide, t);

        if (t >= ResultTimeline.FIRST_CROWN)
            graphics.drawCenteredString(font, "VS", cx, top + 84, 0xFFFFFF);
        if (timeline.wordShown(t))
            drawWord(graphics, cx, top + 107, draw ? "DRAW" :
                    won ? "VICTORY!" : "DEFEAT",
                    draw ? 0xFFFFFF : won ? 0xFFD84A : 0xFF5555,
                    timeline.wordScale(t));
        if (timeline.finalShown(t))
            drawMvp(graphics, cx, top + 192);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void playCues(float t) {
        for (ResultTimeline.Cue cue : timeline.cues(cueTime, t)) {
            switch (cue.kind()) {
                case RIBBONS -> sound(SoundEvents.UI_TOAST_IN, 1.0F);
                case CROWN -> sound(SoundEvents.EXPERIENCE_ORB_PICKUP,
                        0.8F + 0.12F * cue.index());
                case WORD -> sound(result.winner() == null ?
                        SoundEvents.UI_TOAST_IN : result.winner() == local ?
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE :
                        SoundEvents.BEACON_DEACTIVATE, 1.0F);
                case FINAL -> {
                }
            }
        }
        cueTime = t;
    }

    private void sound(SoundEvent event, float pitch) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(event,
                pitch));
    }

    /** Light rays turning slowly behind the winner's King crown. */
    private void drawRays(GuiGraphics graphics, int x, int y, float alpha,
            float t)
    {
        int size = 115;
        RenderSystem.enableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(t * 0.6F));
        graphics.blit(RAYS, -size / 2, -size / 2, size, size, 0.0F, 0.0F,
                128, 128, 128, 128);
        graphics.pose().popPose();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    /** Three crown slots: left, the King (1.5x) in the middle, right. */
    private void drawCrowns(GuiGraphics graphics, int cx, int y, boolean own,
            float t)
    {
        drawSlot(graphics, cx - 52, y + 13, 1.0F, own, 0, t);
        drawSlot(graphics, cx + 26, y + 13, 1.0F, own, 2, t);
        drawSlot(graphics, cx - 20, y, 1.5F, own, 1, t);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y, float size,
            boolean own, int slot, float t)
    {
        boolean shown = timeline.crownShown(own, slot, t);
        float scale = size * timeline.crownScale(own, slot, t);
        float half = FRAME * size / 2.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(x + half, y + half, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-FRAME / 2.0F, -FRAME / 2.0F, 0.0F);
        RenderSystem.enableBlend();
        graphics.blit(ADVANCEMENT_WIDGETS, 0, 0, FRAME_U,
                shown ? FRAME_GOLD_V : FRAME_GREY_V, FRAME, FRAME);
        RenderSystem.disableBlend();
        if (shown)
            graphics.renderItem(CROWN, 5, 5);
        graphics.pose().popPose();
    }

    /** A name ribbon with the player's face, name and crown count. */
    private void drawRibbon(GuiGraphics graphics, TeamSide side,
            ResourceLocation texture, int x, int y, int labelColor,
            float slide, float t)
    {
        float alpha = 1.0F - 0.25F * slide;
        RenderSystem.enableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(texture, x, y, 0.0F, 0.0F, RIBBON_W, RIBBON_H,
                RIBBON_W, RIBBON_H);
        PlayerFaceRenderer.draw(graphics, skinOf(side), x + 20, y + 4, 16);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        int textX = x + 126;
        graphics.drawCenteredString(font, names[side.ordinal()], textX, y + 3,
                0xFFFFFF);
        if (t >= ResultTimeline.FIRST_CROWN) {
            int crowns = result.crowns(side);
            graphics.drawCenteredString(font, crowns +
                    (crowns == 1 ? " crown" : " crowns"), textX, y + 14,
                    labelColor);
        }
    }

    private ResourceLocation skinOf(TeamSide side) {
        if (result.vsBot() && side == TeamSide.RED)
            return ZOMBIE_SKIN;
        UUID id = players[side.ordinal()];
        if (minecraft.player != null && minecraft.player.getUUID().equals(id))
            return minecraft.player.getSkinTextureLocation();
        PlayerInfo info = minecraft.getConnection() == null ? null :
                minecraft.getConnection().getPlayerInfo(id);
        return info != null ? info.getSkinLocation() :
                DefaultPlayerSkin.getDefaultSkin(id);
    }

    /** VICTORY! / DEFEAT / DRAW, three times the font size, with its shadow. */
    private void drawWord(GuiGraphics graphics, int cx, int y, String word,
            int color, float zoom)
    {
        float scale = 3.0F * zoom;
        graphics.pose().pushPose();
        graphics.pose().translate(cx, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawCenteredString(font, word, 0, -4, color);
        graphics.pose().popPose();
    }

    /** Your best card on a toast: its art, name and the damage it dealt. */
    private void drawMvp(GuiGraphics graphics, int cx, int y) {
        int w = 240;
        int h = 23;
        int x = cx - w / 2;
        graphics.blitNineSliced(TOASTS, x, y, w, h, 4, 160, 32, 0, 0);
        BattleCard mvp = result.mvp(local);
        if (mvp == null) {
            graphics.drawCenteredString(font, "No MVP this time", cx, y + 8,
                    0xAAAAAA);
            return;
        }
        CardFace.draw(graphics, mvp, x + 6, y + 3, 13, 17, 0xFF000000, 1);
        graphics.drawString(font, "MVP", x + 26, y + 8, 0xFFFF55);
        graphics.drawString(font, mvp.displayName(), x + 26 +
                font.width("MVP") + 6, y + 8, 0xFFFFFF);
        String damage = String.format(Locale.ROOT, "%.0f dmg",
                result.mvpDamage(local));
        graphics.drawString(font, damage, x + w - 7 - font.width(damage),
                y + 8, 0xAAAAAA);
    }
}
