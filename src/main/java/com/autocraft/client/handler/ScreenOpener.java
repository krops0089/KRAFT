package com.autocraft.client.handler;

import com.autocraft.client.gui.AutoCraftScreen;
import net.minecraft.client.Minecraft;

/**
 * Открывает главный экран мода, только если сейчас не открыт чат,
 * инвентарь или другой экран — чтобы не перебивать игрока неожиданно.
 */
public class ScreenOpener {

    public static void openMainScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen != null) return;
        mc.setScreen(new AutoCraftScreen());
    }
}
