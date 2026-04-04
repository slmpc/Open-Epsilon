package com.github.epsilon.gui.hudeditor;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.gui.panel.PanelScreen;
import com.github.epsilon.managers.ConfigManager;
import com.github.epsilon.managers.RenderManager;
import com.github.epsilon.modules.HudModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.util.List;

public class HudEditorScreen extends Screen {

    private static final Color BOX_COLOR = new Color(0, 0, 0, 100);
    private static final Color HOVER_COLOR = new Color(255, 255, 255, 70);
    private static final Color DRAGGING_COLOR = new Color(120, 190, 255, 80);

    public static final HudEditorScreen INSTANCE = new HudEditorScreen();

    private final RectRenderer rectRenderer = new RectRenderer();
    private final HudEditorOverlayRenderer overlayRenderer = new HudEditorOverlayRenderer();

    private HudModule dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private Float snapPreviewX;
    private Float snapPreviewY;

    private HudEditorScreen() {
        super(Component.literal("HUDEditor"));
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        RenderManager.INSTANCE.applyRenderAfterFrame(delta -> {
            int screenWidth = minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = minecraft.getWindow().getGuiScaledHeight();
            List<HudModule> hudModules = HudEditorModules.collectHudModules(delta);
            HudModule hovered = HudEditorModules.findTopmost(hudModules, mouseX, mouseY);
            HudModule focus = dragging != null ? dragging : hovered;
            boolean draggingFocus = focus != null && focus == dragging;

            if (focus != null) {
                overlayRenderer.addThirdGuides(focus, draggingFocus, screenWidth, screenHeight);
                overlayRenderer.drawAndClear();
            }

            for (HudModule hudModule : hudModules) {
                rectRenderer.addRect(hudModule.x, hudModule.y, hudModule.width, hudModule.height, BOX_COLOR);
                if (hudModule == hovered) {
                    rectRenderer.addRect(hudModule.x, hudModule.y, hudModule.width, hudModule.height, HOVER_COLOR);
                }
                if (hudModule == dragging) {
                    rectRenderer.addRect(hudModule.x, hudModule.y, hudModule.width, hudModule.height, DRAGGING_COLOR);
                }
            }

            rectRenderer.drawAndClear();

            for (HudModule hudModule : hudModules) {
                hudModule.render(delta);
            }

            if (focus != null) {
                overlayRenderer.addAnchorOverlay(focus, draggingFocus, screenWidth, screenHeight);
            }

            overlayRenderer.addSnapPreview(snapPreviewX, snapPreviewY, screenWidth, screenHeight);
            overlayRenderer.drawAndClear();
        });
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();
            List<HudModule> hudModules = HudEditorModules.collectHudModules(null);
            HudModule hovered = HudEditorModules.findTopmost(hudModules, mx, my);
            if (hovered != null) {
                dragging = hovered;
                dragOffsetX = mx - hovered.x;
                dragOffsetY = my - hovered.y;
                clearSnapPreview();
                return true;
            }
        }

        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double mouseX, double mouseY) {
        if (dragging != null && event.button() == 0) {
            int screenWidth = minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = minecraft.getWindow().getGuiScaledHeight();
            List<HudModule> hudModules = HudEditorModules.collectHudModules(null);
            float targetX = (float) (event.x() - dragOffsetX);
            float targetY = (float) (event.y() - dragOffsetY);
            HudEditorSnapper.SnapPosition snap = event.hasAltDown()
                    ? new HudEditorSnapper.SnapPosition(targetX, targetY, null, null)
                    : HudEditorSnapper.snapPosition(dragging, targetX, targetY, screenWidth, screenHeight, hudModules);

            dragging.moveTo(snap.renderX(), snap.renderY());
            snapPreviewX = snap.guideX();
            snapPreviewY = snap.guideY();
            return true;
        }

        return super.mouseDragged(event, mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (dragging != null && event.button() == 0) {
            dragging = null;
            clearSnapPreview();
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
        dragging = null;
        clearSnapPreview();
        ConfigManager.INSTANCE.saveNow();
        super.onClose();
        minecraft.setScreen(PanelScreen.INSTANCE);
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }

    private void clearSnapPreview() {
        snapPreviewX = null;
        snapPreviewY = null;
    }
}
