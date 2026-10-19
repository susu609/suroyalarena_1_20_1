package net.susu609.royalarena.match;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.tower.TowerId;
import net.susu609.royalarena.tower.TowerKind;
import net.susu609.royalarena.tower.TowerState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MatchInstance {
    /** Regulation time (balance.json match.regulation_ticks). */
    public static final int DEFAULT_BATTLE_TICKS = 20 * 180;
    /** Overtime when the crowns are level after regulation (2 min). */
    public static final int OVERTIME_TICKS = 20 * 120;
    /** Battle ticks at which elixir comes 2x, then 3x (balance.json). */
    public static final int DOUBLE_ELIXIR_AT = 2400;
    public static final int TRIPLE_ELIXIR_AT = 4800;
    private static final int[] TICKS_PER_ELIXIR = {56, 28, 19};
    /** Time between an accepted troop card play and the troop appearing (1 s). */
    public static final int DEPLOY_TICKS = BattleCard.DEPLOY_TICKS;

    private final UUID matchId;
    private final ArenaLayout arena;
    private final MatchPlayer blue;
    private final MatchPlayer red;
    private final Map<TowerId, TowerState> towers =
            new EnumMap<>(TowerId.class);
    private final List<PendingDeploy> pending = new ArrayList<>();
    private final List<PendingDeploy> ready = new ArrayList<>();

    private MatchState state = MatchState.WAITING;
    private int ticksRemaining;
    /** Ticks of battle so far, overtime included. */
    private int battleTicks;
    private boolean overtime;
    private TeamSide winner;
    private final MatchRecord record = new MatchRecord();
    /** Set for a sandbox match (docs/SANDBOX.md); null for a real one. */
    private SandboxSettings sandbox;
    /** King towers hit while unbreakable: they wake up all the same. */
    private final Set<TowerId> hitWhileUnbreakable = EnumSet.noneOf(TowerId.class);

    private MatchInstance(UUID matchId, UUID blueId, UUID redId,
            ArenaLayout arena, BattleDeck blueDeck, BattleDeck redDeck)
    {
        this.matchId = matchId;
        this.arena = arena;
        this.blue = new MatchPlayer(blueId, TeamSide.BLUE, blueDeck);
        this.red = new MatchPlayer(redId, TeamSide.RED, redDeck);
        for (TowerId id : TowerId.values()) {
            towers.put(id, new TowerState(id, id.kind().maxHealth()));
        }
    }

    /** A match where both players use the starter deck, unshuffled. */
    public static MatchInstance create(UUID matchId, UUID blueId, UUID redId,
            ArenaLayout arena)
    {
        return create(matchId, blueId, redId, arena, BattleDeck.starter(),
                BattleDeck.starter());
    }

    public static MatchInstance create(UUID matchId, UUID blueId, UUID redId,
            ArenaLayout arena, BattleDeck blueDeck, BattleDeck redDeck)
    {
        if (blueId.equals(redId))
            throw new IllegalArgumentException(
                    "A match needs two distinct players");
        return new MatchInstance(matchId, blueId, redId, arena, blueDeck,
                redDeck);
    }

    public TowerId chooseTowerTarget(TeamSide attacker, double worldX) {
        TeamSide defender = attacker.opponent();
        boolean worldLeft = worldX < arena.center().x();
        TowerId lane;
        if (defender == TeamSide.BLUE) {
            lane = worldLeft ? TowerId.BLUE_LEFT : TowerId.BLUE_RIGHT;
        } else {
            lane = worldLeft ? TowerId.RED_RIGHT : TowerId.RED_LEFT;
        }
        TowerState laneTower = tower(lane);
        if (!laneTower.isDestroyed())
            return lane;
        return TowerId.king(defender);
    }

    public ArenaLayout arena() {
        return arena;
    }

    public MatchPlayer blue() {
        return blue;
    }

    public MatchPlayer red() {
        return red;
    }
    public TowerState tower(TowerId id) {
        return towers.get(id);
    }

    public MatchPlayer player(UUID playerId) {
        if (blue.playerId().equals(playerId))
            return blue;
        if (red.playerId().equals(playerId))
            return red;
        return null;
    }
    public void damageKingTower(TeamSide towerSide, int damage) {
        damageTower(TowerId.king(towerSide), damage);
    }
    public void damageTower(TowerId id, double damage) {
        if (state != MatchState.BATTLE || damage <= 0)
            return;
        if (sandbox != null && sandbox.towersInvulnerable()) {
            hitWhileUnbreakable.add(id);
            return;
        }
        TowerState target = tower(id);
        boolean standing = !target.isDestroyed();
        target.damage(damage);
        // The King ends the match; in overtime, so does the first tower.
        // A sandbox never ends: a fallen tower waits for "restore towers".
        if (standing && target.isDestroyed() && sandbox == null) {
            if (id.kind() == TowerKind.KING)
                finish(id.side().opponent(), MatchRecord.EndReason.KING);
            else if (overtime)
                finish(id.side().opponent(),
                        MatchRecord.EndReason.OVERTIME_TOWER);
        }
    }

    /** Ends the match; {@code winningSide} null is a draw. */
    public void finish(TeamSide winningSide, MatchRecord.EndReason reason) {
        if (state == MatchState.FINISHED)
            return;
        record.end(reason);
        state = MatchState.FINISHED;
        ticksRemaining = 0;
        winner = winningSide;
        pending.clear();
        ready.clear();
    }
    public UUID matchId() {
        return matchId;
    }

    /** Plays, elixir and the end of this match, for the match log. */
    public MatchRecord record() {
        return record;
    }

    /** Ticks of battle so far, overtime included. */
    public int battleTicks() {
        return battleTicks;
    }
    public TeamSide sideOf(UUID playerId) {
        MatchPlayer player = player(playerId);
        return player == null ? null : player.side();
    }

    public void startCountdown(int ticks) {
        if (state != MatchState.WAITING)
            return;
        if (ticks <= 0) {
            startBattle();
            return;
        }
        state = MatchState.COUNTDOWN;
        ticksRemaining = ticks;
    }

    public void startBattle() {
        if (state == MatchState.FINISHED)
            return;
        state = MatchState.BATTLE;
        ticksRemaining = DEFAULT_BATTLE_TICKS;
        battleTicks = 0;
        overtime = false;
        record.start(blue.elixirTenths(), red.elixirTenths());
    }

    /** Whether the match is in overtime (sudden death: first crown wins). */
    public boolean overtime() {
        return overtime;
    }

    /** 1 for normal elixir, 2 in the last minute, 3 late in overtime. */
    public int elixirRate() {
        if (sandbox != null)
            return 1;
        return battleTicks >= TRIPLE_ELIXIR_AT ? 3 :
                battleTicks >= DOUBLE_ELIXIR_AT ? 2 : 1;
    }

    /** Destroyed towers of the other side. */
    public int crowns(TeamSide side) {
        int count = 0;
        for (TowerId id : TowerId.values()) {
            if (id.side() != side && tower(id).isDestroyed())
                count++;
        }
        return count;
    }
    public MatchState state() {
        return state;
    }
    public void tick() {
        if (state == MatchState.COUNTDOWN) {
            if (--ticksRemaining <= 0)
                startBattle();
            return;
        }
        if (state != MatchState.BATTLE)
            return;

        int perElixir = TICKS_PER_ELIXIR[elixirRate() - 1];
        blue.elixir().tick(perElixir);
        red.elixir().tick(perElixir);
        if (sandbox != null && sandbox.infiniteElixir()) {
            blue.elixir().fill();
            red.elixir().fill();
        }
        battleTicks++;
        record.tick(battleTicks, blue.elixirTenths(), red.elixirTenths());
        advancePending();
        // A sandbox has no clock.
        if (sandbox == null && --ticksRemaining <= 0)
            timeUp();
    }
    public int ticksRemaining() {
        return ticksRemaining;
    }
    public DeployResult tryDeploy(TeamSide side, int slot, int worldX,
            int worldZ)
    {
        if (state != MatchState.BATTLE)
            return DeployResult.rejected("match_not_in_battle");
        if (slot < 0 || slot >= 4)
            return DeployResult.rejected("invalid_slot");
        MatchPlayer player = player(side);
        BattleCard card = player.deck().cardAt(slot);
        if (!arena.canPlay(side, card, worldX, worldZ))
            return DeployResult.rejected("outside_deploy_area");
        if (!player.elixir().spend(card.cost()))
            return DeployResult.rejected("not_enough_elixir");
        player.deck().play(slot);
        record.play(side, card, battleTicks);
        pending.add(new PendingDeploy(card, side, worldX, worldZ,
                arena.deployTicks(card, side, worldX + 0.5D, worldZ + 0.5D)));
        return DeployResult.accepted(card);
    }

    // ------------------------------------------------------------- sandbox

    /** Makes this a sandbox match: no clock, no end, any card for any side. */
    public void enableSandbox() {
        if (sandbox == null)
            sandbox = new SandboxSettings();
    }

    /** The sandbox switches, or null for a real match. */
    public SandboxSettings sandbox() {
        return sandbox;
    }

    /**
     * Plays {@code card} for {@code side} at the tile (x, z) outside the
     * hand: the usual deploy zones and flight times, elixir only when it is
     * not endless.
     */
    public DeployResult sandboxDeploy(TeamSide side, BattleCard card,
            int worldX, int worldZ)
    {
        if (sandbox == null)
            return DeployResult.rejected("not_sandbox");
        if (state != MatchState.BATTLE)
            return DeployResult.rejected("match_not_in_battle");
        if (card == null)
            return DeployResult.rejected("unknown_card");
        if (!arena.canPlay(side, card, worldX, worldZ))
            return DeployResult.rejected("outside_deploy_area");
        if (!sandbox.infiniteElixir() &&
                !player(side).elixir().spend(card.cost()))
            return DeployResult.rejected("not_enough_elixir");
        record.play(side, card, battleTicks);
        pending.add(new PendingDeploy(card, side, worldX, worldZ,
                arena.deployTicks(card, side, worldX + 0.5D, worldZ + 0.5D)));
        return DeployResult.accepted(card);
    }

    /** Every tower back to full health (asleep again, for the Kings). */
    public void resetTowers() {
        for (TowerId id : TowerId.values())
            towers.put(id, new TowerState(id, id.kind().maxHealth()));
        hitWhileUnbreakable.clear();
    }

    /** Whether an unbreakable tower was hit (it wakes a King all the same). */
    public boolean hitWhileUnbreakable(TowerId id) {
        return hitWhileUnbreakable.contains(id);
    }

    /** Drops the cards still in flight or waiting to appear. */
    public void clearPending() {
        pending.clear();
        ready.clear();
    }

    /** Cards still waiting out their deploy time, oldest first. */
    public List<PendingDeploy> pendingDeploys() {
        return List.copyOf(pending);
    }

    /** Deploys whose time ran out since the last call; spawn these now. */
    public List<PendingDeploy> drainReadyDeploys() {
        List<PendingDeploy> out = List.copyOf(ready);
        ready.clear();
        return out;
    }

    private void advancePending() {
        for (int i = 0; i < pending.size(); i++) {
            PendingDeploy next = pending.get(i).tickDown();
            if (next.ticksLeft() <= 0) {
                ready.add(next);
                pending.remove(i--);
            } else {
                pending.set(i, next);
            }
        }
    }
    public MatchPlayer player(TeamSide side) {
        return side == TeamSide.BLUE ? blue : red;
    }
    public TeamSide winner() {
        return winner;
    }

    /**
     * Regulation over: more crowns wins, level crowns go to overtime.
     * Overtime over: the most tower health left wins.
     */
    private void timeUp() {
        int blueCrowns = crowns(TeamSide.BLUE);
        int redCrowns = crowns(TeamSide.RED);
        if (blueCrowns != redCrowns) {
            finish(blueCrowns > redCrowns ? TeamSide.BLUE : TeamSide.RED,
                    MatchRecord.EndReason.CROWNS);
        } else if (!overtime) {
            overtime = true;
            ticksRemaining = OVERTIME_TICKS;
        } else {
            finishByTowerHealth();
        }
    }

    private void finishByTowerHealth() {
        int blueTotal = totalRemainingHealth(TeamSide.BLUE);
        int redTotal = totalRemainingHealth(TeamSide.RED);
        TeamSide winning = blueTotal > redTotal ? TeamSide.BLUE :
                redTotal > blueTotal ? TeamSide.RED : null;
        finish(winning, MatchRecord.EndReason.TOWER_HEALTH);
    }

    private int totalRemainingHealth(TeamSide side) {
        return tower(TowerId.left(side)).health() +
                tower(TowerId.right(side)).health() +
                tower(TowerId.king(side)).health();
    }
}
