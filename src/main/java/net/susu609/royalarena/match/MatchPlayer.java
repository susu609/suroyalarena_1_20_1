package net.susu609.royalarena.match;

import net.susu609.royalarena.card.BattleDeck;

import java.util.UUID;

public final class MatchPlayer {
    private final UUID playerId;
    private final TeamSide side;
    private final BattleDeck deck;
    private final ElixirMeter elixir;

    public MatchPlayer(UUID playerId, TeamSide side) {
        this.playerId = playerId;
        this.side = side;
        this.deck = BattleDeck.starter();
        this.elixir = new ElixirMeter();
    }

    public UUID playerId() {
        return playerId;
    }

    public TeamSide side() {
        return side;
    }

    public BattleDeck deck() {
        return deck;
    }

    public ElixirMeter elixir() {
        return elixir;
    }

    public int elixirTenths() {
        return elixir.tenths();
    }
}
