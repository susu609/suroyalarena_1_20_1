package net.susu609.royalarena.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.s2c.S2CQueueStatusPacket;
import net.susu609.royalarena.server.match.MatchManager;

import java.util.function.Supplier;

public record C2SStartPracticePacket() {
    public static void encode(C2SStartPracticePacket packet,
            FriendlyByteBuf buffer)
    {
    }
    public static C2SStartPracticePacket decode(FriendlyByteBuf buffer) {
        return new C2SStartPracticePacket();
    }

    public static void handle(C2SStartPracticePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender == null)
            return;
        if (!MatchManager.INSTANCE.startPractice(sender)) {
            ModNetwork.sendToPlayer(
                    sender,
                    new S2CQueueStatusPacket(false, 0, "Already in a match"));
        }
    }
}
