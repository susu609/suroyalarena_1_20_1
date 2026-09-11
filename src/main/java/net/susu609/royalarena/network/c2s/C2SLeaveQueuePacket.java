package net.susu609.royalarena.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.server.match.MatchQueue;

import java.util.function.Supplier;

public record C2SLeaveQueuePacket() {
    public static void encode(C2SLeaveQueuePacket packet,
            FriendlyByteBuf buffer)
    {
    }
    public static C2SLeaveQueuePacket decode(FriendlyByteBuf buffer) {
        return new C2SLeaveQueuePacket();
    }

    public static void handle(C2SLeaveQueuePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null)
            MatchQueue.INSTANCE.leave(sender, "Cancelled");
    }
}
