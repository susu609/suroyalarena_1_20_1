package net.susu609.royalarena.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.network.c2s.C2SDeployCardPacket;
import net.susu609.royalarena.network.c2s.C2SJoinQueuePacket;
import net.susu609.royalarena.network.c2s.C2SLeaveQueuePacket;
import net.susu609.royalarena.network.c2s.C2SStartPracticePacket;
import net.susu609.royalarena.network.s2c.S2CMatchSnapshotPacket;
import net.susu609.royalarena.network.s2c.S2CQueueStatusPacket;

public final class ModNetwork {
    private static final String PROTOCOL = "3";
    public static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(RoyalArena.MOD_ID, "main"),
                    () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static boolean registered;

    private ModNetwork() {
    }

    public static void register() {
        if (registered)
            return;
        registered = true;
        int id = 0;
        CHANNEL.messageBuilder(
                        C2SJoinQueuePacket.class, id++,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SJoinQueuePacket::encode)
                .decoder(C2SJoinQueuePacket::decode)
                .consumerMainThread(C2SJoinQueuePacket::handle).add();
        CHANNEL.messageBuilder(
                        C2SLeaveQueuePacket.class, id++,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SLeaveQueuePacket::encode)
                .decoder(C2SLeaveQueuePacket::decode)
                .consumerMainThread(C2SLeaveQueuePacket::handle).add();
        CHANNEL.messageBuilder(
                        C2SDeployCardPacket.class, id++,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SDeployCardPacket::encode)
                .decoder(C2SDeployCardPacket::decode)
                .consumerMainThread(C2SDeployCardPacket::handle).add();
        CHANNEL.messageBuilder(
                        S2CQueueStatusPacket.class, id++,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CQueueStatusPacket::encode)
                .decoder(S2CQueueStatusPacket::decode)
                .consumerMainThread(S2CQueueStatusPacket::handle).add();
        CHANNEL.messageBuilder(
                        S2CMatchSnapshotPacket.class, id++,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CMatchSnapshotPacket::encode)
                .decoder(S2CMatchSnapshotPacket::decode)
                .consumerMainThread(S2CMatchSnapshotPacket::handle).add();
        CHANNEL.messageBuilder(
                        C2SStartPracticePacket.class, id,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SStartPracticePacket::encode)
                .decoder(C2SStartPracticePacket::decode)
                .consumerMainThread(C2SStartPracticePacket::handle).add();
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
