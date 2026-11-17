package fr.bingocraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fr.bingocraft.BingoCraft;
import fr.bingocraft.bingo.BingoState;
import fr.bingocraft.race.RaceManager;
import fr.bingocraft.bingo.BingoMode;
import fr.bingocraft.bingo.Modes;
import fr.bingocraft.screen.ModeScreenHandler;
import fr.bingocraft.config.BingoCraftConfig;
import fr.bingocraft.screen.CardScreenHandler;
import fr.bingocraft.screen.TeamScreenHandler;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * /bingo administrative + utility commands:
 *   /bingo card      - open your card (double chest view; your side's card during a race)
 *   /bingo team      - pick a race team (or spectate), race mode only
 *   /bingo mode [endless|race|off] - OP, pick the world's game mode (menu without argument)
 *   /bingo start     - OP, start a race (race mode)
 *   /bingo stop      - OP, cancel the race in progress
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
                .then(CommandManager.literal("team")
                        .executes(BingoCommand::onTeam))
                .then(CommandManager.literal("mode")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(BingoCommand::onModeMenu)
                        .then(CommandManager.literal("endless").executes(ctx -> onMode(ctx, BingoMode.ENDLESS)))
                        .then(CommandManager.literal("race").executes(ctx -> onMode(ctx, BingoMode.RACE)))
                        .then(CommandManager.literal("off").executes(ctx -> onMode(ctx, BingoMode.NONE))))
                .then(CommandManager.literal("start")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(BingoCommand::onStart))
                .then(CommandManager.literal("stop")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(BingoCommand::onStop))
                .then(CommandManager.literal("version")
                        .executes(BingoCommand::onVersion))
                .then(CommandManager.literal("reload")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(BingoCommand::onReload))
                .then(DebugCommand.build())
        );
    }

    private static int onTeam(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.translatable("bingocraft.command.players_only"));
            return 0;
        }
        if (BingoState.get(ctx.getSource().getServer()).mode() != BingoMode.RACE) {
            ctx.getSource().sendError(Text.translatable("bingocraft.teams.wrong_mode"));
            return 0;
        }
        TeamScreenHandler.open(player);
        return 1;
    }

    private static int onModeMenu(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.translatable("bingocraft.command.players_only"));
            return 0;
        }
        ModeScreenHandler.open(player);
        return 1;
    }

    private static int onMode(CommandContext<ServerCommandSource> ctx, BingoMode mode) {
        Text error = Modes.set(ctx.getSource().getServer(), mode);
        if (error != null) {
            ctx.getSource().sendError(error);
            return 0;
        }
        return 1;
    }

    private static int onStart(CommandContext<ServerCommandSource> ctx) {
        Text error = RaceManager.start(ctx.getSource().getServer());
        if (error != null) {
            ctx.getSource().sendError(error);
            return 0;
        }
        return 1;
    }

    private static int onStop(CommandContext<ServerCommandSource> ctx) {
        if (!RaceManager.stop(ctx.getSource().getServer())) {
            ctx.getSource().sendError(Text.translatable("bingocraft.race.none"));
            return 0;
        }
        return 1;
    }

    private static int onCard(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.translatable("bingocraft.command.players_only"));
            return 0;
        }
        BingoState state = BingoState.get(ctx.getSource().getServer());
        if (!CardScreenHandler.open(player, state)) {
            String key = switch (state.mode()) {
                case NONE -> "bingocraft.mode.none_active";
                case ENDLESS -> "bingocraft.card.none";
                case RACE -> state.race() == null || !state.race().isRunning()
                        ? "bingocraft.race.not_started" : "bingocraft.race.not_racing";
            };
            ctx.getSource().sendError(Text.translatable(key));
            return 0;
        }
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
