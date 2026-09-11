package net.susu609.royalarena.server.battle;

import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.UUID;

public final class ServerTroop {
    private final UUID entityId;
    private final TeamSide side;
    private final int towerDamage;
    private final double moveSpeed;
    private TowerId targetTowerId;
    private int attackCooldown;

    public ServerTroop(UUID entityId, TeamSide side, int towerDamage,
            double moveSpeed)
    {
        this.entityId = entityId;
        this.side = side;
        this.towerDamage = towerDamage;
        this.moveSpeed = moveSpeed;
    }

    public UUID entityId() {
        return entityId;
    }
    public TeamSide side() {
        return side;
    }
    public TowerId targetTower(MatchInstance match, double worldX) {
        if (targetTowerId == null) {
            targetTowerId = match.chooseTowerTarget(side, worldX);
        } else if (match.tower(targetTowerId).isDestroyed()) {
            targetTowerId = TowerId.king(side.opponent());
        }
        return targetTowerId;
    }
    public double moveSpeed() {
        return moveSpeed;
    }
    public TowerId targetTowerId() {
        return targetTowerId;
    }
    public int towerDamage() {
        return towerDamage;
    }

    public boolean readyToAttack() {
        if (attackCooldown > 0) {
            attackCooldown--;
            return false;
        }
        return true;
    }

    public void resetAttackCooldown() {
        attackCooldown = 20;
    }
}
