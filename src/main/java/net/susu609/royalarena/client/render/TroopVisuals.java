package net.susu609.royalarena.client.render;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.match.TeamSide;

/**
 * What the client can tell about a troop mob. The server names every troop
 * after its card, coloured by team (entity tags are not synced), so the name
 * gives both the card and the side.
 */
public final class TroopVisuals {
    private static final TextColor BLUE =
            TextColor.fromLegacyFormat(ChatFormatting.BLUE);
    private static final TextColor RED =
            TextColor.fromLegacyFormat(ChatFormatting.RED);

    private TroopVisuals() {
    }

    /** The troop's card, or null when the entity is not a troop. */
    public static BattleCard cardOf(Entity entity) {
        Component name = entity.getCustomName();
        return name == null ? null : BattleCard.byDisplayName(name.getString());
    }

    /** The troop's side, or null when the entity is not a troop. */
    public static TeamSide sideOf(Entity entity) {
        Component name = entity.getCustomName();
        TextColor color = name == null ? null : name.getStyle().getColor();
        if (BLUE.equals(color))
            return TeamSide.BLUE;
        if (RED.equals(color))
            return TeamSide.RED;
        return null;
    }

    /** How big the troop is drawn (1 for anything that is not a troop). */
    public static float scale(Entity entity) {
        BattleCard card = cardOf(entity);
        return card == null ? 1.0F : card.modelScale();
    }
}
