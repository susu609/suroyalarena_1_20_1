package net.susu609.royalarena.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.susu609.modlib.resource.RL;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.network.c2s.C2SDeckRequestPacket;
import net.susu609.royalarena.network.c2s.C2SDeployCardPacket;
import net.susu609.royalarena.network.c2s.C2SJoinQueuePacket;
import net.susu609.royalarena.network.c2s.C2SLeaveQueuePacket;
import net.susu609.royalarena.network.c2s.C2SResultDonePacket;
import net.susu609.royalarena.network.c2s.C2SSaveDeckPacket;
import net.susu609.royalarena.network.c2s.C2SStartPracticePacket;
import net.susu609.royalarena.network.c2s.C2SWatchSpeedPacket;
import net.susu609.royalarena.network.s2c.S2CAoeFlashPacket;
import net.susu609.royalarena.network.s2c.S2CBotDragPacket;
import net.susu609.royalarena.network.s2c.S2CBurnPacket;
import net.susu609.royalarena.network.s2c.S2CDeckPacket;
import net.susu609.royalarena.network.s2c.S2CMatchResultPacket;
import net.susu609.royalarena.network.s2c.S2CProjectilePacket;
import net.susu609.royalarena.network.s2c.S2CMatchSnapshotPacket;
import net.susu609.royalarena.network.s2c.S2CQueueStatusPacket;
import net.susu609.royalarena.network.s2c.S2CShakePacket;

public final class ModNetwork {
    private static final String PROTOCOL = "17";
    public static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    RL.of(RoyalArena.MOD_ID, "main"),
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
                        C2SStartPracticePacket.class, id++,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SStartPracticePacket::encode)
                .decoder(C2SStartPracticePacket::decode)
                .consumerMainThread(C2SStartPracticePacket::handle).add();
        CHANNEL.messageBuilder(
                        C2SWatchSpeedPacket.class, id++,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SWatchSpeedPacket::encode)
                .decoder(C2SWatchSpeedPacket::decode)
                .consumerMainThread(C2SWatchSpeedPacket::handle).add();
        CHANNEL.messageBuilder(
                        C2SDeckRequestPacket.class, id++,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SDeckRequestPacket::encode)
                .decoder(C2SDeckRequestPacket::decode)
                .consumerMainThread(C2SDeckRequestPacket::handle).add();
        CHANNEL.messageBuilder(
                        C2SSaveDeckPacket.class, id++,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SSaveDeckPacket::encode)
                .decoder(C2SSaveDeckPacket::decode)
                .consumerMainThread(C2SSaveDeckPacket::handle).add();
        CHANNEL.messageBuilder(
                        S2CDeckPacket.class, id++,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CDeckPacket::encode)
                .decoder(S2CDeckPacket::decode)
                .consumerMainThread(S2CDeckPacket::handle).add();
        CHANNEL.messageBuilder(
                        S2CProjectilePacket.class, id++,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CProjectilePacket::encode)
                .decoder(S2CProjectilePacket::decode)
                .consumerMainThread(S2CProjectilePacket::handle).add();
        CHANNEL.messageBuilder(
                        S2CAoeFlashPacket.class, id++,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CAoeFlashPacket::encode)
                .decoder(S2CAoeFlashPacket::decode)
                .consumerMainThread(S2CAoeFlashPacket::handle).add();
        CHANNEL.messageBuilder(
                        S2CBurnPacket.class, id++,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CBurnPacket::encode)
                .decoder(S2CBurnPacket::decode)
                .consumerMainThread(S2CBurnPacket::handle).add();
        CHANNEL.messageBuilder(
                        S2CBotDragPacket.class, id++,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CBotDragPacket::encode)
                .decoder(S2CBotDragPacket::decode)
                .consumerMainThread(S2CBotDragPacket::handle).add();
        CHANNEL.messageBuilder(
                        S2CShakePacket.class, id++,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CShakePacket::encode)
                .decoder(S2CShakePacket::decode)
                .consumerMainThread(S2CShakePacket::handle).add();
        CHANNEL.messageBuilder(
                        S2CMatchResultPacket.class, id++,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CMatchResultPacket::encode)
                .decoder(S2CMatchResultPacket::decode)
                .consumerMainThread(S2CMatchResultPacket::handle).add();
        CHANNEL.messageBuilder(
                        C2SResultDonePacket.class, id++,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SResultDonePacket::encode)
                .decoder(C2SResultDonePacket::decode)
                .consumerMainThread(C2SResultDonePacket::handle).add();
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    /** Sends to every player within {@code radius} blocks in that level. */
    public static void sendNear(ServerLevel level, double x, double y,
            double z, double radius, Object packet)
    {
        CHANNEL.send(PacketDistributor.NEAR.with(
                () -> new PacketDistributor.TargetPoint(x, y, z, radius,
                        level.dimension())), packet);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
