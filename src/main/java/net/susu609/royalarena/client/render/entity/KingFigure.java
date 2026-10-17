package net.susu609.royalarena.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.client.render.WorldHealthBar;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.network.s2c.S2CProjectilePacket;
import net.susu609.royalarena.tower.TowerId;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * The player standing on top of each King tower, wearing the skin of the
 * player of that side (the bot gets Steve or Alex). While its tower sleeps
 * the King dozes, head down, with snore bubbles. When the tower wakes (it is
 * hit, or a Princess tower falls) the King jumps up with both arms raised,
 * then idles, looks down the arena and throws its arm forward each time its
 * tower shoots. It flashes red with its tower when the tower is hit. Drawn by
 * {@link KingTowerRenderer} with the tower's pose; it is not an entity.
 */
public final class KingFigure {
    /** Height of the figure's feet above the tower's base. */
    public static final float FEET = 4.0F;
    private static final int SWING_TICKS = 8;
    /** Length of the jump up and arms raised when the King wakes. */
    private static final int WAKE_TICKS = 24;

    /** Game tick each side's King woke up; absent while it sleeps. */
    private static final Map<TowerId, Integer> WOKE_AT =
            new EnumMap<>(TowerId.class);
    private static final Map<TowerId, Integer> SNORED =
            new EnumMap<>(TowerId.class);

    private static final Map<TowerId, Integer> LAST_SHOT =
            new EnumMap<>(TowerId.class);

    private final PlayerModel<?> wide;
    private final PlayerModel<?> slim;

    public KingFigure(EntityRendererProvider.Context context) {
        this.wide = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER),
                false);
        this.slim = new PlayerModel<>(
                context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    /** A shot leaving a King tower: that King throws its arm. */
    public static void onProjectile(S2CProjectilePacket p) {
        if (!ClientMatchState.active() ||
                p.kind() != S2CProjectilePacket.Kind.ARROW)
            return;
        ArenaLayout arena = ClientMatchState.arena();
        for (TowerId id : new TowerId[] {TowerId.BLUE_KING, TowerId.RED_KING}) {
            ArenaPoint at = arena.towerPoint(id);
            if (Math.abs(p.fromX() - at.x()) < 1.0F &&
                    Math.abs(p.fromZ() - at.z()) < 1.0F &&
                    p.fromY() > at.y() + FEET)
                LAST_SHOT.put(id, age());
        }
    }

    private static int age() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : (int) mc.level.getGameTime();
    }

    /**
     * Draws the King of the tower {@code tower} stands for; {@code yaw} is
     * the tower's, so the King faces the enemy like the tower does.
     */
    public void render(Entity tower, float yaw, float partialTick,
            PoseStack pose, MultiBufferSource buffer, int packedLight)
    {
        if (!ClientMatchState.active())
            return;
        TowerId id = ClientMatchState.arena().towerAt(tower.getX(),
                tower.getZ());
        if (id == null || ClientMatchState.towerHealth(id) <= 0)
            return;
        UUID player = ClientMatchState.player(id.side());
        ResourceLocation skin = DefaultPlayerSkin.getDefaultSkin(player);
        boolean thin = "slim".equals(DefaultPlayerSkin.getSkinModelName(player));
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = connection == null ? null :
                connection.getPlayerInfo(player);
        if (info != null) {
            skin = info.getSkinLocation();
            thin = "slim".equals(info.getModelName());
        }
        PlayerModel<?> model = thin ? slim : wide;
        float time = tower.tickCount + partialTick;
        boolean awake = ClientMatchState.kingAwake(id.side());
        if (!awake) {
            WOKE_AT.remove(id);
            sleep(model, time);
            snore(id, tower);
        } else {
            Integer woke = WOKE_AT.get(id);
            if (woke == null) {
                woke = age();
                WOKE_AT.put(id, woke);
                wakeSound(tower);
            }
            Integer shot = LAST_SHOT.get(id);
            float swing = shot == null ? 1.0F :
                    Mth.clamp((age() - shot + partialTick) / SWING_TICKS,
                            0.0F, 1.0F);
            float wake = Mth.clamp((age() - woke + partialTick) / WAKE_TICKS,
                    0.0F, 1.0F);
            pose(model, time, swing, wake);
        }
        copyLayers(model);

        float wake = awake && WOKE_AT.containsKey(id) ? Mth.clamp(
                (age() - WOKE_AT.get(id) + partialTick) / WAKE_TICKS, 0.0F,
                1.0F) : 1.0F;
        // The jump: up and back down in the first part of the wake.
        float jump = wake < 0.5F ? Mth.sin(wake * 2.0F * Mth.PI) * 0.6F : 0.0F;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, -1.501F - FEET - Math.max(0.0F, jump), 0.0F);
        model.renderToBuffer(pose,
                buffer.getBuffer(RenderType.entityTranslucent(skin)),
                packedLight, WorldHealthBar.towerOverlay(tower), 1.0F, 1.0F,
                1.0F, 1.0F);
        pose.popPose();
    }

    /** Dozing: head down, arms hanging, a slow sway. */
    private static void sleep(PlayerModel<?> model, float time) {
        reset(model);
        model.head.xRot = 0.75F;
        model.head.zRot = Mth.sin(time * 0.04F) * 0.12F;
        model.body.xRot = 0.08F;
        float sway = Mth.sin(time * 0.04F) * 0.04F;
        model.rightArm.zRot = 0.03F + sway;
        model.leftArm.zRot = -0.03F + sway;
    }

    /** A snore bubble rising from the dozing King every 1.5 s. */
    private static void snore(TowerId id, Entity tower) {
        Minecraft mc = Minecraft.getInstance();
        int now = age();
        if (mc.level == null || now - SNORED.getOrDefault(id, -1000) < 30)
            return;
        SNORED.put(id, now);
        mc.level.addParticle(ParticleTypes.BUBBLE_POP, tower.getX(),
                tower.getY() + FEET + 1.9D, tower.getZ(), 0.0D, 0.03D, 0.0D);
    }

    private static void wakeSound(Entity tower) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        mc.level.playLocalSound(tower.getX(), tower.getY() + FEET,
                tower.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE,
                0.7F, 0.6F, false);
        mc.level.addParticle(ParticleTypes.EXPLOSION, tower.getX(),
                tower.getY() + FEET + 1.0D, tower.getZ(), 0.0D, 0.0D, 0.0D);
    }

    private static void reset(PlayerModel<?> model) {
        model.young = false;
        model.crouching = false;
        model.riding = false;
        model.head.xRot = 0.0F;
        model.head.yRot = 0.0F;
        model.head.zRot = 0.0F;
        model.body.xRot = 0.0F;
        model.body.yRot = 0.0F;
        model.rightArm.xRot = 0.0F;
        model.rightArm.yRot = 0.0F;
        model.rightArm.zRot = 0.0F;
        model.leftArm.xRot = 0.0F;
        model.leftArm.yRot = 0.0F;
        model.leftArm.zRot = 0.0F;
        model.rightLeg.xRot = 0.0F;
        model.leftLeg.xRot = 0.0F;
    }

    /**
     * Awake: idle breathing and a slow look around; {@code swing} 0..1 is how
     * far through the throw of the last shot it is, {@code wake} 0..1 how far
     * through waking up (1 = done).
     */
    private static void pose(PlayerModel<?> model, float time, float swing,
            float wake)
    {
        reset(model);
        model.head.yRot = Mth.sin(time * 0.03F) * 0.35F;
        model.head.xRot = 0.25F;
        float breathe = Mth.sin(time * 0.067F) * 0.05F;
        model.rightArm.zRot = 0.05F + breathe;
        model.leftArm.zRot = -0.05F - breathe;
        if (wake < 1.0F) {
            // Both arms thrown up, then lowered: "I'm up!"
            float up = Mth.sin(wake * Mth.PI);
            model.head.yRot = 0.0F;
            model.head.xRot = -0.3F * up;
            model.rightArm.xRot = -Mth.PI * 0.95F * up;
            model.leftArm.xRot = -Mth.PI * 0.95F * up;
            model.rightArm.zRot = 0.35F * up;
            model.leftArm.zRot = -0.35F * up;
            return;
        }
        if (swing < 1.0F) {
            // Arm thrown forward from above the head, as if casting.
            float s = Mth.sin(swing * Mth.PI);
            model.rightArm.xRot = -Mth.PI * 0.9F * s;
            model.body.yRot = 0.2F * s;
        }
    }

    private static void copyLayers(PlayerModel<?> model) {
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }

    public static void clear() {
        LAST_SHOT.clear();
        WOKE_AT.clear();
        SNORED.clear();
    }
}
