package net.susu609.royalarena.card;

/**
 * What a troop is made of, for poison: living flesh takes it hard, the
 * undead and spirits (Zombie, Skeleton, Blaze, Vex) take it as it comes, and
 * a construct (the golems) takes none. Mirrors {@code body} in the card's
 * JSON ({@link BattleCard#body()}).
 */
public enum Body {
    LIVING("living", 1.5D, "Living"),
    UNDEAD("undead", 1.0D, "Undead or spirit"),
    CONSTRUCT("construct", 0.0D, "Construct");

    private final String id;
    private final double poisonFactor;
    private final String label;

    Body(String id, double poisonFactor, String label) {
        this.id = id;
        this.poisonFactor = poisonFactor;
        this.label = label;
    }

    /** The word used in the card JSON. */
    public String id() {
        return id;
    }

    /** Poison damage taken, as a multiple of the poison's own damage. */
    public double poisonFactor() {
        return poisonFactor;
    }

    public String label() {
        return label;
    }

    public boolean poisonable() {
        return poisonFactor > 0.0D;
    }
}
