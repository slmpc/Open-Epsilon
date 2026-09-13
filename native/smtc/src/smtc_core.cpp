#include "smtc_core.hpp"

#include <algorithm>
#include <atomic>
#include <chrono>
#include <mutex>
#include <string_view>

#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Media.Control.h>
#include <winrt/Windows.Storage.Streams.h>

namespace epsilon::smtc {
namespace {

using namespace std::chrono_literals;
using winrt::Windows::Media::Control::GlobalSystemMediaTransportControlsSessionManager;
using winrt::Windows::Storage::Streams::DataReader;

constexpr std::uint64_t kMaximumThumbnailBytes = 16ULL * 1024ULL * 1024ULL;
constexpr std::uint64_t kFnvOffset = 14695981039346656037ULL;
constexpr std::uint64_t kFnvPrime = 1099511628211ULL;

struct State {
    GlobalSystemMediaTransportControlsSessionManager manager{nullptr};
    std::wstring track_key;
    std::vector<std::uint8_t> thumbnail;
    std::uint64_t thumbnail_revision = 0;
    std::uint64_t delivered_revision = 0;
    std::chrono::steady_clock::time_point thumbnail_refresh{};
};

struct Context {
    std::mutex mutex;
    std::atomic_bool reset_requested{false};
    State state;
};

Context& process_context() {
    // WinRT handles and an in-use mutex must not be destroyed under the Windows loader lock.
    static Context* const instance = new Context();
    return *instance;
}

void ensure_apartment() {
    static thread_local bool initialized = false;
    if (!initialized) {
        winrt::init_apartment(winrt::apartment_type::multi_threaded);
        initialized = true;
    }
}

std::uint64_t hash_bytes(std::uint64_t value, const void* data, std::size_t size) {
    const auto* bytes = static_cast<const std::uint8_t*>(data);
    for (std::size_t index = 0; index < size; ++index) {
        value ^= bytes[index];
        value *= kFnvPrime;
    }
    return value;
}

std::uint64_t hash_text(std::uint64_t value, std::wstring_view text) {
    return hash_bytes(value, text.data(), text.size() * sizeof(wchar_t));
}

// WinRT TimeSpan 以 100ns 为单位。
std::int64_t timespan_to_ms(winrt::Windows::Foundation::TimeSpan span) {
    return span.count() / 10000;
}

// WinRT DateTime 纪元为 1601-01-01（100ns），换算到 Unix 纪元毫秒。
std::int64_t datetime_to_unix_ms(winrt::Windows::Foundation::DateTime time) {
    constexpr std::int64_t kUnixEpochOffsetTicks = 11644473600000LL * 10000LL;
    return (time.time_since_epoch().count() - kUnixEpochOffsetTicks) / 10000;
}

std::wstring build_track_key(const Snapshot& snapshot) {
    std::wstring key;
    key.reserve(snapshot.source_app_id.size() + snapshot.title.size() + snapshot.artist.size()
            + snapshot.album_title.size() + 4);
    key.append(snapshot.source_app_id).push_back(L'\n');
    key.append(snapshot.title).push_back(L'\n');
    key.append(snapshot.artist).push_back(L'\n');
    key.append(snapshot.album_title);
    return key;
}

std::vector<std::uint8_t> read_thumbnail(
        const winrt::Windows::Storage::Streams::IRandomAccessStreamReference& reference) {
    if (!reference) {
        return {};
    }

    auto stream = reference.OpenReadAsync().get();
    const std::uint64_t size = stream.Size();
    if (size == 0 || size > kMaximumThumbnailBytes) {
        stream.Close();
        return {};
    }

    auto input = stream.GetInputStreamAt(0);
    DataReader reader(input);
    const auto requested = static_cast<std::uint32_t>(size);
    const std::uint32_t loaded = reader.LoadAsync(requested).get();
    std::vector<std::uint8_t> bytes(loaded);
    if (!bytes.empty()) {
        reader.ReadBytes(bytes);
    }
    reader.Close();
    input.Close();
    stream.Close();
    return bytes;
}

std::uint64_t thumbnail_revision(std::wstring_view track_key, const std::vector<std::uint8_t>& bytes) {
    std::uint64_t value = hash_text(kFnvOffset, track_key);
    value = hash_bytes(value, bytes.data(), bytes.size());
    value ^= static_cast<std::uint64_t>(bytes.size());
    value *= kFnvPrime;
    return value == 0 ? 1 : value;
}

Snapshot unavailable(std::wstring error = {}) {
    Snapshot snapshot;
    snapshot.error = std::move(error);
    return snapshot;
}

} // namespace

Snapshot poll() {
    Context& context = process_context();
    std::scoped_lock lock(context.mutex);
    State& state = context.state;

    try {
        if (context.reset_requested.exchange(false, std::memory_order_acq_rel)) {
            state = State{};
        }

        ensure_apartment();
        if (!state.manager) {
            state.manager = GlobalSystemMediaTransportControlsSessionManager::RequestAsync().get();
        }

        auto session = state.manager.GetCurrentSession();
        if (!session) {
            state.track_key.clear();
            state.thumbnail.clear();
            state.thumbnail_revision = 0;
            state.delivered_revision = 0;
            return unavailable();
        }

        auto properties = session.TryGetMediaPropertiesAsync().get();
        auto playback = session.GetPlaybackInfo();

        Snapshot snapshot;
        snapshot.available = true;
        snapshot.title = properties.Title().c_str();
        snapshot.artist = properties.Artist().c_str();
        if (snapshot.artist.empty()) {
            snapshot.artist = properties.AlbumArtist().c_str();
        }
        snapshot.album_title = properties.AlbumTitle().c_str();
        snapshot.source_app_id = session.SourceAppUserModelId().c_str();
        snapshot.playback_status = static_cast<int>(playback.PlaybackStatus());

        // 时间线与控制能力是可选信息：部分播放器（如未装 SMTC 增强插件的网易云）
        // 不上报时间线，读取失败时保持 0，由 Java 侧回退为无进度条展示。
        try {
            auto timeline = session.GetTimelineProperties();
            snapshot.position_ms = timespan_to_ms(timeline.Position());
            snapshot.duration_ms = timespan_to_ms(timeline.EndTime());
            snapshot.position_updated_at_ms = datetime_to_unix_ms(timeline.LastUpdatedTime());
        } catch (...) {
        }
        try {
            auto controls = playback.Controls();
            if (controls) {
                if (controls.IsPlayEnabled()) snapshot.controls |= kControlPlay;
                if (controls.IsPauseEnabled()) snapshot.controls |= kControlPause;
                if (controls.IsNextEnabled()) snapshot.controls |= kControlNext;
                if (controls.IsPreviousEnabled()) snapshot.controls |= kControlPrevious;
                if (controls.IsStopEnabled()) snapshot.controls |= kControlStop;
                if (controls.IsPlaybackPositionEnabled()) snapshot.controls |= kControlSeek;
            }
        } catch (...) {
        }

        const std::wstring track_key = build_track_key(snapshot);
        const auto now = std::chrono::steady_clock::now();
        const bool changed_track = track_key != state.track_key;
        const bool retry_missing_thumbnail = state.thumbnail.empty()
                && now - state.thumbnail_refresh >= 2s;

        if (changed_track || retry_missing_thumbnail) {
            state.track_key = track_key;
            state.thumbnail_refresh = now;
            state.thumbnail = read_thumbnail(properties.Thumbnail());
            state.thumbnail_revision = thumbnail_revision(track_key, state.thumbnail);
        }

        snapshot.thumbnail_revision = state.thumbnail_revision;
        if (state.thumbnail_revision != state.delivered_revision) {
            snapshot.thumbnail = state.thumbnail;
            state.delivered_revision = state.thumbnail_revision;
        }
        return snapshot;
    } catch (const winrt::hresult_error& error) {
        return unavailable(std::wstring(error.message().c_str()));
    } catch (const std::exception& error) {
        return unavailable(std::wstring(winrt::to_hstring(error.what()).c_str()));
    } catch (...) {
        return unavailable(L"Unknown native SMTC failure");
    }
}

void reset() {
    process_context().reset_requested.store(true, std::memory_order_release);
}

bool send_command(Command command, std::int64_t position_ms) {
    Context& context = process_context();
    std::scoped_lock lock(context.mutex);
    try {
        ensure_apartment();
        State& state = context.state;
        if (!state.manager) {
            state.manager = GlobalSystemMediaTransportControlsSessionManager::RequestAsync().get();
        }

        auto session = state.manager.GetCurrentSession();
        if (!session) return false;

        // Try*Async 在 MTA 线程上以 .get() 等待完成；调用方为 SMTC 轮询线程，
        // 与 poll() 共用互斥锁串行执行，不会交叉访问会话。
        switch (command) {
            case Command::Play:
                session.TryPlayAsync().get();
                break;
            case Command::Pause:
                session.TryPauseAsync().get();
                break;
            case Command::Next:
                session.TrySkipNextAsync().get();
                break;
            case Command::Previous:
                session.TrySkipPreviousAsync().get();
                break;
            case Command::Stop:
                session.TryStopAsync().get();
                break;
            case Command::Seek:
                // 播放位置以 100ns 为单位传递。
                session.TryChangePlaybackPositionAsync(position_ms * 10000).get();
                break;
            default:
                return false;
        }
        return true;
    } catch (const winrt::hresult_error&) {
        return false;
    } catch (...) {
        return false;
    }
}

} // namespace epsilon::smtc
