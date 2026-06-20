package com.autocraft.common.model;

import java.util.UUID;

/**
 * Полностью сохранённый кастомный рецепт.
 * grid — это ровно 9 слотов в порядке верстака (0..8, слева-направо, сверху-вниз),
 * как они были при записи. Мод не пытается понять "что значит" этот рецепт —
 * просто помнит раскладку и слепо повторяет её при крафте.
 */
public class SavedRecipe {

    public String id;            // уникальный id профиля (UUID), не путать с itemId
    public String name;          // имя, которое ввёл игрок при сохранении
    public RecipeSlot[] grid;    // 9 ячеек сетки верстака

    // Информация о результате — только для красивого отображения в GUI.
    // Мод не использует это для логики крафта, только чтобы показать иконку и имя.
    public String resultItemId;
    public int resultCount;
    public String resultNbt;

    public SavedRecipe() {
    }

    public SavedRecipe(String name, RecipeSlot[] grid,
                        String resultItemId, int resultCount, String resultNbt) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.grid = grid;
        this.resultItemId = resultItemId;
        this.resultCount = resultCount;
        this.resultNbt = resultNbt;
    }
}
