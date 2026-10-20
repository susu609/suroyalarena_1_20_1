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
        // An End Crystal is no mob: it has no sounds of its own.
        if (path.equals("end_crystal"))
            return "entity.generic.hurt";
        return "entity." + path + ".hurt" + small(path, tiny) +
                land(path);
    }

    /** Sound when the mob dies. */
    public static String death(String entityId, boolean tiny) {
        String path = mob(entityId);
        if (path.equals("end_crystal"))
            return "entity.generic.explode";
        return "entity." + path + ".death" + small(path, tiny) +
                land(path);
    }

    /**
     * The mob's idle sound (vanilla's ambient), or null for a mob that has
     * none (Creeper, Iron Golem, slimes, End Crystal). Vanilla still plays
     * it now and then on its own; this is for when a troop appears.
     */
    public static String ambient(String entityId) {
        String path = mob(entityId);
        return switch (path) {
            case "creeper", "iron_golem", "slime", "magma_cube", "end_crystal" ->
                    null;
            case "bee" -> "entity.bee.loop";
            case "guardian" -> "entity.guardian.ambient_land";
            case "turtle" -> "entity.turtle.ambient_land";
            case "axolotl" -> "entity.axolotl.idle_air";
            case "allay" -> "entity.allay.ambient_without_item";
            default -> "entity." + path + ".ambient";
        };
    }

    /**
     * What a troop says when it is summoned: its idle sound, or what vanilla
     * plays when that mob comes into the world (the Warden digs out, the
     * Shulker opens, a slime lands); null for none.
     */
    public static String summon(String entityId, boolean tiny) {
        String path = mob(entityId);
        return switch (path) {
            case "warden" -> "entity.warden.emerge";
            case "shulker" -> "entity.shulker.open";
            case "slime", "magma_cube" ->
                    "entity." + path + ".squish" + small(path, tiny);
            case "iron_golem" -> "entity.iron_golem.repair";
            case "creeper" -> "entity.creeper.primed";
            default -> ambient(entityId);
        };
    }

    /**
     * The cheer of a troop whose side won: vanilla's raid celebration for
     * illagers, witches and piglins, the idle sound for the rest.
     */
    public static String celebrate(String entityId) {
        String path = mob(entityId);
        return switch (path) {
            case "pillager", "vindicator", "evoker", "ravager", "witch",
                    "piglin" -> "entity." + path + ".celebrate";
            default -> ambient(entityId);
        };
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
            case "cat" -> "entity.cat.hiss";
            case "frog" -> "entity.frog.tongue";
            case "axolotl" -> "entity.axolotl.attack";
            default -> null;
        };
    }
}
