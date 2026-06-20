package com.autocraft.client.handler;

import com.autocraft.client.gui.SaveRecipeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.WorkbenchScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Добавляет кнопку "Записать рецепт" прямо в обычный ванильный экран
 * верстака (включая кастомный /wb сервера — подтверждено, что он
 * выглядит как обычный верстак 3x3, значит это WorkbenchScreen).
 */
@Mod.EventBusSubscriber(value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class WorkbenchOverlayHandler {

    @SubscribeEvent
    public static void onInitGui(GuiScreenEvent.InitGuiEvent event) {
        Screen screen = event.getGui();
        if (!(screen instanceof WorkbenchScreen)) return;

        WorkbenchScreen workbench = (WorkbenchScreen) screen;
        int guiLeft = getGuiLeft();
        int guiTop = getGuiTop();

        Button recordButton = new Button(
                guiLeft + 178, guiTop + 8, 90, 20,
                new StringTextComponent("Записать"),
                btn -> openSaveDialog(workbench)
        );
        event.addWidget(recordButton);
    }

    private static void openSaveDialog(WorkbenchScreen workbench) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new SaveRecipeScreen(workbench));
    }

    // ContainerScreen.leftPos/topPos защищены (protected) и недоступны
    // напрямую из события — пересчитываем те же стандартные размеры
    // верстака (176x166), отцентрованные на экране, как делает сам ContainerScreen.
    private static int getGuiLeft() {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        return (screenWidth - 176) / 2;
    }

    private static int getGuiTop() {
        Minecraft mc = Minecraft.getInstance();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        return (screenHeight - 166) / 2;
    }
}
