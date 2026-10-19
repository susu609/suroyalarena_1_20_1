package net.susu609.royalarena.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.client.state.ClientSandbox;
import net.susu609.royalarena.match.MatchSnapshot;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The sandbox stats overlay (docs/SANDBOX.md): around every troop its
 * attack range (team colour, from the body's edge) and its sight (faint
 * white, how far it notices enemies), and a line to what it is aiming at
 * (another troop, or its tower), as the server last sent it.
 */
public final class SandboxStatsRenderer {
    private SandboxStatsRenderer() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() !=
                RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;
        Minecraft mc = Minecraft.getInstance();
        MatchSnapshot.Sandbox box = ClientSandbox.settings();
        if (mc.level == null || box == null || !ClientSandbox.stats())
            return;
        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers =
                mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick();
        ArenaLayout arena = ClientMatchState.arena();
        double floor = arena.floorY() + 1.0D;

        Map<UUID, Entity> troops = new HashMap<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity && entity.isAlive() &&
                    TroopVisuals.cardOf(entity) != null)
                troops.put(entity.getUUID(), entity);
        }

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        for (Entity entity : troops.values()) {
            BattleCard card = TroopVisuals.cardOf(entity);
            if (card.isSpell())
                continue;
            TroopStats stats = card.stats();
            Vec3 at = entity.getPosition(partial);
            int rgb = TroopVisuals.sideOf(entity) == TeamSide.BLUE ?
                    WorldHealthBar.BLUE_RGB : WorldHealthBar.RED_RGB;
            WorldShapes.disc(pose, buffers, at.x, floor + 0.01D, at.z,
                    stats.radius() + stats.range(), rgb, 0.08F, 0.9F);
            if (!stats.traits().buildingsOnly())
                WorldShapes.disc(pose, buffers, at.x, floor + 0.02D, at.z,
                        stats.sight(), 0xFFFFFF, 0.0F, 0.35F);
        }
        for (MatchSnapshot.Aim aim : box.aims()) {
            Entity from = troops.get(aim.unit());
            if (from == null)
                continue;
            Vec3 a = from.getPosition(partial);
            double y = a.y + from.getBbHeight() * TroopVisuals.scale(from) * 0.5D;
            if (aim.target() != null) {
                Entity to = troops.get(aim.target());
                if (to == null)
                    continue;
                Vec3 b = to.getPosition(partial);
                WorldShapes.line(pose, buffers, a.x, y, a.z, b.x,
                        b.y + to.getBbHeight() * TroopVisuals.scale(to) * 0.5D,
                        b.z, 0xFFE040, 1.0F);
            } else if (aim.tower() >= 0 &&
                    aim.tower() < TowerId.values().length) {
                ArenaPoint tower = arena.towerPoint(TowerId.values()[aim.tower()]);
                WorldShapes.line(pose, buffers, a.x, y, a.z, tower.x() + 0.5D,
                        tower.y() + 2.0D, tower.z() + 0.5D, 0xFF9030, 0.8F);
            }
        }
        pose.popPose();
        buffers.endBatch();
    }
}
