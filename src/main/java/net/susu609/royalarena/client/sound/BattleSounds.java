package net.susu609.royalarena.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.susu609.modlib.resource.RL;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.config.ArenaClientConfig;
import net.susu609.royalarena.match.ArenaSounds;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.SoundCues;

/**
 * The match's rhythm in sound (docs/SOUND_DESIGN.md): once per client tick
 * it compares the match now with the last tick ({@link SoundCues}) and plays
 * the cues, and it keeps the battle music (Pigstep, looping, on the Music
 * volume slider) going while the battle runs.
 */
public final class BattleSounds {
    private static SoundCues.Frame last = SoundCues.Frame.NONE;
    private static SoundInstance music;
    /** Which track {@link #music} plays: true for the mod's own tune. */
    private static boolean musicIsOwn;
    /** Ticks since the music was started (a stream takes a moment to begin). */
    private static int musicAge;

    private BattleSounds() {
    }

    public static void tick() {
        SoundCues.Frame now = frame();
        for (SoundCues.Fired fired : SoundCues.between(last, now))
            play(fired);
        last = now;
        music(now);
    }

    private static SoundCues.Frame frame() {
        if (!ClientMatchState.active())
            return SoundCues.Frame.NONE;
        return new SoundCues.Frame(true, ClientMatchState.matchId(),
                ClientMatchState.state(), ClientMatchState.ticksRemaining(),
                ClientMatchState.elixirRate(), ClientMatchState.overtime(),
                ClientMatchState.elixirTenths());
    }

    private static void play(SoundCues.Fired fired) {
        switch (fired.cue()) {
            case MATCH_FOUND -> ClientSounds.ui(ArenaSounds.MATCH_FOUND);
            // 3, 2, 1 rising.
            case COUNTDOWN -> ClientSounds.ui(ArenaSounds.COUNTDOWN,
                    1.0F + 0.15F * (3 - Math.min(3, fired.seconds())), 1.0F);
            case FIGHT -> ClientSounds.ui(ArenaSounds.FIGHT, 1.0F, 0.8F);
            case ELIXIR_RATE -> ClientSounds.ui(ArenaSounds.ELIXIR_RATE);
            case OVERTIME -> ClientSounds.ui(ArenaSounds.OVERTIME);
            // The last ten seconds tick higher as they run out.
            case FINAL_SECOND -> ClientSounds.ui(ArenaSounds.FINAL_SECONDS,
                    0.8F + 0.06F * (10 - fired.seconds()), 0.9F);
            case ELIXIR_FULL -> ClientSounds.ui(ArenaSounds.ELIXIR_FULL,
                    1.3F, 0.6F);
        }
    }

    /** Battle music while the battle runs; vanilla's own music waits. */
    private static void music(SoundCues.Frame now) {
        Minecraft mc = Minecraft.getInstance();
        // The first phase (x1 elixir, from the countdown) has the mod's own
        // tune; once the elixir speeds up or overtime begins, Pigstep.
        boolean own = ArenaClientConfig.ARENA_MUSIC.get() &&
                now.elixirRate() == 1 && !now.overtime();
        boolean wanted = now.active() && ArenaClientConfig.BATTLE_MUSIC.get()
                && (now.state() == MatchState.BATTLE ||
                now.state() == MatchState.COUNTDOWN && own);
        if (music != null && wanted && own != musicIsOwn) {
            // The phase changed: the other track starts.
            mc.getSoundManager().stop(music);
            music = null;
        }
        if (!wanted) {
            if (music != null) {
                mc.getSoundManager().stop(music);
                music = null;
            }
            return;
        }
        mc.getMusicManager().stopPlaying();
        musicAge++;
        if (music != null &&
                (musicAge < 40 || mc.getSoundManager().isActive(music)))
            return;
        ResourceLocation track;
        if (own) {
            track = RL.of(RoyalArena.MOD_ID, ArenaSounds.PHASE1_MUSIC);
        } else {
            SoundEvent vanilla = ClientSounds.event(ArenaSounds.BATTLE_MUSIC);
            if (vanilla == null)
                return;
            track = vanilla.getLocation();
        }
        music = new SimpleSoundInstance(track, SoundSource.MUSIC,
                1.0F, 1.0F, RandomSource.create(), true, 0,
                SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true);
        musicIsOwn = own;
        mc.getSoundManager().play(music);
        musicAge = 0;
    }
}
