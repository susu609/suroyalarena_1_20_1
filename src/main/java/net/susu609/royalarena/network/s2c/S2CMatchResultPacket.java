package net.susu609.royalarena.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.client.screen.ResultScreen;
import net.susu609.royalarena.match.MatchRecord;
import net.susu609.royalarena.match.MatchResult;
import net.susu609.royalarena.match.TeamSide;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * The match is over: open the result screen with the crowns, the winner and
 * the MVP. Names and ids are for the ribbons (a bot has no name and no skin).
 */
public record S2CMatchResultPacket(MatchResult result, TeamSide localSide,
                                   String blueName, String redName,
                                   UUID bluePlayer, UUID redPlayer)
{
    private static final int NAME_LENGTH = 32;

    public static void encode(S2CMatchResultPacket p, FriendlyByteBuf buffer) {
        MatchResult r = p.result;
        buffer.writeByte(r.winner() == null ? -1 : r.winner().ordinal());
        buffer.writeByte(r.blueCrowns());
        buffer.writeByte(r.redCrowns());
        buffer.writeByte(r.endReason().ordinal());
        buffer.writeVarInt(r.battleTicks());
        buffer.writeUtf(r.blueMvp() == null ? "" : r.blueMvp().id());
        buffer.writeFloat((float) r.blueMvpDamage());
        buffer.writeUtf(r.redMvp() == null ? "" : r.redMvp().id());
        buffer.writeFloat((float) r.redMvpDamage());
        buffer.writeBoolean(r.vsBot());
        buffer.writeByte(p.localSide.ordinal());
        buffer.writeUtf(p.blueName, NAME_LENGTH);
        buffer.writeUtf(p.redName, NAME_LENGTH);
        buffer.writeUUID(p.bluePlayer);
        buffer.writeUUID(p.redPlayer);
    }

    public static S2CMatchResultPacket decode(FriendlyByteBuf buffer) {
        TeamSide[] sides = TeamSide.values();
        int winner = buffer.readByte();
        int blue = clampCrowns(buffer.readByte());
        int red = clampCrowns(buffer.readByte());
        MatchRecord.EndReason[] reasons = MatchRecord.EndReason.values();
        int reason = buffer.readByte();
        int ticks = Math.max(0, buffer.readVarInt());
        BattleCard blueMvp = card(buffer.readUtf());
        float blueDamage = buffer.readFloat();
        BattleCard redMvp = card(buffer.readUtf());
        float redDamage = buffer.readFloat();
        boolean vsBot = buffer.readBoolean();
        int local = buffer.readByte();
        MatchResult result = new MatchResult(
                winner >= 0 && winner < sides.length ? sides[winner] : null,
                blue, red, reason >= 0 && reason < reasons.length ?
                        reasons[reason] : MatchRecord.EndReason.ABANDONED,
                ticks, blueMvp, blueDamage, redMvp, redDamage, vsBot);
        return new S2CMatchResultPacket(result,
                local == 1 ? TeamSide.RED : TeamSide.BLUE,
                buffer.readUtf(NAME_LENGTH), buffer.readUtf(NAME_LENGTH),
                buffer.readUUID(), buffer.readUUID());
    }

    private static int clampCrowns(int crowns) {
        return Math.max(0, Math.min(3, crowns));
    }

    private static BattleCard card(String id) {
        for (BattleCard card : BattleCard.values()) {
            if (card.id().equals(id))
                return card;
        }
        return null;
    }

    public static void handle(S2CMatchResultPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ResultScreen.open(packet));
    }
}
