#pragma once

#include <cstdint>
#include <optional>
#include <string>
#include <vector>

namespace epsilon::smtc {

// 播放控制命令 ID，须与 Java 侧 SmtcNativeBridge 的 COMMAND_* 常量保持一致。
enum class Command : int {
    Play = 0,
    Pause,
    Next,
    Previous,
    Stop,
    Seek,
};

// 会话播放控制能力位，须与 Java 侧 SmtcControls 的位定义保持一致。
enum PlaybackControlFlags : std::uint32_t {
    kControlPlay = 1u << 0,
    kControlPause = 1u << 1,
    kControlNext = 1u << 2,
    kControlPrevious = 1u << 3,
    kControlStop = 1u << 4,
    kControlSeek = 1u << 5,
};

struct Snapshot {
    bool available = false;
    std::wstring title;
    std::wstring artist;
    std::wstring album_title;
    std::wstring source_app_id;
    int playback_status = 0;
    std::uint64_t thumbnail_revision = 0;
    std::optional<std::vector<std::uint8_t>> thumbnail;
    // 时间线为可选信息：部分播放器（如未装 SMTC 增强插件的网易云）不上报，
    // 读取失败或恒为 0 时由 Java 侧回退为无进度条展示。
    std::int64_t position_ms = 0;
    std::int64_t duration_ms = 0;
    std::int64_t position_updated_at_ms = 0;
    std::uint32_t controls = 0;
    std::wstring error;
};

Snapshot poll();
void reset();
bool send_command(Command command, std::int64_t position_ms);

} // namespace epsilon::smtc
