package net.susu609.royalarena.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.card.DeckRules;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.s2c.S2CDeckPacket;
import net.susu609.royalarena.server.deck.DeckStorage;

import java.util.function.Supplier;

/** The deck screen opened: send the player their saved deck. */
public record C2SDeckRequestPacket() {
    public static void encode(C2SDeckRequestPacket packet,
            FriendlyByteBuf buffer)
    {
    }

    public static C2SDeckRequestPacket decode(FriendlyByteBuf buffer) {
        return new C2SDeckRequestPacket();
    }

    public static void handle(C2SDeckRequestPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null)
            return;
        var deck = DeckStorage.get(sender.server).deckOf(sender.getUUID());
        ModNetwork.sendToPlayer(sender,
                new S2CDeckPacket(DeckRules.ids(deck), true, ""));
    }
}
