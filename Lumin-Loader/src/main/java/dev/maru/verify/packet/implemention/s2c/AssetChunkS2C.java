package dev.maru.verify.packet.implemention.s2c;

import dev.maru.verify.packet.IRCPacket;
import dev.maru.verify.packet.annotations.ProtocolField;

public class AssetChunkS2C implements IRCPacket {
    @ProtocolField("data")
    private byte[] data;

    @ProtocolField("offset")
    private long offset;

    @ProtocolField("last")
    private boolean last;

    public AssetChunkS2C() {
    }

    public AssetChunkS2C(byte[] data, long offset, boolean last) {
        this.data = data;
        this.offset = offset;
        this.last = last;
    }

    public byte[] getData() {
        return data;
    }

    public long getOffset() {
        return offset;
    }

    public boolean isLast() {
        return last;
    }
}
