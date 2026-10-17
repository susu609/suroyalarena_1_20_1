package net.susu609.royalarena.match;

import java.util.ArrayList;
import java.util.List;

/**
 * The timing of the result screen (docs/UI.md), in client ticks since it
 * opened: the arena dims and the name ribbons slide in, the crowns pop one
 * by one (the opponent's, then yours), the result word zooms in, then the
 * MVP line and the buttons appear. Pure Java so the order can be tested.
 *
 * <p>Crown slots are 0 = left, 1 = King (middle), 2 = right; a side earns
 * them left, right, then the King.</p>
 */
public final class ResultTimeline {
    public static final int RIBBON_TICKS = 6;
    public static final int FIRST_CROWN = 8;
    public static final int CROWN_GAP = 6;
    public static final int POP_TICKS = 4;
    public static final float POP_SCALE = 1.25F;
    public static final int WORD_DELAY = 4;
    public static final int WORD_TICKS = 6;
    public static final float WORD_SCALE = 1.35F;
    public static final int FINAL_DELAY = 12;
    public static final float DIM_START = 0.35F;
    public static final float DIM_END = 0.62F;
    /** Slots in the order a side earns them: left, right, King. */
    private static final int[] EARN_ORDER = {0, 2, 1};

    /** A sound to play when the animation passes a tick. */
    public enum CueKind { RIBBONS, CROWN, WORD, FINAL }

    /** {@code index} counts the crowns popped so far (for a rising pitch). */
    public record Cue(CueKind kind, int index) {
    }

    private final int mine;
    private final int theirs;

    /** {@code mine} and {@code theirs}: crowns of each side, 0 to 3. */
    public ResultTimeline(int mine, int theirs) {
        this.mine = Math.max(0, Math.min(3, mine));
        this.theirs = Math.max(0, Math.min(3, theirs));
    }

    /** Tick a crown slot pops, or -1 if that side did not earn it. */
    public int crownTick(boolean own, int slot) {
        int count = own ? mine : theirs;
        for (int i = 0; i < count; i++) {
            if (EARN_ORDER[i] == slot)
                return FIRST_CROWN + CROWN_GAP * (own ? theirs + i : i);
        }
        return -1;
    }

    public boolean crownShown(boolean own, int slot, float t) {
        int at = crownTick(own, slot);
        return at >= 0 && t >= at;
    }

    /** Scale of a crown slot: {@link #POP_SCALE} as it pops, back to 1. */
    public float crownScale(boolean own, int slot, float t) {
        int at = crownTick(own, slot);
        if (at < 0 || t < at)
            return 1.0F;
        return grow(POP_SCALE, (t - at) / POP_TICKS);
    }

    public int wordTick() {
        return FIRST_CROWN + CROWN_GAP * (mine + theirs) + WORD_DELAY;
    }

    public boolean wordShown(float t) {
        return t >= wordTick();
    }

    public float wordScale(float t) {
        return t < wordTick() ? 1.0F :
                grow(WORD_SCALE, (t - wordTick()) / WORD_TICKS);
    }

    public int finalTick() {
        return wordTick() + FINAL_DELAY;
    }

    /** MVP line and buttons. */
    public boolean finalShown(float t) {
        return t >= finalTick();
    }

    /** How far the name ribbons have slid in, 0 to 1. */
    public float ribbons(float t) {
        return clamp(t / RIBBON_TICKS);
    }

    /** Opacity of the dark layer over the arena. */
    public float dim(float t) {
        return DIM_START + (DIM_END - DIM_START) * ribbons(t);
    }

    /** Sounds between two moments ({@code from} exclusive, {@code to} inclusive). */
    public List<Cue> cues(float from, float to) {
        List<Cue> cues = new ArrayList<>();
        if (from < 0.0F && to >= 0.0F)
            cues.add(new Cue(CueKind.RIBBONS, 0));
        int popped = 0;
        for (int i = 0; i < theirs + mine; i++) {
            int at = FIRST_CROWN + CROWN_GAP * i;
            if (from < at && to >= at)
                cues.add(new Cue(CueKind.CROWN, popped));
            popped++;
        }
        if (from < wordTick() && to >= wordTick())
            cues.add(new Cue(CueKind.WORD, 0));
        if (from < finalTick() && to >= finalTick())
            cues.add(new Cue(CueKind.FINAL, 0));
        return cues;
    }

    /** From {@code peak} back to 1 as {@code progress} goes 0 to 1. */
    private static float grow(float peak, float progress) {
        return peak + (1.0F - peak) * clamp(progress);
    }

    private static float clamp(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }
}
