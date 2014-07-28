// based heavily on ZXing's capture activity
// still using their own color-space conversion, binarizer, and qr-code detector and parser
// should try a c++ qr-code detector

package com.augmate.scany;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

final class ScanThreadHandler extends Handler {
    private static final String TAG = ScanThreadHandler.class.getName();

    private ScanActivity scanActivity;
    private Scanner scanner = new Scanner();
    private boolean processingRequests = true;

    ScanThreadHandler(ScanActivity activity) {
        this.scanActivity = activity;
    }

    @Override
    public void handleMessage(Message message) {

        if (!processingRequests) {
            Log.d(TAG, "No longer processing requests, ignoring message.");
            return;
        }
        switch (message.what) {
            case R.id.decode:
                // got a decode request from another thread
                ScannerDecodeJob job = (ScannerDecodeJob) message.obj;
                processDecodeRequest(job);
                break;

            case R.id.quit:
                // asked to quit
                Log.d(TAG, "Got request to quit looper.");
                processingRequests = false;
                Looper.myLooper().quit();
                break;
        }
    }

    private void processDecodeRequest(ScannerDecodeJob job) {

        // updates job with results
        scanner.detectQrCodeOnly(job);

        if(!processingRequests) {
            Log.d(TAG, "Got decoding results, but no longer processing requests. Ignoring.");
            return;
        }

        Log.d(TAG, "Got decoding result to broadcast: " + job.result);

        Handler target = scanActivity.getHandler();

        if (target == null) {
            Log.w(TAG, "Got decoding result but no target-handler to send it to.");
            return;
        }

        // TODO: simplify this, always return results regardless of status
        if (job.result != null) {
            // tell calling activity we have decoded the image and got a possibly-meaningful result
            Message
                    .obtain(target, R.id.decode_succeeded, job)
                    .sendToTarget();
        } else {
            // failed to decode image, got no results
            Message
                    .obtain(target, R.id.decode_failed, job)
                    .sendToTarget();
        }
    }
}
