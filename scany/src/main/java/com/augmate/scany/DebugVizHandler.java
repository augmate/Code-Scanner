package com.augmate.scany;

import android.graphics.Point;
import android.os.Handler;
import android.os.Message;

public final class DebugVizHandler extends Handler {

    private static final String TAG = "ActivityHandler";
    private final DebugViz mScanActivity;

    DebugVizHandler(DebugViz activity) {
        this.mScanActivity = activity;
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {

            case R.id.submit_viz:
                Point pt = (Point) message.obj;
                mScanActivity.AddPoint(pt);

                //ResultPoint[] pts = (ResultPoint[]) message.obj;
                //mScanActivity.AddPoints(pts);
                break;
        }
    }
}