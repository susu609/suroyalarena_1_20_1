package net.susu609.royalarena.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.susu609.royalarena.client.render.entity.KingFigure;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.config.ArenaClientConfig;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;
import net.susu609.royalarena.tower.TowerKind;
import org.joml.Matrix4f;

/**
 * Simple billboard health bar: ten segments drawn as text backgrounds, the
 * same way vanilla name tags draw their dark box. Filled and empty segments
 * do not overlap, so there is no z-fighting.
 */
public final class WorldHealthBar {
    /** Deep team colours: bars, rings and previews. */
    public static final int BLUE_RGB = 0x1F4FD6;
    public static final int RED_RGB = 0xC81E1E;
    private static final int SEGMENTS = 10;
    private static final int TOWER_SEGMENTS = 16;
    private static final float TROOP_SCALE = 0.032F;
    private static final float TOWER_SCALE = 0.065F;
    private static final int EMPTY = 0xC0202020;
    /** Black frame around a bar, in font pixels: thick on towers. */
    private static final float TROOP_BORDER = 1.0F;
    private static final float TOWER_BORDER = 2.0F;
    private static final int BORDER = 0xFF000000;
    private static final int OUTLINE = 0x000000;

    private WorldHealthBar() {
    }

    /** Troops: only once damaged, in their team's deep colour. */
    public static void renderTroop(LivingEntity entity, PoseStack pose,
            MultiBufferSource buffer, float scale)
    {
        if (!ClientMatchState.active())
            return;
        float max = entity.getMaxHealth();
        float health = entity.getHealth();
        if (health <= 0.0F || health >= max)
            return;
        // The server names a troop in vanilla's pale chat blue or red (tags
        // do not reach the client); the bar uses the deep team colours.
        Component name = entity.getCustomName();
        TextColor color = name == null ? null : name.getStyle().getColor();
        if (color == null)
            return;
        int rgb = color.getValue() == ChatFormatting.BLUE.getColor() ?
                BLUE_RGB : RED_RGB;
        render(pose, buffer, entity.getBbHeight() * scale + 0.5D, health / max,
                rgb, null, SEGMENTS, TROOP_SCALE, TROOP_BORDER);
    }

    /** Ticks a tower (and its King) is tinted red after losing health. */
    public static final int TOWER_HURT_TICKS = 8;

    /**
     * Overlay for drawing a tower: vanilla's red hurt tint for a moment
     * after it loses health, like a troop that is hit, else none.
     */
    public static int towerOverlay(Entity entity) {
        if (!ClientMatchState.active())
            return OverlayTexture.NO_OVERLAY;
        TowerId id = ClientMatchState.arena().towerAt(
                entity.getX(), entity.getZ());
        boolean hurt = id != null &&
                ClientMatchState.ticksSinceTowerHurt(id) < TOWER_HURT_TICKS;
        // A selected spell that would hit this tower lights it up white.
        return OverlayTexture.pack(OverlayTexture.u(
                        SpellTargetHighlighter.whiteness(entity)),
                OverlayTexture.v(hurt));
    }

    /**
     * Towers: always shown, a long bar twice the size of a troop's with
     * "health/max" written on it. The King's floats above the player standing
     * on its tower.
     */
    public static void renderTower(Entity entity, PoseStack pose,
            MultiBufferSource buffer)
    {
        if (!ClientMatchState.active())
            return;
        TowerId id = ClientMatchState.arena().towerAt(
                entity.getX(), entity.getZ());
        if (id == null)
            return;
        int health = ClientMatchState.towerHealth(id);
        if (health <= 0)
            return;
        int max = id.kind().maxHealth();
        double above = id.kind() == TowerKind.KING ?
                KingFigure.FEET + 2.5D : entity.getBbHeight() + 0.9D;
        render(pose, buffer, above, health / (float) max,
                id.side() == TeamSide.BLUE ? BLUE_RGB : RED_RGB,
                health + "/" + max, TOWER_SEGMENTS, TOWER_SCALE, TOWER_BORDER);
    }

    private static void render(PoseStack pose, MultiBufferSource buffer,
            double heightAbove, float fraction, int rgb, String label,
            int segments, float scale, float border)
    {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        scale *= ArenaClientConfig.WORLD_TEXT_SCALE.get().floatValue();
        pose.pushPose();
        pose.translate(0.0D, heightAbove, 0.0D);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-scale, -scale, scale);
        Matrix4f matrix = pose.last().pose();
        int light = LightTexture.FULL_BRIGHT;

        // The black frame: the same background box, stretched by the border
        // on every side and pushed a little behind the bar (+z is away from
        // the camera here).
        float barWidth = font.width(" ".repeat(segments)) + 1.0F;
        float boxLeft = -font.width(" ".repeat(segments)) / 2.0F - 1.0F;
        float centerX = boxLeft + barWidth / 2.0F;
        pose.pushPose();
        pose.translate(centerX, 4.0F, 0.6F);
        pose.scale((barWidth + 2.0F * border) / barWidth,
                (10.0F + 2.0F * border) / 10.0F, 1.0F);
        pose.translate(-centerX, -4.0F, 0.0F);
        font.drawInBatch(" ".repeat(segments), boxLeft + 1.0F, 0.0F,
                0xFFFFFFFF, false, pose.last().pose(), buffer,
                Font.DisplayMode.NORMAL, BORDER, light);
        pose.popPose();

        int filled = Math.round(Math.max(0.0F, Math.min(1.0F, fraction)) *
                segments);
        if (filled == 0 && fraction > 0.0F)
            filled = 1;
        float left = -font.width(" ".repeat(segments)) / 2.0F;
        if (filled > 0) {
            font.drawInBatch(" ".repeat(filled), left, 0.0F, 0xFFFFFFFF,
                    false, matrix, buffer, Font.DisplayMode.NORMAL,
                    0xFF000000 | rgb, light);
        }
        if (filled < segments) {
            float x = left + font.width(" ".repeat(filled));
            font.drawInBatch(" ".repeat(segments - filled), x, 0.0F,
                    0xFFFFFFFF, false, matrix, buffer, Font.DisplayMode.NORMAL,
                    EMPTY, light);
        }
        if (label != null) {
            // On the bar, pushed in front of it so the two never z-fight,
            // with a black outline so it reads on any colour.
            outlined(font, label, -font.width(label) / 2.0F, 0.0F, 0xFFFFFF,
                    matrix, buffer, light);
        }
        pose.popPose();
    }

    /**
     * Text with a black outline on all sides, drawn the way vanilla draws
     * glow-ink signs: the eight black copies first in the normal layer, then
     * the text in the polygon-offset layer, which is pulled toward the camera
     * so the text always wins. (Drawing both in one layer made the equal-depth
     * quads z-fight, and black speckled through the letters.) Always fully
     * opaque: a translucent text over the black copies would show them.
     */
    public static void outlined(Font font, String text, float x, float y,
            int rgb, Matrix4f matrix, MultiBufferSource buffer, int light)
    {
        int outline = 0xFF000000 | OUTLINE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0)
                    font.drawInBatch(text, x + dx, y + dy, outline, false,
                            matrix, buffer, Font.DisplayMode.NORMAL, 0, light);
            }
        }
        font.drawInBatch(text, x, y, 0xFF000000 | rgb, false, matrix, buffer,
                Font.DisplayMode.POLYGON_OFFSET, 0, light);
    }
}
