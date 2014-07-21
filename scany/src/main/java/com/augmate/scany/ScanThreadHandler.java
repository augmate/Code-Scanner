// based heavily on ZXing's capture activity
// still using their own color-space conversion, binarizer, and qr-code detector and parser
// should try a c++ qr-code detector

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
			Log.i(TAG, "QR-decode succeeded (!) in " + (end - start) + " ms (last frame = " + time_since_last_frame + " ms) (conversion = " + (conversion - start) + " ms)");

			// tell scan-activity we got a decoded image
			if(targetHandler != null) {
				Message msg = Message.obtain(targetHandler, R.id.decode_succeeded, rawResult);
				msg.sendToTarget();
			}
		}
		else
		{
			// nothing decoded from image, request another
			Log.d(TAG, "QR-decode failed in " + (end - start) + " ms (last frame = "+time_since_last_frame+" ms)");
			
			if(targetHandler != null) {
				Message msg = Message.obtain(targetHandler, R.id.decode_failed);
				msg.sendToTarget();
			}
		}
	}

}
