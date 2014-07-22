package com.augmate.scany;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CountDownLatch;

final class ScanThread extends Thread {

    public static final String TAG = "DecodeThread";

    private final ScanActivity activity;
    private final CountDownLatch handlerInitLatch;
    private Handler handler;

    ScanThread(ScanActivity activity) {
        this.activity = activity;
        handlerInitLatch = new CountDownLatch(1);
    }

    Handler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
        return handler;
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new ScanThreadHandler(activity);
        handlerInitLatch.countDown();
        Looper.loop();
    }

}
