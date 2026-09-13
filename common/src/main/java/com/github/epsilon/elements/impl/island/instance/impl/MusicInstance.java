package com.github.epsilon.elements.impl.island.instance.impl;

import com.github.epsilon.Constants;
import com.github.epsilon.elements.impl.island.IslandPalette;
import com.github.epsilon.elements.impl.island.instance.LandInstance;
import com.github.epsilon.elements.impl.island.pattern.LCPattern;
import com.github.epsilon.graphics.LuminRenderSystem;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.graphics.text.IconChars;
import com.github.epsilon.graphics.text.StaticFontLoader;
import com.github.epsilon.gui.lib.UiRect;
import com.github.epsilon.gui.lib.UiTree;
import com.github.epsilon.music.ParsedLyrics;
import com.github.epsilon.music.SmtcLyricsProvider;
import com.mojang.blaze3d.platform.NativeImage;
import me.sofurry.smtc.SmtcService;
import me.sofurry.smtc.SmtcSnapshot;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static com.github.epsilon.Constants.mc;

/**
 * 将 Windows SMTC 当前媒体会话绘制为 Island 内容。
 * <p>
 * 时间线（进度/时长）是可选信息：部分播放器（如未装 SMTC 增强插件的网易云）
 * 不上报时间线，此时不绘制进度条与歌词行，保持原始三行布局。
 */
public class MusicInstance extends LandInstance {

    private static final Identifier COVER_TEXTURE = Identifier.fromNamespaceAndPath("epsilon", "smtc/album_art");

    private static final float PADDING = 7f;
    private static final float COVER_SIZE = 42f;
    private static final float COVER_RADIUS = 8f;
    private static final float COVER_GAP = 8f;
    private static final float MIN_WIDTH = 216f;
    private static final float MAX_WIDTH = 286f;
    private static final float TITLE_SCALE = 1.0f;
    private static final float ARTIST_SCALE = 0.78f;
    private static final float META_SCALE = 0.68f;
    private static final float LYRIC_SCALE = 0.8f;
    private static final float TIME_SCALE = 0.62f;
    private static final float ICON_SCALE = 0.78f;
    private static final float WAVE_WIDTH = 18f;
    private static final float TITLE_STATUS_GAP = 8f;
    private static final float MARQUEE_SPEED = 22f;
    private static final float MARQUEE_START_HOLD_SECONDS = 1.25f;
    private static final float MARQUEE_GAP = 28f;
    private static final float LYRIC_TOP_GAP = 3f;
    private static final float LYRIC_ROW_HEIGHT = 11f;
    private static final float PROGRESS_TOP_GAP = 2f;
    private static final float PROGRESS_ROW_HEIGHT = 11f;
    private static final float PROGRESS_BAR_HEIGHT = 3.5f;
    private static final float PROGRESS_TEXT_GAP = 6f;
    private static final int COVER_TEXTURE_SIZE = 256;

    private final SmtcService service;
    private final Supplier<TextRenderer> textRendererSupplier;
    private final Supplier<Boolean> lyricEnabled;

    private SmtcSnapshot snapshot = SmtcSnapshot.UNAVAILABLE;
    private long coverRevision = Long.MIN_VALUE;
    private DynamicTexture coverTexture;
    private boolean coverAvailable;
    private final MarqueeState titleMarquee = new MarqueeState();
    private final MarqueeState lyricMarquee = new MarqueeState();
    private String lyricLine = "";

    public MusicInstance(SmtcService service, Supplier<TextRenderer> textRendererSupplier,
                         Supplier<Boolean> lyricEnabled, LCPattern pattern) {
        super(pattern, 1);
        this.service = service;
        this.textRendererSupplier = textRendererSupplier;
        this.lyricEnabled = lyricEnabled;
    }

    @Override
    public void update() {
        snapshot = service.snapshot();
        updateCoverTexture(snapshot);

        titleMarquee.advance(title(snapshot));

        // 歌词反查幂等，可按帧调用；实际请求在工作线程执行。
        lyricLine = "";
        if (lyricEnabled.get() && snapshot.hasTimeline()) {
            SmtcLyricsProvider.onSongChanged(displayKey(snapshot));
            lyricLine = currentLyricLine();
        }
        lyricMarquee.advance(lyricLine);

        targetRadius = 0.42f;
        boolean showLyric = !lyricLine.isEmpty();
        boolean showProgress = snapshot.hasTimeline();
        float contentBottom = PADDING + COVER_SIZE;
        if (showLyric) contentBottom += LYRIC_TOP_GAP + LYRIC_ROW_HEIGHT;
        if (showProgress) contentBottom += PROGRESS_TOP_GAP + PROGRESS_ROW_HEIGHT;
        targetHeight = contentBottom + PADDING;

        TextRenderer textRenderer = textRendererSupplier.get();
        String title = title(snapshot);
        String artist = artist(snapshot);
        String metadata = metadata(snapshot);
        float desiredContent = Math.max(
                textRenderer.getWidth(title, TITLE_SCALE) + WAVE_WIDTH + TITLE_STATUS_GAP,
                Math.max(textRenderer.getWidth(artist, ARTIST_SCALE), textRenderer.getWidth(metadata, META_SCALE))
        );
        float desiredWidth = PADDING * 2f + COVER_SIZE + COVER_GAP + desiredContent;
        float screenMaxWidth = Math.max(MIN_WIDTH, LuminRenderSystem.getScaledWidthInt() - 50f);
        targetWidth = Mth.clamp(desiredWidth, MIN_WIDTH, Math.min(MAX_WIDTH, screenMaxWidth));
    }

    @Override
    public void draw(UiTree.Scope scope, float translateX, float translateY, float width, float height) {
        if (!snapshot.available()) return;

        drawCover(scope);

        TextRenderer textRenderer = textRendererSupplier.get();
        float textX = PADDING + COVER_SIZE + COVER_GAP;
        float contentRight = width - PADDING;
        float titleRight = contentRight - WAVE_WIDTH - TITLE_STATUS_GAP;
        float titleViewport = Math.max(1f, titleRight - textX);

        // 三行基础内容始终约束在封面区域内；附加行在封面下方全宽展开。
        float titleY = PADDING + 1f;
        drawMarqueeText(scope, textRenderer, title(snapshot), TITLE_SCALE, IslandPalette.TEXT_PRIMARY,
                titleMarquee, textX, titleY, titleViewport);
        drawPlaybackWave(scope, contentRight - WAVE_WIDTH, titleY + 1f, WAVE_WIDTH, 9f);

        float artistY = titleY + textRenderer.getHeight(TITLE_SCALE) + 3f;
        drawIconText(scope, IconChars.ARTIST, artist(snapshot), textX, artistY,
                Math.max(1f, contentRight - textX), ARTIST_SCALE, IslandPalette.TEXT_SECONDARY);

        float metaY = PADDING + COVER_SIZE - textRenderer.getHeight(META_SCALE) - 1f;
        drawIconText(scope, snapshot.albumTitle().isBlank() ? IconChars.AUDIO_FILE : IconChars.ALBUM, metadata(snapshot), textX, metaY, Math.max(1f, contentRight - textX), META_SCALE, IslandPalette.TEXT_MUTED);

        float contentBottom = PADDING + COVER_SIZE;
        if (!lyricLine.isEmpty()) {
            float lyricY = contentBottom + LYRIC_TOP_GAP;
            drawMarqueeText(scope, textRenderer, lyricLine, LYRIC_SCALE, IslandPalette.TEXT_PRIMARY,
                    lyricMarquee, PADDING, lyricY, Math.max(1f, width - PADDING * 2f));
            contentBottom = lyricY + LYRIC_ROW_HEIGHT;
        }

        if (snapshot.hasTimeline()) {
            drawProgress(scope, textRenderer, contentBottom + PROGRESS_TOP_GAP, width);
        }
    }

    private void drawProgress(UiTree.Scope scope, TextRenderer textRenderer, float y, float width) {
        long positionMs = snapshot.estimatedPositionMs();
        float fraction = snapshot.durationMs() > 0L
                ? Mth.clamp(positionMs / (float) snapshot.durationMs(), 0f, 1f)
                : 0f;

        String timeText = formatTime(positionMs) + " / " + formatTime(snapshot.durationMs());
        float timeWidth = textRenderer.getWidth(timeText, TIME_SCALE);
        float timeY = y + (PROGRESS_ROW_HEIGHT - textRenderer.getHeight(TIME_SCALE)) * 0.5f;
        scope.text(timeText, width - PADDING - timeWidth, timeY, TIME_SCALE, fade(IslandPalette.TEXT_MUTED));

        float barWidth = Math.max(1f, (width - PADDING - PROGRESS_TEXT_GAP - timeWidth) - PADDING * 2f);
        float barY = y + (PROGRESS_ROW_HEIGHT - PROGRESS_BAR_HEIGHT) * 0.5f;
        scope.roundRect(PADDING, barY, barWidth, PROGRESS_BAR_HEIGHT, PROGRESS_BAR_HEIGHT * 0.5f, fade(IslandPalette.TRACK));

        float fillWidth = barWidth * fraction;
        if (fillWidth >= 2f) {
            scope.roundRectHorizontalGradient(PADDING, barY, fillWidth, PROGRESS_BAR_HEIGHT,
                    Math.min(PROGRESS_BAR_HEIGHT * 0.5f, fillWidth * 0.5f), fade(IslandPalette.ACCENT), fade(IslandPalette.ACCENT_ALT));
        }
    }

    private void drawCover(UiTree.Scope scope) {
        scope.outline(PADDING, PADDING, COVER_SIZE, COVER_SIZE, COVER_RADIUS, 0.75f, fade(new Color(255, 255, 255, 48)));

        if (coverAvailable) {
            scope.roundedTexture(COVER_TEXTURE, PADDING, PADDING, COVER_SIZE, COVER_SIZE, COVER_RADIUS, 0f, 0f, 1f, 1f, fade(Color.WHITE), true);
        } else {
            scope.roundRectHorizontalGradient(PADDING, PADDING, COVER_SIZE, COVER_SIZE, COVER_RADIUS, fade(new Color(75, 88, 116)), fade(new Color(67, 125, 122)));
            float iconScale = 1.35f;
            TextRenderer textRenderer = textRendererSupplier.get();
            float iconWidth = textRenderer.getWidth(IconChars.ALBUM, iconScale, StaticFontLoader.ICONS);
            float iconHeight = textRenderer.getHeight(iconScale, StaticFontLoader.ICONS);
            scope.text(IconChars.ALBUM, PADDING + (COVER_SIZE - iconWidth) * 0.5f, PADDING + (COVER_SIZE - iconHeight) * 0.5f, iconScale, fade(IslandPalette.TEXT_PRIMARY), StaticFontLoader.ICONS);
        }
    }

    private void drawMarqueeText(UiTree.Scope scope, TextRenderer textRenderer, String text, float scale,
                                 Color color, MarqueeState state, float x, float y, float viewportWidth) {
        float textWidth = textRenderer.getWidth(text, scale);
        if (textWidth <= viewportWidth) {
            scope.text(text, x, y, scale, fade(color));
            return;
        }

        float elapsedSeconds = (System.nanoTime() - state.startedAtNs) / 1_000_000_000.0f;
        float scrollSeconds = Math.max(0f, elapsedSeconds - MARQUEE_START_HOLD_SECONDS);
        float cycle = textWidth + MARQUEE_GAP;
        float offset = -(scrollSeconds * MARQUEE_SPEED % cycle);
        UiRect clip = new UiRect(x, y - 1f, viewportWidth, textRenderer.getHeight(scale) + 2f);
        scope.scissor(clip, inner -> {
            inner.text(text, x + offset, y, scale, fade(color));
            inner.text(text, x + offset + cycle, y, scale, fade(color));
        });
    }

    private void drawIconText(UiTree.Scope scope, String icon, String text, float x, float y,
                              float viewportWidth, float textScale, Color color) {
        TextRenderer textRenderer = textRendererSupplier.get();
        float iconWidth = textRenderer.getWidth(icon, ICON_SCALE, StaticFontLoader.ICONS);
        float iconY = y + (textRenderer.getHeight(textScale) - textRenderer.getHeight(ICON_SCALE, StaticFontLoader.ICONS)) * 0.5f;
        scope.text(icon, x, iconY, ICON_SCALE, fade(IslandPalette.ACCENT), StaticFontLoader.ICONS);

        float textX = x + iconWidth + 3f;
        float textViewport = Math.max(1f, viewportWidth - iconWidth - 3f);
        UiRect clip = new UiRect(textX, y - 1f, textViewport, textRenderer.getHeight(textScale) + 2f);
        scope.scissor(clip, inner -> inner.text(text, textX, y, textScale, fade(color)));
    }

    private void drawPlaybackWave(UiTree.Scope scope, float x, float y, float width, float height) {
        int bars = 4;
        float barWidth = 2f;
        float gap = (width - bars * barWidth) / (bars - 1);
        double time = System.nanoTime() / 1_000_000_000.0;
        boolean playing = snapshot.isPlaying();

        for (int index = 0; index < bars; index++) {
            // 暂停时波形静止在低位，避免“已暂停但仍跳动”的误导。
            float amplitude = playing
                    ? 0.28f + 0.72f * (float) ((Math.sin(time * 4.8 + index * 1.7) + 1.0) * 0.5)
                    : 0.34f;
            float barHeight = Math.max(2f, height * amplitude);
            float barX = x + index * (barWidth + gap);
            float barY = y + (height - barHeight) * 0.5f;
            Color color = index < 2 ? IslandPalette.ACCENT : IslandPalette.ACCENT_ALT;
            scope.roundRect(barX, barY, barWidth, barHeight, barWidth * 0.5f, fade(color));
        }
    }

    /** 按当前播放位置定位歌词行；时间线不可用或无缓存歌词时返回空串。 */
    private String currentLyricLine() {
        ParsedLyrics lyrics = SmtcLyricsProvider.lyrics();
        if (lyrics.isEmpty()) return "";

        long position = snapshot.estimatedPositionMs();
        List<Long> timestamps = lyrics.timestamps();
        int index = -1;
        for (int i = 0; i < timestamps.size(); i++) {
            if (position >= timestamps.get(i)) {
                index = i;
            } else {
                break;
            }
        }
        return index < 0 ? "" : lyrics.lines().get(index);
    }

    private void updateCoverTexture(SmtcSnapshot next) {
        if (next.thumbnailRevision() == coverRevision) return;

        coverRevision = next.thumbnailRevision();
        coverAvailable = false;
        byte[] thumbnail = next.thumbnail();
        if (thumbnail == null || thumbnail.length == 0) return;

        NativeImage image = null;
        DynamicTexture newTexture = null;
        try {
            image = decodeThumbnail(thumbnail);
            if (coverTexture == null) {
                newTexture = new DynamicTexture(() -> "Epsilon SMTC album art", image);
                image = null;
                mc.getTextureManager().register(COVER_TEXTURE, newTexture);
                coverTexture = newTexture;
                newTexture = null;
            } else {
                coverTexture.setPixels(image);
                image = null;
                coverTexture.upload();
            }
            coverAvailable = true;
        } catch (IOException | RuntimeException e) {
            if (newTexture != null) {
                newTexture.close();
            } else if (image != null) {
                image.close();
            }
            Constants.LOGGER.warn("Failed to decode SMTC album art revision {}", coverRevision, e);
        }
    }

    /**
     * SMTC 封面可能是 JPEG；Minecraft 26.2 的 NativeImage.read 会先强制校验 PNG。
     * 这里先通过 ImageIO 解码并裁成方形，再显式写入 NativeImage。
     */
    private static NativeImage decodeThumbnail(byte[] thumbnail) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(thumbnail));
        if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
            throw new IOException("Unsupported SMTC thumbnail image format");
        }

        int cropSize = Math.min(source.getWidth(), source.getHeight());
        int cropX = (source.getWidth() - cropSize) / 2;
        int cropY = (source.getHeight() - cropSize) / 2;
        BufferedImage normalized = new BufferedImage(COVER_TEXTURE_SIZE, COVER_TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = normalized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, COVER_TEXTURE_SIZE, COVER_TEXTURE_SIZE, cropX, cropY, cropX + cropSize, cropY + cropSize, null);
        graphics.dispose();

        NativeImage result = new NativeImage(COVER_TEXTURE_SIZE, COVER_TEXTURE_SIZE, false);
        try {
            for (int y = 0; y < COVER_TEXTURE_SIZE; y++) {
                for (int x = 0; x < COVER_TEXTURE_SIZE; x++) {
                    result.setPixel(x, y, normalized.getRGB(x, y));
                }
            }
            return result;
        } catch (RuntimeException e) {
            result.close();
            throw e;
        }
    }

    private void releaseCoverTexture() {
        if (coverTexture != null) {
            mc.getTextureManager().release(COVER_TEXTURE);
            coverTexture = null;
        }
        coverAvailable = false;
    }

    private static String title(SmtcSnapshot snapshot) {
        return snapshot.title().isBlank() ? "Unknown track" : snapshot.title();
    }

    private static String artist(SmtcSnapshot snapshot) {
        return snapshot.artist().isBlank() ? "Unknown artist" : snapshot.artist();
    }

    private static String metadata(SmtcSnapshot snapshot) {
        String source = sourceName(snapshot.sourceAppId());
        if (snapshot.albumTitle().isBlank()) return source;
        if (source.isBlank()) return snapshot.albumTitle();
        return snapshot.albumTitle() + "  /  " + source;
    }

    /** 歌词反查键：艺术家为空时仅用标题，避免把回退文案带进搜索词。 */
    private static String displayKey(SmtcSnapshot snapshot) {
        if (snapshot.title().isBlank()) return "";
        return snapshot.artist().isBlank() ? snapshot.title() : snapshot.artist() + " - " + snapshot.title();
    }

    private static String formatTime(long milliseconds) {
        long totalSeconds = Math.max(0L, milliseconds) / 1000L;
        return totalSeconds / 60 + ":" + String.format(Locale.ROOT, "%02d", totalSeconds % 60);
    }

    private static String sourceName(String sourceAppId) {
        if (sourceAppId == null || sourceAppId.isBlank()) return "Windows media";

        String value = sourceAppId;
        int appSeparator = value.lastIndexOf('!');
        if (appSeparator >= 0 && appSeparator + 1 < value.length()) {
            value = value.substring(appSeparator + 1);
        }
        if (value.toLowerCase(Locale.ROOT).endsWith(".exe")) {
            value = value.substring(0, value.length() - 4);
        }
        return value.isBlank() ? "Windows media" : value;
    }

    @Override
    public void onRemoved() {
        releaseCoverTexture();
    }

    /**
     * 跑马灯状态：文本变化时重置滚动起点，未溢出时不需要滚动。
     */
    private static final class MarqueeState {

        private String text = "";
        private long startedAtNs = System.nanoTime();

        void advance(String next) {
            if (next.equals(text)) return;
            text = next;
            startedAtNs = System.nanoTime();
        }

    }

}
