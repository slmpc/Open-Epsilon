package dev.maru.verify.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MultiplexIRCHandler implements IRCHandler {
    private final CopyOnWriteArrayList<IRCHandler> handlers = new CopyOnWriteArrayList<>();

    public void add(IRCHandler handler) {
        if (handler == null) {
            return;
        }
        handlers.remove(handler);
        handlers.add(handler);
    }

    public void remove(IRCHandler handler) {
        handlers.remove(handler);
    }

    public List<IRCHandler> snapshot() {
        return List.copyOf(handlers);
    }

    @Override
    public void onMessage(String sender, String message) {
        for (IRCHandler h : handlers) {
            try {
                h.onMessage(sender, message);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onDisconnected(String message) {
        for (IRCHandler h : handlers) {
            try {
                h.onDisconnected(message);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onConnected() {
        for (IRCHandler h : handlers) {
            try {
                h.onConnected();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public String getInGameUsername() {
        for (IRCHandler h : handlers) {
            try {
                String v = h.getInGameUsername();
                if (v != null && !v.isBlank()) {
                    return v;
                }
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    @Override
    public void onLoginResult(boolean success, long expireAt, long timeWindow, String message) {
        for (IRCHandler h : handlers) {
            try {
                h.onLoginResult(success, expireAt, timeWindow, message);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onRegisterResult(boolean success, long expireAt, long timeWindow, String message) {
        for (IRCHandler h : handlers) {
            try {
                h.onRegisterResult(success, expireAt, timeWindow, message);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onRechargeResult(boolean success, long expireAt, long timeWindow, String message) {
        for (IRCHandler h : handlers) {
            try {
                h.onRechargeResult(success, expireAt, timeWindow, message);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onCloudConfigUploadResult(boolean success, String message, int max) {
        for (IRCHandler h : handlers) {
            try {
                h.onCloudConfigUploadResult(success, message, max);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onCloudConfigGetResult(boolean success, String owner, String name, String content, String message) {
        for (IRCHandler h : handlers) {
            try {
                h.onCloudConfigGetResult(success, owner, name, content, message);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onCloudConfigListResult(boolean success, List<String> names, int max, String message) {
        for (IRCHandler h : handlers) {
            try {
                h.onCloudConfigListResult(success, names, max, message);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onCloudConfigDeleteResult(boolean success, String owner, String name, String message) {
        for (IRCHandler h : handlers) {
            try {
                h.onCloudConfigDeleteResult(success, owner, name, message);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onModDownload(String content, String hash) {
        for (IRCHandler h : handlers) {
            try {
                h.onModDownload(content, hash);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onModListResult(List<String> names, List<String> versions) {
        for (IRCHandler h : handlers) {
            try {
                h.onModListResult(names, versions);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onAssetInfo(boolean exists, String hash, long size) {
        for (IRCHandler h : handlers) {
            try {
                h.onAssetInfo(exists, hash, size);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onAssetChunk(byte[] data, long offset, boolean last) {
        for (IRCHandler h : handlers) {
            try {
                h.onAssetChunk(data, offset, last);
            } catch (Exception ignored) {
            }
        }
    }
}

