package com.github.epsilon.gui.hudeditor;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.gui.panel.PanelScreen;
import com.github.epsilon.managers.ConfigManager;
import com.github.epsilon.managers.ModuleManager;
import com.github.epsilon.managers.RenderManager;
import com.github.epsilon.modules.HudModule;
import com.github.epsilon.modules.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.awt.*;

public class HudEditorScreen extends Screen {

    public static final HudEditorScreen INSTANCE = new HudEditorScreen();

    private HudEditorScreen() {
        super(Component.literal("Epsilon-HUDEditor"));
    }

    private final RectRenderer rectRenderer = new RectRenderer();

    private HudModule dragging = null;
    private double dragOffsetX, dragOffsetY;

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {

        RenderManager.INSTANCE.applyRenderAfterFrame(delta -> {
            ModuleManager.INSTANCE.getModules().forEach(module -> {
                if (module instanceof HudModule hudModule) {
                    rectRenderer.addRect(hudModule.x, hudModule.y, hudModule.width, hudModule.height, new Color(0, 0, 0, 100));
                }
            });

            rectRenderer.drawAndClear();

            ModuleManager.INSTANCE.getModules().forEach(module -> {
                if (module instanceof HudModule hudModule) {
                    hudModule.render(delta);
                }
            });
        });

    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();
            var modules = ModuleManager.INSTANCE.getModules();
            for (Module module : modules) {
                if (module instanceof HudModule hud) {
                    if (mx >= hud.x && mx <= hud.x + hud.width && my >= hud.y && my <= hud.y + hud.height) {
                        dragging = hud;
                        dragOffsetX = mx - hud.x;
                        dragOffsetY = my - hud.y;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double mouseX, double mouseY) {
        if (dragging != null && event.button() == 0) {
            dragging.x = (float) (event.x() - dragOffsetX);
            dragging.y = (float) (event.y() - dragOffsetY);
            return true;
        }
        return super.mouseDragged(event, mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (dragging != null && event.button() == 0) {
            dragging = null;
            ConfigManager.INSTANCE.saveNow();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        minecraft.setScreen(PanelScreen.INSTANCE);
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }

}
