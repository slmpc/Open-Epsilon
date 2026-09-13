# Epsilon SMTC bridge

该目录包含 Windows x64 的 C++/WinRT SMTC 读取核心、JNI 桥接和独立探针。桥接返回当前媒体会话的标题、艺术家、专辑、来源应用、播放状态、时间线（位置/时长/采样时刻，播放器不上报时间线时恒为 0，由 Java 侧回退为无进度条展示）、播放控制能力位与变更后的封面字节，并提供 play/pause/next/previous/stop/seek 控制命令。播放位置的外推（本地时钟插值）在 Java 侧 `SmtcSnapshot#estimatedPositionMs` 完成。

## Build

```powershell
$env:JAVA_HOME = "C:/Program Files/Java/jdk-25.0.4"
cmake -S native/smtc -B native/smtc/build -A x64 -DJAVA_HOME="$env:JAVA_HOME"
cmake --build native/smtc/build --config Release --parallel
```

生成文件：

- `common/src/main/resources/natives/windows-x86_64/epsilon_smtc.dll`
- `native/smtc/build/Release/epsilon_smtc_probe.exe`

运行 `epsilon_smtc_probe.exe` 可以直接检查当前 Windows SMTC 会话及封面字节数。
