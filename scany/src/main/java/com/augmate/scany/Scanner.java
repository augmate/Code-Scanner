package com.augmate.scany;

import android.os.SystemClock;
import android.util.Log;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

/**
 * Scanner performs no message passing
 * it doesn't care who gives it data or who takes it afterwards
 * it must be wrapped by someone that is aware of threads, activities, and messages
 */
public class Scanner {
    private static final String TAG = Scanner.class.getName();
    private byte[] binaryMatrix = new byte[0];
    private long lastFrameRecieved = 0;

    /**
     * The data buffer must not be overwritten while it is being processed
     */
    public void detectQrCodeOnly(ScannerDecodeJob job) {

        byte[] data = job.getBytes();
        int width = job.getWidth();
        int height = job.getHeight();

        if(binaryMatrix.length < width * height)
            binaryMatrix = new byte[width * height];

        if (lastFrameRecieved == 0)
            lastFrameRecieved = SystemClock.elapsedRealtime();

        long start = SystemClock.elapsedRealtime();
        long timeSinceLastFrame = start - lastFrameRecieved;
        lastFrameRecieved = start;

        NativeUtils.binarize(data, binaryMatrix, 640, 360);

        long binarizeTime = SystemClock.elapsedRealtime();

        Result result = null;

        try {
            PatternFinder finder = new PatternFinder(binaryMatrix, width, height);
            FinderPatternInfo info = finder.find(null);

            result = new Result("Detect Only", null, new ResultPoint[]{info.getBottomLeft(), info.getTopLeft(), info.getTopRight()}, BarcodeFormat.QR_CODE);

        } catch (NotFoundException err) {
            // expected failure case when zxing-lib doesn't locate a qr-code. don't log.
        } catch (Exception err) {
            // unexpected failure
            Log.w(TAG, "QR detector threw: " + err);
        }

        job.result = result;
        job.decodedAt = SystemClock.elapsedRealtime();

        long totalTime = job.decodedAt - start;

        if (result != null) {
            Log.i(TAG, "QR-decode success in " + totalTime + " ms (request->decode = " + (job.decodedAt - job.requestedAt) + " ms) (last frame = " + timeSinceLastFrame + " ms) (binarization = " + (binarizeTime - start) + " ms)");
        } else {
            Log.d(TAG, "QR-decode failure in " + totalTime + " ms (last frame = " + timeSinceLastFrame + " ms) (binarization = " + (binarizeTime - start) + " ms)");
        }
    }
}
