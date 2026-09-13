package com.github.epsilon.music;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LRC 歌词解析器。支持一行多个时间标签的扩展写法，同一时刻保持原文出现顺序。
 */
public final class LrcParser {

    private static final Pattern TIME_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{2})[.:](\\d{2,3})]");

    private LrcParser() {
    }

    public static ParsedLyrics parse(String content) {
        if (content == null || content.isBlank()) {
            return ParsedLyrics.EMPTY;
        }

        List<Line> collected = new ArrayList<>();
        for (String rawLine : content.split("\n")) {
            if (rawLine.isBlank() || !rawLine.contains("[")) {
                continue;
            }

            Matcher matcher = TIME_PATTERN.matcher(rawLine);
            List<int[]> groups = new ArrayList<>();
            int lastEnd = -1;
            while (matcher.find()) {
                groups.add(new int[]{matcher.start(), matcher.end()});
                lastEnd = matcher.end();
            }
            if (groups.isEmpty()) {
                continue;
            }

            // 文本取最后一个时间标签之后的内容，逐字裁剪以同时兼容 \r\n 与多余空白。
            String text = rawLine.substring(lastEnd).trim();
            if (text.isEmpty()) {
                continue;
            }

            for (int[] group : groups) {
                Matcher single = TIME_PATTERN.matcher(rawLine.substring(group[0], group[1]));
                if (!single.find()) {
                    continue;
                }
                long minutes = Long.parseLong(single.group(1));
                long seconds = Long.parseLong(single.group(2));
                String millis = single.group(3);
                long millisValue = Long.parseLong(millis) * (millis.length() == 2 ? 10L : 1L);
                collected.add(new Line(minutes * 60_000L + seconds * 1_000L + millisValue, text));
            }
        }

        if (collected.isEmpty()) {
            return ParsedLyrics.EMPTY;
        }

        // 稳定排序：同一时间戳的行保持 LRC 原文顺序。
        collected.sort(Comparator.comparingLong(Line::timestamp));
        List<Long> timestamps = new ArrayList<>(collected.size());
        List<String> lines = new ArrayList<>(collected.size());
        for (Line line : collected) {
            timestamps.add(line.timestamp);
            lines.add(line.text);
        }
        return new ParsedLyrics(List.copyOf(timestamps), List.copyOf(lines));
    }

    private record Line(long timestamp, String text) {
    }

}
