package net.susu609.royalarena.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.susu609.royalarena.network.s2c.S2CProjectilePacket;
import net.susu609.royalarena.network.s2c.S2CProjectilePacket.Kind;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the arrows and fireballs the server announces with
 * {@link S2CProjectilePacket}: real 3D vanilla models flying along an arc,
 * pointing where they fly. They are client-only entities that are never
 * added to the level, so they have no physics, collisions or sounds.
 */
public final class ProjectileRenderer {
    private static final List<Flight> FLIGHTS = new ArrayList<>();
    private static final Map<Kind, Entity> MODELS = new EnumMap<>(Kind.class);
    private static ClientLevel modelLevel;
    private static int clientTicks;

    private record Flight(Kind kind, Vec3 from, Vec3 to, int start,
                          int ticks, float arc)
    {
        /** Position at {@code t} in 0..1. */
        Vec3 at(double t) {
            return from.lerp(to, t).add(0.0D, arc * 4.0D * t * (1.0D - t),
                    0.0D);
        }

        /** Direction of flight at {@code t}. */
        Vec3 heading(double t) {
            return to.subtract(from).add(0.0D, arc * 4.0D * (1.0D - 2.0D * t),
                    0.0D);
        }

        double progress(int now, float partial) {
            return Math.max(0.0D, Math.min(1.0D,
                    (now - start + partial) / (double) ticks));
        }
    }

    private ProjectileRenderer() {
    }

    public static void add(S2CProjectilePacket p) {
        FLIGHTS.add(new Flight(p.kind(),
                new Vec3(p.fromX(), p.fromY(), p.fromZ()),
                new Vec3(p.toX(), p.toY(), p.toZ()), clientTicks, p.ticks(),
                p.arc()));
    }

    public static void clear() {
        FLIGHTS.clear();
    }

    /** Once per client tick: drop landed flights, trail the fireballs. */
    public static void tick() {
        clientTicks++;
        FLIGHTS.removeIf(f -> clientTicks - f.start() > f.ticks());
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            FLIGHTS.clear();
            return;
        }
        for (Flight f : FLIGHTS) {
            if (f.kind() != Kind.FIREBALL)
                continue;
            Vec3 p = f.at(f.progress(clientTicks, 0.0F));
            mc.level.addParticle(ParticleTypes.FLAME, p.x, p.y, p.z, 0.0D,
                    0.02D, 0.0D);
            mc.level.addParticle(ParticleTypes.SMOKE, p.x, p.y, p.z, 0.0D,
                    0.01D, 0.0D);
        }
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES ||
                FLIGHTS.isEmpty())
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        if (modelLevel != mc.level) {
            MODELS.clear();
            modelLevel = mc.level;
        }
        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers =
                mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick();

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        for (Flight f : FLIGHTS) {
            Entity model = MODELS.computeIfAbsent(f.kind(),
                    k -> create(mc.level, k));
            if (model == null)
                continue;
            double t = f.progress(clientTicks, partial);
            Vec3 p = f.at(t);
            Vec3 d = f.heading(t);
            float yaw = (float) Math.toDegrees(Math.atan2(d.x, d.z));
            float pitch = (float) Math.toDegrees(Math.atan2(d.y,
                    Math.sqrt(d.x * d.x + d.z * d.z)));
            model.setPos(p.x, p.y, p.z);
            model.xo = model.xOld = p.x;
            model.yo = model.yOld = p.y;
            model.zo = model.zOld = p.z;
            model.setYRot(yaw);
            model.yRotO = yaw;
            model.setXRot(pitch);
            model.xRotO = pitch;
            model.tickCount = 10;
            int light = LevelRenderer.getLightColor(mc.level,
                    BlockPos.containing(p));
            mc.getEntityRenderDispatcher().render(model, p.x, p.y, p.z, yaw,
                    partial, pose, buffers, light);
        }
        pose.popPose();
        buffers.endBatch();
    }

    private static Entity create(ClientLevel level, Kind kind) {
        EntityType<?> type = switch (kind) {
            case FIREBALL -> EntityType.FIREBALL;
            case POTION -> EntityType.POTION;
            case ARROW -> EntityType.ARROW;
        };
        Entity entity = type.create(level);
        if (entity != null)
            entity.setSilent(true);
        return entity;
    }
}
