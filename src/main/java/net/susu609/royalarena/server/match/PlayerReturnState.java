package net.susu609.royalarena.server.match;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

public record PlayerReturnState(ResourceKey<Level> dimension, double x,
                                double y, double z, float yaw, float pitch,
                                GameType gameType)
{

    public static PlayerReturnState capture(ServerPlayer player) {
        return new PlayerReturnState(
                player.level().dimension(), player.getX(), player.getY(),
                player.getZ(), player.getYRot(), player.getXRot(),
                player.gameMode.getGameModeForPlayer());
    }
}
