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
    MELEE("Melee"),
    /** Does not fight: heals, draws fire, makes or calls others. */
    SUPPORT("Support"),
    /** A building: stands still and lives 30 s. */
    BUILDING("Building");

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
        if (card.isBuilding())
            roles.add(BUILDING);
        // A card that never hits (a Parrot, a Beacon, a Sheep) is a support
        // card, not a melee or ranged one: the deck needs real attackers.
        if (!fights(card)) {
            roles.add(SUPPORT);
            return roles;
        }
        roles.add(s.melee() ? MELEE : RANGED);
        if (s.count() >= 3)
            roles.add(SWARM);
        if (s.traits().splash() || s.traits().lines())
            roles.add(AREA);
        if (s.traits().buildingsOnly())
            roles.add(BUILDINGS);
        return roles;
    }

    /** Whether a troop card hits anything (a support card does not). */
    private static boolean fights(BattleCard card) {
        return card.stats().damage() > 0.0D && !card.peaceful() &&
                !card.followsTroops();
    }

    /**
     * Health of one body plus every child it splits into, counting armour
     * as the cost formula does (health / armour factor).
     */
    public static double bodyHealth(TroopStats s) {
        SplitRule split = s.split();
        if (split == null)
            return s.effectiveHealth();
        if (split.into() != null)
            return s.effectiveHealth() + split.into().effectiveHealth();
        double total = 0.0D;
        double health = s.effectiveHealth();
        int bodies = 1;
        for (int g = 0; g <= split.generations(); g++) {
            total += health * bodies;
            health *= split.healthFactor();
            bodies *= split.children();
        }
        return total;
    }
}
