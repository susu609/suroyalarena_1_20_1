package net.susu609.royalarena.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.client.hud.CrownFlights;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.config.ArenaClientConfig;
import net.susu609.royalarena.tower.TowerId;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Numbers floating up where troops and towers lose or gain health: red for
 * damage, green for healing, ice blue "Frozen" when a troop freezes. Worked
 * out on this client from the health the server already syncs (troop mobs'
 * health, tower health in the match snapshot), so no extra packets. Small
 * losses (a burn) are added up and shown at most twice a second.
 */
public final class DamageNumbers {
    private static final int LIFE = 24;
    private static final int DAMAGE = 0xFF5040;
    private static final int TOWER_DAMAGE = 0xFFB040;
    private static final int HEAL = 0x50FF70;
    private static final int FROZEN = 0x9FE4FF;
    /** Losses below this wait (up to {@link #GATHER_TICKS}) to add up. */
    private static final float MIN_SHOWN = 1.0F;
    private static final int GATHER_TICKS = 10;

    private record Popup(Vec3 at, String text, int rgb, float size,
                         int start, float drift)
    {
    }

    private static final List<Popup> POPUPS = new ArrayList<>();
    private static final Map<Integer, Float> HEALTH = new HashMap<>();
    private static final Map<Integer, Float> GATHERED = new HashMap<>();
    private static final Map<Integer, Integer> GATHER_START = new HashMap<>();
    private static final Set<Integer> FROZEN_SHOWN = new HashSet<>();
    private static final int[] TOWER_HEALTH = new int[TowerId.values().length];
    private static int ticks;

    static {
        java.util.Arrays.fill(TOWER_HEALTH, -1);
    }

    private DamageNumbers() {
    }

    public static void clear() {
        POPUPS.clear();
        HEALTH.clear();
        GATHERED.clear();
        GATHER_START.clear();
        FROZEN_SHOWN.clear();
        java.util.Arrays.fill(TOWER_HEALTH, -1);
    }

    /** Once per client tick. */
    public static void tick() {
        ticks++;
        POPUPS.removeIf(p -> ticks - p.start() > LIFE);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !ClientMatchState.active()) {
            clear();
            return;
        }
        Set<Integer> seen = new HashSet<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !entity.isAlive() ||
                    TroopVisuals.sideOf(entity) == null)
                continue;
            int id = entity.getId();
            seen.add(id);
            trackTroop(living, id);
        }
        HEALTH.keySet().retainAll(seen);
        GATHERED.keySet().retainAll(seen);
        GATHER_START.keySet().retainAll(seen);
        FROZEN_SHOWN.retainAll(seen);
        trackTowers();
    }

    private static void trackTroop(LivingEntity troop, int id) {
        float health = troop.getHealth();
        Float before = HEALTH.put(id, health);
        Vec3 head = troop.position().add(0.0D,
                troop.getBbHeight() * TroopVisuals.scale(troop) + 0.3D, 0.0D);
        if (before != null && health < before) {
            GATHERED.merge(id, before - health, Float::sum);
            GATHER_START.putIfAbsent(id, ticks);
        } else if (before != null && health > before) {
            add(head, "+" + amount(health - before), HEAL, 1.0F);
        }
        Float gathered = GATHERED.get(id);
        if (gathered != null && (gathered >= MIN_SHOWN ||
                ticks - GATHER_START.get(id) >= GATHER_TICKS)) {
            add(head, "-" + amount(gathered), DAMAGE,
                    gathered >= 10.0F ? 1.35F : 1.0F);
            GATHERED.remove(id);
            GATHER_START.remove(id);
        }
        boolean frozen = troop.isFullyFrozen();
        if (frozen && FROZEN_SHOWN.add(id))
            add(head.add(0.0D, 0.3D, 0.0D), "Frozen", FROZEN, 0.9F);
        else if (!frozen)
            FROZEN_SHOWN.remove(id);
    }

    private static void trackTowers() {
        for (TowerId id : TowerId.values()) {
            int health = ClientMatchState.towerHealth(id);
            int before = TOWER_HEALTH[id.ordinal()];
            TOWER_HEALTH[id.ordinal()] = health;
            if (before < 0 || health == before)
                continue;
            ArenaPoint p = ClientMatchState.arena().towerPoint(id);
            Vec3 top = new Vec3(p.x(), p.y() + 4.6D, p.z());
            if (health < before)
                add(top, "-" + (before - health), TOWER_DAMAGE, 1.5F);
            if (health <= 0 && before > 0)
                CrownFlights.towerFell(id);
        }
    }

    private static String amount(float value) {
        return Math.abs(value - Math.round(value)) < 0.05F || value >= 10.0F ?
                Integer.toString(Math.round(value)) :
                String.format(Locale.ROOT, "%.1f", value);
    }

    private static void add(Vec3 at, String text, int rgb, float size) {
        if (!ArenaClientConfig.DAMAGE_NUMBERS.get())
            return;
        // A little sideways drift, so numbers in a row do not stack.
        float drift = ((ticks * 37 + POPUPS.size() * 11) % 9 - 4) * 0.05F;
        POPUPS.add(new Popup(at, text, rgb, size, ticks, drift));
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES ||
                POPUPS.isEmpty())
            return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers =
                mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick();
        for (Popup p : POPUPS) {
            float age = (ticks - p.start() + partial) / LIFE;
            if (age < 0.0F || age > 1.0F)
                continue;
            // Pops up big, settles, rises and fades out.
            float pop = age < 0.15F ? 1.0F + (0.15F - age) * 3.0F : 1.0F;
            float alpha = age < 0.6F ? 1.0F : 1.0F - (age - 0.6F) / 0.4F;
            double rise = 1.2D * Math.sqrt(age);
            pose.pushPose();
            pose.translate(p.at().x - cam.x + p.drift() * age,
                    p.at().y - cam.y + rise, p.at().z - cam.z);
            pose.mulPose(event.getCamera().rotation());
            float scale = 0.035F * p.size() * pop;
            pose.scale(-scale, -scale, scale);
            Matrix4f matrix = pose.last().pose();
            int a = Math.max(5, Math.round(alpha * 255.0F));
            font.drawInBatch(p.text(), -font.width(p.text()) / 2.0F, 0.0F,
                    (a << 24) | p.rgb(), true, matrix, buffers,
                    Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
        buffers.endBatch();
    }
}
