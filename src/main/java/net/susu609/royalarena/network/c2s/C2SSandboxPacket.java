package net.susu609.royalarena.network.c2s;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.s2c.S2CQueueStatusPacket;
import net.susu609.royalarena.server.match.MatchManager;

import java.util.function.Supplier;

/**
 * Everything the sandbox panel asks of the server (docs/SANDBOX.md):
 * {@code card}, {@code side} and {@code pos} are used by DEPLOY, {@code side}
 * and {@code tribe} by TRIBE, {@code value} by the switches, the speed and
 * TRIBE (the step).
 */
public record C2SSandboxPacket(Action action, String card, TeamSide side,
                               BlockPos pos, int value, int tribe)
{
    public enum Action {
        START, DEPLOY, CLEAR, RESTORE_TOWERS, TOWERS_UNBREAKABLE,
        INFINITE_ELIXIR, SPEED, TRIBE, EXIT
    }

    public static C2SSandboxPacket of(Action action) {
        return new C2SSandboxPacket(action, "", TeamSide.BLUE, BlockPos.ZERO,
                0, 0);
    }

    public static C2SSandboxPacket of(Action action, int value) {
        return new C2SSandboxPacket(action, "", TeamSide.BLUE, BlockPos.ZERO,
                value, 0);
    }

    public static C2SSandboxPacket deploy(String card, TeamSide side,
            BlockPos pos)
    {
        return new C2SSandboxPacket(Action.DEPLOY, card, side, pos, 0, 0);
    }

    public static C2SSandboxPacket tribe(TeamSide side, int tribe, int level) {
        return new C2SSandboxPacket(Action.TRIBE, "", side, BlockPos.ZERO,
                level, tribe);
    }

    public static void encode(C2SSandboxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        buffer.writeUtf(packet.card, 32);
        buffer.writeEnum(packet.side);
        buffer.writeBlockPos(packet.pos);
        buffer.writeVarInt(packet.value);
        buffer.writeVarInt(packet.tribe);
    }

    public static C2SSandboxPacket decode(FriendlyByteBuf buffer) {
        return new C2SSandboxPacket(buffer.readEnum(Action.class),
                buffer.readUtf(32), buffer.readEnum(TeamSide.class),
                buffer.readBlockPos(), buffer.readVarInt(),
                buffer.readVarInt());
    }

    public static void handle(C2SSandboxPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null)
            return;
        if (packet.action == Action.START) {
            if (!MatchManager.INSTANCE.startSandbox(sender))
                ModNetwork.sendToPlayer(sender,
                        new S2CQueueStatusPacket(false, 0, "Already in a match"));
            return;
        }
        MatchManager.INSTANCE.handleSandbox(sender, packet);
    }
}
