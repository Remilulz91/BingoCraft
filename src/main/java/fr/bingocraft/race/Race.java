package fr.bingocraft.race;

import fr.bingocraft.bingo.BingoCard;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A race: one 5x5 card, every team plays its own copy; the first full row, column or diagonal wins. Teams are frozen at /bingo start
 * ({@link #members}): changing scoreboard team during the race changes nothing.
 * Saved with the world through {@code BingoState}, so a race survives a restart.
 */
public final class Race {

    public static final int COUNTDOWN_TICKS = 200;
    /** Ticks between the win and everybody's return to the world spawn. */
    public static final int RETURN_DELAY_TICKS = 600;

    private final BingoCard template;
    /** Team id -> that team's card. */
    private final Map<String, BingoCard> cards = new LinkedHashMap<>();
    /** Player -> team id, fixed when the race starts. */
    private final Map<UUID, String> members = new HashMap<>();
    /** Team id -> where the team lands (chosen at GO). */
    private final Map<String, BlockPos> spawns = new HashMap<>();
    /** Players already dropped in (cleared, teleported): a member who was offline at GO is dropped when they come back. */
    private final Set<UUID> deployed = new HashSet<>();
    /** Player -> world time until which fall damage is ignored. */
    private final Map<UUID, Long> protectedUntil = new HashMap<>();
    private boolean running;
    private long startTime;
    /** World time of the win, or -1 while the race is on. */
    private long finishedAt = -1;
    private int countdown = COUNTDOWN_TICKS; // not saved: a restart replays the countdown

    public Race(BingoCard template) {
        this.template = template;
    }

    /** True between GO and the win: squares are tracked. */
    public boolean isRunning() { return running && finishedAt < 0; }
    /** True after the win, until everybody is sent back to spawn. */
    public boolean isFinished() { return finishedAt >= 0; }
    long finishedAt() { return finishedAt; }
    void finish(long worldTime) { finishedAt = worldTime; }
    /** True during the countdown. */
    public boolean isCountingDown() { return !running && finishedAt < 0; }
    /** World time (ticks) at which the countdown ended. */
    public long startTime() { return startTime; }

    void begin(long worldTime) {
        running = true;
        startTime = worldTime;
    }

    int tickCountdown() { return --countdown; }

    void addMember(UUID player, String team) {
        members.put(player, team);
        cards.computeIfAbsent(team, k -> template.blankCopy());
    }

    /** The player's team id, or null if they are not racing. */
    public String teamOf(UUID player) { return members.get(player); }

    public BingoCard card(String team) { return cards.get(team); }

    Set<String> teams() { return cards.keySet(); }

    Map<UUID, String> members() { return members; }

    BlockPos spawn(String team) { return spawns.get(team); }
    void setSpawn(String team, BlockPos pos) { spawns.put(team, pos); }

    boolean isDeployed(UUID player) { return deployed.contains(player); }
    void markDeployed(UUID player) { deployed.add(player); }

    void protect(UUID player, long until) { protectedUntil.put(player, until); }
    public boolean isProtected(UUID player, long now) {
        Long until = protectedUntil.get(player);
        return until != null && now < until;
    }
    Map<UUID, Long> protections() { return protectedUntil; }

    // === NBT ===

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.put("template", template.toNbt());
        nbt.putBoolean("running", running);
        nbt.putLong("startTime", startTime);
        nbt.putLong("finishedAt", finishedAt);

        NbtList teams = new NbtList();
        for (Map.Entry<String, BingoCard> entry : cards.entrySet()) {
            NbtCompound tag = new NbtCompound();
            tag.putString("team", entry.getKey());
            tag.put("card", entry.getValue().toNbt());
            BlockPos spawn = spawns.get(entry.getKey());
            if (spawn != null) tag.putLong("spawn", spawn.asLong());
            teams.add(tag);
        }
        nbt.put("teams", teams);

        NbtList players = new NbtList();
        for (Map.Entry<UUID, String> entry : members.entrySet()) {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("player", entry.getKey());
            tag.putString("team", entry.getValue());
            tag.putBoolean("deployed", deployed.contains(entry.getKey()));
            Long until = protectedUntil.get(entry.getKey());
            if (until != null) tag.putLong("protectedUntil", until);
            players.add(tag);
        }
        nbt.put("players", players);
        return nbt;
    }

    /** Returns null if the data is unusable (the race is then dropped). */
    public static Race fromNbt(NbtCompound nbt) {
        BingoCard template = BingoCard.fromNbt(nbt.getCompound("template"));
        if (template == null) return null;
        Race race = new Race(template);
        race.running = nbt.getBoolean("running");
        race.startTime = nbt.getLong("startTime");
        race.finishedAt = nbt.contains("finishedAt") ? nbt.getLong("finishedAt") : -1;

        NbtList teams = nbt.getList("teams", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < teams.size(); i++) {
            NbtCompound tag = teams.getCompound(i);
            BingoCard card = BingoCard.fromNbt(tag.getCompound("card"));
            if (card == null) continue;
            race.cards.put(tag.getString("team"), card);
            if (tag.contains("spawn")) race.spawns.put(tag.getString("team"), BlockPos.fromLong(tag.getLong("spawn")));
        }
        NbtList players = nbt.getList("players", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < players.size(); i++) {
            NbtCompound tag = players.getCompound(i);
            if (!tag.containsUuid("player")) continue;
            UUID uuid = tag.getUuid("player");
            race.members.put(uuid, tag.getString("team"));
            if (tag.getBoolean("deployed")) race.deployed.add(uuid);
            if (tag.contains("protectedUntil")) race.protectedUntil.put(uuid, tag.getLong("protectedUntil"));
        }
        return race;
    }
}
