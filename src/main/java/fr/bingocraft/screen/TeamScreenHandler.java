package fr.bingocraft.screen;

import fr.bingocraft.bingo.BingoState;
import fr.bingocraft.team.BingoTeam;
import fr.bingocraft.team.Teams;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Unit;

import java.util.ArrayList;
import java.util.List;

/**
 * Team picker ({@code /bingo team}): a single chest with one wool per team and an ender eye to
 * spectate. The player's current choice glows. Locked while a race is in progress.
 */
public class TeamScreenHandler extends GenericContainerScreenHandler {

    private static final int ROWS = 3;
    /** Slots of the eight wools, middle row, centre slot left empty. */
    private static final int[] TEAM_SLOTS = {9, 10, 11, 12, 14, 15, 16, 17};
    private static final int SPECTATOR_SLOT = 22;

    private final SimpleInventory display;
    private final ServerPlayerEntity viewer;

    private TeamScreenHandler(int syncId, PlayerInventory playerInventory, SimpleInventory display,
                              ServerPlayerEntity viewer) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, display, ROWS);
        this.display = display;
        this.viewer = viewer;
    }

    public static void open(ServerPlayerEntity player) {
        SimpleInventory display = new SimpleInventory(ROWS * 9);
        fill(display, player);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, p) -> new TeamScreenHandler(syncId, playerInventory, display, player),
                Text.translatable("bingocraft.teams.title")));
    }

    /** Redraws the menu if the player has it open (members changed, race started or ended). */
    public static void refresh(ServerPlayerEntity player) {
        if (player.currentScreenHandler instanceof TeamScreenHandler handler) {
            fill(handler.display, player);
            handler.sendContentUpdates();
        }
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        syncState(); // read-only: nothing moves
        if (actionType != SlotActionType.PICKUP && actionType != SlotActionType.QUICK_MOVE) return;

        BingoTeam chosen = null;
        for (int i = 0; i < TEAM_SLOTS.length; i++) if (TEAM_SLOTS[i] == slotIndex) chosen = BingoTeam.values()[i];
        if (chosen == null && slotIndex != SPECTATOR_SLOT) return;

        if (BingoState.get(viewer.server).race() != null) {
            viewer.sendMessage(Text.translatable("bingocraft.teams.locked").formatted(Formatting.RED), false);
            viewer.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
            return;
        }
        if (chosen != null) {
            if (chosen == Teams.of(viewer)) return;
            Teams.join(viewer, chosen);
            viewer.sendMessage(Text.translatable("bingocraft.teams.joined", chosen.displayName()), false);
        } else {
            if (Teams.isSpectator(viewer)) return;
            Teams.spectate(viewer);
            viewer.sendMessage(Text.translatable("bingocraft.teams.spectating").formatted(Formatting.GRAY), false);
        }
        viewer.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 0.6f, 1.0f);
        // Everybody looking at the menu sees the new member lists.
        for (ServerPlayerEntity p : viewer.server.getPlayerManager().getPlayerList()) refresh(p);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    // === Drawing ===

    private static void fill(SimpleInventory display, ServerPlayerEntity player) {
        ItemStack background = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        background.set(DataComponentTypes.HIDE_TOOLTIP, Unit.INSTANCE);
        for (int slot = 0; slot < display.size(); slot++) display.setStack(slot, background.copy());

        boolean locked = BingoState.get(player.server).race() != null;
        BingoTeam current = Teams.of(player);
        for (int i = 0; i < TEAM_SLOTS.length; i++) {
            BingoTeam team = BingoTeam.values()[i];
            Team scoreboardTeam = player.server.getScoreboard().getTeam(team.scoreboardName());
            List<String> members = scoreboardTeam != null ? new ArrayList<>(scoreboardTeam.getPlayerList()) : List.of();
            display.setStack(TEAM_SLOTS[i], choice(player, team.wool().getDefaultStack(),
                    Text.translatable("bingocraft.teams.team", team.displayName()).formatted(team.color(), Formatting.BOLD),
                    members, team == current, locked));
        }
        display.setStack(SPECTATOR_SLOT, choice(player, Items.ENDER_EYE.getDefaultStack(),
                Text.translatable("bingocraft.teams.spectator").formatted(Formatting.WHITE, Formatting.BOLD),
                List.of(), Teams.isSpectator(player), locked));
    }

    private static ItemStack choice(ServerPlayerEntity viewer, ItemStack stack, Text name, List<String> members,
                                    boolean selected, boolean locked) {
        stack.set(DataComponentTypes.CUSTOM_NAME, plain(name.copy()));
        List<Text> lore = new ArrayList<>();
        for (String member : members) {
            // Offline members stay in their team (they keep it when they come back) but are
            // greyed out: only online players take part in a race.
            boolean online = viewer.server.getPlayerManager().getPlayer(member) != null;
            lore.add(plain(online
                    ? Text.literal(" • " + member).formatted(Formatting.WHITE)
                    : Text.literal(" • " + member + " ").formatted(Formatting.DARK_GRAY)
                            .append(Text.translatable("bingocraft.teams.offline"))));
        }
        if (!members.isEmpty()) lore.add(Text.empty());
        lore.add(plain(Text.translatable(locked ? "bingocraft.teams.lore.locked"
                        : selected ? "bingocraft.teams.lore.selected" : "bingocraft.teams.lore.join")
                .formatted(locked ? Formatting.RED : selected ? Formatting.GREEN : Formatting.YELLOW)));
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        if (selected) stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private static Text plain(MutableText text) {
        return text.styled(style -> style.withItalic(false));
    }
}
