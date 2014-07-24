// based heavily on ZXing's capture activity
// still using their own color-space conversion, binarizer, and qr-code detector and parser
// should try a c++ qr-code detector

package com.augmate.scany;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import com.google.zxing.*;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.detector.Detector;

final class ScanThreadHandler extends Handler {

    private static final String TAG = "DecodeHandler";

    private final ScanActivity mScanActivity;
    QRCodeReader mReaderQRCode = new QRCodeReader();
    long mLastFrame = 0;
    private boolean mIsRunning = true;

    ScanThreadHandler(ScanActivity activity) {
        this.mScanActivity = activity;
        mLastFrame = SystemClock.elapsedRealtime();
    }

    private static byte luminance_fast(int width, byte[] imgData, int y, int x) {
        int pos = (y * width + x) * 3;

        int r = imgData[pos + 0] & 0xff;
        int g = imgData[pos + 1] & 0xff;
        int b = imgData[pos + 2] & 0xff;

        return (byte) (((r << 1) + (g << 2) + g + b) >> 3);
    }

    private static double luminance(int width, byte[] imgData, int y, int x) {
        int pos = (y * width + x) * 3;

        int r = imgData[pos + 0] & 0xff;
        int g = imgData[pos + 1] & 0xff;
        int b = imgData[pos + 2] & 0xff;

        return luminance(r, g, b);
    }

    /**
     * Returns a value 0-1
     *
     * @param r [0-255]
     * @param g [0-255]
     * @param b [0-255]
     * @return double [0-1]
     */
    private static double luminance(double r, double g, double b) {
        return (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;
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
                detect((byte[]) message.obj, message.arg1, message.arg2);
                break;

            case R.id.quit:
                // told to quit
                Log.d(TAG, "Got request to quit looper.");
                mIsRunning = false;
                Looper.myLooper().quit();
                break;
        }
    }

    private final int fixed_buffer_size = 640 * 360;
    private final byte[] binaryMatrix = new byte[fixed_buffer_size];

    private void detect(byte[] data, int width, int height) {

        long start, end;

        start = SystemClock.elapsedRealtime();
        long time_since_last_frame = start - mLastFrame;
        mLastFrame = start;

//        for (int i = 0; i < fixed_buffer_size; i++) {
//            binaryIntMatrix[i] = (byte) (((data[i] & 0xff) < 70) ? 0 : 1);
//        }

        NativeUtils.binarize(data, binaryMatrix, 640, 360);

        long conversion = SystemClock.elapsedRealtime();

        Result rawResult = null;

        try {
            PatternFinder finder = new PatternFinder(binaryMatrix, width, height);
            FinderPatternInfo info = finder.find(null);

            rawResult = new Result("Detect Only", null, new ResultPoint[]{info.getBottomLeft(), info.getTopLeft(), info.getTopRight()}, BarcodeFormat.QR_CODE);

            Log.d(TAG, "Got info: " + rawResult);

        } catch (NotFoundException err) {
            // expected failure case
        } catch (Exception err) {
            Log.w(TAG, "QR detector threw: " + err);
        }

        end = SystemClock.elapsedRealtime();

        Handler targetHandler = mScanActivity.getHandler();

        if (rawResult != null) {
            Log.i(TAG, "QR-decode succeeded (!) in " + (end - start) + " ms (last frame = " + time_since_last_frame + " ms) (conversion = " + (conversion - start) + " ms)");

            // tell scan-activity we got a decoded image
            if (targetHandler != null) {
                Message msg = Message.obtain(targetHandler, R.id.decode_succeeded, rawResult);
                msg.sendToTarget();
            }
        } else {
            // nothing decoded from image, request another
            Log.d(TAG, "QR-decode failed in " + (end - start) + " ms (conversion = " + (conversion - start) + " ms)");

            if (targetHandler != null) {
                Message msg = Message.obtain(targetHandler, R.id.decode_failed);
                msg.sendToTarget();
            }
        }
    }

    private void decode(byte[] data, int width, int height) {

        long start, end;

        start = SystemClock.elapsedRealtime();
        long time_since_last_frame = start - mLastFrame;
        mLastFrame = start;

        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        long conversion = SystemClock.elapsedRealtime();

        //Result rawResult = null;
        DetectorResult detectorResult = null;

        try {
            //rawResult = mReaderQRCode.decode(bitmap);
            detectorResult = new Detector(bitmap.getBlackMatrix()).detect(null);
            //Log.d(TAG, "detectorResult = num points = " + detectorResult.getPoints().length);
        } catch (Exception re) {
            // tag not found. shouldn't throw, but whatever.
            // Log.w(TAG, "QR Code reader faulted: " + re.toString());
        } finally {
            mReaderQRCode.reset();
        }

        end = SystemClock.elapsedRealtime();

        Handler targetHandler = mScanActivity.getHandler();

        if (detectorResult != null) {
            Log.i(TAG, "QR-decode succeeded (!) in " + (end - start) + " ms (last frame = " + time_since_last_frame + " ms) (conversion = " + (conversion - start) + " ms)");

            // tell scan-activity we got a decoded image
            if (targetHandler != null) {
                Message msg = Message.obtain(targetHandler, R.id.decode_succeeded, detectorResult);
                msg.sendToTarget();
            }
        } else {
            // nothing decoded from image, request another
            //Log.d(TAG, "QR-decode failed in " + (end - start) + " ms (last frame = " + time_since_last_frame + " ms)");

            if (targetHandler != null) {
                Message msg = Message.obtain(targetHandler, R.id.decode_failed);
                msg.sendToTarget();
            }
        }
    }

}
