package net.susu609.royalarena.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.config.ArenaClientConfig;
import net.susu609.royalarena.match.TeamSide;

/**
 * Per-troop effects drawn on this client only: a ring of the team's colour
 * on the floor under every troop, and dust kicked up by the heavy ones
 * (Iron Golem, Ravager, Hoglin) as they walk.
 */
public final class TroopEffects {
    private TroopEffects() {
    }

    /** Once per client tick: footstep dust. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused() || !ClientMatchState.active())
            return;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof IronGolem || entity instanceof Ravager ||
                    entity instanceof Hoglin) ||
                    TroopVisuals.sideOf(entity) == null || !entity.onGround())
                continue;
            double dx = entity.getX() - entity.xo;
            double dz = entity.getZ() - entity.zo;
            // Hoglins run, so they kick up dust more often.
            int every = entity instanceof Hoglin ? 2 : 4;
            if (dx * dx + dz * dz < 1.0E-4D || entity.tickCount % every != 0)
                continue;
            BlockState ground = mc.level.getBlockState(
                    BlockPos.containing(entity.getX(), entity.getY() - 0.2D,
                            entity.getZ()));
            if (ground.isAir())
                continue;
            BlockParticleOption dust =
                    new BlockParticleOption(ParticleTypes.BLOCK, ground);
            double width = entity.getBbWidth() * 0.5D;
            for (int i = 0; i < 4; i++) {
                double ox = (mc.level.random.nextDouble() - 0.5D) * width * 2.0D;
                double oz = (mc.level.random.nextDouble() - 0.5D) * width * 2.0D;
                mc.level.addParticle(dust, entity.getX() + ox,
                        entity.getY() + 0.1D, entity.getZ() + oz, -dx * 2.0D,
                        0.15D, -dz * 2.0D);
            }
        }
    }

    /** A team-coloured ring under every troop, flat on the arena floor. */
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() !=
                RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS ||
                !ClientMatchState.active() ||
                !ArenaClientConfig.TEAM_RINGS.get())
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers =
                mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick();
        double floor = ClientMatchState.arena().floorY() + 1.0D;
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        for (Entity entity : mc.level.entitiesForRendering()) {
            TeamSide side = TroopVisuals.sideOf(entity);
            if (side == null || !(entity instanceof LivingEntity) ||
                    !entity.isAlive())
                continue;
            double x = Mth.lerp(partial, entity.xo, entity.getX());
            double z = Mth.lerp(partial, entity.zo, entity.getZ());
            double radius = entity.getBbWidth() * TroopVisuals.scale(entity) *
                    0.5D + 0.12D;
            int rgb = side == TeamSide.BLUE ? WorldHealthBar.BLUE_RGB :
                    WorldHealthBar.RED_RGB;
            WorldShapes.disc(pose, buffers, x, floor, z, radius, rgb, 0.18F,
                    0.9F);
        }
        pose.popPose();
        buffers.endBatch();
    }
}
