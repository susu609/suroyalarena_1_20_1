package net.susu609.royalarena.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.susu609.modlib.resource.RL;
import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.client.camera.ArenaCameraController;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.PendingDeploy;
import net.susu609.royalarena.match.TeamSide;
import org.joml.Matrix4f;

import java.util.EnumMap;
import java.util.Map;

/**
 * World-space deploy feedback:
 * <ul>
 * <li>while a card is selected, the tile under the cursor is tinted green
 * (deployable) or red (not), and a deployable tile shows a ghost of the
 * troop;</li>
 * <li>each pending deploy shows its ghost plus a team-coloured square that
 * fills up during the one-second deploy time.</li>
 * </ul>
 * Ghosts are invisible client-only entities that are never added to the
 * level: the arena player is a spectator, and vanilla renders invisible mobs
 * translucent for spectators.
 */
public final class DeployPreviewRenderer {
    private static final int VALID_RGB = 0x55FF66;
    private static final int INVALID_RGB = 0xFF3A3A;
    private static final Map<BattleCard, Entity> GHOSTS =
            new EnumMap<>(BattleCard.class);
    private static ClientLevel ghostLevel;

    private DeployPreviewRenderer() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() !=
                RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !ClientMatchState.active() ||
                ClientMatchState.state() != MatchState.BATTLE)
            return;
        if (ghostLevel != mc.level) {
            GHOSTS.clear();
            ghostLevel = mc.level;
        }

        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers =
                mc.renderBuffers().bufferSource();
        ArenaLayout arena = ClientMatchState.arena();
        double floor = arena.floorY() + 1.0D;
        float partial = event.getPartialTick();

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        for (PendingDeploy deploy : ClientMatchState.pendingDeploys()) {
            int rgb = deploy.side() == TeamSide.BLUE ?
                    WorldHealthBar.BLUE_RGB : WorldHealthBar.RED_RGB;
            tile(pose, buffers, deploy.x(), floor, deploy.z(), rgb, 0.25F,
                    1.0F);
            tile(pose, buffers, deploy.x(), floor + 0.005D, deploy.z(), rgb,
                    0.6F, deploy.progress());
            ghost(mc, pose, buffers, deploy.card(), deploy.x(), floor,
                    deploy.z(), deploy.side(), partial);
        }

        int slot = ClientMatchState.selectedSlot();
        BlockPos target = slot >= 0 ?
                ArenaCameraController.projectCursorToArena() : null;
        if (target != null) {
            boolean ok = ClientMatchState.canDeployAt(
                    slot, target.getX(), target.getZ());
            tile(pose, buffers, target.getX(), floor, target.getZ(),
                    ok ? VALID_RGB : INVALID_RGB, 0.35F, 1.0F);
            if (ok) {
                ghost(mc, pose, buffers, ClientMatchState.hand().get(slot),
                        target.getX(), floor, target.getZ(),
                        ClientMatchState.side(), partial);
            }
        }

        pose.popPose();
        buffers.endBatch();
    }

    /**
     * Translucent square on top of the floor block at (x, z), shrunk to
     * {@code size} (0..1) around the block centre, with a matching outline.
     */
    private static void tile(PoseStack pose, MultiBufferSource buffers,
            int x, double y, int z, int rgb, float alpha, float size)
    {
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        float half = Math.max(0.0F, Math.min(1.0F, size)) * 0.5F;
        if (half <= 0.0F)
            return;
        float x0 = x + 0.5F - half;
        float x1 = x + 0.5F + half;
        float z0 = z + 0.5F - half;
        float z1 = z + 0.5F + half;
        float fy = (float) y + 0.02F;

        Matrix4f m = pose.last().pose();
        VertexConsumer fill = buffers.getBuffer(RenderType.debugQuads());
        fill.vertex(m, x0, fy, z0).color(r, g, b, alpha).endVertex();
        fill.vertex(m, x0, fy, z1).color(r, g, b, alpha).endVertex();
        fill.vertex(m, x1, fy, z1).color(r, g, b, alpha).endVertex();
        fill.vertex(m, x1, fy, z0).color(r, g, b, alpha).endVertex();

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(pose, lines,
                new AABB(x0, fy, z0, x1, fy + 0.01D, z1), r, g, b, 1.0F);
    }

    private static void ghost(Minecraft mc, PoseStack pose,
            MultiBufferSource buffers, BattleCard card, int x, double y,
            int z, TeamSide side, float partial)
    {
        Entity entity = GHOSTS.computeIfAbsent(card,
                c -> createGhost(mc.level, c));
        if (entity == null)
            return;
        double cx = x + 0.5D;
        double cz = z + 0.5D;
        float yaw = side == TeamSide.BLUE ? 180.0F : 0.0F;
        entity.setPos(cx, y, cz);
        entity.xo = entity.xOld = cx;
        entity.yo = entity.yOld = y;
        entity.zo = entity.zOld = cz;
        entity.setYRot(yaw);
        entity.yRotO = yaw;
        if (entity instanceof LivingEntity living) {
            living.yBodyRot = living.yBodyRotO = yaw;
            living.yHeadRot = living.yHeadRotO = yaw;
        }
        mc.getEntityRenderDispatcher().render(entity, cx, y, cz, yaw, partial,
                pose, buffers, LightTexture.FULL_BRIGHT);
    }

    private static Entity createGhost(ClientLevel level, BattleCard card) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(
                RL.require(card.entityId()));
        Entity entity = type == null ? null : type.create(level);
        if (entity == null)
            return null;
        if (entity instanceof Slime slime && card.mobSize() > 0)
            slime.setSize(card.mobSize(), false);
        entity.setInvisible(true);
        entity.setSilent(true);
        if (entity instanceof Mob mob)
            mob.setNoAi(true);
        return entity;
    }
}
