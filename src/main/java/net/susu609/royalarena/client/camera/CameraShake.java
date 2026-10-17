package net.susu609.royalarena.client.camera;

import net.minecraftforge.client.event.ViewportEvent;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.config.ArenaClientConfig;

/**
 * Camera shake for heavy hits ({@code S2CShakePacket}): each one adds
 * "trauma" that dies away in well under a second; the camera turns by
 * trauma squared, so small knocks stay small and a falling King tower
 * really rattles. Applied to the view angles only, never to the camera
 * position the cursor is projected from.
 */
public final class CameraShake {
    private static final float MAX = 3.0F;
    /** Trauma lost per tick. */
    private static final float DECAY = 0.12F;
    /** Degrees of pitch and yaw at trauma 1. */
    private static final float DEGREES = 1.4F;

    private static float trauma;
    private static int ticks;

    private CameraShake() {
    }

    public static void add(float strength) {
        trauma = Math.min(MAX, trauma + strength);
    }

    public static void tick() {
        ticks++;
        trauma = Math.max(0.0F, trauma - DECAY);
        if (!ClientMatchState.active())
            trauma = 0.0F;
    }

    public static void apply(ViewportEvent.ComputeCameraAngles event) {
        double setting = ArenaClientConfig.CAMERA_SHAKE.get();
        if (trauma <= 0.0F || setting <= 0.0D || !ClientMatchState.active())
            return;
        float t = ticks + (float) event.getPartialTick();
        float amount = trauma * trauma * DEGREES * (float) setting;
        // Three unrelated wobbles, so the shake never looks like a loop.
        event.setPitch(event.getPitch() + amount *
                (float) Math.sin(t * 2.9D) * 0.8F);
        event.setYaw(event.getYaw() + amount *
                (float) Math.sin(t * 2.3D + 1.7D) * 0.6F);
        event.setRoll(event.getRoll() + amount *
                (float) Math.sin(t * 3.7D + 0.4D) * 0.4F);
    }
}
