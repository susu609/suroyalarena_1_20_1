package net.susu609.royalarena.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.client.render.TroopStatus;

import java.util.function.Supplier;

/**
 * A troop's state bits ({@code combat.TroopFlags}) for the clients to draw:
 * sent when they change, and now and then again for clients that came
 * into view later.
 */
public record S2CTroopStatePacket(int entityId, int flags) {
    public static void encode(S2CTroopStatePacket p, FriendlyByteBuf buffer) {
        buffer.writeVarInt(p.entityId);
        buffer.writeVarInt(p.flags);
    }

    public static S2CTroopStatePacket decode(FriendlyByteBuf buffer) {
        return new S2CTroopStatePacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(S2CTroopStatePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> TroopStatus.set(packet.entityId(), packet.flags()));
    }
}
