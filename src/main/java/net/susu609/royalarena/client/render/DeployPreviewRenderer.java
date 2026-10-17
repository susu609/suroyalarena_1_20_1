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
import net.susu609.royalarena.arena.ArenaWaypoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.combat.Formation;
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
 * (deployable) or red (not), and a deployable tile shows ghosts of every
 * unit the card brings, in formation and at the card's scale;</li>
 * <li>each pending deploy shows its ghost plus a team-coloured square that
 * fills up during the one-second deploy time;</li>
 * <li>a selected spell shows its circle of effect instead (green or red), and
 * a spell in flight shows a team-coloured circle that fills up until it
 * lands.</li>
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
            if (deploy.card().isSpell()) {
                double radius = deploy.card().spell().radius();
                circle(pose, buffers, deploy.x(), floor, deploy.z(), radius,
                        rgb, 0.15F);
                circle(pose, buffers, deploy.x(), floor + 0.005D, deploy.z(),
                        radius * deploy.progress(), rgb, 0.35F);
                continue;
            }
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
            BattleCard card = ClientMatchState.hand().get(slot);
            int rgb = ok ? VALID_RGB : INVALID_RGB;
            if (card.isSpell()) {
                circle(pose, buffers, target.getX(), floor, target.getZ(),
                        card.spell().radius(), rgb, 0.25F);
            } else {
                tile(pose, buffers, target.getX(), floor, target.getZ(), rgb,
                        0.35F, 1.0F);
                if (ok) {
                    ghost(mc, pose, buffers, card, target.getX(), floor,
                            target.getZ(), ClientMatchState.side(), partial);
                }
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

    /** Spell circle centred on block (x, z). */
    private static void circle(PoseStack pose, MultiBufferSource buffers,
            int x, double y, int z, double radius, int rgb, float alpha)
    {
        WorldShapes.disc(pose, buffers, x + 0.5D, y, z + 0.5D, radius, rgb,
                alpha, 1.0F);
    }

    /**
     * Ghosts of every unit the card brings, where the server will put them
     * (the same formation and spacing as BattleField.deploy), drawn at the
     * card's model scale.
     */
    private static void ghost(Minecraft mc, PoseStack pose,
            MultiBufferSource buffers, BattleCard card, int x, double y,
            int z, TeamSide side, float partial)
    {
        ghosts(mc, pose, buffers, card, x + 0.5D, y, z + 0.5D, side, partial);
    }

    /**
     * Ghosts of a card's units around (cx, cz) (a block centre for a real
     * deploy, anywhere for the bot's drag). The pose must already be
     * translated to world space.
     */
    public static void ghosts(Minecraft mc, PoseStack pose,
            MultiBufferSource buffers, BattleCard card, double cx, double y,
            double cz, TeamSide side, float partial)
    {
        if (card.isSpell() || mc.level == null)
            return;
        if (ghostLevel != mc.level) {
            GHOSTS.clear();
            ghostLevel = mc.level;
        }
        Entity entity = GHOSTS.computeIfAbsent(card,
                c -> createGhost(mc.level, c));
        if (entity == null)
            return;
        TroopStats stats = card.stats();
        double spacing = Math.max(0.8D, stats.radius() * 2.0D + 0.3D);
        ArenaLayout arena = ClientMatchState.arena();
        int sign = ArenaLayout.sideSign(side);
        float scale = card.modelScale();
        float yaw = side == TeamSide.BLUE ? 180.0F : 0.0F;
        for (double[] offset : Formation.offsets(stats.count(), spacing)) {
            ArenaWaypoint spot = arena.clampSpawn(cx + offset[0],
                    cz + offset[1], sign);
            double ux = spot.x();
            double uz = spot.z();
            entity.setPos(ux, y, uz);
            entity.xo = entity.xOld = ux;
            entity.yo = entity.yOld = y;
            entity.zo = entity.zOld = uz;
            entity.setYRot(yaw);
            entity.yRotO = yaw;
            if (entity instanceof LivingEntity living) {
                living.yBodyRot = living.yBodyRotO = yaw;
                living.yHeadRot = living.yHeadRotO = yaw;
            }
            pose.pushPose();
            pose.translate(ux, y, uz);
            pose.scale(scale, scale, scale);
            mc.getEntityRenderDispatcher().render(entity, 0.0D, 0.0D, 0.0D,
                    yaw, partial, pose, buffers, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
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
