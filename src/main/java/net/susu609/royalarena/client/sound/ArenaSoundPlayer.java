package net.susu609.royalarena.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.susu609.royalarena.network.s2c.S2CArenaSoundPacket;

/**
 * Plays a battle sound sent by the server with the arena's own fall-off:
 * full volume within a few blocks, fading to silence at
 * {@link S2CArenaSoundPacket#RANGE} blocks. Vanilla's attenuation is off for
 * it (it would silence the sound at 16 blocks), but it keeps its position,
 * so a shot on the left is heard on the left.
 */
public final class ArenaSoundPlayer {
    /** Within this many blocks the sound plays at its full volume. */
    private static final double NEAR = 6.0D;

    private ArenaSoundPlayer() {
    }

    public static void play(S2CArenaSoundPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        Vec3 ear = mc.gameRenderer.getMainCamera().getPosition();
        double distance = Math.sqrt(ear.distanceToSqr(packet.x(), packet.y(),
                packet.z()));
        float volume = packet.volume() * falloff(distance);
        if (volume <= 0.01F)
            return;
        mc.getSoundManager().play(new SimpleSoundInstance(packet.sound(),
                packet.source(), volume, packet.pitch(), RandomSource.create(),
                false, 0, SoundInstance.Attenuation.NONE, packet.x(),
                packet.y(), packet.z(), false));
    }

    /** 1 up close, then a smooth fade to 0 at the range. */
    static float falloff(double distance) {
        if (distance <= NEAR)
            return 1.0F;
        double t = Math.min(1.0D, (distance - NEAR) /
                (S2CArenaSoundPacket.RANGE - NEAR));
        return (float) ((1.0D - t) * (1.0D - t * 0.5D));
    }
}
