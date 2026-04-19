package com.github.epsilon.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;

public class ChatUtils {

    private static final Minecraft mc = Minecraft.getInstance();

    public static final String GRADIENT_SYNC_CODE = "§)";
    private static final String CLIENT_PREFIX_TEXT = "[Epsilon] ";
    private static final String CLIENT_PREFIX = GRADIENT_SYNC_CODE + "§r" + CLIENT_PREFIX_TEXT + "§f ";

    public static void addChatMessage(String message) {
        addChatMessage(true, message);
    }

    public static void addChatMessage(boolean prefix, String message) {
        mc.execute(() -> mc.gui.hud.getChat().addMessage(
                Component.literal((prefix ? CLIENT_PREFIX : "") + message),
                null,
                GuiMessageSource.SYSTEM_CLIENT,
                null
        ));
    }

}
