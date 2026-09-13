package me.sofurry.smtc;

/**
 * JNI 边界返回的原始 SMTC 数据。
 *
 * @param available            当前是否存在媒体会话
 * @param title                标题
 * @param artist               艺术家
 * @param albumTitle           专辑标题
 * @param sourceAppId          Windows 媒体源应用标识
 * @param playbackStatus       Windows SMTC 播放状态枚举值
 * @param thumbnailRevision    封面内容版本
 * @param thumbnail            仅在封面版本变化时返回；空数组表示清除封面，null 表示沿用
 * @param positionMs           上报的播放位置；播放器不上报时间线时恒为 0
 * @param durationMs           曲目时长；播放器不上报时间线时恒为 0
 * @param positionUpdatedAtMs  {@code positionMs} 的采样时刻（Unix 毫秒），用于本地外推
 * @param controls             会话播放控制能力位，见 {@link SmtcControls}
 * @param error                原生查询错误
 */
public record SmtcNativeResult(
        boolean available,
        String title,
        String artist,
        String albumTitle,
        String sourceAppId,
        int playbackStatus,
        long thumbnailRevision,
        byte[] thumbnail,
        long positionMs,
        long durationMs,
        long positionUpdatedAtMs,
        int controls,
        String error
) {
}
