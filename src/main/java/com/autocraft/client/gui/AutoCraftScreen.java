package com.autocraft.client.gui;

import com.autocraft.common.model.SavedRecipe;
import com.autocraft.common.network.CraftSession;
import com.autocraft.common.network.CraftSessionManager;
import com.autocraft.common.storage.RecipeStorage;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Главное окно мода: список всех сохранённых кастомных рецептов,
 * поиск, выбор количества и кнопка "Крафтить".
 */
public class AutoCraftScreen extends Screen {

    private static final int COLOR_BG       = 0xEE1A1A2E;
    private static final int COLOR_ACCENT   = 0xFF0F3460;
    private static final int COLOR_HILIGHT  = 0xFFE94560;
    private static final int COLOR_SUBTEXT  = 0xFFAAAAAA;
    private static final int COLOR_ROW      = 0xFF0D0D1A;
    private static final int COLOR_ROW_HOV  = 0xFF2A2A4A;
    private static final int COLOR_SUCCESS  = 0xFF4CAF50;
    private static final int COLOR_TEXT     = 0xFFEEEEEE;

    private static final int WINDOW_W = 360;
    private static final int WINDOW_H = 280;
    private static final int ROW_H = 24;
    private static final int VISIBLE_ROWS = 7;

    private int winX, winY;
    private List<SavedRecipe> allRecipes = new ArrayList<>();
    private List<SavedRecipe> filtered = new ArrayList<>();
    private int scrollOffset = 0;
    private int hoveredRow = -1;
    private int selectedIndex = -1;

    private TextFieldWidget searchField;
    private int craftCount = 1;

    private String statusMsg = "";
    private int statusColor = COLOR_TEXT;
    private long statusTime = 0;

    public AutoCraftScreen() {
        super(new TranslationTextComponent("gui.autocraft.title"));
    }

    @Override
    protected void init() {
        super.init();
        winX = (width - WINDOW_W) / 2;
        winY = (height - WINDOW_H) / 2;

        allRecipes = RecipeStorage.getAll();
        applyFilter("");

        searchField = new TextFieldWidget(font, winX + 10, winY + 30, WINDOW_W - 20, 16,
                new StringTextComponent(""));
        searchField.setMaxLength(50);
        searchField.setResponder(this::applyFilter);
        addWidget(searchField);
        this.setFocused(searchField);

        addButton(new Button(winX + 10, winY + WINDOW_H - 28, 90, 20,
                new StringTextComponent("\u25b6 Крафтить"), btn -> onCraftClicked()));

        addButton(new Button(winX + 110, winY + WINDOW_H - 28, 20, 20,
                new StringTextComponent("-"), btn -> {
                    if (craftCount > 1) craftCount--;
                }));
        addButton(new Button(winX + 160, winY + WINDOW_H - 28, 20, 20,
                new StringTextComponent("+"), btn -> {
                    if (craftCount < 64) craftCount++;
                }));

        addButton(new Button(winX + WINDOW_W - 100, winY + WINDOW_H - 28, 90, 20,
                new StringTextComponent("Удалить"), btn -> onDeleteClicked()));
    }

    private void applyFilter(String text) {
        String q = text.toLowerCase().trim();
        if (q.isEmpty()) {
            filtered = new ArrayList<>(allRecipes);
        } else {
            filtered = allRecipes.stream()
                    .filter(r -> r.name.toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }
        scrollOffset = 0;
        if (selectedIndex >= filtered.size()) selectedIndex = -1;
    }

    private void onCraftClicked() {
        if (CraftSessionManager.isBusy()) {
            showStatus("Подожди, крафт уже идёт", COLOR_HILIGHT);
            return;
        }
        if (selectedIndex < 0 || selectedIndex >= filtered.size()) {
            showStatus("Выбери рецепт из списка", COLOR_HILIGHT);
            return;
        }
        SavedRecipe recipe = filtered.get(selectedIndex);
        CraftSessionManager.start(new CraftSession(recipe, craftCount));
        showStatus("Крафт запущен: " + recipe.name, COLOR_SUCCESS);
        this.onClose();
    }

    private void onDeleteClicked() {
        if (selectedIndex < 0 || selectedIndex >= filtered.size()) {
            showStatus("Выбери рецепт для удаления", COLOR_HILIGHT);
            return;
        }
        SavedRecipe recipe = filtered.get(selectedIndex);
        RecipeStorage.delete(recipe.id);
        allRecipes = RecipeStorage.getAll();
        applyFilter(searchField.getValue());
        selectedIndex = -1;
        showStatus("Удалено: " + recipe.name, COLOR_TEXT);
    }

    private void showStatus(String msg, int color) {
        statusMsg = msg;
        statusColor = color;
        statusTime = System.currentTimeMillis();
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partial) {
        renderBackground(ms);
        fill(ms, winX, winY, winX + WINDOW_W, winY + WINDOW_H, COLOR_BG);
        fill(ms, winX, winY, winX + WINDOW_W, winY + 22, COLOR_ACCENT);
        drawCenteredString(ms, font, "\u26a1 AutoCraft", winX + WINDOW_W / 2, winY + 7, COLOR_TEXT);

        searchField.render(ms, mouseX, mouseY, partial);

        int listY = winY + 52;
        int listX = winX + 10;
        int listW = WINDOW_W - 20;
        hoveredRow = -1;

        if (filtered.isEmpty()) {
            drawCenteredString(ms, font, "\u00a77Нет сохранённых рецептов.",
                    winX + WINDOW_W / 2, listY + 30, COLOR_SUBTEXT);
            drawCenteredString(ms, font, "\u00a77Открой верстак и нажми \"Записать рецепт\"",
                    winX + WINDOW_W / 2, listY + 42, COLOR_SUBTEXT);
        }

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = scrollOffset + i;
            if (idx >= filtered.size()) break;
            int rowY = listY + i * ROW_H;
            boolean hovered = mouseX >= listX && mouseX < listX + listW
                    && mouseY >= rowY && mouseY < rowY + ROW_H - 2;
            boolean selected = idx == selectedIndex;
            if (hovered) hoveredRow = idx;

            int rowColor = selected ? (COLOR_HILIGHT & 0x55FFFFFF) : (hovered ? COLOR_ROW_HOV : COLOR_ROW);
            fill(ms, listX, rowY, listX + listW, rowY + ROW_H - 2, rowColor);

            SavedRecipe recipe = filtered.get(idx);
            ItemStack icon = resolveIcon(recipe);
            if (!icon.isEmpty()) {
                this.itemRenderer.renderAndDecorateItem(icon, listX + 2, rowY + 2);
            }

            String label = recipe.name;
            int maxWidth = listW - 28;
            if (font.width(label) > maxWidth) {
                label = font.plainSubstrByWidth(label, maxWidth - 6) + "..";
            }
            font.draw(ms, label, listX + 24, rowY + 7, 0xFFFFFF);
        }

        if (filtered.size() > VISIBLE_ROWS) {
            String info = (scrollOffset + 1) + "-" + Math.min(scrollOffset + VISIBLE_ROWS, filtered.size())
                    + " / " + filtered.size();
            font.draw(ms, info, listX, listY + VISIBLE_ROWS * ROW_H + 2, COLOR_SUBTEXT);
        }

        font.draw(ms, "\u00d7" + craftCount, winX + 135, winY + WINDOW_H - 22, COLOR_TEXT);

        if (!statusMsg.isEmpty() && System.currentTimeMillis() - statusTime < 3000) {
            drawCenteredString(ms, font, statusMsg, winX + WINDOW_W / 2, winY + WINDOW_H - 45, statusColor);
        }

        if (hoveredRow >= 0) {
            ItemStack icon = resolveIcon(filtered.get(hoveredRow));
            if (!icon.isEmpty()) {
                renderTooltip(ms, icon, mouseX, mouseY);
            }
        }

        super.render(ms, mouseX, mouseY, partial);
    }

    private ItemStack resolveIcon(SavedRecipe recipe) {
        if (recipe.resultItemId == null) return ItemStack.EMPTY;
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(recipe.resultItemId));
        if (item == null) return ItemStack.EMPTY;
        return new ItemStack(item, Math.max(1, recipe.resultCount));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (searchField.mouseClicked(mx, my, button)) return true;

        if (hoveredRow >= 0 && button == 0) {
            selectedIndex = hoveredRow;
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int maxOffset = Math.max(0, filtered.size() - VISIBLE_ROWS);
        scrollOffset -= (int) Math.signum(delta);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (searchField.isFocused() && searchField.keyPressed(key, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (searchField.isFocused() && searchField.charTyped(c, modifiers)) {
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
