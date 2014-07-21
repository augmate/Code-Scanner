package com.augmate.scany;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.google.zxing.ResultPoint;

import java.util.ArrayList;

public class DebugViz extends View {
	
	private static final String TAG = "ScanActivity";

	public ResultPoint[] points;
	public float scaleX = 1, scaleY = 1;
	
	DebugVizHandler mHandler;
	
	public DebugVizHandler getHandler() {
		return mHandler;
	}
	
    public DebugViz(Context context) {
        super(context);
        
//        if (!isInEditMode()) {
//            setLayerType(View.LAYER_TYPE_HARDWARE, null);
//        }
    }
    
    public DebugViz(Context context, AttributeSet attr) {
        super(context, attr);
    }
    
    Paint _PaintColor;
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if(mHandler == null)
        	mHandler = new DebugVizHandler(this);

        //drawResultPoints(canvas);
        
        ArrayList<Point> pts = new ArrayList<Point>(mMessagePoints);
        
        for (Point point : pts)
        {
    		point.y -= 20;
    		
    		if(point.y < -10)
    			mMessagePoints.remove(point);
        }
        
        if(_PaintColor == null)
        {
        	_PaintColor = new Paint();
        	_PaintColor.setColor(0xaa1abc9c);
        	_PaintColor.setStrokeWidth(25.0f);
        }
        
        for (Point point : pts)
        {
        	//Log.i(TAG, "drawing point: " + ((float)point.x * scaleX) + "," + ((float)point.y * scaleY));
        	canvas.drawCircle((float)point.x * scaleX, (float)point.y * scaleY, 25.0f, _PaintColor);
		}
        
        postInvalidate();
    }
    
    public ArrayList<Point> mMessagePoints = new ArrayList<Point>();
    
    public void AddPoint(Point pt)
    {
    	mMessagePoints.add(pt);
    }
    
    public void AddPoints(ResultPoint[] pts)
    {
    	
    }
    
    private void drawResultPoints(Canvas canvas)
    {
    	if(points == null)
    		return;
    	
    	Log.d(TAG, "Found points: " + points.length + " on " + this.getWidth() + " x " + this.getHeight());
    	
    	if(points.length != 3)
    		return;
    	
    	Paint red = new Paint();
		red.setColor(0xff1abc9c);
		red.setStrokeWidth(5.0f);
		
		ResultPoint[] pts = new ResultPoint[4];
		
		ResultPoint bottomLeft = points[0];
		ResultPoint topLeft = points[1];
		ResultPoint topRight = points[2];
		
		pts[0] = points[0];
		pts[1] = points[1];
		pts[2] = points[2];
		
		float bottomRightX = topRight.getX() - topLeft.getX() + bottomLeft.getX();
		float bottomRightY = topRight.getY() - topLeft.getY() + bottomLeft.getY();
		
		pts[3] = new ResultPoint(bottomRightX, bottomRightY);
	
		drawLine(canvas, red, pts[0], pts[1]);
		drawLine(canvas, red, pts[1], pts[2]);
		drawLine(canvas, red, pts[2], pts[3]);
		drawLine(canvas, red, pts[3], pts[0]);
		
		red.setStrokeWidth(10.0f);
		for(int i = 0; i < 4; i ++)
			canvas.drawPoint(pts[i].getX() * scaleX, pts[i].getY() * scaleY, red);
    	
//		if (points != null && points.length > 0)
//		{
//			Log.d(TAG, "Points: " + points.length);
//			
//			Paint paint = new Paint();
//			paint.setColor(getResources().getColor(R.color.result_points));
//			
//			if (points.length == 2) {
//				paint.setStrokeWidth(4.0f);
//				drawLine(canvas, paint, points[0], points[1], scaleFactor);
//			}
//			else
//			{
//				paint.setStrokeWidth(10.0f);
//				
//				for (ResultPoint point : points) {
//					if (point != null) {
//						canvas.drawPoint(scaleFactor * point.getX(), scaleFactor * point.getY(), paint);
//					}
//				}
//			}
//		}
	}

	private void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b) {
		if (a != null && b != null) {
			canvas.drawLine(scaleX * a.getX(), scaleY * a.getY(),
					scaleX * b.getX(), scaleY * b.getY(), paint);
		}
	}
}