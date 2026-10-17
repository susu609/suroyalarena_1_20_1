package net.susu609.royalarena.card;

import java.util.EnumSet;
import java.util.Set;

/**
 * What a card does in a deck, used to keep random decks balanced and shown
 * in the card library.
 */
public enum CardRole {
    /** Area damage played anywhere. */
    SPELL("Spell"),
    /** One sturdy body that soaks tower shots. */
    TANK("Tank"),
    /** Attacks from 4 blocks or more. */
    RANGED("Ranged"),
    /** Three or more bodies. */
    SWARM("Swarm"),
    /** Hits everything around its target. */
    AREA("Area damage"),
    /** Ignores troops and walks straight to the towers. */
    BUILDINGS("Targets towers"),
    /** Fights up close. */
    MELEE("Melee");

    /** Health of one body (with its split children) that makes it a tank. */
    public static final double TANK_HEALTH = 50.0D;

    private final String label;

    CardRole(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Set<CardRole> of(BattleCard card) {
        Set<CardRole> roles = EnumSet.noneOf(CardRole.class);
        if (card.isSpell()) {
            roles.add(SPELL);
            return roles;
        }
        TroopStats s = card.stats();
        if (s.count() == 1 && bodyHealth(s) >= TANK_HEALTH)
            roles.add(TANK);
        roles.add(s.melee() ? MELEE : RANGED);
        if (s.count() >= 3)
            roles.add(SWARM);
        if (s.traits().splash() || s.traits().lines())
            roles.add(AREA);
        if (s.traits().buildingsOnly())
            roles.add(BUILDINGS);
        return roles;
    }

    /** Health of one body plus every child it splits into. */
    public static double bodyHealth(TroopStats s) {
        SplitRule split = s.split();
        if (split == null)
            return s.health();
        double total = 0.0D;
        double health = s.health();
        int bodies = 1;
        for (int g = 0; g <= split.generations(); g++) {
            total += health * bodies;
            health *= split.healthFactor();
            bodies *= split.children();
        }
        return total;
    }
}
