package com.augmate.scany;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CountDownLatch;

final class ScanThread extends Thread {

    public static final String TAG = ScanThread.class.getName();

    private final ScanActivity activity;
    private final CountDownLatch handlerInitLatch;
    private Handler handler;

    ScanThread(ScanActivity activity) {
        this.activity = activity;
        handlerInitLatch = new CountDownLatch(1);
    }

    // blocks and waits on thread to enter
    Handler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
        return handler;
    }

    // thread entry-point
    @Override
    public void run() {
        Looper.prepare();
        handler = new ScanThreadHandler(activity);
        handlerInitLatch.countDown();
        Looper.loop();
    }

}
