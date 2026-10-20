package net.susu609.royalarena.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.client.sound.ArenaSoundPlayer;
import net.susu609.royalarena.network.ModNetwork;

import java.util.function.Supplier;

/**
 * A battle sound heard across the arena: vanilla's sounds fade out at 16
 * blocks, too close for a player watching the whole field, so the client
 * plays these itself with a fall-off over {@link #RANGE} blocks (still
 * placed left and right by where they happen).
 */
public record S2CArenaSoundPacket(ResourceLocation sound, SoundSource source,
                                  double x, double y, double z, float volume,
                                  float pitch)
{
    public static final double RANGE = 48.0D;

    /** Sends the sound to every player within {@link #RANGE} blocks. */
    public static void send(ServerLevel level, double x, double y, double z,
            SoundEvent sound, SoundSource source, float volume, float pitch)
    {
        ModNetwork.sendNear(level, x, y, z, RANGE, new S2CArenaSoundPacket(
                sound.getLocation(), source, x, y, z, volume, pitch));
    }

    public static void encode(S2CArenaSoundPacket p, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(p.sound);
        buffer.writeEnum(p.source);
        buffer.writeDouble(p.x);
        buffer.writeDouble(p.y);
        buffer.writeDouble(p.z);
        buffer.writeFloat(p.volume);
        buffer.writeFloat(p.pitch);
    }

    public static S2CArenaSoundPacket decode(FriendlyByteBuf buffer) {
        return new S2CArenaSoundPacket(buffer.readResourceLocation(),
                buffer.readEnum(SoundSource.class), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(),
                Math.max(0.0F, Math.min(1.0F, buffer.readFloat())),
                Math.max(0.5F, Math.min(2.0F, buffer.readFloat())));
    }

    public static void handle(S2CArenaSoundPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ArenaSoundPlayer.play(packet));
    }
}
