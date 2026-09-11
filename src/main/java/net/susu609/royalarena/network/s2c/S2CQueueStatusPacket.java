package net.susu609.royalarena.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.client.state.ClientQueueState;

import java.util.function.Supplier;

public record S2CQueueStatusPacket(boolean searching, int position,
                                   String message)
{
    public static void encode(S2CQueueStatusPacket packet,
            FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.searching);
        buffer.writeVarInt(packet.position);
        buffer.writeUtf(packet.message, 128);
    }

    public static S2CQueueStatusPacket decode(FriendlyByteBuf buffer) {
        return new S2CQueueStatusPacket(
                buffer.readBoolean(), buffer.readVarInt(), buffer.readUtf(128));
    }

    public static void handle(S2CQueueStatusPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> () -> ClientQueueState.apply(packet));
    }
}
