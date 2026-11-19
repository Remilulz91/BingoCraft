package fr.bingocraft.race;

import fr.bingocraft.bingo.BingoCard;
import fr.bingocraft.team.BingoTeam;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.scoreboard.number.FixedNumberFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The race scoreboard on the right of the screen (vanilla sidebar, nothing needed client-side):
 * the race clock on top, then one line per team with its squares ("7/25"), best team first.
 * Shown from /bingo start until everybody is back at spawn.
 */
public final class RaceSidebar {

    private static final String OBJECTIVE = "bingocraft_race";
    // Holder names must NOT start with '#': the vanilla sidebar hides those entries. A '.' can't
    // be in a player name, so these never collide with a real player; the shown text is the
    // display text, not the holder name.
    private static final ScoreHolder CLOCK = ScoreHolder.fromName("bingo.clock");
    private static final ScoreHolder SPACER = ScoreHolder.fromName("bingo.spacer");
    /** Above any team score (25 at most), so the clock and the spacer stay on top. */
    private static final int CLOCK_SCORE = 1001;
    private static final int SPACER_SCORE = 1000;

    private RaceSidebar() { }

    /** Creates (or refreshes) the sidebar and shows it to everybody. */
    public static void update(MinecraftServer server, Race race, long elapsedTicks) {
        Scoreboard scoreboard = server.getScoreboard();
        ScoreboardObjective objective = scoreboard.getNullableObjective(OBJECTIVE);
        if (objective == null) {
            objective = scoreboard.addObjective(OBJECTIVE, ScoreboardCriterion.DUMMY,
                    Text.literal("BINGO").formatted(Formatting.GOLD, Formatting.BOLD),
                    ScoreboardCriterion.RenderType.INTEGER, false, null);
        }
        scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);

        line(scoreboard, objective, CLOCK, CLOCK_SCORE,
                Text.literal("⏱ " + RaceManager.formatTime(elapsedTicks)).formatted(Formatting.YELLOW));
        line(scoreboard, objective, SPACER, SPACER_SCORE, Text.empty());

        for (String team : race.teams()) {
            BingoCard card = race.card(team);
            BingoTeam bingoTeam = BingoTeam.byId(team);
            ScoreAccess score = scoreboard.getOrCreateScore(ScoreHolder.fromName("bingo.team." + team), objective);
            score.setScore(card.completedCount());
            score.setDisplayText(Text.literal("■ ").formatted(bingoTeam != null ? bingoTeam.color() : Formatting.WHITE)
                    .append(RaceManager.teamName(team)));
            score.setNumberFormat(new FixedNumberFormat(
                    Text.literal(card.completedCount() + "/" + card.cellCount()).formatted(Formatting.WHITE)));
        }
    }

    /** Removes the sidebar (back at spawn, or race cancelled). */
    public static void remove(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        ScoreboardObjective objective = scoreboard.getNullableObjective(OBJECTIVE);
        if (objective != null) scoreboard.removeObjective(objective);
    }

    private static void line(Scoreboard scoreboard, ScoreboardObjective objective, ScoreHolder holder,
                             int value, Text text) {
        ScoreAccess score = scoreboard.getOrCreateScore(holder, objective);
        score.setScore(value);
        score.setDisplayText(text);
        score.setNumberFormat(BlankNumberFormat.INSTANCE);
    }
}
