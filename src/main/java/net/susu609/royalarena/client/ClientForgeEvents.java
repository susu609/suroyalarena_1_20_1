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
import net.susu609.royalarena.client.hud.BattleHudOverlay;
import net.susu609.royalarena.client.input.RoyalArenaKeyMappings;
import net.susu609.royalarena.client.render.DeployPreviewRenderer;
import net.susu609.royalarena.client.render.AoeFlashRenderer;
import net.susu609.royalarena.client.render.BotDragRenderer;
import net.susu609.royalarena.client.render.BurnEffects;
import net.susu609.royalarena.client.render.ProjectileRenderer;
import net.susu609.royalarena.client.render.SpellTargetHighlighter;
import net.susu609.royalarena.client.render.TroopVisuals;
import net.susu609.royalarena.client.render.WorldHealthBar;
import net.susu609.royalarena.client.screen.MatchingScreen;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.client.state.ClientQueueState;
import net.susu609.royalarena.config.ArenaClientConfig;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.c2s.C2SDeployCardPacket;
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
        ProjectileRenderer.tick();
        SpellTargetHighlighter.tick();
        AoeFlashRenderer.tick();
        BurnEffects.tick();
        BotDragRenderer.tick();

        while (RoyalArenaKeyMappings.OPEN_MATCHING.consumeClick()) {
            if (!ClientMatchState.active() && mc.screen == null)
                mc.setScreen(new MatchingScreen());
        }
        consumeCardKey(RoyalArenaKeyMappings.CARD_1, 0);
        consumeCardKey(RoyalArenaKeyMappings.CARD_2, 1);
        consumeCardKey(RoyalArenaKeyMappings.CARD_3, 2);
        consumeCardKey(RoyalArenaKeyMappings.CARD_4, 3);

        ArenaCameraController.tick();
    }
    private static void consumeCardKey(net.minecraft.client.KeyMapping mapping,
            int slot)
    {
        while (mapping.consumeClick())
            ClientMatchState.selectSlot(slot);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ArenaCameraController.release();
        ProjectileRenderer.clear();
        SpellTargetHighlighter.clear();
        AoeFlashRenderer.clear();
        BurnEffects.clear();
        BotDragRenderer.clear();
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
    }

    /** Small troops (the three Skeletons, the four Spiders) drawn smaller. */
    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        float scale = TroopVisuals.scale(event.getEntity());
        if (scale != 1.0F) {
            event.getPoseStack().pushPose();
            event.getPoseStack().scale(scale, scale, scale);
        }
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        float scale = TroopVisuals.scale(event.getEntity());
        if (scale != 1.0F)
            event.getPoseStack().popPose();
        WorldHealthBar.renderTroop(
                event.getEntity(), event.getPoseStack(),
                event.getMultiBufferSource(), scale);
    }

    @SubscribeEvent
    public static void onMouse(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || !ClientMatchState.active() ||
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
            }
        }
        event.setCanceled(true);
    }
}
