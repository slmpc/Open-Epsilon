package com.github.epsilon.music;

import com.github.epsilon.Constants;
import com.github.epsilon.utils.network.Http;
import com.google.gson.JsonObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 网易云音乐 Web API 的最小客户端，仅提供搜索与歌词获取（SMTC 歌词反查使用）。
 * 请求须在工作线程调用；携带 Referer 模拟网页端以通过网易云的基础校验。
 */
public final class NeteaseClient {

    private static final String BASE_URL = "https://music.163.com";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10L);

    private NeteaseClient() {
    }

    /**
     * @param id         网易云歌曲 ID
     * @param title      歌曲名
     * @param artist     艺术家（多人以 "/" 连接）
     * @param durationMs 曲目时长，未知为 0
     */
    public record NeteaseTrack(long id, String title, String artist, long durationMs) {
    }

    /**
     * 关键词搜索歌曲，网络失败或解析失败返回空列表。
     */
    public static List<NeteaseTrack> search(String keyword, int limit) {
        String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String url = BASE_URL + "/api/search/get/web?s=" + encoded + "&type=1&offset=0&limit=" + limit;
        JsonObject root = requestJson(url);
        if (root == null) return List.of();

        try {
            JsonObject result = root.getAsJsonObject("result");
            if (result == null || !result.has("songs")) return List.of();

            List<NeteaseTrack> tracks = new ArrayList<>();
            for (var element : result.getAsJsonArray("songs")) {
                JsonObject song = element.getAsJsonObject();
                if (!song.has("id") || !song.has("name")) continue;

                List<String> artists = new ArrayList<>();
                if (song.has("artists") && song.getAsJsonArray("artists") != null) {
                    for (var artistElement : song.getAsJsonArray("artists")) {
                        JsonObject artist = artistElement.getAsJsonObject();
                        if (artist.has("name") && !artist.get("name").isJsonNull()) {
                            artists.add(artist.get("name").getAsString());
                        }
                    }
                }
                tracks.add(new NeteaseTrack(
                        song.get("id").getAsLong(),
                        song.get("name").getAsString(),
                        String.join("/", artists),
                        song.has("duration") && !song.get("duration").isJsonNull() ? song.get("duration").getAsLong() : 0L
                ));
            }
            return List.copyOf(tracks);
        } catch (RuntimeException e) {
            Constants.LOGGER.warn("Failed to parse NetEase search response for '{}'", keyword, e);
            return List.of();
        }
    }

    /**
     * 拉取并解析歌曲歌词；无歌词或歌曲不存在返回 {@link ParsedLyrics#EMPTY}，网络失败抛出异常。
     */
    public static ParsedLyrics loadLyrics(long id) throws java.io.IOException {
        String url = BASE_URL + "/api/song/lyric?id=" + id + "&lv=1&kv=1&tv=-1";
        JsonObject root = requestJson(url);
        if (root == null) throw new java.io.IOException("NetEase lyric request failed for id " + id);

        try {
            JsonObject lrc = root.getAsJsonObject("lrc");
            if (lrc == null || !lrc.has("lyric") || lrc.get("lyric").isJsonNull()) {
                return ParsedLyrics.EMPTY;
            }
            return LrcParser.parse(lrc.get("lyric").getAsString());
        } catch (RuntimeException e) {
            Constants.LOGGER.warn("Failed to parse NetEase lyric response for id {}", id, e);
            return ParsedLyrics.EMPTY;
        }
    }

    private static JsonObject requestJson(String url) {
        JsonObject body = Http.get(url)
                .header("Referer", BASE_URL)
                .timeout(REQUEST_TIMEOUT)
                .exceptionHandler(e -> Constants.LOGGER.warn("NetEase request failed: {}", url, e))
                .sendJson(JsonObject.class);
        return body != null && body.isJsonObject() ? body : null;
    }

}
