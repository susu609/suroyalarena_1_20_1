package net.susu609.royalarena.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.client.hud.BattleHudOverlay;
import net.susu609.royalarena.client.input.RoyalArenaKeyMappings;
import net.susu609.royalarena.client.model.CrownTowerModel;
import net.susu609.royalarena.client.model.KingTowerModel;
import net.susu609.royalarena.client.render.entity.CrownTowerRenderer;
import net.susu609.royalarena.client.render.entity.KingTowerRenderer;
import net.susu609.royalarena.entity.ArenaEntityTypes;

@Mod.EventBusSubscriber(modid = RoyalArena.MOD_ID,
                        bus = Mod.EventBusSubscriber.Bus.MOD,
                        value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(RoyalArenaKeyMappings.OPEN_MATCHING);
        event.register(RoyalArenaKeyMappings.CARD_1);
        event.register(RoyalArenaKeyMappings.CARD_2);
        event.register(RoyalArenaKeyMappings.CARD_3);
        event.register(RoyalArenaKeyMappings.CARD_4);
    }

    @SubscribeEvent public static void registerEntityRenderers(
            EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(
                ArenaEntityTypes.TOWER_HITBOX.get(), NoopRenderer::new);
        event.registerEntityRenderer(
                ArenaEntityTypes.CROWN_TOWER.get(), CrownTowerRenderer::new);
        event.registerEntityRenderer(
                ArenaEntityTypes.KING_TOWER.get(), KingTowerRenderer::new);
    }

    @SubscribeEvent public static void registerLayerDefinitions(
            EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        event.registerLayerDefinition(
                CrownTowerModel.LAYER_LOCATION,
                CrownTowerModel::createBodyLayer);
        event.registerLayerDefinition(
                KingTowerModel.LAYER_LOCATION, KingTowerModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("battle_hud", BattleHudOverlay.INSTANCE);
    }
}
