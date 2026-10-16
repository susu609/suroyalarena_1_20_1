package net.susu609.royalarena.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BattleDeck {
    public static final int DECK_SIZE = 8;
    public static final int HAND_SIZE = 4;

    private final ArrayList<BattleCard> cycle;

    public BattleDeck(List<BattleCard> cards) {
        if (cards.size() != DECK_SIZE) {
            throw new IllegalArgumentException(
                    "Battle deck must contain exactly 8 cards");
        }
        this.cycle = new ArrayList<>(cards);
    }

    public static BattleDeck starter() {
        return new BattleDeck(List.of(
                BattleCard.ZOMBIE, BattleCard.SKELETON_ARCHER,
                BattleCard.SPIDER, BattleCard.SKELETONS,
                BattleCard.BIG_SLIME, BattleCard.STRAY,
                BattleCard.VINDICATOR, BattleCard.IRON_GOLEM));
    }

    public List<BattleCard> hand() {
        return Collections.unmodifiableList(cycle.subList(0, HAND_SIZE));
    }

    public BattleCard nextCard() {
        return cycle.get(HAND_SIZE);
    }

    public BattleCard cardAt(int slot) {
        if (slot < 0 || slot >= HAND_SIZE) {
            throw new IllegalArgumentException("Card slot must be 0..3");
        }
        return cycle.get(slot);
    }

    public BattleCard play(int slot) {
        BattleCard played = cardAt(slot);
        cycle.remove(slot);
        cycle.add(played);
        return played;
    }

    public List<BattleCard> snapshotCycle() {
        return List.copyOf(cycle);
    }
}
