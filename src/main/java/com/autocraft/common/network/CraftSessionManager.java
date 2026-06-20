package com.autocraft.common.network;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Держит текущую активную сессию крафта и продвигает её на один шаг
 * каждый клиентский тик (20 раз в секунду — достаточно отзывчиво,
 * но не похоже на спам-клик для анти-чита сервера).
 */
@Mod.EventBusSubscriber(value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class CraftSessionManager {

    private static CraftSession activeSession = null;
    private static int tickDelay = 0;

    // Сколько игровых тиков ждать между действиями (4 тика ~ 200мс — безопасно)
    private static final int DELAY_BETWEEN_ACTIONS = 4;

    public static boolean isBusy() {
        return activeSession != null && !activeSession.isFinished();
    }

    public static void start(CraftSession session) {
        activeSession = session;
        tickDelay = 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (activeSession == null) return;

        if (tickDelay > 0) {
            tickDelay--;
            return;
        }

        activeSession.tick();
        tickDelay = DELAY_BETWEEN_ACTIONS;

        if (activeSession.isFinished()) {
            reportResult(activeSession);
            activeSession = null;
        }
    }

    private static void reportResult(CraftSession session) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (session.isSuccess()) {
            mc.player.displayClientMessage(
                    new StringTextComponent("\u00a7a[AutoCraft] \u00a7fГотово!"), true);
        } else {
            mc.player.displayClientMessage(
                    new StringTextComponent("\u00a7c[AutoCraft] \u00a7f" + session.getFailReason()), true);
        }
    }
}
