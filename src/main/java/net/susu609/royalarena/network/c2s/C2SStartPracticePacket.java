package net.susu609.royalarena.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.s2c.S2CQueueStatusPacket;
import net.susu609.royalarena.server.match.MatchManager;

import java.util.function.Supplier;

/** Starts a match against the bot, or ({@code watch}) a bot-versus-bot match to watch. */
public record C2SStartPracticePacket(boolean watch) {
    public C2SStartPracticePacket() {
        this(false);
    }

    public static void encode(C2SStartPracticePacket packet,
            FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.watch());
    }
    public static C2SStartPracticePacket decode(FriendlyByteBuf buffer) {
        return new C2SStartPracticePacket(buffer.readBoolean());
    }

    public static void handle(C2SStartPracticePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender == null)
            return;
        if (!(packet.watch() ? MatchManager.INSTANCE.startWatch(sender) :
                MatchManager.INSTANCE.startPractice(sender))) {
            ModNetwork.sendToPlayer(
                    sender,
                    new S2CQueueStatusPacket(false, 0, "Already in a match"));
        }
    }
}
