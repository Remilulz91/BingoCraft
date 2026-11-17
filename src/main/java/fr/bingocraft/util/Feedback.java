package fr.bingocraft.util;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.List;

/** On-screen titles and celebration fireworks. */
public final class Feedback {

    /** Command tag of the celebration rockets: they explode without hurting anyone (see FireworkRocketEntityMixin). */
    public static final String HARMLESS_TAG = "bingocraft_harmless";

    private Feedback() { }

    /** Shows a title; times in ticks (fade in / stay / fade out). */
    public static void title(ServerPlayerEntity player, Text title, Text subtitle, int fadeIn, int stay, int fadeOut) {
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(fadeIn, stay, fadeOut));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
        player.networkHandler.sendPacket(new TitleS2CPacket(title));
    }

    /** Three harmless rockets around the player, in the given colours. */
    public static void fireworks(ServerPlayerEntity player, int... colors) {
        ServerWorld world = player.getServerWorld();
        double[][] offsets = {{1.5, 0}, {-1.0, 1.2}, {-0.5, -1.5}};
        FireworkExplosionComponent.Type[] shapes = {
                FireworkExplosionComponent.Type.LARGE_BALL,
                FireworkExplosionComponent.Type.STAR,
                FireworkExplosionComponent.Type.BURST};
        for (int i = 0; i < offsets.length; i++) {
            ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
            rocket.set(DataComponentTypes.FIREWORKS, new FireworksComponent(1, List.of(
                    new FireworkExplosionComponent(shapes[i], IntList.of(colors), IntList.of(0xFFFFFF), true, true))));
            FireworkRocketEntity entity = new FireworkRocketEntity(world,
                    player.getX() + offsets[i][0], player.getY() + 0.5, player.getZ() + offsets[i][1], rocket);
            entity.addCommandTag(HARMLESS_TAG);
            world.spawnEntity(entity);
        }
    }
}
