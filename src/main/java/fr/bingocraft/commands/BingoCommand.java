package fr.bingocraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fr.bingocraft.BingoCraft;
import fr.bingocraft.bingo.BingoCard;
import fr.bingocraft.bingo.BingoState;
import fr.bingocraft.config.BingoCraftConfig;
import fr.bingocraft.screen.CardScreenHandler;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * /bingo administrative + utility commands:
 *   /bingo card      - open your card (double chest view)
 *   /bingo version   - print mod version + build type
 *   /bingo reload    - OP, reload config from disk
 *   /bingo debug ... - DEBUG build only
 */
public class BingoCommand {

    public static void register(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment
    ) {
        dispatcher.register(CommandManager.literal("bingo")
                .then(CommandManager.literal("card")
                        .executes(BingoCommand::onCard))
                .then(CommandManager.literal("version")
                        .executes(BingoCommand::onVersion))
                .then(CommandManager.literal("reload")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(BingoCommand::onReload))
                .then(DebugCommand.build())
        );
    }

    private static int onCard(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.translatable("bingocraft.command.players_only"));
            return 0;
        }
        BingoCard card = BingoState.get(ctx.getSource().getServer()).get(player.getUuid());
        if (card == null) {
            ctx.getSource().sendError(Text.translatable("bingocraft.card.none"));
            return 0;
        }
        CardScreenHandler.open(player, card);
        return 1;
    }

    private static int onVersion(CommandContext<ServerCommandSource> ctx) {
        String buildType = BingoCraft.isDebugBuild() ? "DEBUG" : "PUBLIC";
        ctx.getSource().sendFeedback(
                () -> Text.literal("BingoCraft v" + BingoCraft.getVersion() + " ").formatted(Formatting.GOLD)
                        .append(Text.literal("(" + buildType + " build)").formatted(Formatting.GRAY)),
                false
        );
        return 1;
    }

    private static int onReload(CommandContext<ServerCommandSource> ctx) {
        BingoCraftConfig.load();
        ctx.getSource().sendFeedback(
                () -> Text.translatable("bingocraft.command.reloaded").formatted(Formatting.GREEN),
                true
        );
        return 1;
    }
}
