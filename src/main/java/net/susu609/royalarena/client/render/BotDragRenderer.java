package net.susu609.royalarena.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.network.s2c.S2CBotDragPacket;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * The bot "dragging" a card like a player would: a red hand cursor carries
 * the card's ghosts (or a spell's circle) from the bot's side to where it
 * will play, easing in and out with a little wobble, before the server plays
 * the card.
 */
public final class BotDragRenderer {
    private static final int RED = 0xFF4040;
    /** A BLUE bot's hand, when two bots play each other. */
    private static final int BLUE = 0x4A8CFF;
    /** Ticks the ghosts wait at the target until the real deploy shows. */
    private static final int LINGER = 6;
    private static final List<Drag> DRAGS = new ArrayList<>();
    private static int clientTicks;

    private record Drag(BattleCard card, TeamSide side, double fromX,
                        double fromZ, double toX, double toZ, int start,
                        int ticks)
    {
        double progress(int now, float partial) {
            double t = (now - start + partial) / (double) ticks;
            t = Math.max(0.0D, Math.min(1.0D, t));
            return t * t * (3.0D - 2.0D * t);
        }
    }

    private BotDragRenderer() {
    }

    public static void add(S2CBotDragPacket p) {
        DRAGS.add(new Drag(p.card(), p.side(), p.fromX(), p.fromZ(), p.toX(),
                p.toZ(), clientTicks, p.ticks()));
    }

    public static void tick() {
        clientTicks++;
        DRAGS.removeIf(d -> clientTicks - d.start() > d.ticks() + LINGER);
    }

    public static void clear() {
        DRAGS.clear();
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() !=
                RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS ||
                DRAGS.isEmpty() || !ClientMatchState.active())
            return;
        Minecraft mc = Minecraft.getInstance();
        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers =
                mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick();
        double floor = ClientMatchState.arena().floorY() + 1.0D;

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        for (Drag d : DRAGS) {
            double e = d.progress(clientTicks, partial);
            double dx = d.toX() - d.fromX();
            double dz = d.toZ() - d.fromZ();
            double length = Math.max(1.0E-3D, Math.hypot(dx, dz));
            // Sideways wobble, gone by the time the card lands.
            double wobble = Math.sin(e * Math.PI * 2.0D) * 0.6D * (1.0D - e);
            double x = d.fromX() + dx * e - dz / length * wobble;
            double z = d.fromZ() + dz * e + dx / length * wobble;
            double radius = d.card().isSpell() ? d.card().spell().radius() :
                    0.7D;
            int color = d.side() == TeamSide.BLUE ? BLUE : RED;
            WorldShapes.disc(pose, buffers, x, floor + 0.01D, z, radius, color,
                    0.22F, 0.8F);
            DeployPreviewRenderer.ghosts(mc, pose, buffers, d.card(), x,
                    floor, z, d.side(), partial);
            cursor(pose, buffers, event, x, floor + 2.2D, z, color);
        }
        pose.popPose();
        buffers.endBatch();
    }

    /** An arrow cursor pointing down, always facing the camera. */
    private static void cursor(PoseStack pose, MultiBufferSource buffers,
            RenderLevelStageEvent event, double x, double y, double z,
            int color)
    {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(event.getCamera().rotation());
        pose.scale(0.5F, 0.5F, 0.5F);
        Matrix4f m = pose.last().pose();
        VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        // Arrow head (tip down) and stem, both windings so it never culls.
        float[][] head = {{0.0F, -0.6F}, {-0.5F, 0.1F}, {0.5F, 0.1F},
                {0.0F, -0.6F}};
        float[][] stem = {{-0.15F, 0.1F}, {-0.15F, 0.7F}, {0.15F, 0.7F},
                {0.15F, 0.1F}};
        for (float[][] shape : new float[][][] {head, stem}) {
            for (int i = 0; i < 4; i++)
                quads.vertex(m, shape[i][0], shape[i][1], 0.0F)
                        .color(r, g, b, 0.9F).endVertex();
            for (int i = 3; i >= 0; i--)
                quads.vertex(m, shape[i][0], shape[i][1], 0.0F)
                        .color(r, g, b, 0.9F).endVertex();
        }
        pose.popPose();
    }
}
