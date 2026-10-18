package net.susu609.royalarena.server.match;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.susu609.royalarena.bot.BotBrain;
import net.susu609.royalarena.bot.BotDeck;
import net.susu609.royalarena.bot.BotPersonality;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.config.ArenaCommonConfig;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.match.WatchSetup;
import net.susu609.royalarena.server.arena.ArenaAllocator;
import net.susu609.royalarena.server.arena.ArenaDimension;
import net.susu609.royalarena.server.arena.ArenaReservation;
import net.susu609.royalarena.server.deck.DeckStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class MatchManager {
    public static final MatchManager INSTANCE = new MatchManager();

    private final Map<UUID, ServerMatchSession> matches = new HashMap<>();
    private final Map<UUID, UUID> playerToMatch = new HashMap<>();
    /** Players to start a new bot match for on the next tick. */
    private final List<UUID> rematches = new ArrayList<>();
    /** Players who asked to watch another bot-versus-bot match. */
    private final List<UUID> rewatches = new ArrayList<>();
    /** The last watch setup of each player, for "Play again". */
    private final Map<UUID, WatchSetup> watchSetups = new HashMap<>();

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
        return launch(blue.getServer(), blue.getUUID(), red.getUUID(), false,
                false);
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
                true, false);
    }

    /**
     * A bot-versus-bot match the player watches from BLUE's seat: the BLUE
     * hand is the BLUE bot's, and the player plays nothing.
     */
    public boolean startWatch(ServerPlayer player) {
        return startWatch(player, watchSetups.getOrDefault(player.getUUID(),
                WatchSetup.DEFAULT));
    }

    /** As {@link #startWatch(ServerPlayer)} with the decks and personalities picked. */
    public boolean startWatch(ServerPlayer player, WatchSetup setup) {
        if (isInMatch(player.getUUID()))
            return false;
        MatchQueue.INSTANCE.leave(player, "Watching bots");
        // "Play again" watches the same setup.
        watchSetups.put(player.getUUID(), setup);
        return launch(player.getServer(), player.getUUID(), UUID.randomUUID(),
                true, true, setup);
    }

    private boolean launch(MinecraftServer server, UUID blueId, UUID redId,
            boolean botForRed, boolean botForBlue)
    {
        return launch(server, blueId, redId, botForRed, botForBlue, null);
    }

    /** A watched match's deck: random, the spectator's own, or the starter. */
    private static BattleDeck watchDeck(String choice, DeckStorage decks,
            UUID spectator, Random random)
    {
        return switch (choice) {
            case "mine" ->
                    new BattleDeck(decks.deckOf(spectator)).shuffled(random);
            case "starter" -> BattleDeck.starter().shuffled(random);
            default -> BotDeck.random(random);
        };
    }

    private boolean launch(MinecraftServer server, UUID blueId, UUID redId,
            boolean botForRed, boolean botForBlue, WatchSetup setup)
    {
        if (server == null)
            return false;
        ServerLevel level = ArenaDimension.level(server);
        ArenaReservation reservation = ArenaAllocator.INSTANCE.allocate(level);
        UUID matchId = UUID.randomUUID();
        // Saved decks, shuffled for this match; the bot draws a random
        // balanced deck.
        Random random = new Random(matchId.getMostSignificantBits());
        DeckStorage decks = DeckStorage.get(server);
        BattleDeck blueDeck = setup != null ?
                watchDeck(setup.blueDeck(), decks, blueId, random) :
                botForBlue ? BotDeck.random(random) :
                        new BattleDeck(decks.deckOf(blueId)).shuffled(random);
        BattleDeck redDeck = setup != null ?
                watchDeck(setup.redDeck(), decks, blueId, random) :
                botForRed ? BotDeck.random(random) :
                        new BattleDeck(decks.deckOf(redId)).shuffled(random);
        MatchInstance match =
                MatchInstance.create(matchId, blueId, redId,
                        reservation.layout(), blueDeck, redDeck);
        BotBrain bot = botForRed ?
                new BotBrain(TeamSide.RED, matchId.getLeastSignificantBits()) :
                null;
        // The watched BLUE bot draws its own personality; the config only
        // sets the opponent's.
        BotBrain blueBot = botForBlue ? new BotBrain(TeamSide.BLUE,
                matchId.getMostSignificantBits() ^ 0x5DEECE66DL) : null;
        ServerPlayer player = server.getPlayerList().getPlayer(blueId);
        if (setup != null) {
            blueBot.setPersonality(BotPersonality.byId(setup.blueStyle()));
            BotPersonality red = BotPersonality.byId(setup.redStyle());
            bot.setPersonality(red);
        } else if (bot != null) {
            bot.setPersonality(ArenaCommonConfig.botPersonality());
        }
        if (bot != null) {
            if (player != null && blueBot == null)
                player.sendSystemMessage(Component.translatable(
                        "chat.royalarena.bot_personality",
                        Component.literal(bot.personality().displayName())
                                .withStyle(ChatFormatting.RED)));
        }
        if (blueBot != null && player != null)
            player.sendSystemMessage(Component.translatable(
                    "chat.royalarena.watching",
                    Component.literal(blueBot.personality().displayName())
                            .withStyle(ChatFormatting.BLUE),
                    Component.literal(bot.personality().displayName())
                            .withStyle(ChatFormatting.RED)));
        List<BotBrain> bots = new ArrayList<>();
        if (blueBot != null)
            bots.add(blueBot);
        if (bot != null)
            bots.add(bot);
        ServerMatchSession session = new ServerMatchSession(match, level,
                reservation, bots, botForBlue ? blueId : null);
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
            // A watched match at x2 runs two match ticks per server tick.
            for (int i = 0; i < session.speed() && !session.complete(); i++)
                session.tick(server);
            if (session.complete()) {
                playerToMatch.remove(session.match().blue().playerId());
                playerToMatch.remove(session.match().red().playerId());
                ArenaAllocator.INSTANCE.release(session.reservation());
                it.remove();
                (session.watching() ? rewatches : rematches)
                        .addAll(session.rematchPlayers());
            }
        }
        // "Play again" on the result screen: a new bot match once the old
        // one is cleared away.
        while (!rewatches.isEmpty()) {
            ServerPlayer player =
                    server.getPlayerList().getPlayer(rewatches.remove(0));
            if (player != null)
                startWatch(player);
        }
        while (!rematches.isEmpty()) {
            ServerPlayer player =
                    server.getPlayerList().getPlayer(rematches.remove(0));
            if (player != null)
                startPractice(player);
        }
    }

    /** The spectator of a bot-versus-bot match toggles x1 / x2. */
    public void toggleWatchSpeed(ServerPlayer player) {
        UUID matchId = playerToMatch.get(player.getUUID());
        ServerMatchSession session =
                matchId == null ? null : matches.get(matchId);
        if (session != null && session.watching())
            session.toggleSpeed(player.getServer(), player.getUUID());
    }

    /** A player closed the result screen (OK, or Play again). */
    public void resultDone(ServerPlayer player, boolean again) {
        UUID matchId = playerToMatch.get(player.getUUID());
        ServerMatchSession session = matchId == null ? null :
                matches.get(matchId);
        if (session != null)
            session.resultDone(player.getServer(), player.getUUID(), again);
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
