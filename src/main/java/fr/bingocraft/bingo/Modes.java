package fr.bingocraft.bingo;

import fr.bingocraft.screen.CardScreenHandler;
import fr.bingocraft.screen.ModeScreenHandler;
import fr.bingocraft.screen.TeamScreenHandler;
import fr.bingocraft.team.Teams;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Switching the world's game mode ({@code /bingo mode}, OP only). */
public final class Modes {

    private Modes() { }

    /** Switches mode. Returns an error message, or null on success. */
    public static Text set(MinecraftServer server, BingoMode mode) {
        BingoState state = BingoState.get(server);
        if (state.race() != null) return Text.translatable("bingocraft.mode.race_in_progress");
        if (state.mode() == mode) return Text.translatable("bingocraft.mode.already", name(mode));

        if (state.mode() == BingoMode.RACE) Teams.clearAll(server); // teams only exist in race mode
        state.setMode(mode);

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.currentScreenHandler instanceof CardScreenHandler) p.closeHandledScreen();
            if (p.currentScreenHandler instanceof TeamScreenHandler) p.closeHandledScreen();
            ModeScreenHandler.refresh(p);
        }
        server.getPlayerManager().broadcast(Text.translatable("bingocraft.mode.changed", name(mode))
                .formatted(Formatting.GOLD, Formatting.BOLD), false);
        server.getPlayerManager().broadcast(Text.translatable("bingocraft.mode.hint." + mode.id())
                .formatted(Formatting.YELLOW), false);
        return null;
    }

    public static Text name(BingoMode mode) {
        return Text.translatable("bingocraft.mode." + mode.id());
    }
}
