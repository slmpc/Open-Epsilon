package com.github.lumin.gui.menu;

import com.github.lumin.assets.resources.ResourceLocationUtils;
import com.github.lumin.graphics.LuminRenderable;
import com.github.lumin.graphics.LuminTexture;
import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.renderers.TextureRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.io.InputStream;
import java.util.Optional;

public class MainMenuScreen extends Screen implements LuminRenderable {

    public static final MainMenuScreen INSTANCE = new MainMenuScreen();

    private static final Identifier bg = ResourceLocationUtils.getIdentifier("textures/gui/mainmenu/1.png");

    private final RectRenderer rectRenderer = new RectRenderer();
    private final RoundRectRenderer roundRectRenderer = new RoundRectRenderer();
    private final TextRenderer textRenderer = new TextRenderer();
    private final TextureRenderer textureRenderer = new TextureRenderer();
    private final Minecraft mc = Minecraft.getInstance();
    private LuminTexture backgroundTexture;
    private boolean textureLoaded = false;

    private MainMenuScreen() {
        super(Component.literal("Lumin-MainMenu"));
    }

    @Override
    public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!textureLoaded) {
            try {
                Optional<Resource> resource = mc.getResourceManager().getResource(bg);
                if (resource.isPresent()) {
                    try (InputStream is = resource.get().open(); NativeImage image = NativeImage.read(is)) {
                        GpuTexture texture = RenderSystem.getDevice().createTexture(
                                () -> "Lumin-Background: " + bg,
                                GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                                TextureFormat.RGBA8,
                                image.getWidth(),
                                image.getHeight(),
                                1,
                                1
                        );
                        var view = RenderSystem.getDevice().createTextureView(texture);
                        var sampler = RenderSystem.getDevice().createSampler(
                                AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                                FilterMode.LINEAR, FilterMode.LINEAR,
                                1,
                                java.util.OptionalDouble.empty()
                        );
                        RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, image);
                        backgroundTexture = new LuminTexture(texture, view, sampler);
                    }
                }
            } catch (Exception ignored) {
            }
            textureLoaded = true;
        }

        if (backgroundTexture != null) {
            textureRenderer.addRoundedTexture(backgroundTexture, 0, 0, width, height, 0, 0, 0, 1, 1, Color.WHITE);
        }
        rectRenderer.addRect(0, 0, width, height, new Color(0, 0, 0, 100));

        float menuXOffset = 0;
        float guiScale = (float) mc.getWindow().getGuiScale();

        float titleX = 30 / guiScale + 15 / guiScale + (200 / guiScale - textRenderer.getWidth("Lumin", 4.0f / guiScale)) / 2f;
        textRenderer.addText("Lumin", titleX, height / 2f - 180 / guiScale, 4.0f / guiScale, new Color(255, 255, 255, 115));

        float subTitleX = 30 / guiScale + 35 / guiScale + (200 / guiScale - textRenderer.getWidth("NeoForge 1.21.11", 1.3f / guiScale)) / 2f;
        textRenderer.addText("NeoForge 1.21.11", subTitleX, height / 2f - 180 / guiScale + textRenderer.getHeight(4.0f / guiScale) + 10 / guiScale, 1.3f / guiScale, Color.WHITE);

        rectRenderer.addRect(0, 0, 30 / guiScale + (200 / guiScale + 15 / guiScale * 2) + 30 / guiScale, height, new Color(15, 15, 15, 200));

        roundRectRenderer.addRoundRect(30 / guiScale, height / 2f - 40 / guiScale - 15 / guiScale, 200 / guiScale + 15 / guiScale * 2, (4 * (40 / guiScale + 10 / guiScale)) + 15 / guiScale * 2 - 10 / guiScale, 12f / guiScale, new Color(25, 25, 25, 230));

        String[] buttons = {
                I18n.get("menu.singleplayer"),
                I18n.get("menu.multiplayer"),
                I18n.get("menu.options"),
                I18n.get("menu.quit"),
        };
        for (int i = 0; i < 4; i++) {
            float bx = 30 / guiScale + 15 / guiScale;
            float by = height / 2f - 40 / guiScale + i * (40 / guiScale + 10 / guiScale);
            float bw = 200 / guiScale;
            float bh = 40 / guiScale;

            if (mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh) {
                roundRectRenderer.addRoundRect(bx, by, bw, bh, 8f / guiScale, new Color(60, 60, 60, 255));
                roundRectRenderer.addRoundRect(bx, by, bw, bh, 8f / guiScale, new Color(255, 255, 255, 30));
            } else {
                roundRectRenderer.addRoundRect(bx, by, bw, bh, 8f / guiScale, new Color(40, 40, 40, 255));
            }

            float textX = bx + (bw - textRenderer.getWidth(buttons[i], 1.5f / guiScale)) / 2f;
            float textY = by + (bh - textRenderer.getHeight(1.5f / guiScale)) / 2f;
            textRenderer.addText(buttons[i], textX, textY, 1.5f / guiScale, Color.WHITE);
        }
    }

    @Override
    public void luminRender(DeltaTracker partialTick) {
        if (mc.screen != this) return;

        textureRenderer.drawAndClear();
        rectRenderer.drawAndClear();
        roundRectRenderer.drawAndClear();
        textRenderer.drawAndClear();
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean focused) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            float guiScale = (float) mc.getWindow().getGuiScale();
            for (int i = 0; i < 4; i++) {
                float bx = 30 / guiScale + 15 / guiScale;
                float by = height / 2f - 40 / guiScale + i * (40 / guiScale + 10 / guiScale);
                float bw = 200 / guiScale;
                float bh = 40 / guiScale;

                if (event.x() >= bx && event.x() <= bx + bw && event.y() >= by && event.y() <= by + bh) {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
                    switch (i) {
                        case 0 -> mc.setScreen(new SelectWorldScreen(this));
                        case 1 -> mc.setScreen(new JoinMultiplayerScreen(this));
                        case 2 -> mc.setScreen(new OptionsScreen(this, mc.options, false));
                        case 3 -> mc.stop();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, focused);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

}