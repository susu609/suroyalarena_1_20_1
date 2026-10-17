package net.susu609.royalarena.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.susu609.royalarena.network.s2c.S2CAoeFlashPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Red circles flashing on the floor where area damage lands (Creeper blast,
 * Witch potion, Fireball, each Arrow Storm volley): two quick flashes that
 * fade out over {@link #DURATION} ticks.
 */
public final class AoeFlashRenderer {
    public static final int DURATION = 12;
    private static final int RED = 0xFF3030;
    private static final List<Flash> FLASHES = new ArrayList<>();
    private static int clientTicks;

    private record Flash(double x, double y, double z, double radius,
                         int start)
    {
    }

    private AoeFlashRenderer() {
    }

    public static void add(S2CAoeFlashPacket p) {
        FLASHES.add(new Flash(p.x(), p.y(), p.z(), p.radius(), clientTicks));
    }

    public static void tick() {
        clientTicks++;
        FLASHES.removeIf(f -> clientTicks - f.start() > DURATION);
    }

    public static void clear() {
        FLASHES.clear();
    }

    /** Brightness at {@code t} (0..1): two flashes, fading out. */
    static float intensity(double t) {
        if (t < 0.0D || t > 1.0D)
            return 0.0F;
        return (float) ((1.0D - t) * (0.55D + 0.45D * Math.cos(t * Math.PI * 4)));
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() !=
                RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS ||
                FLASHES.isEmpty())
            return;
        Minecraft mc = Minecraft.getInstance();
        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers =
                mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        for (Flash f : FLASHES) {
            float a = intensity((clientTicks - f.start() + partial) /
                    (double) DURATION);
            if (a <= 0.01F)
                continue;
            WorldShapes.disc(pose, buffers, f.x(), f.y() + 0.01D, f.z(),
                    f.radius(), RED, 0.45F * a, a);
        }
        pose.popPose();
        buffers.endBatch();
    }
}
