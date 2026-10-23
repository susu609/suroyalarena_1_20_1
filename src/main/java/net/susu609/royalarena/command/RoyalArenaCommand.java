package net.susu609.royalarena.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.server.match.MatchManager;
import net.susu609.royalarena.server.match.MatchQueue;
import net.susu609.royalarena.sim.CardCompare;

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

    /**
     * /royalarena compare &lt;a&gt; &lt;b&gt;: the two-card comparison
     * (docs/COMPARE.md), measured with the real combat engine without
     * starting a match; takes about half a second.
     */
    private static int compare(CommandContext<CommandSourceStack> ctx) {
        BattleCard a = CardCompare.find(StringArgumentType.getString(ctx, "a"));
        BattleCard b = CardCompare.find(StringArgumentType.getString(ctx, "b"));
        if (a == null || b == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown card: " +
                    StringArgumentType.getString(ctx, a == null ? "a" : "b")));
            return 0;
        }
        List<String> lines = CardCompare.format(CardCompare.compare(a, b));
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            ChatFormatting color = i == 0 ? ChatFormatting.YELLOW :
                    i == lines.size() - 1 ? ChatFormatting.GREEN :
                    line.startsWith("  ") ? ChatFormatting.GRAY :
                    ChatFormatting.WHITE;
            ctx.getSource().sendSuccess(() -> Component.literal(line)
                    .withStyle(color), false);
        }
        return 1;
    }

    private static com.mojang.brigadier.suggestion.SuggestionProvider<
            CommandSourceStack> cardIds() {
        return (ctx, builder) -> SharedSuggestionProvider.suggest(
                java.util.Arrays.stream(BattleCard.values())
                        .map(BattleCard::id), builder);
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
                .then(Commands.literal("sandbox").executes(ctx -> {
                    // Any card for either side, no clock (docs/SANDBOX.md).
                    ServerPlayer player =
                            ctx.getSource().getPlayerOrException();
                    if (!MatchManager.INSTANCE.startSandbox(player)) {
                        ctx.getSource().sendFailure(Component.literal(
                                "You are already in a match."));
                        return 0;
                    }
                    return 1;
                }))
                .then(Commands.literal("compare")
                        .then(Commands.argument("a",
                                        StringArgumentType.string())
                                .suggests(cardIds())
                                .then(Commands.argument("b",
                                                StringArgumentType.string())
                                        .suggests(cardIds())
                                        .executes(RoyalArenaCommand::compare))))
                .then(Commands.literal("watch").executes(ctx -> {
                    // Bot against bot, watched from BLUE's seat.
                    ServerPlayer player =
                            ctx.getSource().getPlayerOrException();
                    if (!MatchManager.INSTANCE.startWatch(player)) {
                        ctx.getSource().sendFailure(Component.literal(
                                "You are already in a match."));
                        return 0;
                    }
                    return 1;
                }))
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
