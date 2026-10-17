package net.susu609.royalarena.match;

import net.susu609.royalarena.card.BattleCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The course of one match as {@link MatchInstance} plays it: every card
 * played and when, each side's elixir over time and how long it sat full,
 * and why the match ended. Damage is counted by the combat engine
 * ({@code CombatStats}); the match log puts both together.
 */
public final class MatchRecord {
    /** Battle ticks between two elixir samples (10 s). */
    public static final int SAMPLE_TICKS = 200;

    public enum EndReason {
        /** A King tower fell. */
        KING("king"),
        /** The first tower fell in overtime. */
        OVERTIME_TOWER("overtime_tower"),
        /** Regulation ended with one side ahead on crowns. */
        CROWNS("crowns"),
        /** Overtime ran out; the most tower health won (or a draw). */
        TOWER_HEALTH("tower_health"),
        /** A player left; the other side wins. */
        DISCONNECT("disconnect"),
        /** Stopped without a result (server stopping, command). */
        ABANDONED("abandoned");

        private final String id;

        EndReason(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    /** One card of one side. */
    public static final class Plays {
        private int count;
        private int elixir;
        private int firstTick = -1;

        public int count() {
            return count;
        }
        public int elixir() {
            return elixir;
        }
        /** Battle tick of the first play, -1 if never played. */
        public int firstTick() {
            return firstTick;
        }
    }

    private final Map<TeamSide, Map<BattleCard, Plays>> plays =
            new EnumMap<>(TeamSide.class);
    private final Map<TeamSide, List<Integer>> samples =
            new EnumMap<>(TeamSide.class);
    private final Map<TeamSide, int[]> fullTicks = new EnumMap<>(TeamSide.class);
    private EndReason endReason;
    private int battleTicks;

    MatchRecord() {
        for (TeamSide side : TeamSide.values()) {
            plays.put(side, new EnumMap<>(BattleCard.class));
            samples.put(side, new ArrayList<>());
            fullTicks.put(side, new int[1]);
        }
    }

    void play(TeamSide side, BattleCard card, int tick) {
        Plays p = plays.get(side).computeIfAbsent(card, c -> new Plays());
        p.count++;
        p.elixir += card.cost();
        if (p.firstTick < 0)
            p.firstTick = tick;
    }

    /** Elixir (in tenths) of both sides at the start of the battle. */
    void start(int blueTenths, int redTenths) {
        for (TeamSide side : TeamSide.values()) {
            samples.get(side).clear();
            fullTicks.get(side)[0] = 0;
        }
        samples.get(TeamSide.BLUE).add(blueTenths);
        samples.get(TeamSide.RED).add(redTenths);
    }

    /** After each battle tick, with the elixir of both sides. */
    void tick(int battleTick, int blueTenths, int redTenths) {
        battleTicks = battleTick;
        if (blueTenths >= ElixirMeter.MAX_TENTHS)
            fullTicks.get(TeamSide.BLUE)[0]++;
        if (redTenths >= ElixirMeter.MAX_TENTHS)
            fullTicks.get(TeamSide.RED)[0]++;
        if (battleTick % SAMPLE_TICKS == 0) {
            samples.get(TeamSide.BLUE).add(blueTenths);
            samples.get(TeamSide.RED).add(redTenths);
        }
    }

    void end(EndReason reason) {
        if (endReason == null)
            endReason = reason;
    }

    /** Cards {@code side} played, with how often and for how much. */
    public Map<BattleCard, Plays> plays(TeamSide side) {
        return Collections.unmodifiableMap(plays.get(side));
    }

    /** What {@code side} played of {@code card} (zero if never). */
    public Plays plays(TeamSide side, BattleCard card) {
        Plays p = plays.get(side).get(card);
        return p == null ? new Plays() : p;
    }

    /** Elixir in tenths at the start and every {@link #SAMPLE_TICKS}. */
    public List<Integer> elixirSamples(TeamSide side) {
        return Collections.unmodifiableList(samples.get(side));
    }

    /** Battle ticks {@code side} spent at full elixir (wasted income). */
    public int fullElixirTicks(TeamSide side) {
        return fullTicks.get(side)[0];
    }

    /** Why the match ended, or null while it runs. */
    public EndReason endReason() {
        return endReason;
    }

    /** Ticks of battle played, overtime included. */
    public int battleTicks() {
        return battleTicks;
    }
}
