package com.augmate.scany;

import android.app.Activity;
import android.graphics.*;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.zxing.ResultPoint;

import java.io.IOException;

public class ScanActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback, MediaPlayer.OnCompletionListener {
    private static final String TAG = ScanActivity.class.getName();

    Camera mCamera;
    Boolean mHasSurface = false;
    byte[] previewBufferOne;
    byte[] previewBufferTwo;

    ScanActivityHelper scannerHandler;
    DebugViz mDebugViz;

    // start of SurfaceHolder.Callback
    Boolean mBeepLoaded = false;
    int mBeepSoundId;
    SoundPool mSoundPool;

    // end of SurfaceHolder.Callback
    int previewFrameWidth = 640;
    int previewFrameHeight = 360;
    float visualizationScaleX = 1;
    float visualizationScaleY = 1;
    Boolean mPaused = false;
    Boolean mReadyForMore = true;
    int mDetectCount = 0;
    private Point debugCanvasSize;
    private SurfaceView surfaceView;

    public Handler getHandler() {
        return scannerHandler;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!mHasSurface) {
            mHasSurface = true;
            Log.d(TAG, "Got preview surface");
            initCamera(holder);
            scannerHandler = new ScanActivityHelper(this);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    ImageView mDebugImg;
    Point renderingSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mDebugImg = (ImageView) findViewById(R.id.debug_view_img);
        mDebugViz = (DebugViz) findViewById(R.id.debug_view);
        surfaceView = (SurfaceView) findViewById(R.id.preview_view);

        Display display = getWindowManager().getDefaultDisplay();
        debugCanvasSize = new Point();
        display.getSize(debugCanvasSize);

        ViewGroup.LayoutParams layoutParams = mDebugViz.getLayoutParams();
        layoutParams.width = debugCanvasSize.x;
        layoutParams.height = (int) (layoutParams.width / 1.777777);
        renderingSize = new Point(layoutParams.width, layoutParams.height);
        Log.d(TAG, "New layout: " + layoutParams.width + " x " + layoutParams.height);

        mDebugViz.setLayoutParams(layoutParams);
        mDebugImg.setLayoutParams(layoutParams);
        surfaceView.setLayoutParams(layoutParams);

        mSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        mSoundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                mBeepLoaded = true;
            }
        });
        mBeepSoundId = mSoundPool.load(this, R.raw.beep2, 1);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    protected void initCamera(SurfaceHolder surfaceHolder) {

        // should only be here if we have a preview surface
        if (surfaceHolder == null) {
            throw new IllegalStateException("Cannot initialize Camera without a valid SurfaceHolder");
        }

        Display display = getWindowManager().getDefaultDisplay();
        //Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        Log.d(TAG, "Display rotation: " + display.getRotation());

        if (mCamera == null) {
            Log.d(TAG, "Initializing Camera..");

            int numCameras = Camera.getNumberOfCameras();
            CameraInfo cameraInfo = new CameraInfo();
            for (int i = 0; i < numCameras; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                Log.d(TAG, String.format("  Camera %d facing=%d orientation=%d", i, cameraInfo.facing, cameraInfo.orientation));
            }

            try {
                mCamera = Camera.open(0);
                mCamera.setPreviewDisplay(surfaceHolder);
                Log.d(TAG, "Camera opened.");

            } catch (IOException e) {
                Log.d(TAG, "Error setting preview surface: " + e);

            }
        }

        // configure camera
        Camera.Parameters params = mCamera.getParameters();

        Log.d(TAG, "Hardware build: " + Build.PRODUCT + " / " + Build.DEVICE + " / " + Build.MODEL + " / " + Build.BRAND);

        Log.d(TAG, "Current camera params: " + params.flatten());

        String deviceManufacturerName = params.get("exif-make");
        if (deviceManufacturerName == null)
            deviceManufacturerName = "Unknown";

        Log.d(TAG, "  deviceManufacturerName = [" + deviceManufacturerName + "]");

        switch (deviceManufacturerName) {
            case "Vuzix":
                Log.d(TAG, "Optimizing for Vuzix");

                // glass tweaks
                params.setAutoExposureLock(true);
                params.setAutoWhiteBalanceLock(true);
                params.setExposureCompensation(0);
                params.setVideoStabilization(true);

                params.setPreviewFpsRange(27000, 27000);
                params.set("iso", "800");
                params.set("scene-mode", "barcode");

                // vuzix tweaks
                params.setPreviewSize(previewFrameWidth, previewFrameHeight);
                break;
            case "Epson":
                Log.d(TAG, "Optimizing for Epson Moverio");

                params.set("auto-exposure-lock", "false");
                params.set("manual-exposure", 0);
                params.set("contrast", 80);
                params.set("iso-mode-values", 100);
                params.set("zoom", 2);
                params.set("scene-mode-values", "barcode");
                break;
            case "Google":
                Log.d(TAG, "Optimizing for Google Glass");

                //params.set("manual-exposure", 2);
                //params.set("mode", "high-performance");
                //params.setExposureCompensation(50);
                //params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_SHADE);
                //params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                //params.setAutoWhiteBalanceLock(true);
                //params.setRecordingHint(true);
                //params.setVideoStabilization(true);

                params.set("iso", 800);
                params.setSceneMode(Camera.Parameters.SCENE_MODE_BARCODE);
                params.setPreviewFormat(ImageFormat.NV21);
                params.setPreviewFpsRange(30000, 30000);
                params.setPreviewSize(previewFrameWidth, previewFrameHeight);
                break;
            case "Emulator":
                Log.d(TAG, "Emulator run");

                params.setAutoExposureLock(false);
                params.setAutoWhiteBalanceLock(false);

                params.set("manual-exposure", 0);
                params.set("contrast", 80);
                params.set("zoom", 10);
                params.set("video-stabilization", 80);
                params.set("whitebalance", "warm-fluorescent");

                params.setPreviewFormat(ImageFormat.NV21);
                params.setPreviewFpsRange(30000, 30000);
                params.setPreviewSize(previewFrameWidth, previewFrameHeight);

                break;
            default:
                Log.d(TAG, "Unrecognized device run");

                params.setPreviewSize(previewFrameWidth, previewFrameHeight);
                break;
        }

        Log.d(TAG, "Proposing new camera preview size: " + params.getPreviewSize().width + " x " + params.getPreviewSize().height);

        Log.d(TAG, "Configuring Camera..");
        mCamera.setParameters(params);

        params = mCamera.getParameters();
        previewFrameWidth = params.getPreviewSize().width;
        previewFrameHeight = params.getPreviewSize().height;

        Log.d(TAG, "Camera accepted preview size: " + previewFrameWidth + " x " + previewFrameHeight);

        //AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //audio.playSoundEffect(Sounds.SUCCESS);

        previewBufferOne = new byte[previewFrameWidth * previewFrameHeight * 3];
        previewBufferTwo = new byte[previewFrameWidth * previewFrameHeight * 3];
        mCamera.addCallbackBuffer(previewBufferOne);
        mCamera.addCallbackBuffer(previewBufferTwo);
        mCamera.setPreviewCallbackWithBuffer(this);

        Log.d(TAG, String.format("Using preview format: %X", params.getPreviewFormat()));

        visualizationScaleX = (float) renderingSize.x / (float) previewFrameWidth;
        visualizationScaleY = (float) renderingSize.y / (float) previewFrameHeight;

        Log.d(TAG, String.format("Visualization scale: %.2f x %.2f", visualizationScaleX, visualizationScaleY));

        Log.d(TAG, "Starting preview..");

        // start capture
        mCamera.startPreview();
        mCamera.startSmoothZoom(14);

        Log.d(TAG, "Starting preview.. Done");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "Resuming");

        SurfaceHolder surfaceHolder = surfaceView.getHolder();

        if (mHasSurface) {
            // we already have a preview surface
            // so go ahead and reinitialize the camera
            Log.d(TAG, "Re-initializing Camera after resume");
            initCamera(surfaceHolder);
        } else {
            // install a preview surface callback. when the surface is ready, init the camera
            Log.d(TAG, "Don't have a valid surface, adding add-callback, delaying camera-init");
            surfaceHolder.addCallback(this);
        }
    }

    @Override
    protected void onPause() {

        Log.d(TAG, "Pausing..");

        mPaused = true;

        if (scannerHandler != null) {
            scannerHandler.quitSynchronously();
            scannerHandler = null;
        }

        // release camera
        if (mCamera != null) {
            Log.d(TAG, "Releasing camera..");
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        // if we don't have a surface -- detach callback from it
        // otherwise leave it alone
        if (!mHasSurface) {
            Log.d(TAG, "Detaching callback from preview surface..");
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Destroying");
        super.onDestroy();
    }

    int[] binaryIntMatrix = new int[0];

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {

        if (scannerHandler == null || mPaused)
            return;

        // we grabbed a frame, send it over to the scan-decoding thread
        if (mReadyForMore) {
            mReadyForMore = false;
            scannerHandler.startDecode(new ScannerDecodeJob(previewFrameWidth, previewFrameHeight, bytes));
        }

        if (binaryIntMatrix.length < previewFrameWidth * previewFrameHeight) {
            binaryIntMatrix = new int[previewFrameWidth * previewFrameHeight];
            Log.d(TAG, "growing binary int-matrix buffer");
        }

        NativeUtils.binarizeToIntBuffer(bytes, binaryIntMatrix, previewFrameWidth, previewFrameHeight);

        Bitmap bmp = Bitmap.createBitmap(binaryIntMatrix, previewFrameWidth, previewFrameHeight, Bitmap.Config.ARGB_8888);
        mDebugImg.setImageBitmap(bmp);

        mCamera.addCallbackBuffer(previewBufferOne);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.seekTo(0);
    }

    // called  by ScanActivity's Handler
    // with data from the decoding thread
    // re-entry point for queued up messages from decoder thread
    public void onQRCodeDecoded(ScannerDecodeJob job) {

        if (job.result != null) {
//            if (mBeepLoaded)
//                mSoundPool.play(mBeepSoundId, 0.9f, 0.9f, 1, 0, 1f);

            ResultPoint[] pts = job.result.getResultPoints();

            Point newPts[] = new Point[pts.length];
            for(int i = 0; i < pts.length; i++) {
                newPts[i] = new Point((int) ((pts[i].getX() * visualizationScaleX)), (int)(pts[i].getY() * visualizationScaleY));
            }

            DebugVizHandler handler = mDebugViz.getHandler();
            if (handler != null) {
                Message
                        .obtain(mDebugViz.getHandler(), R.id.visualizationNewData, newPts)
                        .sendToTarget();
            }

            mDetectCount++;
            TextView textView = (TextView) findViewById(R.id.textCount);
            textView.setText(String.format("%d", mDetectCount));
        }

        mReadyForMore = true;
    }
}
