package net.susu609.royalarena.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.susu609.royalarena.client.sound.ClientSounds;
import net.susu609.royalarena.match.ArenaSounds;
import net.susu609.royalarena.client.hud.HudLayout.Rect;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.ArrayList;
import java.util.List;

/**
 * A crown popping up in the middle of the screen when a tower falls, then
 * flying into the crown counter of the side that took it.
 */
public final class CrownFlights {
    private static final long DURATION_MS = 1600L;
    /** Share of the flight spent growing in the middle of the screen. */
    private static final float POP = 0.35F;

    private record Flight(TeamSide taker, long start) {
    }

    private static final List<Flight> FLIGHTS = new ArrayList<>();

    private CrownFlights() {
    }

    /** {@code tower} was destroyed: its opponent earns a crown. */
    public static void towerFell(TowerId tower) {
        FLIGHTS.add(new Flight(tower.side().opponent(), Util.getMillis()));
    }

    public static void clear() {
        FLIGHTS.clear();
    }

    static void draw(GuiGraphics graphics, HudLayout layout) {
        long now = Util.getMillis();
        FLIGHTS.removeIf(f -> {
            if (now - f.start() <= DURATION_MS)
                return false;
            // The crown lands on its counter: higher for your own.
            ClientSounds.ui(ArenaSounds.CROWN, f.taker() ==
                    ClientMatchState.side() ? 1.3F : 0.8F, 1.0F);
            return true;
        });
        if (FLIGHTS.isEmpty())
            return;
        ItemStack crown = new ItemStack(Items.GOLDEN_HELMET);
        Rect middle = layout.banner();
        for (Flight f : FLIGHTS) {
            float t = (now - f.start()) / (float) DURATION_MS;
            Rect box = f.taker() == ClientMatchState.side() ?
                    layout.bottomCrowns() : layout.topCrowns();
            float x;
            float y;
            float size;
            if (t < POP) {
                // Grows with a little overshoot, then holds.
                float p = t / POP;
                float grow = p < 0.6F ? p / 0.6F * 1.2F :
                        1.2F - (p - 0.6F) / 0.4F * 0.2F;
                x = middle.centerX();
                y = middle.centerY();
                size = 4.0F * grow;
            } else {
                float p = (t - POP) / (1.0F - POP);
                float ease = p * p * (3.0F - 2.0F * p);
                x = Mth.lerp(ease, middle.centerX(), box.centerX());
                y = Mth.lerp(ease, middle.centerY(), box.centerY());
                size = Mth.lerp(ease, 4.0F, 1.0F);
            }
            float scale = size * layout.u() * 10.0F / 16.0F;
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(x, y, 300.0F);
            pose.scale(scale, scale, 1.0F);
            graphics.renderItem(crown, -8, -8);
            pose.popPose();
        }
    }
}
