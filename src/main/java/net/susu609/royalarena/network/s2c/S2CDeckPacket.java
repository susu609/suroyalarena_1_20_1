package net.susu609.royalarena.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.client.state.ClientDeckState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** The player's saved deck, with the result of a save (if any). */
public record S2CDeckPacket(List<String> cards, boolean ok, String message) {
    public static void encode(S2CDeckPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.cards.size());
        for (String id : packet.cards)
            buffer.writeUtf(id, 32);
        buffer.writeBoolean(packet.ok);
        buffer.writeUtf(packet.message, 128);
    }

    public static S2CDeckPacket decode(FriendlyByteBuf buffer) {
        int size = Math.min(buffer.readVarInt(), BattleDeck.DECK_SIZE + 1);
        List<String> cards = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
            cards.add(buffer.readUtf(32));
        return new S2CDeckPacket(List.copyOf(cards), buffer.readBoolean(),
                buffer.readUtf(128));
    }

    public static void handle(S2CDeckPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> () -> ClientDeckState.apply(packet));
    }
}
