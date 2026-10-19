package net.susu609.royalarena.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaWaypoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.client.camera.ArenaCameraController;
import net.susu609.royalarena.config.ArenaClientConfig;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.TeamSide;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * While a troop card is selected over a tile it can be deployed on, a
 * bouncing yellow "!" over every enemy troop the new troops would draw:
 * one whose sight reaches a ghost, by the rule {@code CombatEngine.pickEnemy}
 * uses (troops that only hit towers ignore troops, melee troops do not see
 * across the river unless one side stands on a bridge). It is "may be
 * drawn": a troop already fighting something near it keeps that target.
 */
public final class AggroMarkers {
    private static final int RGB = 0xFFE040;
    private static final Set<Entity> MARKED = new HashSet<>();

    private AggroMarkers() {
    }

    /** Once per client tick. */
    public static void tick() {
        MARKED.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !ClientMatchState.active() ||
                ClientMatchState.state() != MatchState.BATTLE)
            return;
        BattleCard card = ClientMatchState.selectedCard();
        if (card == null || card.isSpell())
            return;
        BlockPos target = ArenaCameraController.projectCursorToArena();
        if (target == null || !ClientMatchState.canDeploySelected(
                target.getX(), target.getZ()))
            return;
        TeamSide side = ClientMatchState.deploySide();
        List<ArenaWaypoint> spots = DeployPreviewRenderer.spots(card,
                target.getX() + 0.5D, target.getZ() + 0.5D, side);
        ArenaLayout arena = ClientMatchState.arena();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity) || !entity.isAlive() ||
                    TroopVisuals.sideOf(entity) != side.opponent())
                continue;
            BattleCard enemy = TroopVisuals.cardOf(entity);
            if (enemy == null || enemy.isSpell())
                continue;
            TroopStats stats = enemy.stats();
            if (stats.traits().buildingsOnly())
                continue;
            int theirs = arena.sideOf(entity.getZ());
            for (ArenaWaypoint spot : spots) {
                int mine = arena.sideOf(spot.z());
                if (stats.melee() && mine != theirs && mine != 0 && theirs != 0)
                    continue;
                if (Math.hypot(entity.getX() - spot.x(),
                        entity.getZ() - spot.z()) <= stats.sight()) {
                    MARKED.add(entity);
                    break;
                }
            }
        }
    }

    public static void clear() {
        MARKED.clear();
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES ||
                MARKED.isEmpty())
            return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers =
                mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick();
        double bounce = Math.abs(Math.sin(
                (Util.getMillis() % 600L) / 600.0D * Math.PI)) * 0.25D;
        for (Entity entity : MARKED) {
            Vec3 at = entity.getPosition(partial);
            // Above the health bar (body height + 0.5).
            double y = at.y + entity.getBbHeight() * TroopVisuals.scale(entity)
                    + 1.0D + bounce;
            pose.pushPose();
            pose.translate(at.x - cam.x, y - cam.y, at.z - cam.z);
            pose.mulPose(event.getCamera().rotation());
            float scale = 0.08F *
                    ArenaClientConfig.WORLD_TEXT_SCALE.get().floatValue();
            pose.scale(-scale, -scale, scale);
            Matrix4f matrix = pose.last().pose();
            WorldHealthBar.outlined(font, "!", -font.width("!") / 2.0F,
                    -font.lineHeight / 2.0F, RGB, matrix, buffers,
                    LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
        buffers.endBatch();
    }
}
