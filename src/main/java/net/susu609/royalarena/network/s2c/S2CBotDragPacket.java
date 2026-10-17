package net.susu609.royalarena.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.DeckRules;
import net.susu609.royalarena.client.render.BotDragRenderer;
import net.susu609.royalarena.match.TeamSide;

import java.util.function.Supplier;

/**
 * The bot is dragging a card: show it sliding from (fromX, fromZ) to the
 * block centre (toX, toZ) over {@code ticks}, then the server plays it.
 */
public record S2CBotDragPacket(BattleCard card, TeamSide side, float fromX,
                               float fromZ, float toX, float toZ, int ticks)
{
    public static void encode(S2CBotDragPacket p, FriendlyByteBuf buffer) {
        buffer.writeUtf(p.card.id(), 32);
        buffer.writeEnum(p.side);
        buffer.writeFloat(p.fromX);
        buffer.writeFloat(p.fromZ);
        buffer.writeFloat(p.toX);
        buffer.writeFloat(p.toZ);
        buffer.writeVarInt(p.ticks);
    }

    public static S2CBotDragPacket decode(FriendlyByteBuf buffer) {
        BattleCard card = DeckRules.find(buffer.readUtf(32));
        return new S2CBotDragPacket(card, buffer.readEnum(TeamSide.class),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), Math.max(1, Math.min(100,
                        buffer.readVarInt())));
    }

    public static void handle(S2CBotDragPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        if (packet.card == null)
            return;
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> BotDragRenderer.add(packet));
    }
}
