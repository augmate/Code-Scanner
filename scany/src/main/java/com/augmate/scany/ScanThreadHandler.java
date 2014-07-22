// based heavily on ZXing's capture activity
// still using their own color-space conversion, binarizer, and qr-code detector and parser
// should try a c++ qr-code detector

package com.augmate.scany;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

final class ScanThreadHandler extends Handler {

    private static final String TAG = "DecodeHandler";

    private final ScanActivity mScanActivity;
    QRCodeReader mReaderQRCode = new QRCodeReader();
    long mLastFrame = 0;
    byte[] binaryMatrix = new byte[0];
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

    private void detect(byte[] data, int width, int height) {

        long start, end;

        start = SystemClock.elapsedRealtime();
        long time_since_last_frame = start - mLastFrame;
        mLastFrame = start;

        if (binaryMatrix.length < width * height)
            binaryMatrix = new byte[width * height];

//        // calculating even one row of luminance seems extremely painful
//        double avgLuminance = 0;
//        for(int i = 0; i < width; i ++) {
//            avgLuminance += luminance(width, data, 200, i);
//        }
//        avgLuminance /= (double) width;
//
//        // scale from 0-255
//        Log.d(TAG, String.format("avg luminance: %.3f", avgLuminance));

        // for each scanline
//        for (int y = 0; y < height; y++) {
//            // for each pixel
//            for (int x = 0; x < width; x++) {
//                //double luminance = luminance(width, data, y, x);
//                //binaryMatrix[y * width + x] = (byte) (luminance > 128 ? 1 : 0);
//
//                byte luminance = data[(y * width + x) * 3];
//                binaryMatrix[y * width + x] = (byte) (luminance > 70 ? 1 : 0);
//            }
//        }

        for (int i = 0; i < width * height; i++) {
            binaryMatrix[i] = (byte) (data[i * 3] < 60 ? 0 : 1);
        }

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
            if (targetHandler != null) {
                Message msg = Message.obtain(targetHandler, R.id.decode_succeeded, rawResult);
                msg.sendToTarget();
            }
        } else {
            // nothing decoded from image, request another
            Log.d(TAG, "QR-decode failed in " + (end - start) + " ms (last frame = " + time_since_last_frame + " ms)");

            if (targetHandler != null) {
                Message msg = Message.obtain(targetHandler, R.id.decode_failed);
                msg.sendToTarget();
            }
        }
    }

}
