/*
 * Copyright (C) 2008 ZXing authors
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
import android.os.Message;
import android.util.Log;

import com.google.zxing.Result;

// activity-handler spawns the decoding-thread
// and handles communication with it
public final class ScanActivityHandler extends Handler {

	private static final String TAG = "ActivityHandler";

	private final ScanActivity mScanActivity;
	private final ScanThread mDecodingThread;
	private State state;

	private enum State {
		PREVIEW, SUCCESS, DONE
	}

	ScanActivityHandler(ScanActivity activity)
	{
		this.mScanActivity = activity;
		mDecodingThread = new ScanThread(activity);
		mDecodingThread.start();
		state = State.SUCCESS;
	}

	public void submitDecodeJob(int width, int height, byte[] bytes)
	{
		//Log.d(TAG, "Submitting decode job");
		
		Message msg = Message.obtain(mDecodingThread.getHandler(), R.id.decode, width, height, bytes);
    	msg.sendToTarget();
	}
	
	@Override
	public void handleMessage(Message message) {
		switch (message.what) {

		case R.id.decode_succeeded:
			//Log.i(TAG, "Decode succeeded");
			
			// got message from decoding thread, pass it to the scan-activity
			mScanActivity.onQRCodeDecoded((Result) message.obj);
			break;
			
		case R.id.decode_failed:
			// queue up another frame for decoding
			mScanActivity.requestFrame();
			
			break;

		}
	}

	public void quitSynchronously() {
		Log.i(TAG, "Syncing and quitting..");
		
		Message quit = Message.obtain(mDecodingThread.getHandler(), R.id.quit);
		quit.sendToTarget();
		
		try {
			// Wait at most half a second; should be enough time, and onPause()
			// will timeout quickly
			mDecodingThread.join(500L);
		} catch (InterruptedException e) {
			// continue
		}

		// Be absolutely sure we don't send any queued up messages
		removeMessages(R.id.decode_succeeded);
		removeMessages(R.id.decode_failed);
	}

}
