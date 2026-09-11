package net.susu609.royalarena.match;

import net.susu609.royalarena.card.BattleCard;

public record DeployResult(boolean accepted, BattleCard card, String reason) {
    public static DeployResult accepted(BattleCard card) {
        return new DeployResult(true, card, "");
    }

    public static DeployResult rejected(String reason) {
        return new DeployResult(false, null, reason);
    }
}
