package me.sofurry.smtc;

/**
 * 外部媒体会话暴露的播放控制能力。
 * 位定义与原生侧 smtc_core.hpp 的 kControl* 常量保持一致。
 *
 * @param play     是否允许继续播放
 * @param pause    是否允许暂停
 * @param next     是否允许下一曲
 * @param previous 是否允许上一曲
 * @param stop     是否允许停止
 * @param seek     是否允许调整播放位置
 */
public record SmtcControls(
        boolean play,
        boolean pause,
        boolean next,
        boolean previous,
        boolean stop,
        boolean seek
) {

    public static final SmtcControls NONE = new SmtcControls(false, false, false, false, false, false);

    private static final int PLAY_BIT = 1;
    private static final int PAUSE_BIT = 1 << 1;
    private static final int NEXT_BIT = 1 << 2;
    private static final int PREVIOUS_BIT = 1 << 3;
    private static final int STOP_BIT = 1 << 4;
    private static final int SEEK_BIT = 1 << 5;

    public static SmtcControls fromNative(int flags) {
        return new SmtcControls(
                (flags & PLAY_BIT) != 0,
                (flags & PAUSE_BIT) != 0,
                (flags & NEXT_BIT) != 0,
                (flags & PREVIOUS_BIT) != 0,
                (flags & STOP_BIT) != 0,
                (flags & SEEK_BIT) != 0
        );
    }

}
