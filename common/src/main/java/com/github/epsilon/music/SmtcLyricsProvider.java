package com.github.epsilon.music;

import com.github.epsilon.Constants;
import com.github.epsilon.managers.ExecutorManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SMTC 外部歌曲的歌词提供器。SMTC 本身不含歌词，这里按 "artist - title"
 * 反查网易云歌词并缓存，供 HUD 按播放进度同步显示。
 * 请求全程在 {@link ExecutorManager} 工作线程执行，渲染线程只读缓存。
 */
public final class SmtcLyricsProvider {

    private static final long RETRY_BACKOFF_MS = 3000L;

    private record CachedLyrics(String lookupKey, ParsedLyrics lyrics) {
    }

    private static final AtomicReference<CachedLyrics> CURRENT = new AtomicReference<>();
    private static final AtomicReference<String> LOADING_KEY = new AtomicReference<>("");

    private static volatile String lastFailedKey = "";
    private static volatile long lastFailedAt;

    private SmtcLyricsProvider() {
    }

    /** 当前缓存歌词；歌名变化时由后台刷新，加载完成前返回旧缓存。 */
    public static ParsedLyrics lyrics() {
        CachedLyrics cached = CURRENT.get();
        return cached == null ? ParsedLyrics.EMPTY : cached.lyrics();
    }

    /**
     * 外部歌曲名变化时触发异步歌词加载。幂等，可在渲染线程按帧调用：
     * 相同 key 直接返回，只有 key 变化时才提交一次加载。
     */
    public static void onSongChanged(String displayKey) {
        if (displayKey == null || displayKey.isBlank()) {
            reset();
            return;
        }

        CachedLyrics cached = CURRENT.get();
        if (cached != null && cached.lookupKey().equals(displayKey)) return;
        if (displayKey.equals(LOADING_KEY.get())) return;
        // 上一首加载失败时按退避重试，否则一次失败会让旧歌词一直挂着（差一首）
        if (lastFailedKey.equals(displayKey) && System.currentTimeMillis() - lastFailedAt < RETRY_BACKOFF_MS) return;

        LOADING_KEY.set(displayKey);
        ExecutorManager.INSTANCE.execute(() -> {
            ParsedLyrics fetched = fetchLyrics(displayKey);
            if (fetched != null) {
                if (displayKey.equals(LOADING_KEY.get())) {
                    CURRENT.set(new CachedLyrics(displayKey, fetched));
                }
            } else {
                // 失败：释放加载标记允许下次重试
                LOADING_KEY.compareAndSet(displayKey, "");
                lastFailedKey = displayKey;
                lastFailedAt = System.currentTimeMillis();
            }
        });
    }

    /** 根据 "artist - title" 反查网易云歌词；反查或网络失败返回 null。 */
    private static ParsedLyrics fetchLyrics(String displayKey) {
        String title = displayKey;
        int separator = title.lastIndexOf(" - ");
        if (separator >= 0) {
            title = title.substring(separator + 3);
        }
        title = title.trim();
        if (title.isEmpty()) return null;

        try {
            List<NeteaseClient.NeteaseTrack> results = NeteaseClient.search(title, 1);
            if (results.isEmpty()) return null;
            return NeteaseClient.loadLyrics(results.getFirst().id());
        } catch (Exception e) {
            Constants.LOGGER.warn("SMTC lyric lookup failed for '{}'", displayKey, e);
            return null;
        }
    }

    public static void reset() {
        CURRENT.set(null);
        LOADING_KEY.set("");
        lastFailedKey = "";
        lastFailedAt = 0L;
    }

}
