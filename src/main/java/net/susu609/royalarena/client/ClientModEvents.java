package net.susu609.royalarena.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.client.hud.BattleHudOverlay;
import net.susu609.royalarena.client.input.RoyalArenaKeyMappings;

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

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("battle_hud", BattleHudOverlay.INSTANCE);
    }
}
