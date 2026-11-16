package fr.bingocraft.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.bingocraft.BingoCraft;
import fr.bingocraft.bingo.BingoCard;
import fr.bingocraft.bingo.BingoState;
import fr.bingocraft.bingo.CardGenerator;
import fr.bingocraft.bingo.CardTracker;
import fr.bingocraft.bingo.GoalFilter;
import fr.bingocraft.config.BingoCraftConfig;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Random;

/**
 * /bingo debug subcommands. Only available in the DEBUG build (OP level 2).
 *
 *   /bingo debug info              - print runtime debug info
 *   /bingo debug pool              - number of items a card can draw from
 *   /bingo debug new [size]        - deal yourself a new card (size 3-5, default from config)
 *   /bingo debug complete <cell>   - mark cell n (1 = top left, row by row) as done
 *   /bingo debug clear             - remove your card
 */
public class DebugCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("debug")
                .requires(src -> BingoCraft.isDebugBuild() && src.hasPermissionLevel(2))
                .then(CommandManager.literal("info")
                        .executes(DebugCommand::onInfo))
                .then(CommandManager.literal("pool")
                        .executes(DebugCommand::onPool))
                .then(CommandManager.literal("new")
                        .executes(ctx -> onNew(ctx, BingoCraftConfig.get().cardSize))
                        .then(CommandManager.argument("size",
                                        IntegerArgumentType.integer(CardGenerator.MIN_SIZE, CardGenerator.MAX_SIZE))
                                .executes(ctx -> onNew(ctx, IntegerArgumentType.getInteger(ctx, "size")))))
                .then(CommandManager.literal("complete")
                        .then(CommandManager.argument("cell", IntegerArgumentType.integer(1))
                                .executes(DebugCommand::onComplete)))
                .then(CommandManager.literal("clear")
                        .executes(DebugCommand::onClear));
    }

    private static int onInfo(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("[BingoCraft debug]").formatted(Formatting.LIGHT_PURPLE), false);
        src.sendFeedback(() -> Text.literal(" version: " + BingoCraft.getVersion()).formatted(Formatting.GRAY), false);
        src.sendFeedback(() -> Text.literal(" players online: "
                + src.getServer().getCurrentPlayerCount()).formatted(Formatting.GRAY), false);
        return 1;
    }

    private static int onPool(CommandContext<ServerCommandSource> ctx) {
        int eligible = GoalFilter.candidates(ctx.getSource().getServer()).size();
        int total = net.minecraft.registry.Registries.ITEM.size();
        ctx.getSource().sendFeedback(() -> Text.literal(" " + eligible + " / " + total
                + " items can appear on a card").formatted(Formatting.GRAY), false);
        return 1;
    }

    private static int onNew(CommandContext<ServerCommandSource> ctx, int size) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return playersOnly(ctx);
        BingoCard card = CardGenerator.generate(size, ctx.getSource().getServer(), new Random());
        if (card == null) {
            ctx.getSource().sendError(Text.translatable("bingocraft.card.pool_too_small", size * size));
            return 0;
        }
        BingoState.get(ctx.getSource().getServer()).put(player.getUuid(), card);
        ctx.getSource().sendFeedback(() -> Text.translatable("bingocraft.debug.new", size, size)
                .formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int onComplete(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return playersOnly(ctx);
        BingoState state = BingoState.get(ctx.getSource().getServer());
        BingoCard card = state.get(player.getUuid());
        if (card == null) {
            ctx.getSource().sendError(Text.translatable("bingocraft.card.none"));
            return 0;
        }
        int cell = IntegerArgumentType.getInteger(ctx, "cell");
        if (cell > card.cellCount()) {
            ctx.getSource().sendError(Text.literal("Cell must be between 1 and " + card.cellCount()));
            return 0;
        }
        CardTracker.complete(player, state, card, cell - 1);
        return 1;
    }

    private static int onClear(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return playersOnly(ctx);
        boolean removed = BingoState.get(ctx.getSource().getServer()).remove(player.getUuid());
        ctx.getSource().sendFeedback(() -> Text.translatable(removed ? "bingocraft.debug.cleared" : "bingocraft.card.none")
                .formatted(Formatting.GRAY), false);
        return 1;
    }

    private static int playersOnly(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendError(Text.translatable("bingocraft.command.players_only"));
        return 0;
    }
}
