package net.susu609.royalarena.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.client.camera.ArenaCameraController;
import net.susu609.royalarena.client.camera.CameraShake;
import net.susu609.royalarena.client.hud.BattleHudOverlay;
import net.susu609.royalarena.client.hud.CrownFlights;
import net.susu609.royalarena.client.input.RoyalArenaKeyMappings;
import net.susu609.royalarena.client.render.AoeFlashRenderer;
import net.susu609.royalarena.client.render.BotDragRenderer;
import net.susu609.royalarena.client.render.BurnEffects;
import net.susu609.royalarena.client.render.AggroMarkers;
import net.susu609.royalarena.client.render.SandboxStatsRenderer;
import net.susu609.royalarena.client.hud.SandboxHud;
import net.susu609.royalarena.client.state.ClientSandbox;
import net.susu609.royalarena.network.c2s.C2SSandboxPacket;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.client.render.DamageNumbers;
import net.susu609.royalarena.client.render.DeployPreviewRenderer;
import net.susu609.royalarena.client.render.ProjectileRenderer;
import net.susu609.royalarena.client.render.SpellTargetHighlighter;
import net.susu609.royalarena.client.render.TroopEffects;
import net.susu609.royalarena.client.render.TroopStatus;
import net.susu609.royalarena.client.render.TroopVisuals;
import net.susu609.royalarena.client.render.WorldHealthBar;
import net.susu609.royalarena.client.render.entity.KingFigure;
import net.susu609.royalarena.client.screen.MatchingScreen;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.client.state.ClientQueueState;
import net.susu609.royalarena.config.ArenaClientConfig;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.c2s.C2SDeployCardPacket;
import net.susu609.royalarena.network.c2s.C2SWatchSpeedPacket;
import net.susu609.royalarena.client.sound.BattleSounds;
import net.susu609.royalarena.client.sound.ClientSounds;
import net.susu609.royalarena.match.ArenaSounds;

import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = RoyalArena.MOD_ID,
                        bus = Mod.EventBusSubscriber.Bus.FORGE,
                        value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        Minecraft mc = Minecraft.getInstance();
        ClientMatchState.tick();
        BattleSounds.tick();
        ProjectileRenderer.tick();
        SpellTargetHighlighter.tick();
        AggroMarkers.tick();
        AoeFlashRenderer.tick();
        BurnEffects.tick();
        BotDragRenderer.tick();
        TroopStatus.tick();
        DamageNumbers.tick();
        TroopEffects.tick();
        CameraShake.tick();

        while (RoyalArenaKeyMappings.OPEN_MATCHING.consumeClick()) {
            if (!ClientMatchState.active() && mc.screen == null)
                mc.setScreen(new MatchingScreen());
        }
        consumeCardKey(RoyalArenaKeyMappings.CARD_1, 0);
        consumeCardKey(RoyalArenaKeyMappings.CARD_2, 1);
        consumeCardKey(RoyalArenaKeyMappings.CARD_3, 2);
        consumeCardKey(RoyalArenaKeyMappings.CARD_4, 3);
        consumeCardKey(RoyalArenaKeyMappings.CARD_5, 4);
        consumeCardKey(RoyalArenaKeyMappings.CARD_6, 5);
        consumeCardKey(RoyalArenaKeyMappings.CARD_7, 6);
        consumeCardKey(RoyalArenaKeyMappings.CARD_8, 7);
        while (RoyalArenaKeyMappings.WATCH_SPEED.consumeClick()) {
            if (ClientMatchState.spectating())
                ModNetwork.sendToServer(new C2SWatchSpeedPacket());
        }
        while (RoyalArenaKeyMappings.SANDBOX_SIDE.consumeClick()) {
            if (ClientSandbox.active() && mc.screen == null)
                ClientSandbox.toggleSide();
        }
        while (RoyalArenaKeyMappings.SANDBOX_CARDS.consumeClick()) {
            if (ClientSandbox.active() && mc.screen == null)
                SandboxHud.openCards();
        }

        ArenaCameraController.tick();
    }
    private static void consumeCardKey(net.minecraft.client.KeyMapping mapping,
            int slot)
    {
        while (mapping.consumeClick()) {
            // In a sandbox the keys pick from the quick bar.
            if (ClientSandbox.active())
                ClientSandbox.selectQuick(slot);
            else
                ClientMatchState.selectSlot(slot);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ArenaCameraController.release();
        ProjectileRenderer.clear();
        KingFigure.clear();
        SpellTargetHighlighter.clear();
        AggroMarkers.clear();
        AoeFlashRenderer.clear();
        BurnEffects.clear();
        BotDragRenderer.clear();
        DamageNumbers.clear();
        CrownFlights.clear();
        ClientMatchState.reset();
        ClientQueueState.reset();
    }
    /** Optional match FOV from royalarena-client.toml (0 = game FOV). */
    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!ClientMatchState.active())
            return;
        double fov = ArenaClientConfig.FOV.get();
        if (fov > 0.0D)
            event.setFOV(fov);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        DeployPreviewRenderer.render(event);
        ProjectileRenderer.render(event);
        AoeFlashRenderer.render(event);
        BotDragRenderer.render(event);
        TroopEffects.render(event);
        DamageNumbers.render(event);
        AggroMarkers.render(event);
        SandboxStatsRenderer.render(event);
    }

    /** Camera shake on heavy hits (S2CShakePacket). */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        CameraShake.apply(event);
    }

    /**
     * Small troops (the three Skeletons, the four Spiders) drawn smaller, a
     * raging Zoglin bigger, and a troop a Shulker bullet lifted drawn higher.
     */
    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        float scale = TroopVisuals.scale(event.getEntity());
        double lift = TroopStatus.lift(event.getEntity());
        if (scale != 1.0F || lift > 0.0D) {
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(0.0D, lift, 0.0D);
            event.getPoseStack().scale(scale, scale, scale);
        }
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        float scale = TroopVisuals.scale(event.getEntity());
        if (scale != 1.0F || TroopStatus.lift(event.getEntity()) > 0.0D)
            event.getPoseStack().popPose();
        WorldHealthBar.renderTroop(
                event.getEntity(), event.getPoseStack(),
                event.getMultiBufferSource(), scale);
    }

    /** A left click in a sandbox: the panel first, then the arena. */
    private static void sandboxClick() {
        if (SandboxHud.click(ArenaCameraController.cursorGuiX(),
                ArenaCameraController.cursorGuiY()))
            return;
        BattleCard card = ClientSandbox.card();
        if (card == null)
            return;
        BlockPos target = ArenaCameraController.projectCursorToArena();
        if (target != null && ClientMatchState.canDeploySelected(
                target.getX(), target.getZ())) {
            // The pick stays, to play it again (a right click drops it).
            ModNetwork.sendToServer(C2SSandboxPacket.deploy(card.id(),
                    ClientSandbox.side(), target));
        } else {
            ClientSounds.ui(ArenaSounds.CARD_REFUSED, 0.8F, 1.0F);
        }
    }

    @SubscribeEvent
    public static void onMouse(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || !ClientMatchState.active() ||
                ClientMatchState.spectating() ||
                ClientMatchState.state() != MatchState.BATTLE)
            return;
        if (event.getAction() != GLFW.GLFW_PRESS)
            return;

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            ClientMatchState.clearSelection();
            event.setCanceled(true);
            return;
        }
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT)
            return;

        if (ClientSandbox.active()) {
            sandboxClick();
            event.setCanceled(true);
            return;
        }
        int clickedSlot = BattleHudOverlay.cardSlotAt(
                ArenaCameraController.cursorGuiX(),
                ArenaCameraController.cursorGuiY(),
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight());
        if (clickedSlot >= 0) {
            ClientMatchState.selectSlot(clickedSlot);
            event.setCanceled(true);
            return;
        }
        int selectedSlot = ClientMatchState.selectedSlot();
        if (selectedSlot >= 0) {
            BlockPos target = ArenaCameraController.projectCursorToArena();
            // Red tiles (enemy half, river, outside, not enough elixir) are
            // not sent; the card stays selected.
            if (target != null && ClientMatchState.canDeployAt(
                    selectedSlot, target.getX(), target.getZ()))
            {
                ModNetwork.sendToServer(
                        new C2SDeployCardPacket(selectedSlot, target));
                ClientMatchState.clearSelection();
            } else {
                ClientSounds.ui(ArenaSounds.CARD_REFUSED, 0.8F, 1.0F);
            }
        }
        event.setCanceled(true);
    }
}
