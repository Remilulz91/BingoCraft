package fr.bingocraft.race;

import fr.bingocraft.bingo.BingoCard;
import fr.bingocraft.bingo.BingoMode;
import fr.bingocraft.bingo.BingoState;
import fr.bingocraft.bingo.CardGenerator;
import fr.bingocraft.bingo.CardTracker;
import fr.bingocraft.config.BingoCraftConfig;
import fr.bingocraft.screen.CardScreenHandler;
import fr.bingocraft.screen.TeamScreenHandler;
import fr.bingocraft.team.BingoTeam;
import fr.bingocraft.team.Teams;
import fr.bingocraft.util.Feedback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Race mode (world mode {@link BingoMode#RACE}). {@code /bingo start} checks that every online
 * player chose a team or to spectate ({@code /bingo team}), freezes the teams and runs a 10 s
 * countdown. At GO every racer is cleared, put in survival and dropped from the top of the world
 * at their team's random spot, with a minute of fall protection, regeneration and saturation;
 * spectators switch to spectator mode where they are. The first team to fill a row or a column
 * wins. 30 s later (last 10 s counted down) everybody is sent back to the world spawn in creative
 * mode and the teams are dissolved.
 * Teams are locked from /bingo start until then.
 */
public final class RaceManager {

    private static final Random RANDOM = new Random();
    /** The last seconds before the return to spawn are counted down in the action bar. */
    private static final int RETURN_COUNTDOWN_TICKS = 200;

    private RaceManager() { }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(RaceManager::tick);

        // Fall protection after the drop.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player) || !source.isIn(DamageTypeTags.IS_FALL)) return true;
            Race race = BingoState.get(player.server).race();
            return race == null || !race.isProtected(player.getUuid(), player.getServerWorld().getTime());
        });
    }

    private static void tick(MinecraftServer server) {
        BingoState state = BingoState.get(server);
        Race race = state.race();
        if (race == null) return;

        long now = server.getOverworld().getTime();
        if (race.isFinished()) {
            long left = race.finishedAt() + Race.RETURN_DELAY_TICKS - now;
            if (left <= 0) {
                reset(server, state);
            } else if (left <= RETURN_COUNTDOWN_TICKS && left % 20 == 0) {
                Text message = Text.translatable("bingocraft.race.returning_in", left / 20).formatted(Formatting.YELLOW);
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    p.sendMessage(message, true); // action bar
                    p.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.MASTER, 0.7f,
                            left <= 60 ? 1.4f : 1.0f);
                }
            }
            return;
        }
        if (race.isCountingDown()) {
            countdown(server, state, race);
            return;
        }
        if (server.getTicks() % 20 != 0) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String team = race.teamOf(player.getUuid());
            if (team != null && !race.isDeployed(player.getUuid())) {
                deploy(server, state, race, player, team); // was offline at GO
            }
            // The race clock in the card view, once per second.
            CardScreenHandler.refresh(player, state);
        }
        Iterator<Map.Entry<UUID, Long>> it = race.protections().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (now < entry.getValue()) continue;
            it.remove();
            state.raceChanged();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                player.sendMessage(Text.translatable("bingocraft.race.protection_over").formatted(Formatting.RED), false);
                player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.MASTER, 1.0f, 0.6f);
            }
        }
    }

    private static void countdown(MinecraftServer server, BingoState state, Race race) {
        int left = race.tickCountdown();
        if (left > 0) {
            if (left % 20 == 0) {
                Text number = Text.literal(String.valueOf(left / 20)).formatted(Formatting.YELLOW, Formatting.BOLD);
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    Feedback.title(p, number, Text.empty(), 0, 25, 5);
                    p.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.MASTER, 1.0f, 1.0f);
                }
            }
            return;
        }

        ServerWorld world = server.getOverworld();
        race.begin(world.getTime());
        int radius = Math.max(16, BingoCraftConfig.get().raceSpawnRadius);
        for (String team : race.teams()) race.setSpawn(team, SpawnFinder.find(world, RANDOM, radius));
        state.raceChanged();

        Text go = Text.translatable("bingocraft.race.go").formatted(Formatting.GREEN, Formatting.BOLD);
        Text goal = Text.translatable("bingocraft.race.goal").formatted(Formatting.WHITE);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            String team = race.teamOf(p.getUuid());
            if (team != null) {
                deploy(server, state, race, p, team);
            } else {
                // Spectators stay where they are; vanilla spectator mode lets them teleport to
                // any player (hotbar menu, key 1), no OP needed.
                p.changeGameMode(GameMode.SPECTATOR);
            }
            Feedback.title(p, go, goal, 5, 60, 20);
            p.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
        }
        server.getPlayerManager().broadcast(Text.translatable("bingocraft.race.started")
                .formatted(Formatting.AQUA).append(Text.literal(" ")).append(openLink()), false);
    }

    /** Clears the player and drops them at their team's spot with a minute of protection. */
    private static void deploy(MinecraftServer server, BingoState state, Race race, ServerPlayerEntity player, String team) {
        ServerWorld world = server.getOverworld();
        BlockPos spawn = race.spawn(team);
        if (spawn == null) return;

        player.getInventory().clear();
        player.clearStatusEffects();
        player.changeGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.getHungerManager().setFoodLevel(20);
        player.getHungerManager().setSaturationLevel(5.0f);

        // Team mates land side by side rather than inside each other.
        double x = spawn.getX() + 0.5 + RANDOM.nextInt(5) - 2;
        double z = spawn.getZ() + 0.5 + RANDOM.nextInt(5) - 2;
        player.teleport(world, x, spawn.getY(), z, player.getYaw(), 0.0f);
        player.fallDistance = 0.0f;

        int ticks = Math.max(0, BingoCraftConfig.get().raceProtectionSeconds) * 20;
        if (ticks > 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, ticks, 1));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, ticks, 0));
            race.protect(player.getUuid(), world.getTime() + ticks);
            player.sendMessage(Text.translatable("bingocraft.race.protected", ticks / 20).formatted(Formatting.GREEN), false);
        }
        race.markDeployed(player.getUuid());
        state.raceChanged();
    }

    // === Start / stop ===

    /** Starts a race. Returns an error message, or null on success. */
    public static Text start(MinecraftServer server) {
        BingoState state = BingoState.get(server);
        if (state.mode() != BingoMode.RACE) return Text.translatable("bingocraft.race.wrong_mode");
        if (state.race() != null) return Text.translatable("bingocraft.race.already_running");

        List<String> undecided = new ArrayList<>();
        List<ServerPlayerEntity> racers = new ArrayList<>();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (Teams.of(p) != null) racers.add(p);
            else if (!Teams.isSpectator(p)) undecided.add(p.getGameProfile().getName());
        }
        if (!undecided.isEmpty()) {
            return Text.translatable("bingocraft.race.undecided", String.join(", ", undecided));
        }
        if (racers.isEmpty()) return Text.translatable("bingocraft.race.no_players");

        // Inventories are emptied at GO, so nothing carried now matters.
        BingoCard card = CardGenerator.generate(CardGenerator.SIZE, server, RANDOM, Set.of());
        if (card == null) return Text.translatable("bingocraft.card.pool_too_small", CardGenerator.SIZE * CardGenerator.SIZE);

        Race race = new Race(card);
        for (ServerPlayerEntity p : racers) race.addMember(p.getUuid(), Teams.of(p).id());
        state.setRace(race);

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.currentScreenHandler instanceof CardScreenHandler) p.closeHandledScreen();
            TeamScreenHandler.refresh(p);
        }
        server.getPlayerManager().broadcast(Text.translatable("bingocraft.race.announce",
                Text.translatable("bingocraft.race.goal")).formatted(Formatting.GOLD), false);
        return null;
    }

    /** Cancels the race in progress: everybody goes back to spawn right away. False if there was none. */
    public static boolean stop(MinecraftServer server) {
        BingoState state = BingoState.get(server);
        if (state.race() == null) return false;
        server.getPlayerManager().broadcast(Text.translatable("bingocraft.race.stopped").formatted(Formatting.RED), false);
        reset(server, state);
        return true;
    }

    /**
     * After a race: everybody online back to the world spawn in creative mode, all bingo teams
     * dissolved and spectator choices forgotten, so teams are picked again.
     */
    private static void reset(MinecraftServer server, BingoState state) {
        state.setRace(null);
        ServerWorld world = server.getOverworld();
        BlockPos spawn = world.getSpawnPos();
        world.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.currentScreenHandler instanceof CardScreenHandler) p.closeHandledScreen();
            p.changeGameMode(GameMode.CREATIVE);
            p.teleport(world, spawn.getX() + 0.5, y, spawn.getZ() + 0.5, p.getYaw(), p.getPitch());
            p.fallDistance = 0.0f;
        }
        Teams.clearAll(server);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) TeamScreenHandler.refresh(p);
        server.getPlayerManager().broadcast(Text.translatable("bingocraft.race.reset").formatted(Formatting.AQUA), false);
    }

    // === Tracking ===

    /** Called by CardTracker at every check while a race is running. */
    public static void check(MinecraftServer server, BingoState state) {
        Race race = state.race();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isSpectator() || !race.isDeployed(player.getUuid())) continue;
            BingoCard card = cardOf(player, state);
            if (card == null) continue;
            Set<Item> owned = CardTracker.ownedItems(player);
            for (int i = 0; i < card.cellCount(); i++) {
                Item item = card.item(i);
                if (!card.isDone(i) && item != null && owned.contains(item)) {
                    complete(server, state, player, card, i);
                    if (state.race() != race) return; // that square won the race
                }
            }
        }
    }

    /** The card of the player's team, or null if they are not racing. */
    public static BingoCard cardOf(ServerPlayerEntity player, BingoState state) {
        String team = state.race().teamOf(player.getUuid());
        return team != null ? state.race().card(team) : null;
    }

    /** Ticks a square for the player's team. Also used by /bingo debug complete. */
    public static void complete(MinecraftServer server, BingoState state, ServerPlayerEntity player,
                                BingoCard card, int index) {
        Race race = state.race();
        String team = race != null ? race.teamOf(player.getUuid()) : null;
        if (team == null || !card.markDone(index)) return;
        state.raceChanged();

        // The team learns which item; everybody else only that the team scored.
        Text forTeam = Text.translatable("bingocraft.race.goal_done",
                        card.goalName(index).copy().formatted(Formatting.WHITE),
                        player.getDisplayName(), card.completedCount(), card.cellCount())
                .formatted(Formatting.GREEN);
        Text forOthers = Text.translatable("bingocraft.race.rival_scored",
                        teamName(team), card.completedCount(), card.cellCount())
                .formatted(Formatting.GRAY);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            boolean mate = team.equals(race.teamOf(p.getUuid()));
            p.sendMessage(mate ? forTeam : forOthers, false);
            if (mate) {
                p.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.6f, 1.2f);
                CardScreenHandler.refresh(p, state);
            }
        }

        if (card.hasRowOrColumn()) win(server, state, race, team);
    }

    private static void win(MinecraftServer server, BingoState state, Race race, String team) {
        String time = formatTime(server.getOverworld().getTime() - race.startTime());
        Text name = teamName(team);
        BingoTeam bingoTeam = BingoTeam.byId(team);
        int color = bingoTeam != null ? bingoTeam.rgb() : 0xFFD700;
        List<UUID> winners = new ArrayList<>();
        for (Map.Entry<UUID, String> e : race.members().entrySet()) if (e.getValue().equals(team)) winners.add(e.getKey());
        race.finish(server.getOverworld().getTime());
        state.raceChanged();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.currentScreenHandler instanceof CardScreenHandler) p.closeHandledScreen();
        }

        Text title = Text.translatable("bingocraft.race.winner", name).formatted(Formatting.GOLD, Formatting.BOLD);
        Text subtitle = Text.translatable("bingocraft.race.winner.time", time).formatted(Formatting.YELLOW);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            Feedback.title(p, title, subtitle, 10, 100, 30);
            p.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0f, 1.0f);
            if (winners.contains(p.getUuid())) Feedback.fireworks(p, color, 0xFFD700);
        }
        server.getPlayerManager().broadcast(Text.translatable("bingocraft.race.winner.chat", name, time)
                .formatted(Formatting.GOLD, Formatting.BOLD), false);
        server.getPlayerManager().broadcast(Text.translatable("bingocraft.race.returning",
                Race.RETURN_DELAY_TICKS / 20).formatted(Formatting.GRAY), false);
    }

    // === Helpers ===

    /** "Red" in red, for a team id. */
    public static Text teamName(String team) {
        BingoTeam bingoTeam = BingoTeam.byId(team);
        return bingoTeam != null ? bingoTeam.displayName() : Text.literal(team);
    }

    public static String formatTime(long ticks) {
        long seconds = Math.max(0, ticks / 20);
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }

    private static MutableText openLink() {
        return Text.translatable("bingocraft.endless.open").styled(style -> style
                .withColor(Formatting.YELLOW)
                .withUnderline(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bingo card")));
    }
}
