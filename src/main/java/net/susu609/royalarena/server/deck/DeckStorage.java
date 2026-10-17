package net.susu609.royalarena.server.deck;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.card.DeckRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Each player's deck, saved with the world (data/royalarena_decks.dat in the
 * overworld folder). A player without a saved deck, or with one that no
 * longer passes {@link DeckRules} (a card was removed from the mod), plays
 * the starter deck.
 */
public final class DeckStorage extends SavedData {
    private static final String NAME = "royalarena_decks";

    private final Map<UUID, List<String>> decks = new HashMap<>();

    public static DeckStorage get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                DeckStorage::load, DeckStorage::new, NAME);
    }

    /** The player's deck, or the starter deck. */
    public List<BattleCard> deckOf(UUID player) {
        List<String> ids = decks.get(player);
        List<BattleCard> cards = ids == null ? null : DeckRules.parse(ids);
        if (cards == null || DeckRules.problem(cards) != null)
            return BattleDeck.starter().cards();
        return cards;
    }

    /** Saves a deck that passed {@link DeckRules#problem}. */
    public void setDeck(UUID player, List<BattleCard> cards) {
        if (DeckRules.problem(cards) != null)
            throw new IllegalArgumentException("invalid deck");
        decks.put(player, DeckRules.ids(cards));
        setDirty();
    }

    private static DeckStorage load(CompoundTag tag) {
        DeckStorage storage = new DeckStorage();
        CompoundTag players = tag.getCompound("players");
        for (String key : players.getAllKeys()) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ListTag list = players.getList(key, Tag.TAG_STRING);
            List<String> ids = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++)
                ids.add(list.getString(i));
            storage.decks.put(id, ids);
        }
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag players = new CompoundTag();
        for (Map.Entry<UUID, List<String>> entry : decks.entrySet()) {
            ListTag list = new ListTag();
            for (String id : entry.getValue())
                list.add(StringTag.valueOf(id));
            players.put(entry.getKey().toString(), list);
        }
        tag.put("players", players);
        return tag;
    }
}
