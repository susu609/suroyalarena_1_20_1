package net.susu609.royalarena.match;

/**
 * The vanilla sound events (ids under {@code minecraft:}) chosen for each
 * moment of a match (docs/SOUND_DESIGN.md, picked by ear on the listening
 * page). Mob hurt, death and attack sounds are in {@code card.MobSounds}.
 */
public final class ArenaSounds {
    private ArenaSounds() {
    }

    // Match rhythm (client, see SoundCues)
    public static final String MATCH_FOUND = "block.note_block.pling";
    public static final String COUNTDOWN = "block.note_block.hat";
    public static final String FIGHT = "item.goat_horn.sound.0";
    public static final String ELIXIR_RATE = "block.beacon.power_select";
    public static final String OVERTIME = "block.bell.resonate";
    public static final String FINAL_SECONDS = "block.note_block.hat";
    public static final String ELIXIR_FULL = "entity.experience_orb.pickup";
    public static final String BATTLE_MUSIC = "music_disc.pigstep";

    // Cards and the deck (client)
    public static final String CARD_REFUSED = "block.note_block.bass";
    public static final String DECK_PLACE = "item.armor.equip_chain";
    public static final String DECK_SAVED = "ui.toast.in";

    // On the field (server)
    public static final String TROOP_APPEARS = "block.respawn_anchor.charge";
    public static final String TOWER_HIT = "entity.iron_golem.damage";
    public static final String KING_HIT = "block.anvil.land";
    public static final String TOWER_CRACKING = "block.stone.break";
    public static final String CROWN = "entity.experience_orb.pickup";
    public static final String RAVAGER_ROAR = "entity.ravager.roar";
    public static final String CREEPER_HISS = "entity.creeper.primed";
    public static final String SLIME_SPLIT = "entity.slime.squish";
    public static final String MAGMA_SPLIT = "entity.magma_cube.squish";
    public static final String POISONED = "entity.puffer_fish.sting";
    public static final String FROSTED = "entity.player.hurt_freeze";
    public static final String BLAZE_IN_WATER = "entity.generic.extinguish_fire";
    public static final String VEX_CHARGE = "entity.vex.charge";
    public static final String GUARDIAN_LOCK = "block.conduit.ambient.short";

    // Cards still to come (docs/CARD_QUEUE.md)
    public static final String SHULKER = "entity.shulker.teleport";
    public static final String ENDER_PEARL = "entity.enderman.teleport";
    public static final String WARDEN_BOOM = "entity.warden.sonic_boom";

    /** Every id above, for the test against the 1.20.1 sound list. */
    public static final String[] ALL = {
            MATCH_FOUND, COUNTDOWN, FIGHT, ELIXIR_RATE, OVERTIME,
            FINAL_SECONDS, ELIXIR_FULL, BATTLE_MUSIC, CARD_REFUSED,
            DECK_PLACE, DECK_SAVED, TROOP_APPEARS, TOWER_HIT, KING_HIT,
            TOWER_CRACKING, CROWN, RAVAGER_ROAR, CREEPER_HISS, SLIME_SPLIT,
            MAGMA_SPLIT, POISONED, FROSTED, BLAZE_IN_WATER, VEX_CHARGE,
            GUARDIAN_LOCK, SHULKER, ENDER_PEARL, WARDEN_BOOM};

    /** A tower below this share of its health starts to crack. */
    public static final double CRACKING_AT = 0.25D;
    /** Seconds left (regulation or overtime) when the ticking starts. */
    public static final int FINAL_SECONDS_FROM = 10;
}
