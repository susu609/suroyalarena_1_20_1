package net.susu609.royalarena.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.client.render.AoeFlashRenderer;

import java.util.function.Supplier;

/** Area damage just landed: flash a red circle on the floor. */
public record S2CAoeFlashPacket(float x, float y, float z, float radius) {
    public static void encode(S2CAoeFlashPacket p, FriendlyByteBuf buffer) {
        buffer.writeFloat(p.x);
        buffer.writeFloat(p.y);
        buffer.writeFloat(p.z);
        buffer.writeFloat(p.radius);
    }

    public static S2CAoeFlashPacket decode(FriendlyByteBuf buffer) {
        return new S2CAoeFlashPacket(buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), Math.min(16.0F, buffer.readFloat()));
    }

    public static void handle(S2CAoeFlashPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> AoeFlashRenderer.add(packet));
    }
}
