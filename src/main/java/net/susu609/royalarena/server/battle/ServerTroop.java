package net.susu609.royalarena.server.battle;

import net.susu609.royalarena.match.TeamSide;

import java.util.UUID;

public final class ServerTroop {
    private final UUID entityId;
    private final TeamSide side;
    private final int towerDamage;
    private final double moveSpeed;
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
    public double moveSpeed() {
        return moveSpeed;
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
    public TeamSide side() {
        return side;
    }
    public int towerDamage() {
        return towerDamage;
    }
}
