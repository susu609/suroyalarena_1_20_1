package net.susu609.royalarena.match;

import net.susu609.royalarena.card.BattleCard;

/**
 * An accepted card waiting out its deploy time before the troop spawns, or a
 * spell in flight before it lands.
 */
public record PendingDeploy(BattleCard card, TeamSide side, int x, int z,
                            int ticksLeft)
{
    public PendingDeploy tickDown() {
        return new PendingDeploy(card, side, x, z, Math.max(0, ticksLeft - 1));
    }

    /** 0 when just placed, 1 when the troop appears or the spell lands. */
    public float progress() {
        return 1.0F - ticksLeft / (float) Math.max(1, card.deployTicks());
    }
}
