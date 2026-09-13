#include "smtc_core.hpp"

#include <iostream>

#include <windows.h>
#include <winrt/base.h>

int main() {
    SetConsoleOutputCP(CP_UTF8);
    const auto snapshot = epsilon::smtc::poll();

    auto utf8 = [](const std::wstring& value) {
        return winrt::to_string(winrt::hstring(value));
    };

    std::cout << "available=" << (snapshot.available ? "true" : "false") << '\n'
              << "title=" << utf8(snapshot.title) << '\n'
              << "artist=" << utf8(snapshot.artist) << '\n'
              << "album=" << utf8(snapshot.album_title) << '\n'
              << "source=" << utf8(snapshot.source_app_id) << '\n'
              << "playback_status=" << snapshot.playback_status << '\n'
              << "position_ms=" << snapshot.position_ms << '\n'
              << "duration_ms=" << snapshot.duration_ms << '\n'
              << "position_updated_at_ms=" << snapshot.position_updated_at_ms << '\n'
              << "controls=0x" << std::hex << snapshot.controls << std::dec << '\n'
              << "thumbnail_revision=" << snapshot.thumbnail_revision << '\n'
              << "thumbnail_bytes="
              << (snapshot.thumbnail.has_value() ? snapshot.thumbnail->size() : 0) << '\n';

    if (!snapshot.error.empty()) {
        std::cerr << "error=" << utf8(snapshot.error) << '\n';
        return 1;
    }
    return 0;
}
