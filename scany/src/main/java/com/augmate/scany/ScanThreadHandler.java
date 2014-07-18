/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.augmate.scany;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

final class ScanThreadHandler extends Handler {

	private static final String TAG = "DecodeHandler";

	private final ScanActivity mScanActivity;
	private boolean mIsRunning = true;

	QRCodeReader mReaderQRCode = new QRCodeReader();

	ScanThreadHandler(ScanActivity activity) {
		this.mScanActivity = activity;
		mLastFrame = SystemClock.elapsedRealtime();
	}

	@Override
	public void handleMessage(Message message) {
		if (!mIsRunning) {
			return;
		}
		switch (message.what) {
		case R.id.decode:
			// got a decode request from another thread
			//Log.d(TAG, "Got request to decode.");
			decode((byte[]) message.obj, message.arg1, message.arg2);
			break;
			
		case R.id.quit:
			// told to quit
			Log.d(TAG, "Got request to quit looper.");
			mIsRunning = false;
			Looper.myLooper().quit();
			break;
		}
	}

	long mLastFrame = 0;

	private void decode(byte[] data, int width, int height) {

		long start, end;

		start = SystemClock.elapsedRealtime();
		long time_since_last_frame = start - mLastFrame;
		mLastFrame = start;

		PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        
        long conversion = SystemClock.elapsedRealtime();

		Result rawResult = null;

		try {
			rawResult = mReaderQRCode.decode(bitmap);
		} catch (Exception re) {
			// tag not found. shouldn't throw, but whatever.
			// Log.w(TAG, "QR Code reader faulted: " + re.toString());
		} finally {
			mReaderQRCode.reset();
		}

		end = SystemClock.elapsedRealtime();
		
		Handler targetHandler = mScanActivity.getHandler();

		if (rawResult != null) {
			Log.i(TAG, "QR-decode took " + (end - start) + " ms (time since last frame = " + time_since_last_frame + " ms) (conversion = " + (conversion - start) + " ms)");

			// tell scan-activity we got a decoded image
			if(targetHandler != null) {
				Message msg = Message.obtain(targetHandler, R.id.decode_succeeded, rawResult);
				msg.sendToTarget();
			}
		}
		else
		{
			// nothing decoded from image, request another
			//Log.d(TAG, "Spent " + (end - start) + " ms and found nothing  (time since last frame = "+time_since_last_frame+" ms)");
			
			if(targetHandler != null) {
				Message msg = Message.obtain(targetHandler, R.id.decode_failed);
				msg.sendToTarget();
			}
		}
	}

}
