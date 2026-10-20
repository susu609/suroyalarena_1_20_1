package net.susu609.royalarena.sound;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.sounds.SoundEvent;
import net.susu609.modlib.resource.RL;
import net.susu609.royalarena.RoyalArena;

/**
 * The mod's own shot and spell-cast sounds (scripts/sfx/compose_sfx.py,
 * sounds/sfx/*.ogg). Played at volume {@link #RANGE_VOLUME}: a variable-range
 * sound carries volume times its 16 blocks, so the shot is heard across the
 * arena instead of only up close.
 */
public final class ArenaSfx {
    /** Volume 2.5 carries these sounds 40 blocks. */
    public static final float RANGE_VOLUME = 2.5F;

    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
                    RoyalArena.MOD_ID);

    /** An archer's or Pillager's arrow. */
    public static final RegistryObject<SoundEvent> BOW_SHOT = register("bow_shot");
    /** A tower's shot: heavier than a bow. */
    public static final RegistryObject<SoundEvent> TOWER_SHOT = register("tower_shot");
    /** The King's Arrow Storm launch: five arrows in a cluster. */
    public static final RegistryObject<SoundEvent> VOLLEY = register("volley");
    public static final RegistryObject<SoundEvent> ZAP_CAST = register("zap_cast");
    public static final RegistryObject<SoundEvent> FREEZE_CAST = register("freeze_cast");
    public static final RegistryObject<SoundEvent> POISON_CAST = register("poison_cast");

    private ArenaSfx() {
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                RL.of(RoyalArena.MOD_ID, name)));
    }
}
