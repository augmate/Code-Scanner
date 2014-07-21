package com.augmate.scany;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

import java.io.IOException;

public class ScanActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback, MediaPlayer.OnCompletionListener
{
	private static final String TAG = "Activity";
	
	Camera mCamera;
	Boolean mHasSurface = false;
	byte[] mFrameBuffer;
	byte[] mDebugBuffer;
	Bitmap mDebugBitmap;
	Canvas mDebugCanvas;
	
	ScanActivityHandler mHandler;
	
	public Handler getHandler() {
		return mHandler;
	}

	// start of SurfaceHolder.Callback
	
	@Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!mHasSurface) {
            mHasSurface = true;
            Log.d(TAG, "Got preview surface");
            initCamera(holder);
            mHandler = new ScanActivityHandler(this);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }
    
    // end of SurfaceHolder.Callback
    
    DebugViz mDebugViz;
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Starting.. v1");

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.activity_main);
        
        mDebugViz = (DebugViz) findViewById(R.id.debug_view);
        
        mSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        mSoundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
            	mBeepLoaded = true;
            }
          });
        mBeepSoundId = mSoundPool.load(this, R.raw.beep2, 1);
    }
	
	Boolean mBeepLoaded = false;
	int mBeepSoundId;
	SoundPool mSoundPool;

    @Override
    public void onBackPressed() {
        finish();
    }	
	
	int camera_width = 640;
	int camera_height = 360;
	
	protected void initCamera(SurfaceHolder surfaceHolder) {
		
		// should only be here if we have a preview surface
		if (surfaceHolder == null) {
            throw new IllegalStateException("Cannot initialize Camera without a valid SurfaceHolder");
        }
		
		if(mCamera == null) {
			Log.d(TAG, "Initializing Camera..");
			
			int numCameras = Camera.getNumberOfCameras();
			CameraInfo cameraInfo = new CameraInfo();
			for(int i = 0; i < numCameras; i ++) {
				Camera.getCameraInfo(i, cameraInfo);
				Log.d(TAG, String.format("Camera %d facing=%d orientation=%d", i, cameraInfo.facing, cameraInfo.orientation));
			}
			
			try {
				mCamera = Camera.open(0);
				mCamera.setPreviewDisplay(surfaceHolder);

                Log.d(TAG, "Setting up frame-capture buffers");

			} catch (IOException e) {
				Log.d(TAG, "Error setting preview surface: " + e);
				
			}
		}
		
		// configure camera
		Camera.Parameters params = mCamera.getParameters();
		
		Log.d(TAG, "Camera params: " + params.flatten());

		/*
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
        
        Log.d(TAG, "Supported camera focus modes: " + TextUtils.join(",", params.getSupportedFocusModes()));

        Log.d(TAG, "Camera auto-exposure lock supported: " + params.isAutoExposureLockSupported());
        Log.d(TAG, "Camera white-balance lock supported: " + params.isAutoWhiteBalanceLockSupported());
        Log.d(TAG, "Camera zoom supported: " + params.isZoomSupported());
        Log.d(TAG, "Camera smooth zoom supported: " + params.isSmoothZoomSupported());
        Log.d(TAG, "Camera video stabilization supported: " + params.isVideoStabilizationSupported());

        Log.d(TAG, "Camera white-balance: " + params.getWhiteBalance());
        Log.d(TAG, "Camera exposure-compensation: " + params.getExposureCompensation());
        Log.d(TAG, "Camera exposure-compensation step: " + params.getExposureCompensationStep());
        Log.d(TAG, "Camera exposure-compensation min: " + params.getMinExposureCompensation());
        Log.d(TAG, "Camera exposure-compensation max: " + params.getMaxExposureCompensation());
        Log.d(TAG, "Camera focal length: " + params.getFocalLength());
        Log.d(TAG, "Camera fov: " + params.getHorizontalViewAngle());
        Log.d(TAG, "Camera focus mode: " + params.getFocusMode());
        Log.d(TAG, "Camera flash mode: " + params.getFlashMode());
        Log.d(TAG, "Camera auto-exposure lock: " + params.getAutoExposureLock());
        Log.d(TAG, "Camera auto-white-balance lock: " + params.getAutoWhiteBalanceLock());
        
        Log.d(TAG, "Camera zoom ratios: " + (params.getZoomRatios() == null ? "N/A" : TextUtils.join(",", params.getZoomRatios())));
        Log.d(TAG, "Camera zoom max: " + params.getMaxZoom());
        //Log.d(TAG, "Camera preferred preview size: " +  params.getPreferredPreviewSizeForVideo().width + " x " + params.getPreferredPreviewSizeForVideo().height);
        Log.d(TAG, "Camera old preview size: " + params.getPreviewSize().width + " x " + params.getPreviewSize().height);
        Log.d(TAG, "Camera preview format: " + params.getPreviewFormat());

        List<int[]> supportedPreviewFpsRanges = params.getSupportedPreviewFpsRange();
        int[] minimumPreviewFpsRange = supportedPreviewFpsRanges.get(0);
        Log.d(TAG, "Camera preview fps range: " + minimumPreviewFpsRange[0] + " - " + minimumPreviewFpsRange[1]);
        */

		Log.d(TAG, "Camera zoom ratios: " + (params.getZoomRatios() == null ? "N/A" : TextUtils.join(",", params.getZoomRatios())));
        Log.d(TAG, "Camera zoom max: " + params.getMaxZoom());
		
        String deviceManufacturerName = params.get("exif-make");
        Log.d(TAG, "deviceManufacturerName = [" + deviceManufacturerName + "]");

        switch (deviceManufacturerName) {
            case "Vuzix":
                Log.d(TAG, "Optimizing for Vuzix");

                // glass tweaks
                params.setAutoExposureLock(true);
                params.setAutoWhiteBalanceLock(true);
                params.setExposureCompensation(0);
                params.setVideoStabilization(true);

                ///?params.setFocusMode("FOCUS_MODE_MACRO");
                ///params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_SHADE);
                ///params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED); // doesn't work
                //params.setSceneMode(Camera.Parameters.SCENE_MODE_BARCODE);

                //params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_SHADE);
                //params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

                params.setPreviewFpsRange(27000, 27000);
                ///params.set("saturation", "70");
                params.set("iso", "800");
                params.set("scene-mode", "barcode");
                ///params.set("mode", "high-quality");

                ///params.set("whitebalance", "shade");
                ///params.set("manual-exposure", "1");
                ///params.set("exposure", "1");

                //params.set("focus-mode", "on");

                // vuzix tweaks
                params.setPreviewSize(camera_width, camera_height);
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
                params.set("iso", 800);
                //params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_SHADE);
                params.setSceneMode(Camera.Parameters.SCENE_MODE_BARCODE);
                //params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                params.setAutoWhiteBalanceLock(true);
                //params.setRecordingHint(true);
                //params.setVideoStabilization(true);

                params.setPreviewFormat(ImageFormat.NV21);
                params.setPreviewFpsRange(30000, 30000);
                params.setPreviewSize(camera_width, camera_height);
                break;
            case "Emulator":
                Log.d(TAG, "Emulator run");

                params.setAutoExposureLock(false);
                params.setAutoWhiteBalanceLock(false);

                params.set("manual-exposure", 0);
                params.set("contrast", 80);
                //params.set("iso-mode-values", 100);
                params.set("zoom", 10);
                //params.set("scene-mode-values", "barcode");
                params.set("video-stabilization", 80);
                params.set("whitebalance", "warm-fluorescent");

                params.setPreviewFormat(ImageFormat.NV21);
                params.setPreviewFpsRange(30000, 30000);
                params.setPreviewSize(camera_width, camera_height);

                break;
            default:
                Log.d(TAG, "Unrecognized run");

                params.set("manual-exposure", 0);
                params.set("contrast", 80);
                params.set("zoom", 5);

                params.setPreviewFpsRange(20000, 20000);
                params.setPreviewSize(camera_width, camera_height);
                break;
        }
        
        // 10-30 fps
        // will run at 30fps if the exposure allows (good lighting)
        //params.setPreviewFpsRange(30000, 30000);
        //params.setPreviewSize(camera_width, camera_height);
        //params.setPreviewFormat(ImageFormat.NV21);

        // for a 1280x720 image with a single preallocated buffer:
        // YUY2 = 600ms
        // YV12 = 30-60ms
        // NV12 = 40-70ms
        
        // for 640x360 (quarter-res)
        // the bottleneck becomes the actual frame-length. if there is little light, exposure will grow to be 1/10th
        // which means each frame will only be available from the camera hardware every 100ms
        // if lighting is great and exposure is faster than 1/30, then we can get frames at 30fps
        // NV12 = 30ms
        
        // in emulator @ 640x360
        // NV12 = 60ms

        Log.d(TAG, "Camera new preview size: " + params.getPreviewSize().width + " x " + params.getPreviewSize().height);
        
        Log.d(TAG, "Configuring Camera..");
        mCamera.setParameters(params);
        
        params = mCamera.getParameters();
        camera_width = params.getPreviewSize().width;
        camera_height = params.getPreviewSize().height;
        
        Log.d(TAG, "Camera set preview size: " + camera_width + " x " + camera_height);
        
        //AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //audio.playSoundEffect(Sounds.SUCCESS);

        mLastFrame = SystemClock.elapsedRealtime();
        
        mFrameBuffer = new byte[camera_width * camera_height * 3];
        mCamera.addCallbackBuffer(mFrameBuffer);
        mCamera.setPreviewCallbackWithBuffer(this);
        
        mDebugBuffer = new byte[camera_width * camera_height * 3];

        mDebugBitmap = Bitmap.createBitmap(camera_width, camera_height, Bitmap.Config.RGB_565);
        mDebugCanvas = new Canvas(mDebugBitmap);
        
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        
        mScaleWidth = (float)size.x / (float)camera_width;
        mScaleHeight = (float)size.y / (float)camera_height;
        
        Log.d(TAG, String.format("Scale: %3.2f x %3.2f", mScaleWidth, mScaleHeight));
        
        mDebugViz.scaleX = mScaleWidth;
        mDebugViz.scaleY = mScaleHeight;

        Log.d(TAG, "Starting preview..");
        
        // start capture
        mCamera.startPreview();
        mCamera.startSmoothZoom(14);

        Log.d(TAG, "Starting preview.. Done");
	}
	
	float mScaleWidth = 1;
	float mScaleHeight = 1;
	
	@Override
    protected void onResume() {
        super.onResume();
        
        Log.d(TAG, "Resuming");

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
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
		
		if(mHandler != null) {
			mHandler.quitSynchronously();
			mHandler = null;
		}
		
		// release camera
		if(mCamera != null) {
			Log.d(TAG, "Releasing camera..");
            mCamera.setPreviewCallbackWithBuffer(null);
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
		
		// if we don't have a surface -- detach callback from it
		// otherwise leave it alone
		if(!mHasSurface) {
			Log.d(TAG, "Detaching callback from preview surface..");
			SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
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
	
	Boolean mPaused = false;
	
    long mLastFrame;
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
    	
    	//Log.d(TAG, "onPreviewFrame(); mReadyForMore = " + mReadyForMore);
    	
    	if(mHandler == null || mPaused)
    		return;
    	
    	// we grabbed a frame, send it over to the scan-decoding thread
    	if(mReadyForMore) {
    		mHandler.submitDecodeJob(camera_width, camera_height, bytes);
    		mReadyForMore = false;
    	}
    	
    	mCamera.addCallbackBuffer(mFrameBuffer);
    }
    
    Boolean mReadyForMore = true;
    
    public void requestFrame() {
        mLastFrame = SystemClock.elapsedRealtime();
    	//Log.d(TAG, "Frame-request delta: " + time_since_last_frame + " ms");
    	mReadyForMore = true;
    }
    
    int mDetectCount = 0;
    
    // called  by ScanActivity's Handler
    // with data from the decoding thread
    public void onQRCodeDecoded(Result rawResult) {
    	if (rawResult != null)
    	{
    		if(mBeepLoaded)
    			mSoundPool.play(mBeepSoundId, 0.9f, 0.9f, 1, 0, 1f);
    		
    		ResultPoint[] pts = rawResult.getResultPoints();
    		
    		Point pt = new Point();
    		pt.x = (int) (pts[0].getX() + pts[1].getX() + pts[2].getX()) / 3;
    		pt.y = (int) (pts[0].getY() + pts[1].getY() + pts[2].getY()) / 3;
    		
    		DebugVizHandler handler = mDebugViz.getHandler();
    		if(handler != null) {
	    		Message msg = Message.obtain(mDebugViz.getHandler(), R.id.submit_viz, pt);
	        	msg.sendToTarget();
    		}
    		
    		mDetectCount ++;
    		TextView textView = (TextView) findViewById(R.id.textCount);
    	    textView.setText(String.format("%d", mDetectCount));
    	}
    	
    	mReadyForMore = true;
    }

	@Override
	public void onCompletion(MediaPlayer mp) {
		mp.seekTo(0);
	}
}
