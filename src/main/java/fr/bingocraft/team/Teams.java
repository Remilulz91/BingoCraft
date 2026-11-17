package fr.bingocraft.team;

import fr.bingocraft.bingo.BingoState;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Team membership for races: which bingo team a player is in, or whether they spectate. */
public final class Teams {

    private Teams() { }

    /** The player's bingo team, or null (no team, or a non-bingo scoreboard team). */
    public static BingoTeam of(ServerPlayerEntity player) {
        Team team = player.getScoreboardTeam();
        return team != null ? BingoTeam.byScoreboardName(team.getName()) : null;
    }

    /** Joins a bingo team (creating the scoreboard team if needed) and stops spectating. */
    public static void join(ServerPlayerEntity player, BingoTeam bingoTeam) {
        Team team = ensure(player.server, bingoTeam);
        player.server.getScoreboard().addScoreHolderToTeam(player.getNameForScoreboard(), team);
        BingoState.get(player.server).setSpectator(player.getUuid(), false);
    }

    /** Leaves any team and watches the next race as a spectator. */
    public static void spectate(ServerPlayerEntity player) {
        player.server.getScoreboard().clearTeam(player.getNameForScoreboard());
        BingoState.get(player.server).setSpectator(player.getUuid(), true);
    }

    public static boolean isSpectator(ServerPlayerEntity player) {
        return BingoState.get(player.server).isSpectator(player.getUuid());
    }

    /** Dissolves every bingo team and forgets spectator choices (after a race, or leaving race mode). */
    public static void clearAll(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        for (BingoTeam bingoTeam : BingoTeam.values()) {
            Team team = scoreboard.getTeam(bingoTeam.scoreboardName());
            if (team != null) scoreboard.removeTeam(team);
        }
        BingoState.get(server).clearSpectators();
    }

    private static Team ensure(MinecraftServer server, BingoTeam bingoTeam) {
        Scoreboard scoreboard = server.getScoreboard();
        Team team = scoreboard.getTeam(bingoTeam.scoreboardName());
        if (team == null) {
            team = scoreboard.addTeam(bingoTeam.scoreboardName());
        }
        team.setDisplayName(bingoTeam.displayName());
        team.setColor(bingoTeam.color());
        return team;
    }
}
