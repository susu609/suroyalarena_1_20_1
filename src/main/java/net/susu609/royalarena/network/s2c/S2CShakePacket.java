package net.susu609.royalarena.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.client.camera.CameraShake;

import java.util.function.Supplier;

/**
 * Something heavy happened (TNT, Lightning, a Golem blow, a tower falling):
 * shake the arena camera with {@code strength} (about 1 for a falling
 * Princess tower).
 */
public record S2CShakePacket(float strength) {
    public static void encode(S2CShakePacket p, FriendlyByteBuf buffer) {
        buffer.writeFloat(p.strength);
    }

    public static S2CShakePacket decode(FriendlyByteBuf buffer) {
        return new S2CShakePacket(Math.max(0.0F, Math.min(3.0F,
                buffer.readFloat())));
    }

    public static void handle(S2CShakePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> CameraShake.add(packet.strength()));
    }
}
