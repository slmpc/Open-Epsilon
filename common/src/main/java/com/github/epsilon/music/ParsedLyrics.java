package com.github.epsilon.music;

import java.util.List;

/**
 * 解析后的逐行歌词：时间戳与行一一对应，按时间升序。
 *
 * @param timestamps 每行歌词的起始时间（毫秒）
 * @param lines      每行歌词文本
 */
public record ParsedLyrics(List<Long> timestamps, List<String> lines) {

    public static final ParsedLyrics EMPTY = new ParsedLyrics(List.of(), List.of());

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /** 无时长信息时按末行时间加 10 秒粗略估算整曲时长。 */
    public long durationHintMs() {
        return timestamps.isEmpty() ? 0L : timestamps.getLast() + 10_000L;
    }

}
