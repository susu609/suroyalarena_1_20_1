package net.susu609.royalarena.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.server.match.MatchQueue;

import java.util.function.Supplier;

public record C2SJoinQueuePacket() {
    public static void encode(C2SJoinQueuePacket packet, FriendlyByteBuf buffer)
    {
    }
    public static C2SJoinQueuePacket decode(FriendlyByteBuf buffer) {
        return new C2SJoinQueuePacket();
    }

    public static void handle(C2SJoinQueuePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null)
            MatchQueue.INSTANCE.join(sender);
    }
}
