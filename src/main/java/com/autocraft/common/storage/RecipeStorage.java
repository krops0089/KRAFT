package com.autocraft.common.storage;

import com.autocraft.common.model.RecipeSlot;
import com.autocraft.common.model.SavedRecipe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Хранит сохранённые рецепты в файле:
 * <папка_игры>/config/autocraft/recipes.json
 *
 * Это правильное место (через FMLPaths.GAMEDIR), а не корень рабочей директории —
 * так файл не потеряется и не будет общим случайно с сервером/другими процессами.
 */
public class RecipeStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<SavedRecipe>>() {}.getType();

    private static List<SavedRecipe> cache;

    private static Path getFile() {
        Path dir = FMLPaths.GAMEDIR.get().resolve("config").resolve("autocraft");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
        return dir.resolve("recipes.json");
    }

    public static List<SavedRecipe> getAll() {
        if (cache == null) {
            cache = load();
        }
        return cache;
    }

    private static List<SavedRecipe> load() {
        Path file = getFile();
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try {
            String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            List<SavedRecipe> result = GSON.fromJson(json, LIST_TYPE);
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void save(SavedRecipe recipe) {
        List<SavedRecipe> all = getAll();
        all.removeIf(r -> r.id.equals(recipe.id));
        all.add(recipe);
        persist(all);
    }

    public static void delete(String id) {
        List<SavedRecipe> all = getAll();
        all.removeIf(r -> r.id.equals(id));
        persist(all);
    }

    public static void rename(String id, String newName) {
        for (SavedRecipe r : getAll()) {
            if (r.id.equals(id)) {
                r.name = newName;
                break;
            }
        }
        persist(getAll());
    }

    private static void persist(List<SavedRecipe> all) {
        cache = all;
        try {
            String json = GSON.toJson(all, LIST_TYPE);
            Files.write(getFile(), json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
