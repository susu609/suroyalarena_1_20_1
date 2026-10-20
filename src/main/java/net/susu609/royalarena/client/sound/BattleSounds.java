package net.susu609.royalarena.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.susu609.modlib.resource.RL;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.config.ArenaClientConfig;
import net.susu609.royalarena.match.ArenaSounds;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.MusicPlan;
import net.susu609.royalarena.match.SoundCues;

/**
 * The match's rhythm in sound (docs/SOUND_DESIGN.md): once per client tick
 * it compares the match now with the last tick ({@link SoundCues}) and plays
 * the cues, and it keeps the battle music (Pigstep, looping, on the Music
 * volume slider) going while the battle runs.
 */
public final class BattleSounds {
    private static SoundCues.Frame last = SoundCues.Frame.NONE;
    private static FadingMusic music;
    /** Name of the track {@link #music} plays (a MusicPlan.Track or PIGSTEP). */
    private static String musicName;
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

    /**
     * Battle music while the battle runs; vanilla's own music waits. The
     * mod's own pieces follow the match ({@link MusicPlan}); with
     * {@code arenaMusic} off it is Pigstep all battle.
     */
    private static void music(SoundCues.Frame now) {
        Minecraft mc = Minecraft.getInstance();
        boolean enabled = now.active() && ArenaClientConfig.BATTLE_MUSIC.get();
        MusicPlan.Track plan = enabled ? MusicPlan.trackFor(now.state(),
                now.ticksRemaining(), now.overtime()) : null;
        boolean own = ArenaClientConfig.ARENA_MUSIC.get();
        // Pigstep plays through the battle only, not the countdown.
        if (plan != null && !own && now.state() != MatchState.BATTLE)
            plan = null;
        // One name per track: the plan's piece, or Pigstep.
        String wanted = plan == null ? null : own ? plan.name() : "PIGSTEP";
        if (music != null && !java.util.Objects.equals(wanted, musicName)) {
            // The phase changed (or the battle ended): fade out what plays.
            music.fadeOut();
            music = null;
            musicName = null;
        }
        if (wanted == null) {
            // Nothing plays: forget the last track too (a piece that played
            // once and ended is still remembered until now).
            music = null;
            musicName = null;
            return;
        }
        mc.getMusicManager().stopPlaying();
        musicAge++;
        boolean loops = !own || plan.loops();
        if (music != null) {
            // A piece that plays once and has ended is not restarted.
            if (!loops || musicAge < 40 ||
                    mc.getSoundManager().isActive(music))
                return;
        }
        SoundEvent track;
        if (own) {
            track = SoundEvent.createVariableRangeEvent(RL.of(RoyalArena.MOD_ID,
                    switch (plan) {
                        case PHASE1 -> ArenaSounds.PHASE1_MUSIC;
                        case RUSH -> ArenaSounds.RUSH_MUSIC;
                        case LAST_STAND -> ArenaSounds.LAST_STAND_MUSIC;
                    }));
        } else {
            track = ClientSounds.event(ArenaSounds.BATTLE_MUSIC);
            if (track == null)
                return;
        }
        music = new FadingMusic(track, loops);
        musicName = wanted;
        mc.getSoundManager().play(music);
        musicAge = 0;
    }
}
