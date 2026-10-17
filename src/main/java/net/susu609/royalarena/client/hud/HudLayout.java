package net.susu609.royalarena.client.hud;

/**
 * Battle HUD geometry in reference units measured from a 200x356 portrait
 * Clash Royale screenshot. One unit {@code u} is screenHeight / 356, and the
 * 200-unit-wide reference column is centred horizontally, so the layout keeps
 * the original proportions at any resolution. Pure Java (no Minecraft
 * imports) so the smoke test can check it.
 */
public final class HudLayout {
    public static final float REF_WIDTH = 200.0F;
    public static final float REF_HEIGHT = 356.0F;

    public static final float PANEL_TOP = 290.0F;
    public static final float CARD_LEFT = 47.0F;
    public static final float CARD_PITCH = 37.5F;
    public static final float CARD_TOP = 297.0F;
    public static final float CARD_W = 31.0F;
    public static final float CARD_H = 39.0F;
    /** How far a card rises when hovered / selected. */
    public static final float HOVER_LIFT = 3.0F;
    public static final float SELECT_LIFT = 8.0F;
    /** Time constant of the card lift animation, in milliseconds. */
    public static final float LIFT_MILLIS = 45.0F;
    /** Cost badge size, and how far it sticks out over the card corner. */
    public static final float COST_SIZE = 11.0F;
    public static final float COST_OVERHANG = 2.5F;

    private final float u;
    private final float originX;

    private HudLayout(float u, float originX) {
        this.u = u;
        this.originX = originX;
    }

    public static HudLayout of(int screenWidth, int screenHeight) {
        float u = screenHeight / REF_HEIGHT;
        return new HudLayout(u, (screenWidth - REF_WIDTH * u) / 2.0F);
    }

    public float u() {
        return u;
    }

    /** Converts a reference rectangle to screen pixels. */
    public Rect ref(float x, float y, float w, float h) {
        int left = Math.round(originX + x * u);
        int top = Math.round(y * u);
        int right = Math.round(originX + (x + w) * u);
        int bottom = Math.round((y + h) * u);
        return new Rect(left, top, right - left, bottom - top);
    }

    /** Wooden control panel behind the hand and elixir bar. */
    public Rect panel() {
        return ref(37.0F, PANEL_TOP, REF_WIDTH - 37.0F, REF_HEIGHT - PANEL_TOP);
    }
    public Rect card(int slot) {
        return card(slot, 0.0F);
    }
    /** Card raised by {@code lift} units. */
    public Rect card(int slot, float lift) {
        return ref(CARD_LEFT + slot * CARD_PITCH, CARD_TOP - lift, CARD_W,
                CARD_H);
    }
    /**
     * Cost badge on the top-left corner of a card raised by {@code lift},
     * sticking out a little over the corner so it moves with the card.
     */
    public Rect cardCost(int slot, float lift) {
        return ref(CARD_LEFT + slot * CARD_PITCH - COST_OVERHANG,
                CARD_TOP - lift - COST_OVERHANG, COST_SIZE, COST_SIZE);
    }

    /**
     * One animation step of a card's lift toward {@code target}: eases out
     * over about {@link #LIFT_MILLIS} ms, never overshoots, and snaps when it
     * is within a hundredth of a unit.
     */
    public static float approachLift(float current, float target,
            float elapsedMillis)
    {
        if (elapsedMillis <= 0.0F)
            return current;
        float k = 1.0F - (float) Math.exp(-elapsedMillis / LIFT_MILLIS);
        float next = current + (target - current) * k;
        return Math.abs(target - next) < 0.01F ? target : next;
    }
    /** Clickable area: the card plus the space it rises into. */
    public Rect cardHitArea(int slot) {
        return ref(CARD_LEFT + slot * CARD_PITCH, CARD_TOP - SELECT_LIFT,
                CARD_W, CARD_H + SELECT_LIFT);
    }
    public Rect nextLabel() {
        return ref(6.0F, 314.0F, 28.0F, 8.0F);
    }
    /** Next card, about 58% of a hand card. */
    public Rect nextCard() {
        return ref(10.0F, 324.0F, 18.0F, 23.0F);
    }
    public Rect elixirBar() {
        return ref(41.0F, 341.0F, 153.0F, 8.0F);
    }
    public Rect elixirDrop() {
        return ref(36.0F, 336.0F, 16.0F, 17.0F);
    }
    public Rect timerBox() {
        return ref(164.0F, 1.0F, 35.0F, 15.0F);
    }
    /** Crown counter of the side shown at the top (the opponent). */
    public Rect topCrowns() {
        return ref(186.0F, 116.0F, 13.0F, 15.0F);
    }
    /** Crown counter of the local side, below the river. */
    public Rect bottomCrowns() {
        return ref(186.0F, 182.0F, 13.0F, 15.0F);
    }
    public Rect banner() {
        return ref(40.0F, 130.0F, 120.0F, 30.0F);
    }

    /** Hand slot under the cursor, or -1. */
    public int cardSlotAt(double x, double y) {
        for (int slot = 0; slot < 4; slot++) {
            if (cardHitArea(slot).contains(x, y))
                return slot;
        }
        return -1;
    }

    public record Rect(int x, int y, int w, int h) {
        public int right() {
            return x + w;
        }
        public int bottom() {
            return y + h;
        }
        public int centerX() {
            return x + w / 2;
        }
        public int centerY() {
            return y + h / 2;
        }
        public boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }
}
