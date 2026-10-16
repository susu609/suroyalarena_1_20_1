package net.susu609.royalarena.match;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.tower.TowerId;
import net.susu609.royalarena.tower.TowerKind;
import net.susu609.royalarena.tower.TowerState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MatchInstance {
    public static final int DEFAULT_BATTLE_TICKS = 20 * 180;
    /** Time between an accepted card play and the troop appearing (1 s). */
    public static final int DEPLOY_TICKS = 20;

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
    private TeamSide winner;

    private MatchInstance(UUID matchId, UUID blueId, UUID redId,
            ArenaLayout arena)
    {
        this.matchId = matchId;
        this.arena = arena;
        this.blue = new MatchPlayer(blueId, TeamSide.BLUE);
        this.red = new MatchPlayer(redId, TeamSide.RED);
        for (TowerId id : TowerId.values()) {
            towers.put(id, new TowerState(id, id.kind().maxHealth()));
        }
    }

    public static MatchInstance create(UUID matchId, UUID blueId, UUID redId,
            ArenaLayout arena)
    {
        if (blueId.equals(redId))
            throw new IllegalArgumentException(
                    "A match needs two distinct players");
        return new MatchInstance(matchId, blueId, redId, arena);
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
        TowerState target = tower(id);
        target.damage(damage);
        if (id.kind() == TowerKind.KING && target.isDestroyed()) {
            finish(id.side().opponent());
        }
    }
    public void finish(TeamSide winningSide) {
        if (state == MatchState.FINISHED)
            return;
        state = MatchState.FINISHED;
        ticksRemaining = 0;
        winner = winningSide;
        pending.clear();
        ready.clear();
    }
    public UUID matchId() {
        return matchId;
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

        blue.elixir().tick();
        red.elixir().tick();
        advancePending();
        if (--ticksRemaining <= 0) {
            finishByTowerHealth();
        }
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
        if (!arena.canDeploy(side, worldX, worldZ))
            return DeployResult.rejected("outside_deploy_area");
        MatchPlayer player = player(side);
        BattleCard card = player.deck().cardAt(slot);
        if (!player.elixir().spend(card.cost()))
            return DeployResult.rejected("not_enough_elixir");
        player.deck().play(slot);
        pending.add(new PendingDeploy(card, side, worldX, worldZ,
                DEPLOY_TICKS));
        return DeployResult.accepted(card);
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

    private void finishByTowerHealth() {
        int blueTotal = totalRemainingHealth(TeamSide.BLUE);
        int redTotal = totalRemainingHealth(TeamSide.RED);
        if (blueTotal > redTotal)
            finish(TeamSide.BLUE);
        else if (redTotal > blueTotal)
            finish(TeamSide.RED);
        else {
            finish(null);
        }
    }

    private int totalRemainingHealth(TeamSide side) {
        return tower(TowerId.left(side)).health() +
                tower(TowerId.right(side)).health() +
                tower(TowerId.king(side)).health();
    }
}
