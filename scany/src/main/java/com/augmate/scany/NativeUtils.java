package com.augmate.scany;

public class NativeUtils {
    public static native void binarize(byte[] src, byte[] dst, int width, int height);
    public static native void binarizeToIntBuffer(byte[] src, int[] dst, int width, int height);

    static {
        System.loadLibrary("native-scanner");
    }
}
