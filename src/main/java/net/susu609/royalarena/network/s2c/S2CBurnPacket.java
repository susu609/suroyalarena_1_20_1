package net.susu609.royalarena.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.client.render.BurnEffects;

import java.util.function.Supplier;

/** A troop (by network entity id) burns for {@code ticks} from now. */
public record S2CBurnPacket(int entityId, int ticks) {
    public static void encode(S2CBurnPacket p, FriendlyByteBuf buffer) {
        buffer.writeVarInt(p.entityId);
        buffer.writeVarInt(p.ticks);
    }

    public static S2CBurnPacket decode(FriendlyByteBuf buffer) {
        return new S2CBurnPacket(buffer.readVarInt(),
                Math.min(400, buffer.readVarInt()));
    }

    public static void handle(S2CBurnPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> BurnEffects.burn(packet));
    }
}
