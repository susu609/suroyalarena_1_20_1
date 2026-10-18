package net.susu609.royalarena.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.match.WatchSetup;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.s2c.S2CQueueStatusPacket;
import net.susu609.royalarena.server.match.MatchManager;

import java.util.function.Supplier;

/**
 * Starts a match against the bot, or ({@code watch} set) a bot-versus-bot
 * match to watch with those decks and personalities.
 */
public record C2SStartPracticePacket(WatchSetup watch) {
    public C2SStartPracticePacket() {
        this(null);
    }

    public static void encode(C2SStartPracticePacket packet,
            FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.watch() != null);
        if (packet.watch() != null) {
            buffer.writeUtf(packet.watch().blueDeck(), 16);
            buffer.writeUtf(packet.watch().redDeck(), 16);
            buffer.writeUtf(packet.watch().blueStyle(), 32);
            buffer.writeUtf(packet.watch().redStyle(), 32);
        }
    }
    public static C2SStartPracticePacket decode(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean())
            return new C2SStartPracticePacket();
        return new C2SStartPracticePacket(new WatchSetup(buffer.readUtf(16),
                buffer.readUtf(16), buffer.readUtf(32), buffer.readUtf(32)));
    }

    public static void handle(C2SStartPracticePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender == null)
            return;
        if (!(packet.watch() != null ?
                MatchManager.INSTANCE.startWatch(sender, packet.watch()) :
                MatchManager.INSTANCE.startPractice(sender))) {
            ModNetwork.sendToPlayer(
                    sender,
                    new S2CQueueStatusPacket(false, 0, "Already in a match"));
        }
    }
}
