package com.autocraft.common.network;

import com.autocraft.common.model.RecipeSlot;
import com.autocraft.common.model.SavedRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.container.Slot;
import net.minecraft.inventory.container.WorkbenchContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Считывает текущую раскладку открытого верстака и оборачивает её в SavedRecipe.
 * Используется когда игрок сам расставил предметы руками и нажал "Записать рецепт".
 */
public class RecipeRecorder {

    /**
     * @return записанный рецепт или null, если верстак не открыт / сетка пуста
     */
    public static SavedRecipe recordFromOpenBench(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        if (!(mc.player.containerMenu instanceof WorkbenchContainer)) return null;

        WorkbenchContainer container = (WorkbenchContainer) mc.player.containerMenu;

        RecipeSlot[] grid = new RecipeSlot[9];
        boolean anyFilled = false;
        for (int i = 0; i < 9; i++) {
            Slot slot = container.slots.get(i + 1); // +1, слот 0 — результат
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                grid[i] = RecipeSlot.empty();
            } else {
                ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
                String id = rl != null ? rl.toString() : "minecraft:air";
                String nbt = stack.hasTag() ? stack.getTag().toString() : null;
                grid[i] = new RecipeSlot(id, 1, nbt);
                anyFilled = true;
            }
        }

        if (!anyFilled) return null;

        Slot resultSlot = container.slots.get(0);
        ItemStack result = resultSlot.getItem();
        String resultId = "minecraft:air";
        int resultCount = 0;
        String resultNbt = null;
        if (!result.isEmpty()) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(result.getItem());
            resultId = rl != null ? rl.toString() : "minecraft:air";
            resultCount = result.getCount();
            resultNbt = result.hasTag() ? result.getTag().toString() : null;
        }

        return new SavedRecipe(name, grid, resultId, resultCount, resultNbt);
    }

    /**
     * Проверяет, открыт ли сейчас верстак (для показа кнопки записи).
     */
    public static boolean isBenchOpen() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.containerMenu instanceof WorkbenchContainer;
    }
}
