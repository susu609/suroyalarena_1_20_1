package net.susu609.royalarena.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.client.render.AoeFlashRenderer;

import java.util.function.Supplier;

/**
 * Area damage (or healing, or freezing) just landed: flash a circle of
 * {@code rgb} on the floor.
 */
public record S2CAoeFlashPacket(float x, float y, float z, float radius,
                                int rgb)
{
    public static final int RED = 0xFF3030;
    public static final int YELLOW = 0xFFE040;
    public static final int GREEN = 0x40FF60;
    public static final int ICE = 0x80D8FF;
    public static final int POISON = 0x6A9A2A;
    public static final int WEB = 0xE8E8E8;
    public static final int SWIFT = 0x7CAFC6;

    public static void encode(S2CAoeFlashPacket p, FriendlyByteBuf buffer) {
        buffer.writeFloat(p.x);
        buffer.writeFloat(p.y);
        buffer.writeFloat(p.z);
        buffer.writeFloat(p.radius);
        buffer.writeInt(p.rgb);
    }

    public static S2CAoeFlashPacket decode(FriendlyByteBuf buffer) {
        return new S2CAoeFlashPacket(buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), Math.min(16.0F, buffer.readFloat()),
                buffer.readInt() & 0xFFFFFF);
    }

    public static void handle(S2CAoeFlashPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> AoeFlashRenderer.add(packet));
    }
}
