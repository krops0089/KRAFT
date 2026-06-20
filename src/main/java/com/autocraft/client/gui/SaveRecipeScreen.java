package com.autocraft.client.gui;

import com.autocraft.common.model.SavedRecipe;
import com.autocraft.common.network.RecipeRecorder;
import com.autocraft.common.storage.RecipeStorage;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;

/**
 * Маленькое окно "Введи имя рецепта" — появляется поверх верстака,
 * когда игрок нажал кнопку "Записать рецепт". Сам верстак на фоне
 * остаётся видимым, окно ввода рисуется как оверлей.
 */
public class SaveRecipeScreen extends Screen {

    private static final int W = 220;
    private static final int H = 80;

    private final Screen parent;
    private TextFieldWidget nameField;
    private String error = "";

    public SaveRecipeScreen(Screen parent) {
        super(new StringTextComponent("Сохранить рецепт"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - W) / 2;
        int y = (height - H) / 2;

        nameField = new TextFieldWidget(font, x + 10, y + 28, W - 20, 16,
                new StringTextComponent(""));
        nameField.setMaxLength(40);
        addWidget(nameField);
        this.setFocused(nameField);

        addButton(new Button(x + 10, y + H - 24, 90, 20,
                new StringTextComponent("Сохранить"), btn -> trySave()));
        addButton(new Button(x + W - 100, y + H - 24, 90, 20,
                new StringTextComponent("Отмена"), btn -> onClose()));
    }

    private void trySave() {
        String name = nameField.getValue().trim();
        if (name.isEmpty()) {
            error = "Введи название рецепта";
            return;
        }

        SavedRecipe recipe = RecipeRecorder.recordFromOpenBench(name);
        if (recipe == null) {
            error = "Верстак пуст или закрыт";
            return;
        }

        RecipeStorage.save(recipe);
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partial) {
        if (parent != null) {
            parent.render(ms, -1, -1, partial);
        }

        int x = (width - W) / 2;
        int y = (height - H) / 2;

        fill(ms, x, y, x + W, y + H, 0xEE16213E);
        fill(ms, x, y, x + W, y + 18, 0xFF0F3460);
        drawCenteredString(ms, font, "Название рецепта", x + W / 2, y + 5, 0xFFFFFF);

        nameField.render(ms, mouseX, mouseY, partial);

        if (!error.isEmpty()) {
            drawCenteredString(ms, font, "\u00a7c" + error, x + W / 2, y + H - 36, 0xFFAA0000);
        }

        super.render(ms, mouseX, mouseY, partial);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (nameField.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (nameField.isFocused() && nameField.keyPressed(key, scanCode, modifiers)) {
            return true;
        }
        if (key == 257 || key == 335) {
            trySave();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (nameField.isFocused() && nameField.charTyped(c, modifiers)) {
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
