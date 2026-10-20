package net.susu609.royalarena.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * A battle music track that can fade out: when the phase changes the old
 * piece fades over {@link #FADE_TICKS} while the next one starts on its
 * opening hit, instead of being cut.
 */
final class FadingMusic extends AbstractTickableSoundInstance {
    static final int FADE_TICKS = 20;
    private int fadeLeft = -1;

    FadingMusic(SoundEvent track, boolean loops) {
        super(track, SoundSource.MUSIC, RandomSource.create());
        this.looping = loops;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.relative = true;
    }

    /** Starts fading out; it stops itself when silent. */
    void fadeOut() {
        if (fadeLeft < 0)
            fadeLeft = FADE_TICKS;
    }

    @Override
    public void tick() {
        if (fadeLeft < 0)
            return;
        if (fadeLeft == 0) {
            stop();
            return;
        }
        fadeLeft--;
        volume = (float) fadeLeft / FADE_TICKS;
    }
}
