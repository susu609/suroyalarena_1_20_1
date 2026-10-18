package net.susu609.royalarena.card;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Mob families that reward a deck for carrying several of their cards
 * (docs/TRIBES.md). A card belongs to one or two tribes
 * ({@link BattleCard#tribes()}); a tribe counts the different cards of the
 * deck that belong to it, and each of {@link #THRESHOLDS} reached adds its
 * step's bonus to every troop of that tribe on that side, and only to them.
 * Five steps, at 2 to 6 cards: small stat bonuses, one effect, and at 5 a
 * special rule (rising again, a totem, a last flame...). Spells belong to
 * no tribe.
 */
public enum Tribe {
    ZOMBIE("Zombie", "Xác sống",
            step(TribeBonus.health(1.05D), "+5% health"),
            step(TribeBonus.regen(0.15D), "heals 0.15 health a second"),
            step(TribeBonus.lifesteal(0.05D), "heals 5% of the damage it deals"),
            step(TribeBonus.revive(0.25D),
                    "Rise again: a fallen Zombie gets up with 25% health (once every 20 s for the side)"),
            step(TribeBonus.health(1.05D), "+5% health")),
    SKELETON("Skeleton", "Xương",
            step(TribeBonus.damage(1.05D), "+5% damage"),
            step(TribeBonus.attackSpeed(1.05D), "attacks 5% faster"),
            step(TribeBonus.piercing(), "hits ignore armour"),
            step(TribeBonus.extraShot(3),
                    "Volley: every 3rd shot also hits a second enemy for half"),
            step(TribeBonus.damage(1.05D), "+5% damage")),
    ILLAGER("Illager", "Illager",
            step(TribeBonus.damage(1.05D), "+5% damage"),
            step(TribeBonus.towerDamage(1.10D), "+10% damage to towers"),
            step(TribeBonus.speed(1.05D), "walks 5% faster"),
            step(TribeBonus.undying(),
                    "Totem of Undying: a killing blow leaves it at 1 health, unhurt for 0.5 s (once every 20 s for the side)"),
            step(TribeBonus.damage(1.05D), "+5% damage")),
    NETHER("Nether", "Nether",
            step(TribeBonus.health(1.05D), "+5% health"),
            step(TribeBonus.burn(40, 0.5D), "hits burn: 0.5 a second for 2 s"),
            step(TribeBonus.armor(1.0D), "+1 armour"),
            step(TribeBonus.deathBurn(60, 1.0D),
                    "Last flame: when it dies, enemies within 2 blocks burn 1 a second for 3 s"),
            step(TribeBonus.health(1.05D), "+5% health")),
    ARTHROPOD("Arthropod", "Chân đốt",
            step(TribeBonus.speed(1.10D), "walks 10% faster"),
            step(TribeBonus.poison(40, 0.5D), "hits poison: 0.5 a second for 2 s"),
            step(TribeBonus.attackSpeed(1.10D), "attacks 10% faster"),
            step(TribeBonus.slowOnHit(10),
                    "Silk: its hits slow the target's walk for 0.5 s"),
            step(TribeBonus.healthAndDamage(1.05D), "+5% health and damage")),
    BEAST("Beast", "Thú",
            step(TribeBonus.damage(1.05D), "+5% damage"),
            step(TribeBonus.lifesteal(0.10D), "heals 10% of the damage it deals"),
            step(TribeBonus.speed(1.05D), "walks 5% faster"),
            step(TribeBonus.pack(0.05D),
                    "Pack: +5% damage for each other Beast within 4 blocks (up to 3)"),
            step(TribeBonus.damage(1.05D), "+5% damage")),
    NIGHT("Night", "Quái đêm",
            step(TribeBonus.healthAndDamage(1.02D), "+2% health and damage"),
            step(TribeBonus.firstHitStun(2), "its first hit freezes the target for 0.1 s"),
            step(TribeBonus.armor(1.0D), "+1 armour"),
            step(TribeBonus.dodge(),
                    "Shadow: an arrow or shot that would hit it misses (once every 16 s for the side)"),
            step(TribeBonus.healthAndDamage(1.02D), "+2% health and damage")),
    GOLEM("Golem", "Golem",
            step(TribeBonus.armor(2.0D), "+2 armour")),
    SLIME("Slime", "Slime",
            step(TribeBonus.health(1.15D), "+15% health")),
    AQUATIC("Aquatic", "Biển",
            step(TribeBonus.riverDamage(1.25D), "+25% damage from the river"));

    /** Different cards of a tribe a deck needs for each step. */
    public static final int[] THRESHOLDS = {2, 3, 4, 5, 6};

    private final String label;
    private final String labelVi;
    private final Step[] steps;

    /** One step's bonus and how it reads on the deck screen. */
    public record Step(TribeBonus bonus, String text) {
    }

    private static Step step(TribeBonus bonus, String text) {
        return new Step(bonus, text);
    }

    Tribe(String label, String labelVi, Step... steps) {
        this.label = label;
        this.labelVi = labelVi;
        this.steps = steps;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String label() {
        return label;
    }

    /** The name on the Vietnamese pages (docs, card pages). */
    public String labelVi() {
        return labelVi;
    }

    /** The steps this tribe has, in threshold order (small tribes have one). */
    public List<Step> steps() {
        return List.of(steps);
    }

    /** Cards of this tribe, in card order. */
    public List<BattleCard> members() {
        List<BattleCard> out = new ArrayList<>();
        for (BattleCard card : BattleCard.values()) {
            if (card.tribes().contains(this))
                out.add(card);
        }
        return out;
    }

    /** Steps reached with {@code cards} different cards of the tribe. */
    public int level(int cards) {
        int level = 0;
        while (level < steps.length && cards >= THRESHOLDS[level])
            level++;
        return level;
    }

    /** Everything the reached steps give, together. */
    public TribeBonus bonus(int level) {
        TribeBonus total = TribeBonus.NONE;
        for (int i = 0; i < Math.min(level, steps.length); i++)
            total = total.plus(steps[i].bonus());
        return total;
    }

    /** How many different cards of each tribe the deck carries. */
    public static Map<Tribe, Integer> counts(List<BattleCard> deck) {
        Map<Tribe, Integer> counts = new EnumMap<>(Tribe.class);
        for (BattleCard card : deck.stream().distinct().toList()) {
            for (Tribe tribe : card.tribes())
                counts.merge(tribe, 1, Integer::sum);
        }
        return counts;
    }

    /** The tribes with at least one step reached, and their levels. */
    public static Map<Tribe, Integer> levels(List<BattleCard> deck) {
        Map<Tribe, Integer> levels = new EnumMap<>(Tribe.class);
        counts(deck).forEach((tribe, n) -> {
            int level = tribe.level(n);
            if (level > 0)
                levels.put(tribe, level);
        });
        return levels;
    }

    /**
     * One line for the deck screen: every tribe with 2+ cards, its count and
     * the next threshold, e.g. "Tribes: Zombie 4/6, Night 2/4" (empty when
     * none). {@code deck} may hold empty slots (null).
     */
    public static String summary(List<BattleCard> deck) {
        List<BattleCard> cards = deck.stream()
                .filter(java.util.Objects::nonNull).toList();
        StringBuilder out = new StringBuilder();
        counts(cards).forEach((tribe, n) -> {
            if (n < THRESHOLDS[0])
                return;
            int level = tribe.level(n);
            out.append(out.length() == 0 ? "Tribes: " : ", ")
                    .append(tribe.label()).append(' ').append(n);
            if (level < tribe.steps.length)
                out.append('/').append(THRESHOLDS[level]);
        });
        return out.toString();
    }

    /** What a card's tribes give it in a deck with these tribe levels. */
    public static TribeBonus bonusFor(BattleCard card, Map<Tribe, Integer> levels) {
        TribeBonus total = TribeBonus.NONE;
        if (card == null)
            return total;
        for (Tribe tribe : card.tribes())
            total = total.plus(tribe.bonus(levels.getOrDefault(tribe, 0)));
        return total;
    }
}
