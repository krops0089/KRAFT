package com.autocraft.common.network;

import com.autocraft.common.model.RecipeSlot;
import com.autocraft.common.model.SavedRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.CraftingScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.inventory.container.WorkbenchContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Управляет процессом автокрафта по шагам, по одному действию за игровой тик,
 * чтобы не словить анти-чит спам-клик защиту сервера и не запутаться, если
 * сервер ещё не успел обработать предыдущий клик.
 *
 * Алгоритм:
 *  1. Если верстак не открыт — отправить команду /wb в чат, подождать открытия.
 *  2. Для каждого из 9 слотов рецепта: если он не пустой, найти в инвентаре игрока
 *     подходящий предмет и кликом переместить 1 штуку в нужный слот сетки верстака.
 *  3. Кликнуть по слоту результата нужное количество раз (повторный крафт).
 *  4. Забрать всё, что осталось в сетке верстака, обратно в инвентарь.
 */
public class CraftSession {

    private enum Stage { WAITING_FOR_BENCH, PLACING, TAKING_RESULT, RETURNING_LEFTOVERS, DONE, FAILED }

    private final SavedRecipe recipe;
    private final int repeatCount;
    private Stage stage;
    private int placeIndex = 0;
    private int takenCount = 0;
    private int waitTicks = 0;
    private int returnSlotIndex = 1; // слоты сетки верстака: 1..9 (0 — результат)
    private String failReason = null;

    private static final int MAX_WAIT_TICKS = 100; // 5 секунд на открытие верстака

    public CraftSession(SavedRecipe recipe, int repeatCount) {
        this.recipe = recipe;
        this.repeatCount = repeatCount;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof CraftingScreen) {
            this.stage = Stage.PLACING;
        } else {
            this.stage = Stage.WAITING_FOR_BENCH;
            sendOpenCommand();
        }
    }

    private void sendOpenCommand() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.chat("/wb");
        }
    }

    public boolean isFinished() {
        return stage == Stage.DONE || stage == Stage.FAILED;
    }

    public boolean isSuccess() {
        return stage == Stage.DONE;
    }

    public String getFailReason() {
        return failReason;
    }

    /**
     * Вызывается каждый клиентский тик. Выполняет максимум одно действие за вызов.
     */
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            fail("Игрок не найден");
            return;
        }

        switch (stage) {
            case WAITING_FOR_BENCH:
                waitForBench(mc);
                break;
            case PLACING:
                doPlaceStep(mc);
                break;
            case TAKING_RESULT:
                doTakeResultStep(mc);
                break;
            case RETURNING_LEFTOVERS:
                doReturnLeftoversStep(mc);
                break;
            default:
                break;
        }
    }

    private void waitForBench(Minecraft mc) {
        if (mc.screen instanceof CraftingScreen) {
            stage = Stage.PLACING;
            return;
        }
        waitTicks++;
        if (waitTicks > MAX_WAIT_TICKS) {
            fail("Верстак не открылся (нет команды /wb на сервере?)");
        }
    }

    private WorkbenchContainer getContainer(Minecraft mc) {
        if (mc.player == null) return null;
        if (mc.player.containerMenu instanceof WorkbenchContainer) {
            return (WorkbenchContainer) mc.player.containerMenu;
        }
        return null;
    }

    /**
     * Расставляет по одному ингредиенту за тик в сетку верстака.
     * Слоты контейнера верстака: 0 = результат, 1..9 = сетка 3x3.
     */
    private void doPlaceStep(Minecraft mc) {
        WorkbenchContainer container = getContainer(mc);
        if (container == null) {
            fail("Верстак закрылся во время крафта");
            return;
        }

        if (placeIndex >= 9) {
            stage = Stage.TAKING_RESULT;
            takenCount = 0;
            return;
        }

        RecipeSlot want = recipe.grid[placeIndex];
        int containerSlot = placeIndex + 1; // +1 потому что слот 0 — результат

        if (want.isEmpty()) {
            placeIndex++;
            return;
        }

        // Если в целевом слоте сетки уже лежит нужный предмет — пропускаем
        Slot targetSlot = container.slots.get(containerSlot);
        ItemStack current = targetSlot.getItem();
        if (!current.isEmpty() && matchesWanted(current, want)) {
            placeIndex++;
            return;
        }

        int sourceSlot = findIngredientInInventory(mc.player.inventory, want);
        if (sourceSlot == -1) {
            fail("Не хватает предмета: " + want.itemId);
            return;
        }

        // Игровой инвентарь в контейнере верстака начинается со слота 10
        // (1..9 — сетка, 0 — результат, 10..36 — инвентарь игрока, 37..45 — хотбар)
        int containerSourceSlot = inventoryIndexToContainerSlot(sourceSlot);

        // Клик ЛКМ по исходному слоту берёт весь стак на курсор, затем
        // клик ПКМ по целевому слоту кладёт туда только 1 штуку,
        // остаток возвращаем обратно ЛКМ по исходному слоту.
        clickSlot(mc, containerSourceSlot, ClickType.PICKUP, 0); // взять весь стак на курсор
        clickSlot(mc, containerSlot, ClickType.PICKUP, 1);       // положить 1 штуку (ПКМ)
        clickSlot(mc, containerSourceSlot, ClickType.PICKUP, 0); // вернуть остаток обратно

        placeIndex++;
    }

    private void doTakeResultStep(Minecraft mc) {
        WorkbenchContainer container = getContainer(mc);
        if (container == null) {
            fail("Верстак закрылся во время крафта");
            return;
        }

        if (takenCount >= repeatCount) {
            stage = Stage.RETURNING_LEFTOVERS;
            returnSlotIndex = 1;
            return;
        }

        Slot resultSlot = container.slots.get(0);
        if (resultSlot.getItem().isEmpty()) {
            // Ничего не вышло — либо рецепт неверный, либо ингредиенты кончились
            if (takenCount == 0) {
                fail("Результат пуст — рецепт не сработал на этом сервере");
            } else {
                // Скрафтили хотя бы раз, ингредиенты закончились — это нормальное завершение
                stage = Stage.RETURNING_LEFTOVERS;
                returnSlotIndex = 1;
            }
            return;
        }

        // Шифт-клик забирает результат и автоматически переносит в инвентарь
        clickSlot(mc, 0, ClickType.QUICK_MOVE, 0);
        takenCount++;
    }

    private void doReturnLeftoversStep(Minecraft mc) {
        WorkbenchContainer container = getContainer(mc);
        if (container == null) {
            // Верстак уже закрыт — ничего не поделать, завершаем
            stage = Stage.DONE;
            return;
        }

        if (returnSlotIndex > 9) {
            stage = Stage.DONE;
            return;
        }

        Slot slot = container.slots.get(returnSlotIndex);
        if (!slot.getItem().isEmpty()) {
            // Шифт-клик переносит остатки сетки обратно в инвентарь
            clickSlot(mc, returnSlotIndex, ClickType.QUICK_MOVE, 0);
        }
        returnSlotIndex++;
    }

    private void fail(String reason) {
        this.failReason = reason;
        this.stage = Stage.FAILED;
    }

    // ── Вспомогательные методы ──────────────────────────────────────────────

    private boolean matchesWanted(ItemStack stack, RecipeSlot want) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null) return false;
        if (!rl.toString().equals(want.itemId)) return false;
        return nbtMatches(stack, want.nbt);
    }

    private boolean nbtMatches(ItemStack stack, String wantNbt) {
        if (wantNbt == null || wantNbt.isEmpty()) {
            return !stack.hasTag();
        }
        return stack.hasTag() && stack.getTag().toString().equals(wantNbt);
    }

    /**
     * Ищет индекс слота (0..35, в системе PlayerInventory) с подходящим предметом.
     */
    private int findIngredientInInventory(PlayerInventory inv, RecipeSlot want) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && matchesWanted(stack, want)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * PlayerInventory индексирует так: 0..8 — хотбар, 9..35 — основной инвентарь.
     * В WorkbenchContainer слоты идут: 0=результат, 1..9=сетка, 10..36=основной инвентарь
     * (27 слотов), 37..45=хотбар (9 слотов).
     */
    private int inventoryIndexToContainerSlot(int invIndex) {
        if (invIndex < 9) {
            // хотбар: invIndex 0..8 -> container 37..45
            return 37 + invIndex;
        } else {
            // основной инвентарь: invIndex 9..35 -> container 10..36
            return 10 + (invIndex - 9);
        }
    }

    private void clickSlot(Minecraft mc, int slotIndex, ClickType type, int button) {
        if (mc.player == null || mc.gameMode == null) return;
        mc.gameMode.handleInventoryMouseClick(
                mc.player.containerMenu.containerId,
                slotIndex,
                button,
                type,
                mc.player
        );
    }
}
