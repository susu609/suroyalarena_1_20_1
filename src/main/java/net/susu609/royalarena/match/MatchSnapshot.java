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
                            boolean spectating)
{
    /** Player of a side (the bot has a random id): its skin is the King's. */
    public UUID player(TeamSide side) {
        return side == TeamSide.BLUE ? bluePlayer : redPlayer;
    }

    public static MatchSnapshot from(MatchInstance match, TeamSide localSide) {
        return from(match, localSide, false);
    }

    /**
     * {@code spectating}: the player only watches (a bot plays this side);
     * the client shows the side's hand but plays nothing.
     */
    public static MatchSnapshot from(MatchInstance match, TeamSide localSide,
            boolean spectating)
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
                match.elixirRate(), spectating);
    }

    public static MatchSnapshot inactive() {
        return new MatchSnapshot(
                false, new UUID(0L, 0L), MatchState.WAITING, TeamSide.BLUE,
                new ArenaPoint(0, 0, 0), 0, 0, List.of(), "",
                new int[TowerId.values().length], null, List.of(),
                new UUID(0L, 0L), new UUID(0L, 0L), false, 1, false);
    }
}
