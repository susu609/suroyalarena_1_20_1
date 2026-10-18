package net.susu609.royalarena.match;

import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.tower.TowerId;

import java.util.List;
import java.util.UUID;

public record MatchSnapshot(boolean active, UUID matchId, MatchState state,
                            TeamSide localSide, ArenaPoint center,
                            int ticksRemaining, int elixirTenths,
                            List<String> hand, String nextCard,
                            int[] towerHealth, TeamSide winner,
                            List<PendingDeploy> pending, UUID bluePlayer,
                            UUID redPlayer, boolean overtime, int elixirRate,
                            Watch watch)
{
    /**
     * Sent to a player watching a bot-versus-bot match (from BLUE's seat):
     * RED's hand, next card and elixir (BLUE's are the usual fields), both
     * bots' personality names, and the playback speed (1 or 2).
     */
    public record Watch(List<String> redHand, String redNext, int redElixir,
                        String blueStyle, String redStyle, int speed)
    {
    }

    /** Watching a bot-versus-bot match: the player plays nothing. */
    public boolean spectating() {
        return watch != null;
    }

    /** Player of a side (the bot has a random id): its skin is the King's. */
    public UUID player(TeamSide side) {
        return side == TeamSide.BLUE ? bluePlayer : redPlayer;
    }

    public static MatchSnapshot from(MatchInstance match, TeamSide localSide) {
        return from(match, localSide, null);
    }

    /** {@code watch}: the player only watches; null for a player's match. */
    public static MatchSnapshot from(MatchInstance match, TeamSide localSide,
            Watch watch)
    {
        List<String> hand = match.player(localSide).deck().hand().stream()
                .map(BattleCard::id).toList();
        int[] hp = new int[TowerId.values().length];
        for (TowerId id : TowerId.values())
            hp[id.ordinal()] = match.tower(id).health();
        return new MatchSnapshot(
                true, match.matchId(), match.state(), localSide,
                match.arena().center(), match.ticksRemaining(),
                match.player(localSide).elixirTenths(), hand,
                match.player(localSide).deck().nextCard().id(), hp,
                match.winner(), match.pendingDeploys(),
                match.player(TeamSide.BLUE).playerId(),
                match.player(TeamSide.RED).playerId(), match.overtime(),
                match.elixirRate(), watch);
    }

    /** The RED half of a {@link Watch} for this match. */
    public static Watch watch(MatchInstance match, String blueStyle,
            String redStyle, int speed)
    {
        MatchPlayer red = match.player(TeamSide.RED);
        return new Watch(red.deck().hand().stream().map(BattleCard::id)
                .toList(), red.deck().nextCard().id(), red.elixirTenths(),
                blueStyle, redStyle, speed);
    }

    public static MatchSnapshot inactive() {
        return new MatchSnapshot(
                false, new UUID(0L, 0L), MatchState.WAITING, TeamSide.BLUE,
                new ArenaPoint(0, 0, 0), 0, 0, List.of(), "",
                new int[TowerId.values().length], null, List.of(),
                new UUID(0L, 0L), new UUID(0L, 0L), false, 1, null);
    }
}
