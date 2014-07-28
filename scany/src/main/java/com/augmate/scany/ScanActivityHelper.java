// based heavily on ZXing's capture activity
// this just passes messages between dedicated decoding thread and the UI thread

package com.augmate.scany;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.google.zxing.Result;

/**
 * Spawn decoding thread. Handle all communication between calling activity and threaded-decoder via messages.
 */
public final class ScanActivityHelper extends Handler {

    private static final String TAG = ScanActivityHelper.class.getName();

    private final ScanActivity scanActivity;
    private final ScanThread decodingThread;

    ScanActivityHelper(ScanActivity activity) {
        this.scanActivity = activity;
        this.decodingThread = new ScanThread(activity);
        this.decodingThread.start();
    }

    public void startDecode(ScannerDecodeJob scannerDecodeJob) {
        Message
                .obtain(decodingThread.getHandler(), R.id.decode, scannerDecodeJob)
                .sendToTarget();
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case R.id.decode_succeeded:
            case R.id.decode_failed:
                // received message from decoding-thread. we are in main-thread so pass it to the scan-activity.
                scanActivity.onQRCodeDecoded((ScannerDecodeJob) message.obj);
                break;
        }
    }

    public void quitSynchronously() {
        Log.i(TAG, "Syncing and quitting..");

        // ask decoding thread to exit
        Message.obtain(decodingThread.getHandler(), R.id.quit).sendToTarget();

        // wait for up to two seconds for decoding thread to exit
        try {
            decodingThread.join(5000L);
        } catch (InterruptedException e) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(R.id.decode_succeeded);
        removeMessages(R.id.decode_failed);
    }
}
