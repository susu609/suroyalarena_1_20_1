package net.susu609.royalarena.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;
import org.joml.Matrix4f;

/**
 * Simple billboard health bar: ten segments drawn as text backgrounds, the
 * same way vanilla name tags draw their dark box. Filled and empty segments
 * do not overlap, so there is no z-fighting.
 */
public final class WorldHealthBar {
    public static final int BLUE_RGB = 0x3A7BFF;
    public static final int RED_RGB = 0xE0403A;
    private static final int SEGMENTS = 10;
    private static final int EMPTY = 0xC0202020;

    private WorldHealthBar() {
    }

    /** Troops: only once damaged, coloured by their team-coloured name. */
    public static void renderTroop(LivingEntity entity, PoseStack pose,
            MultiBufferSource buffer, float scale)
    {
        if (!ClientMatchState.active())
            return;
        float max = entity.getMaxHealth();
        float health = entity.getHealth();
        if (health <= 0.0F || health >= max)
            return;
        Component name = entity.getCustomName();
        TextColor color = name == null ? null : name.getStyle().getColor();
        if (color == null)
            return;
        render(pose, buffer, entity.getBbHeight() * scale + 0.5D, health / max,
                color.getValue(), null);
    }

    /** Towers: always shown, with the remaining health as a number. */
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
        float fraction = health / (float) id.kind().maxHealth();
        render(pose, buffer, entity.getBbHeight() + 0.6D, fraction,
                id.side() == TeamSide.BLUE ? BLUE_RGB : RED_RGB,
                Integer.toString(health));
    }

    private static void render(PoseStack pose, MultiBufferSource buffer,
            double heightAbove, float fraction, int rgb, String label)
    {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        pose.pushPose();
        pose.translate(0.0D, heightAbove, 0.0D);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix = pose.last().pose();
        int light = LightTexture.FULL_BRIGHT;

        int filled = Math.round(Math.max(0.0F, Math.min(1.0F, fraction)) *
                SEGMENTS);
        if (filled == 0 && fraction > 0.0F)
            filled = 1;
        float left = -font.width(" ".repeat(SEGMENTS)) / 2.0F;
        if (filled > 0) {
            font.drawInBatch(" ".repeat(filled), left, 0.0F, 0xFFFFFFFF,
                    false, matrix, buffer, Font.DisplayMode.NORMAL,
                    0xFF000000 | rgb, light);
        }
        if (filled < SEGMENTS) {
            float x = left + font.width(" ".repeat(filled));
            font.drawInBatch(" ".repeat(SEGMENTS - filled), x, 0.0F,
                    0xFFFFFFFF, false, matrix, buffer, Font.DisplayMode.NORMAL,
                    EMPTY, light);
        }
        if (label != null) {
            font.drawInBatch(label, -font.width(label) / 2.0F, -10.0F,
                    0xFFFFFFFF, false, matrix, buffer, Font.DisplayMode.NORMAL,
                    0x80000000, light);
        }
        pose.popPose();
    }
}
