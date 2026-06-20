package com.autocraft;

import com.autocraft.client.keybind.KeyBindings;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AutoCraftMod.MOD_ID)
public class AutoCraftMod {

    public static final String MOD_ID = "autocraft";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public AutoCraftMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        KeyBindings.register();
        LOGGER.info("AutoCraft: клиентский мод загружен");
    }
}
