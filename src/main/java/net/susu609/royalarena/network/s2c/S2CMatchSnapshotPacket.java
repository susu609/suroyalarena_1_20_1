package net.susu609.royalarena.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.match.MatchSnapshot;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.PendingDeploy;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record S2CMatchSnapshotPacket(MatchSnapshot snapshot) {
    public static void encode(S2CMatchSnapshotPacket packet,
            FriendlyByteBuf buffer)
    {
        MatchSnapshot s = packet.snapshot;
        buffer.writeBoolean(s.active());
        if (!s.active())
            return;
        buffer.writeUUID(s.matchId());
        buffer.writeEnum(s.state());
        buffer.writeEnum(s.localSide());
        buffer.writeInt(s.center().x());
        buffer.writeInt(s.center().y());
        buffer.writeInt(s.center().z());
        buffer.writeVarInt(s.ticksRemaining());
        buffer.writeVarInt(s.elixirTenths());
        buffer.writeVarInt(s.hand().size());
        for (String card : s.hand())
            buffer.writeUtf(card, 32);
        buffer.writeUtf(s.nextCard(), 32);
        for (int hp : s.towerHealth())
            buffer.writeVarInt(hp);
        buffer.writeBoolean(s.winner() != null);
        if (s.winner() != null)
            buffer.writeEnum(s.winner());
        buffer.writeVarInt(s.pending().size());
        for (PendingDeploy deploy : s.pending()) {
            buffer.writeUtf(deploy.card().id(), 32);
            buffer.writeEnum(deploy.side());
            buffer.writeInt(deploy.x());
            buffer.writeInt(deploy.z());
            buffer.writeVarInt(deploy.ticksLeft());
        }
        buffer.writeUUID(s.bluePlayer());
        buffer.writeUUID(s.redPlayer());
    }

    public static S2CMatchSnapshotPacket decode(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean())
            return new S2CMatchSnapshotPacket(MatchSnapshot.inactive());
        UUID matchId = buffer.readUUID();
        MatchState state = buffer.readEnum(MatchState.class);
        TeamSide side = buffer.readEnum(TeamSide.class);
        ArenaPoint center = new ArenaPoint(
                buffer.readInt(), buffer.readInt(),
                buffer.readInt());
        int ticks = buffer.readVarInt();
        int elixir = buffer.readVarInt();
        int handSize = buffer.readVarInt();
        List<String> hand = new ArrayList<>(handSize);
        for (int i = 0; i < handSize; i++)
            hand.add(buffer.readUtf(32));
        String next = buffer.readUtf(32);
        int[] towerHp = new int[TowerId.values().length];
        for (int i = 0; i < towerHp.length; i++)
            towerHp[i] = buffer.readVarInt();
        TeamSide winner =
                buffer.readBoolean() ? buffer.readEnum(TeamSide.class) : null;
        int pendingSize = buffer.readVarInt();
        List<PendingDeploy> pending = new ArrayList<>(pendingSize);
        for (int i = 0; i < pendingSize; i++) {
            pending.add(new PendingDeploy(
                    BattleCard.byId(buffer.readUtf(32)),
                    buffer.readEnum(TeamSide.class), buffer.readInt(),
                    buffer.readInt(), buffer.readVarInt()));
        }
        UUID bluePlayer = buffer.readUUID();
        UUID redPlayer = buffer.readUUID();
        return new S2CMatchSnapshotPacket(
                new MatchSnapshot(
                        true, matchId, state, side, center, ticks, elixir,
                        List.copyOf(hand), next, towerHp, winner,
                        List.copyOf(pending), bluePlayer, redPlayer));
    }

    public static void handle(S2CMatchSnapshotPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientMatchState.apply(packet.snapshot));
    }
}
