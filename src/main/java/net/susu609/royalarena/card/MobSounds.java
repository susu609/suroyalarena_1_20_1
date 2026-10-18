package net.susu609.royalarena.card;

/**
 * The vanilla sound events a troop's mob makes when it is hurt, dies or
 * strikes (ids under {@code minecraft:}). Arena mobs have no AI and are
 * invulnerable, so vanilla never plays these itself; their idle and step
 * sounds still come from vanilla. A tiny slime (size 1, as vanilla's
 * {@code isTiny}) uses the small slime's sounds, a Guardian on land its land sounds, and a Cave
 * Spider the Spider's, as in vanilla.
 */
public final class MobSounds {
    private MobSounds() {
    }

    /** {@code minecraft:zombie} -> {@code zombie}; the Cave Spider -> spider. */
    private static String mob(String entityId) {
        String path = entityId.substring(entityId.indexOf(':') + 1);
        return path.equals("cave_spider") ? "spider" : path;
    }

    private static String small(String path, boolean tiny) {
        return tiny &&
                (path.equals("slime") || path.equals("magma_cube")) ?
                "_small" : "";
    }

    private static String land(String path) {
        return path.equals("guardian") ? "_land" : "";
    }

    /** Sound when the mob takes damage. */
    public static String hurt(String entityId, boolean tiny) {
        String path = mob(entityId);
        return "entity." + path + ".hurt" + small(path, tiny) +
                land(path);
    }

    /** Sound when the mob dies. */
    public static String death(String entityId, boolean tiny) {
        String path = mob(entityId);
        return "entity." + path + ".death" + small(path, tiny) +
                land(path);
    }

    /**
     * The mob's own sound for a melee blow, or null when vanilla has none
     * (most mobs: the victim's hurt sound is the hit).
     */
    public static String attack(String entityId) {
        return switch (mob(entityId)) {
            case "iron_golem" -> "entity.iron_golem.attack";
            case "ravager" -> "entity.ravager.attack";
            case "hoglin" -> "entity.hoglin.attack";
            case "slime" -> "entity.slime.attack";
            case "phantom" -> "entity.phantom.bite";
            case "polar_bear" -> "entity.polar_bear.warning";
            default -> null;
        };
    }
}
