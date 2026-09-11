package net.susu609.royalarena.server.match;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.s2c.S2CQueueStatusPacket;

import java.util.*;

public final class MatchQueue {
    public static final MatchQueue INSTANCE = new MatchQueue();
    private final Deque<UUID> waiting = new ArrayDeque<>();
    private boolean dirty;

    private MatchQueue() {
    }
    public void clear() {
        waiting.clear();
        dirty = false;
    }
    public void join(ServerPlayer player) {
        UUID id = player.getUUID();
        if (MatchManager.INSTANCE.isInMatch(id)) {
            ModNetwork.sendToPlayer(
                    player,
                    new S2CQueueStatusPacket(false, 0, "Already in a match"));
            return;
        }
        if (!waiting.contains(id)) {
            waiting.addLast(id);
            dirty = true;
        }
        MinecraftServer server = player.getServer();
        if (server != null && dirty) {
            updatePositions(server);
            dirty = false;
        }
    }
    private void updatePositions(MinecraftServer server) {
        List<UUID> ids = new ArrayList<>(waiting);
        for (int i = 0; i < ids.size(); i++) {
            ServerPlayer player = server.getPlayerList().getPlayer(ids.get(i));
            if (player != null) {
                ModNetwork.sendToPlayer(
                        player, new S2CQueueStatusPacket(
                                true, i + 1,
                                "Searching for opponent"));
            }
        }
    }
    public void leave(ServerPlayer player, String message) {
        if (waiting.remove(player.getUUID()))
            dirty = true;
        ModNetwork.sendToPlayer(
                player, new S2CQueueStatusPacket(false, 0, message));
        MinecraftServer server = player.getServer();
        if (server != null && dirty) {
            updatePositions(server);
            dirty = false;
        }
    }
    public void remove(UUID playerId) {
        if (waiting.remove(playerId))
            dirty = true;
    }
    public void tick(MinecraftServer server) {
        boolean changed = dirty;
        if (waiting.removeIf(
                id -> server.getPlayerList().getPlayer(id) == null ||
                        MatchManager.INSTANCE.isInMatch(id)))
        {
            changed = true;
        }
        while (waiting.size() >= 2) {
            ServerPlayer first = pollOnline(server);
            if (first == null) {
                changed = true;
                break;
            }
            ServerPlayer second = pollOnline(server);
            if (second == null) {
                waiting.addFirst(first.getUUID());
                changed = true;
                break;
            }
            changed = true;
            ModNetwork.sendToPlayer(
                    first, new S2CQueueStatusPacket(false, 0, "Match found"));
            ModNetwork.sendToPlayer(
                    second, new S2CQueueStatusPacket(false, 0, "Match found"));
            if (!MatchManager.INSTANCE.startMatch(first, second)) {
                waiting.addFirst(second.getUUID());
                waiting.addFirst(first.getUUID());
                break;
            }
        }
        if (changed) {
            updatePositions(server);
            dirty = false;
        }
    }
    private ServerPlayer pollOnline(MinecraftServer server) {
        while (!waiting.isEmpty()) {
            UUID id = waiting.removeFirst();
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null && !MatchManager.INSTANCE.isInMatch(id))
                return player;
        }
        return null;
    }
}
