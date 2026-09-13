package me.sofurry.smtc;

import com.github.epsilon.Constants;

import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class SmtcService {

    public static final SmtcService INSTANCE = new SmtcService();

    private final AtomicReference<SmtcSnapshot> snapshot = new AtomicReference<>(SmtcSnapshot.UNAVAILABLE);
    private final AtomicLong generation = new AtomicLong();

    private ScheduledExecutorService executor;
    private String lastError = "";

    private SmtcService() {
    }

    public synchronized void start() {
        if (executor != null || !SmtcNativeBridge.isAvailable()) return;

        long activeGeneration = generation.incrementAndGet();
        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "Epsilon-SMTC");
            thread.setDaemon(true);
            return thread;
        });
        executor.execute(SmtcNativeBridge::reset);
        executor.scheduleWithFixedDelay(() -> poll(activeGeneration), 0L, 750L, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        generation.incrementAndGet();
        ScheduledExecutorService current = executor;
        executor = null;
        snapshot.set(SmtcSnapshot.UNAVAILABLE);
        lastError = "";
        if (current != null) current.shutdownNow();
    }

    public SmtcSnapshot snapshot() {
        return snapshot.get();
    }

    /** 向当前媒体会话发送“继续播放”。未启动时忽略。 */
    public void play() {
        sendCommand(SmtcNativeBridge.COMMAND_PLAY, 0L);
    }

    /** 向当前媒体会话发送“暂停”。未启动时忽略。 */
    public void pause() {
        sendCommand(SmtcNativeBridge.COMMAND_PAUSE, 0L);
    }

    /** 向当前媒体会话发送“下一曲”。未启动时忽略。 */
    public void next() {
        sendCommand(SmtcNativeBridge.COMMAND_NEXT, 0L);
    }

    /** 向当前媒体会话发送“上一曲”。未启动时忽略。 */
    public void previous() {
        sendCommand(SmtcNativeBridge.COMMAND_PREVIOUS, 0L);
    }

    /** 向当前媒体会话发送“停止”。未启动时忽略。 */
    public void stopPlayback() {
        sendCommand(SmtcNativeBridge.COMMAND_STOP, 0L);
    }

    /** 向当前媒体会话请求跳转到指定播放位置（毫秒）。未启动时忽略。 */
    public void seek(long positionMs) {
        sendCommand(SmtcNativeBridge.COMMAND_SEEK, positionMs);
    }

    /**
     * 控制命令投递到 SMTC 轮询线程串行执行：原生侧 poll 与 send_command 共用互斥锁，
     * 避免在调用方线程阻塞等待 WinRT 异步完成。
     */
    private synchronized void sendCommand(int command, long positionMs) {
        ScheduledExecutorService current = executor;
        if (current == null) return;

        try {
            current.execute(() -> {
                try {
                    SmtcNativeBridge.sendCommand(command, positionMs);
                } catch (Throwable e) {
                    Constants.LOGGER.warn("Windows SMTC command {} failed", command, e);
                }
            });
        } catch (RejectedExecutionException e) {
            // stop() 与控制命令并发时的正常竞态，忽略即可。
        }
    }

    private void poll(long activeGeneration) {
        if (generation.get() != activeGeneration) return;

        try {
            SmtcNativeResult result = SmtcNativeBridge.poll();
            if (result == null) return;

            synchronized (this) {
                if (generation.get() != activeGeneration) return;

                String error = result.error() == null ? "" : result.error();
                if (!error.isBlank() && !error.equals(lastError)) {
                    Constants.LOGGER.warn("Windows SMTC query failed: {}", error);
                }
                lastError = error;
                snapshot.updateAndGet(previous -> SmtcSnapshot.merge(previous, result));
            }
        } catch (Throwable e) {
            synchronized (this) {
                if (generation.get() != activeGeneration) return;

                String message = e.getClass().getName() + ": " + e.getMessage();
                if (!message.equals(lastError)) {
                    Constants.LOGGER.warn("Windows SMTC polling stopped by a native bridge error", e);
                    lastError = message;
                }
                snapshot.set(SmtcSnapshot.UNAVAILABLE);
            }
        }
    }

}
