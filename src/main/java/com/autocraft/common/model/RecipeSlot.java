package com.autocraft.common.model;

/**
 * Один слот сохранённого рецепта: что за предмет, сколько штук в исходной
 * раскладке (обычно 1) и его NBT-тег (важно для модовых предметов с тегами,
 * например зачарованные/кастомные предметы серверных модов).
 */
public class RecipeSlot {

    public String itemId;   // например "minecraft:stick" или "customserver:magic_dust"
    public int count;       // сколько штук было в этой клетке при записи (обычно 1)
    public String nbt;      // сериализованный NBT-тег в виде строки, может быть null

    public RecipeSlot() {
    }

    public RecipeSlot(String itemId, int count, String nbt) {
        this.itemId = itemId;
        this.count = count;
        this.nbt = nbt;
    }

    public boolean isEmpty() {
        return itemId == null || itemId.isEmpty() || itemId.equals("minecraft:air");
    }

    public static RecipeSlot empty() {
        return new RecipeSlot("minecraft:air", 0, null);
    }
}
