package net.susu609.royalarena.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.client.render.ProjectileRenderer;

import java.util.function.Supplier;

/**
 * A purely visual projectile: the client draws an arrow (or a fireball)
 * flying from one point to another in {@code ticks}, along an arc that rises
 * {@code arc} blocks at its middle. Damage is decided on the server.
 */
public record S2CProjectilePacket(Kind kind, float fromX, float fromY,
                                  float fromZ, float toX, float toY,
                                  float toZ, int ticks, float arc)
{
    public enum Kind {
        ARROW, FIREBALL
    }

    public static void encode(S2CProjectilePacket p, FriendlyByteBuf buffer) {
        buffer.writeEnum(p.kind);
        buffer.writeFloat(p.fromX);
        buffer.writeFloat(p.fromY);
        buffer.writeFloat(p.fromZ);
        buffer.writeFloat(p.toX);
        buffer.writeFloat(p.toY);
        buffer.writeFloat(p.toZ);
        buffer.writeVarInt(p.ticks);
        buffer.writeFloat(p.arc);
    }

    public static S2CProjectilePacket decode(FriendlyByteBuf buffer) {
        return new S2CProjectilePacket(buffer.readEnum(Kind.class),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                Math.max(1, Math.min(200, buffer.readVarInt())),
                buffer.readFloat());
    }

    public static void handle(S2CProjectilePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ProjectileRenderer.add(packet));
    }
}
