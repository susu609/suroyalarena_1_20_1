package net.susu609.royalarena.network.c2s;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.server.match.MatchManager;

import java.util.function.Supplier;

public record C2SDeployCardPacket(int slot, BlockPos target) {
    public static void encode(C2SDeployCardPacket packet,
            FriendlyByteBuf buffer)
    {
        buffer.writeByte(packet.slot);
        buffer.writeBlockPos(packet.target);
    }

    public static C2SDeployCardPacket decode(FriendlyByteBuf buffer) {
        return new C2SDeployCardPacket(
                buffer.readByte(), buffer.readBlockPos());
    }

    public static void handle(C2SDeployCardPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null)
            MatchManager.INSTANCE.handleDeploy(
                    sender, packet.slot, packet.target);
    }
}
