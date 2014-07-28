package com.augmate.scany;

import android.os.SystemClock;
import com.google.zxing.Result;

public class ScannerDecodeJob {
    private final int width;
    private final int height;
    private final byte[] bytes;
    public final long requestedAt;

    // filled out by decoder
    public long decodedAt;
    public Result result;

    public ScannerDecodeJob(int width, int height, byte[] bytes) {
        this.width = width;
        this.height = height;
        this.bytes = bytes;
        this.requestedAt = SystemClock.elapsedRealtime();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
