package com.augmate.scany;


import android.hardware.Camera;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import java.util.Iterator;
import java.util.List;

public class ScannerTesting {

    private static final String TAG = ScannerTesting.class.getName();
    byte[] binaryByteMatrix = new byte[0];
    int[] binaryIntMatrix = new int[0];

    public void test(byte[] bytes, int width, int height) {
        if (binaryIntMatrix.length < width * height) {
            binaryIntMatrix = new int[width * height];
            Log.d(TAG, "growing binary int-matrix buffer");
        }

        if(binaryByteMatrix.length < width * height) {
            binaryByteMatrix = new byte[width * height];
            Log.d(TAG, "growing binary byte-matrix buffer");
        }

        long start;
        long span;

        start = SystemClock.elapsedRealtime();
        NativeUtils.binarize(bytes, binaryByteMatrix, width, height);
        span = SystemClock.elapsedRealtime() - start;
        Log.d(TAG, "Native grayscale convert took " + span + " msec");

        start = SystemClock.elapsedRealtime();
        //decodeYUV420SPtoGrayscale(binaryIntMatrix, bytes, previewFrameWidth, previewFrameHeight);
        for (int i = 0; i < width * height; i++)
            binaryIntMatrix[i] = 0xFF000000 | (binaryByteMatrix[i] << 8) & 0x00ff0000;
        span = SystemClock.elapsedRealtime() - start;
        Log.d(TAG, "Java grayscale convert took   " + span + " msec");

        //Bitmap bmp = Bitmap.createBitmap(binaryIntMatrix, previewFrameWidth, previewFrameHeight, Bitmap.Config.ARGB_8888);
        //mDebugImg.setImageBitmap(bmp);
    }

    // http://stackoverflow.com/questions/5272388/extract-black-and-white-image-from-android-cameras-nv21-format/12702836#12702836
    static public void decodeYUV420SPtoGrayscale(int[] argb, byte[] yuv420sp, int width, int height) {

        for (int i = 0; i < width * height; i++) {
            int luminance = yuv420sp[i] & 0xFF;
            argb[i] = 0xff000000 | luminance << 8;
        }
    }

    public static void dumpCameraParameters(Camera camera) {
        Camera.Parameters params = camera.getParameters();

        Log.d(TAG, "Dumping camera parameters:");

        List<Integer> supportedPreviewFormats = params.getSupportedPreviewFormats();
        Iterator<Integer> supportedPreviewFormatsIterator = supportedPreviewFormats.iterator();
        while(supportedPreviewFormatsIterator.hasNext()){
            Integer previewFormat =supportedPreviewFormatsIterator.next();
            // 16 ~ NV16 ~ YCbCr
            // 17 ~ NV21 ~ YCbCr ~ DEFAULT
            // 4  ~ RGB_565
            // 256~ JPEG
            // 20 ~ YUY2 ~ YcbCr ...
            // 842094169 ~ YV12 ~ 4:2:0 YCrCb comprised of WXH Y plane, W/2xH/2 Cr & Cb. see documentation
            Log.v(TAG, "Supported preview format:" + previewFormat);
        }

        Log.d(TAG, "  supported focus modes: " + TextUtils.join(",", params.getSupportedFocusModes()));

        Log.d(TAG, "  auto-exposure lock supported: " + params.isAutoExposureLockSupported());
        Log.d(TAG, "  white-balance lock supported: " + params.isAutoWhiteBalanceLockSupported());
        Log.d(TAG, "  zoom supported: " + params.isZoomSupported());
        Log.d(TAG, "  smooth zoom supported: " + params.isSmoothZoomSupported());
        Log.d(TAG, "  video stabilization supported: " + params.isVideoStabilizationSupported());

        Log.d(TAG, "  white-balance: " + params.getWhiteBalance());
        Log.d(TAG, "  exposure-compensation: " + params.getExposureCompensation());
        Log.d(TAG, "  exposure-compensation step: " + params.getExposureCompensationStep());
        Log.d(TAG, "  exposure-compensation min: " + params.getMinExposureCompensation());
        Log.d(TAG, "  exposure-compensation max: " + params.getMaxExposureCompensation());
        Log.d(TAG, "  focal length: " + params.getFocalLength());
        Log.d(TAG, "  fov: " + params.getHorizontalViewAngle());
        Log.d(TAG, "  focus mode: " + params.getFocusMode());
        Log.d(TAG, "  flash mode: " + params.getFlashMode());
        Log.d(TAG, "  auto-exposure lock: " + params.getAutoExposureLock());
        Log.d(TAG, "  auto-white-balance lock: " + params.getAutoWhiteBalanceLock());

        Log.d(TAG, "  zoom ratios: " + (params.getZoomRatios() == null ? "N/A" : TextUtils.join(",", params.getZoomRatios())));
        Log.d(TAG, "  zoom max: " + params.getMaxZoom());
        //Log.d(TAG, "  preferred preview size: " +  params.getPreferredPreviewSizeForVideo().width + " x " + params.getPreferredPreviewSizeForVideo().height);
        Log.d(TAG, "  old preview size: " + params.getPreviewSize().width + " x " + params.getPreviewSize().height);
        Log.d(TAG, "  preview format: " + params.getPreviewFormat());

        List<int[]> supportedPreviewFpsRanges = params.getSupportedPreviewFpsRange();
        int[] minimumPreviewFpsRange = supportedPreviewFpsRanges.get(0);
        Log.d(TAG, "  preview fps range: " + minimumPreviewFpsRange[0] + " - " + minimumPreviewFpsRange[1]);

        Log.d(TAG, "  preferred preview size for video: " + params.getPreferredPreviewSizeForVideo());
        Log.d(TAG, "  supported video sizes: " + params.getSupportedVideoSizes());

        Log.d(TAG, "-------------------------------------------------");
    }
}
