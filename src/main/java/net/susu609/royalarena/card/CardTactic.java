package net.susu609.royalarena.card;

/**
 * How a card is meant to be played, written by hand per card (the
 * {@code "roles"} list in its JSON, mirrored by {@link BattleCard#tactics()};
 * a test keeps them equal). The deck screen filters the library by these,
 * next to the {@link CardRole}s worked out from the stats.
 */
public enum CardTactic {
    /** Pushes toward the enemy towers. */
    ATTACK("attack", "Attack"),
    /** Stops enemy pushes on its own half. */
    DEFENSE("defense", "Defense"),
    /** Helps other cards: heals, slows, freezes, covers from behind. */
    SUPPORT("support", "Support");

    private final String id;
    private final String label;

    CardTactic(String id, String label) {
        this.id = id;
        this.label = label;
    }

    /** The word used in the card JSON. */
    public String id() {
        return id;
    }

    public String label() {
        return label;
    }
}
