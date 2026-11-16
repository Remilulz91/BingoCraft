package fr.bingocraft.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.bingocraft.BingoCraft;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * /bingo debug subcommands. Only available in the DEBUG build (OP level 2).
 *
 *   /bingo debug info   - print runtime debug info
 */
public class DebugCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("debug")
                .requires(src -> BingoCraft.isDebugBuild() && src.hasPermissionLevel(2))
                .then(CommandManager.literal("info")
                        .executes(DebugCommand::onInfo));
    }

    private static int onInfo(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("[BingoCraft debug]").formatted(Formatting.LIGHT_PURPLE), false);
        src.sendFeedback(() -> Text.literal(" version: " + BingoCraft.getVersion()).formatted(Formatting.GRAY), false);
        src.sendFeedback(() -> Text.literal(" players online: "
                + src.getServer().getCurrentPlayerCount()).formatted(Formatting.GRAY), false);
        return 1;
    }
}
