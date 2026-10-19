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
                            Watch watch, Sandbox sandbox)
{
    /**
     * Sent in a sandbox match: its switches, both sides' elixir and tribe
     * steps (by tribe ordinal), and what every troop is aiming at, for the
     * stats overlay.
     */
    public record Sandbox(boolean towersInvulnerable, boolean infiniteElixir,
                          int speed, int blueElixir, int redElixir,
                          int[] blueTribes, int[] redTribes, List<Aim> aims)
    {
        public int elixir(TeamSide side) {
            return side == TeamSide.BLUE ? blueElixir : redElixir;
        }

        public int[] tribes(TeamSide side) {
            return side == TeamSide.BLUE ? blueTribes : redTribes;
        }
    }

    /** A troop's target: another troop, or (tower >= 0) a tower by ordinal. */
    public record Aim(UUID unit, UUID target, int tower) {
    }

    public boolean isSandbox() {
        return sandbox != null;
    }

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
        return from(match, localSide, watch, null);
    }

    /** As above, with the sandbox part (null outside a sandbox). */
    public static MatchSnapshot from(MatchInstance match, TeamSide localSide,
            Watch watch, Sandbox sandbox)
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
                match.elixirRate(), watch, sandbox);
    }

    /** The sandbox part of a snapshot, with these troop aims. */
    public static Sandbox sandbox(MatchInstance match, List<Aim> aims) {
        SandboxSettings s = match.sandbox();
        return new Sandbox(s.towersInvulnerable(), s.infiniteElixir(),
                s.speed(), match.player(TeamSide.BLUE).elixirTenths(),
                match.player(TeamSide.RED).elixirTenths(),
                s.tribeArray(TeamSide.BLUE), s.tribeArray(TeamSide.RED),
                List.copyOf(aims));
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
                new UUID(0L, 0L), new UUID(0L, 0L), false, 1, null, null);
    }
}
