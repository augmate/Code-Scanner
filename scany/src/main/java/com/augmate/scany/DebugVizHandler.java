package com.augmate.scany;

import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public final class DebugVizHandler extends Handler {

    private static final String TAG = DebugVizHandler.class.getName();
    private final DebugViz scanActivity;

    DebugVizHandler(DebugViz activity) {
        this.scanActivity = activity;
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {

            // received qr-code visualization data
            case R.id.visualizationNewData:

                Log.d(TAG, "Got new points for visualization");
                Point[] pts = (Point[]) message.obj;
                if (pts.length == 3)
                    scanActivity.setPoints(pts[0], pts[1], pts[2]);

                break;
        }
    }
}