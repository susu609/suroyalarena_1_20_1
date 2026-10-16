package net.susu609.royalarena.server.match;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.susu609.royalarena.bot.BotBrain;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.server.arena.ArenaAllocator;
import net.susu609.royalarena.server.arena.ArenaReservation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class MatchManager {
    public static final MatchManager INSTANCE = new MatchManager();

    private final Map<UUID, ServerMatchSession> matches = new HashMap<>();
    private final Map<UUID, UUID> playerToMatch = new HashMap<>();

    private MatchManager() {
    }

    public void handleDeploy(ServerPlayer player, int slot, BlockPos target) {
        UUID matchId = playerToMatch.get(player.getUUID());
        if (matchId == null)
            return;
        ServerMatchSession session = matches.get(matchId);
        if (session != null)
            session.handleDeploy(player, slot, target);
    }

    public boolean startMatch(ServerPlayer blue, ServerPlayer red) {
        if (blue == red || isInMatch(blue.getUUID()) ||
                isInMatch(red.getUUID()))
            return false;
        return launch(blue.getServer(), blue.getUUID(), red.getUUID(), false);
    }

    /**
     * Solo match: the player is BLUE and a bot plays RED. The bot has no
     * player id (a random one only keeps the match bookkeeping unique).
     */
    public boolean startPractice(ServerPlayer player) {
        if (isInMatch(player.getUUID()))
            return false;
        MatchQueue.INSTANCE.leave(player, "Practice match");
        return launch(player.getServer(), player.getUUID(), UUID.randomUUID(),
                true);
    }

    private boolean launch(MinecraftServer server, UUID blueId, UUID redId,
            boolean botForRed)
    {
        if (server == null)
            return false;
        ServerLevel level = server.overworld();
        ArenaReservation reservation = ArenaAllocator.INSTANCE.allocate(level);
        UUID matchId = UUID.randomUUID();
        MatchInstance match =
                MatchInstance.create(
                        matchId, blueId, redId, reservation.layout());
        BotBrain bot = botForRed ?
                new BotBrain(TeamSide.RED, matchId.getLeastSignificantBits()) :
                null;
        ServerMatchSession session =
                new ServerMatchSession(match, level, reservation, bot);
        matches.put(matchId, session);
        playerToMatch.put(blueId, matchId);
        playerToMatch.put(redId, matchId);
        session.start(server);
        return true;
    }
    public boolean isInMatch(UUID playerId) {
        return playerToMatch.containsKey(playerId);
    }

    public void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, ServerMatchSession>> it =
                matches.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ServerMatchSession> entry = it.next();
            ServerMatchSession session = entry.getValue();
            session.tick(server);
            if (session.complete()) {
                playerToMatch.remove(session.match().blue().playerId());
                playerToMatch.remove(session.match().red().playerId());
                ArenaAllocator.INSTANCE.release(session.reservation());
                it.remove();
            }
        }
    }
    public void onPlayerLoggedOut(UUID playerId) {
        UUID matchId = playerToMatch.get(playerId);
        if (matchId == null)
            return;
        ServerMatchSession session = matches.get(matchId);
        if (session != null)
            session.onPlayerDisconnected(playerId);
    }
    public void shutdown(MinecraftServer server) {
        for (ServerMatchSession session : matches.values()) {
            session.closeNow(server);
            ArenaAllocator.INSTANCE.release(session.reservation());
        }
        matches.clear();
        playerToMatch.clear();
        ArenaAllocator.INSTANCE.reset();
    }
    public void stopAll() {
        for (ServerMatchSession session : matches.values())
            session.forceStop();
    }
}
