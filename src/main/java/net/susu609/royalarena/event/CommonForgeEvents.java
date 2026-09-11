package net.susu609.royalarena.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.command.RoyalArenaCommand;
import net.susu609.royalarena.server.match.MatchManager;
import net.susu609.royalarena.server.match.MatchQueue;

@Mod.EventBusSubscriber(modid = RoyalArena.MOD_ID,
                        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommonForgeEvents {
    private CommonForgeEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        MatchQueue.INSTANCE.tick(event.getServer());
        MatchManager.INSTANCE.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MatchQueue.INSTANCE.remove(player.getUUID());
            MatchManager.INSTANCE.onPlayerLoggedOut(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        RoyalArenaCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MatchQueue.INSTANCE.clear();
        MatchManager.INSTANCE.shutdown(event.getServer());
    }
}
