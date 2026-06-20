package com.autocraft.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.ClientRegistry;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Единственная горячая клавиша мода: открывает главный экран со списком
 * сохранённых рецептов. По умолчанию K (свободная клавиша в ваниле).
 */
@Mod.EventBusSubscriber(value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class KeyBindings {

    public static final KeyBinding OPEN_GUI = new KeyBinding(
            "key.autocraft.open_gui",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_K),
            "category.autocraft"
    );

    public static void register() {
        ClientRegistry.registerKeyBinding(OPEN_GUI);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        if (OPEN_GUI.consumeClick()) {
            com.autocraft.client.handler.ScreenOpener.openMainScreen();
        }
    }
}
