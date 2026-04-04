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

    private static final Color BOX_COLOR = new Color(0, 0, 0, 100);
    private static final Color HOVER_COLOR = new Color(255, 255, 255, 70);
    private static final Color DRAGGING_COLOR = new Color(120, 190, 255, 80);

    public static final HudEditorScreen INSTANCE = new HudEditorScreen();

    private HudEditorScreen() {
        super(Component.literal("Epsilon-HUDEditor"));
    }

    private final RectRenderer rectRenderer = new RectRenderer();

    private HudModule dragging = null;
    private double dragOffsetX, dragOffsetY;

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        HudModule hovered = getHovered(mouseX, mouseY);

        RenderManager.INSTANCE.applyRenderAfterFrame(delta -> {
            ModuleManager.INSTANCE.getModules().forEach(module -> {
                if (module instanceof HudModule hudModule) {
                    hudModule.updateBounds(delta);
                    rectRenderer.addRect(hudModule.x, hudModule.y, hudModule.width, hudModule.height, BOX_COLOR);
                    if (hudModule == hovered) {
                        rectRenderer.addRect(hudModule.x, hudModule.y, hudModule.width, hudModule.height, HOVER_COLOR);
                    }
                    if (hudModule == dragging) {
                        rectRenderer.addRect(hudModule.x, hudModule.y, hudModule.width, hudModule.height, DRAGGING_COLOR);
                    }
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
            HudModule hovered = getHovered((int) mx, (int) my);
            if (hovered != null) {
                dragging = hovered;
                dragOffsetX = mx - hovered.x;
                dragOffsetY = my - hovered.y;
                return true;
            }
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double mouseX, double mouseY) {
        if (dragging != null && event.button() == 0) {
            dragging.moveTo((float) (event.x() - dragOffsetX), (float) (event.y() - dragOffsetY));
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

    private HudModule getHovered(int mouseX, int mouseY) {
        HudModule hovered = null;

        for (Module module : ModuleManager.INSTANCE.getModules()) {
            if (module instanceof HudModule hudModule) {
                hudModule.updateBounds(null);
                if (hudModule.contains(mouseX, mouseY)) {
                    hovered = hudModule;
                }
            }
        }

        return hovered;
    }

}
