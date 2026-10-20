package net.susu609.royalarena.sound;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.sounds.SoundEvent;
import net.susu609.modlib.resource.RL;
import net.susu609.royalarena.RoyalArena;

/**
 * The mod's own shot, hit and spell sounds (scripts/sfx/compose_sfx.py,
 * sounds/sfx/*.ogg, a few variants each in sounds.json). The battle plays
 * them through S2CArenaSoundPacket, so they carry across the arena.
 */
public final class ArenaSfx {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
                    RoyalArena.MOD_ID);

    /** An archer's arrow: a low puff of air, like vanilla's bow. */
    public static final RegistryObject<SoundEvent> BOW_SHOT = sound("bow_shot");
    /** A tower's arrow: the bow pitched down, on a knock of stone. */
    public static final RegistryObject<SoundEvent> TOWER_SHOT = sound("tower_shot");
    /** The King's Arrow Storm: five arrows let go together. */
    public static final RegistryObject<SoundEvent> VOLLEY = sound("volley");
    /** A Pillager's bolt: two latch clicks and the release. */
    public static final RegistryObject<SoundEvent> CROSSBOW_SHOT = sound("crossbow_shot");
    /** An arrow into a troop: a thunk and the shaft's rattle. */
    public static final RegistryObject<SoundEvent> ARROW_HIT = sound("arrow_hit");
    /** An arrow or a blow on a tower's stone. */
    public static final RegistryObject<SoundEvent> STONE_HIT = sound("stone_hit");
    /** The King throws a potion, a snowball or a spell. */
    public static final RegistryObject<SoundEvent> SPELL_THROW = sound("spell_throw");
    public static final RegistryObject<SoundEvent> ZAP = sound("zap");
    /** Glass, powder snow and an amethyst ring. */
    public static final RegistryObject<SoundEvent> FREEZE_HIT = sound("freeze_hit");
    /** A bottle breaking, then bubbles. */
    public static final RegistryObject<SoundEvent> POISON_HIT = sound("poison_hit");
    /** Bubbles rising in the poison cloud, on each of its pulses. */
    public static final RegistryObject<SoundEvent> POISON_BUBBLE = sound("poison_bubble");

    private ArenaSfx() {
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                RL.of(RoyalArena.MOD_ID, name)));
    }
}
