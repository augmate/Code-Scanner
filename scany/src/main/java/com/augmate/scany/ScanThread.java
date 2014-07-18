package com.augmate.scany;

import java.util.concurrent.CountDownLatch;

import android.os.Handler;
import android.os.Looper;

final class ScanThread extends Thread {

	public static final String TAG = "DecodeThread";

	private final ScanActivity activity;
	private Handler handler;
	private final CountDownLatch handlerInitLatch;

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
