package net.susu609.royalarena.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.server.match.MatchManager;

import java.util.function.Supplier;

/** The spectator of a bot-versus-bot match switches between x1 and x2. */
public record C2SWatchSpeedPacket() {
    public static void encode(C2SWatchSpeedPacket packet,
            FriendlyByteBuf buffer)
    {
    }
    public static C2SWatchSpeedPacket decode(FriendlyByteBuf buffer) {
        return new C2SWatchSpeedPacket();
    }

    public static void handle(C2SWatchSpeedPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null)
            MatchManager.INSTANCE.toggleWatchSpeed(sender);
    }
}
