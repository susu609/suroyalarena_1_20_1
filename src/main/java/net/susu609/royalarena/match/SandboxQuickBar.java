package net.susu609.royalarena.match;

import net.susu609.royalarena.card.BattleCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The sandbox's quick bar (docs/SANDBOX.md): up to {@link #SIZE} cards, keys
 * 1 to 8. Pinned cards come first, in the order they were pinned, and stay;
 * the rest are the cards picked last, newest first.
 */
public final class SandboxQuickBar {
    public static final int SIZE = 8;

    private final List<BattleCard> pinned = new ArrayList<>();
    private final List<BattleCard> recent = new ArrayList<>();

    /** A card was picked: it goes to the front of the recent ones. */
    public void use(BattleCard card) {
        if (card == null || pinned.contains(card))
            return;
        recent.remove(card);
        recent.add(0, card);
        trim();
    }

    /** Pins a card (it stays on the bar), or unpins it. */
    public void togglePin(BattleCard card) {
        if (card == null)
            return;
        if (pinned.remove(card)) {
            recent.add(0, card);
        } else {
            if (pinned.size() >= SIZE)
                return;
            recent.remove(card);
            pinned.add(card);
        }
        trim();
    }

    public boolean pinned(BattleCard card) {
        return pinned.contains(card);
    }

    /** The bar, slot 0 first: pinned, then recent. */
    public List<BattleCard> cards() {
        List<BattleCard> out = new ArrayList<>(pinned);
        out.addAll(recent);
        return Collections.unmodifiableList(out);
    }

    /** The card in {@code slot}, or null for an empty slot. */
    public BattleCard at(int slot) {
        List<BattleCard> cards = cards();
        return slot >= 0 && slot < cards.size() ? cards.get(slot) : null;
    }

    /** The pinned cards' ids, to save between games. */
    public List<String> pinnedIds() {
        return pinned.stream().map(BattleCard::id).toList();
    }

    /** Pins these card ids (unknown ones are skipped). */
    public void loadPinned(List<String> ids) {
        Set<BattleCard> seen = new HashSet<>();
        for (String id : ids) {
            BattleCard card;
            try {
                card = BattleCard.byId(id.trim());
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (seen.add(card) && pinned.size() < SIZE) {
                recent.remove(card);
                pinned.add(card);
            }
        }
        trim();
    }

    private void trim() {
        while (pinned.size() + recent.size() > SIZE && !recent.isEmpty())
            recent.remove(recent.size() - 1);
    }
}
