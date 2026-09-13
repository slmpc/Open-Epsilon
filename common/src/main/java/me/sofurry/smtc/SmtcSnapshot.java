package me.sofurry.smtc;

public record SmtcSnapshot(
        boolean available,
        String title,
        String artist,
        String albumTitle,
        String sourceAppId,
        SmtcPlaybackStatus playbackStatus,
        SmtcControls controls,
        long positionMs,
        long durationMs,
        long positionUpdatedAtMs,
        long thumbnailRevision,
        byte[] thumbnail
) {

    public static final SmtcSnapshot UNAVAILABLE = new SmtcSnapshot(
            false, "", "", "", "", SmtcPlaybackStatus.Closed, SmtcControls.NONE, 0L, 0L, 0L, 0L, null
    );

    static SmtcSnapshot merge(SmtcSnapshot previous, SmtcNativeResult result) {
        if (!result.available()) return UNAVAILABLE;

        byte[] nextThumbnail = result.thumbnail() == null ? previous.thumbnail() : result.thumbnail();
        return new SmtcSnapshot(
                true,
                safe(result.title()),
                safe(result.artist()),
                safe(result.albumTitle()),
                safe(result.sourceAppId()),
                SmtcPlaybackStatus.fromNative(result.playbackStatus()),
                SmtcControls.fromNative(result.controls()),
                Math.max(0L, result.positionMs()),
                Math.max(0L, result.durationMs()),
                result.positionUpdatedAtMs(),
                result.thumbnailRevision(),
                nextThumbnail
        );
    }

    public boolean isPlaying() {
        return playbackStatus == SmtcPlaybackStatus.Playing;
    }

    /**
     * 播放器不上报时间线（如未装 SMTC 增强插件的网易云）时 {@link #hasTimeline()} 为 false，
     * 调用方应回退为无进度条展示。
     */
    public boolean hasTimeline() {
        return available && durationMs > 0L;
    }

    /**
     * 估算当前播放位置（毫秒）。SMTC 仅在时间线更新时上报 position，
     * 播放中按 {@code positionUpdatedAtMs}（Unix 毫秒）本地外推；
     * 暂停或时间戳无效时直接返回上报值。
     */
    public long estimatedPositionMs() {
        return estimatedPositionMs(System.currentTimeMillis());
    }

    public long estimatedPositionMs(long now) {
        if (!hasTimeline()) return 0L;

        long position = positionMs;
        if (isPlaying() && positionUpdatedAtMs > 0L && positionUpdatedAtMs <= now) {
            position += now - positionUpdatedAtMs;
        }
        return Math.max(0L, Math.min(position, durationMs));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

}
