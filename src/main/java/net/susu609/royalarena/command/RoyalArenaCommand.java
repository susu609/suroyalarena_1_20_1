package net.susu609.royalarena.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.susu609.royalarena.server.match.MatchManager;
import net.susu609.royalarena.server.match.MatchQueue;

import java.util.List;

public final class RoyalArenaCommand {
    private RoyalArenaCommand() {
    }

    /** /royalarena bot (alias: practice): BLUE is you, a bot plays RED. */
    private static int startSoloVsBot(
            CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException
    {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!MatchManager.INSTANCE.startPractice(player)) {
            ctx.getSource().sendFailure(
                    Component.literal("You are already in a match."));
            return 0;
        }
        return 1;
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("royalarena")
                .then(Commands.literal("queue").executes(ctx -> {
                    ServerPlayer player =
                            ctx.getSource().getPlayerOrException();
                    MatchQueue.INSTANCE.join(player);
                    return 1;
                })).then(Commands.literal("leave").executes(ctx -> {
                    ServerPlayer player =
                            ctx.getSource().getPlayerOrException();
                    MatchQueue.INSTANCE.leave(player, "Cancelled");
                    return 1;
                })).then(Commands.literal("bot")
                        .executes(RoyalArenaCommand::startSoloVsBot))
                .then(Commands.literal("practice")
                        .executes(RoyalArenaCommand::startSoloVsBot))
                .then(Commands.literal("debug_start")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            List<ServerPlayer> players =
                                    ctx.getSource().getServer().getPlayerList()
                                            .getPlayers();
                            if (players.size() < 2) {
                                ctx.getSource().sendFailure(Component.literal(
                                        "Need at least two online players."));
                                return 0;
                            }
                            boolean started = MatchManager.INSTANCE.startMatch(
                                    players.get(0), players.get(1));
                            if (!started) {
                                ctx.getSource().sendFailure(Component.literal(
                                        "Could not start match; a player may " +
                                                "already be busy."));
                                return 0;
                            }
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal(
                                            "Royal Arena debug match started."),
                                    true);
                            return 1;
                        })).then(Commands.literal("stop_all")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            MatchManager.INSTANCE.stopAll();
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal(
                                            "Stopping all Royal Arena matches" +
                                                    "."),
                                    true);
                            return 1;
                        })));
    }
}
