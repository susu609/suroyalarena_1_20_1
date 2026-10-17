package net.susu609.royalarena.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.card.DeckRules;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.s2c.S2CDeckPacket;
import net.susu609.royalarena.server.deck.DeckStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Save a deck. The server checks it again and answers with the deck it now
 * holds, so the client always shows what was really saved.
 */
public record C2SSaveDeckPacket(List<String> cards) {
    public static void encode(C2SSaveDeckPacket packet,
            FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.cards.size());
        for (String id : packet.cards)
            buffer.writeUtf(id, 32);
    }

    public static C2SSaveDeckPacket decode(FriendlyByteBuf buffer) {
        int size = Math.min(buffer.readVarInt(), BattleDeck.DECK_SIZE + 1);
        List<String> cards = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
            cards.add(buffer.readUtf(32));
        return new C2SSaveDeckPacket(List.copyOf(cards));
    }

    public static void handle(C2SSaveDeckPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null)
            return;
        DeckStorage storage = DeckStorage.get(sender.server);
        List<BattleCard> cards = DeckRules.parse(packet.cards);
        String problem = cards == null ? "Unknown card" :
                DeckRules.problem(cards);
        if (problem == null)
            storage.setDeck(sender.getUUID(), cards);
        ModNetwork.sendToPlayer(sender, new S2CDeckPacket(
                DeckRules.ids(storage.deckOf(sender.getUUID())),
                problem == null,
                problem == null ? "Deck saved" : problem));
    }
}
