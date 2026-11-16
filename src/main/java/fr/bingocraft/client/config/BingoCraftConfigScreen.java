package fr.bingocraft.client.config;

import fr.bingocraft.config.BingoCraftConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Cloth Config screen for BingoCraft. Only referenced when Cloth Config is loaded.
 */
public final class BingoCraftConfigScreen {

    private BingoCraftConfigScreen() { }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.bingocraft.title"))
                .setSavingRunnable(BingoCraftConfig::save);

        ConfigEntryBuilder entry = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("config.bingocraft.category.general"));
        general.addEntry(entry.startTextDescription(Text.translatable("config.bingocraft.general.description")).build());

        return builder.build();
    }
}
