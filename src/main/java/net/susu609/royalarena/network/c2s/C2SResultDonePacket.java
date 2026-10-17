package net.susu609.royalarena.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.server.match.MatchManager;

import java.util.function.Supplier;

/**
 * The player closed the result screen: OK sends them home, Play again also
 * starts a new match against the bot.
 */
public record C2SResultDonePacket(boolean again) {
    public static void encode(C2SResultDonePacket packet,
            FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.again);
    }

    public static C2SResultDonePacket decode(FriendlyByteBuf buffer) {
        return new C2SResultDonePacket(buffer.readBoolean());
    }

    public static void handle(C2SResultDonePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender != null)
            MatchManager.INSTANCE.resultDone(sender, packet.again());
    }
}
