package net.susu609.royalarena.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.susu609.modlib.resource.RL;

/** Plays a vanilla sound event by its id for this player only. */
public final class ClientSounds {
    private ClientSounds() {
    }

    public static SoundEvent event(String id) {
        return ForgeRegistries.SOUND_EVENTS.getValue(RL.mc(id));
    }

    /** A sound in the player's ears, like the menu sounds. */
    public static void ui(String id, float pitch, float volume) {
        SoundEvent sound = event(id);
        if (sound != null)
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    public static void ui(String id) {
        ui(id, 1.0F, 1.0F);
    }
}
