package com.camera.simplewebcam;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public static final String ACTION_VEHICLE_UNREVERSED = "com.ford.openxc.VEHICLE_UNREVERSED";
    public static final String NO_CAMERA_DETECTED = "com.ford.openxc.NO_CAMERA_DETECTED";

	private static final boolean DEBUG = true;
	protected Context context;
	private SurfaceHolder holder;
    Thread mainLoop = null;
	private Bitmap bmpVideoFeed=null;
	private Bitmap bmpOverlayLines=null;
	private Bitmap bmpIbook=null;
	private Bitmap bmpWarningText=null;
	private Bitmap bmpBendingLines=null;
	
	private static final String TAG = "CameraPreview";

	private boolean cameraExists=false;
	private boolean shouldStop=false;
	
	// /dev/videox (x=cameraId+cameraBase) is used.
	// In some omap devices, system uses /dev/video[0-3],
	// so users must use /dev/video[4-].
	// In such a case, try cameraId=0 and cameraBase=4
	private int cameraId=0;
	private int cameraBase=0;

	// This definition also exists in ImageProc.h.
	// Webcam must support the resolution 640x480 with YUYV format. 
	static final int IMG_WIDTH=640;
	static final int IMG_HEIGHT=480;

	// The following variables are used to draw camera images.
    private int winWidth=0;
    private int winHeight=0;
    public float screenHeight=0;
    public float screenWidth=0;
    private float rate=0;
    private float screenToFeedHeightRatio=0;
    private float screenToFeedWidthRatio=0;
    private float screenToOverlayHeightRatio=0;
    private float screenToOverlayWidthRatio=0;
    private float screenToIbookHeightRatio=0;
    private float screenToIbookWidthRatio=0;
    private float newBmpVideoFeedHeight=0;
    private float newBmpVideoFeedWidth=0;
    private float adjustedOverlayHeight=0;
    private float adjustedOverlayWidth=0;
    private float ibookHorizontalTranslation=0;
    private float ibookVerticalTranslation=0;
    private float adjustedIbookWidth=0;
    private float adjustedIbookHeight=0;
    private float warningTextHorizontalTranslation=0;
    private float warningTextVerticalTranslation=0;
    private float overlayHorizontalTranslation=0;
    private float overlayVerticalTranslation=0;
    private float overlayBottomCoordinate=0;
    private float overlayTopCoordinate=0;
    private float overlayLeftCoordinate=0;
    private float overlayRightCoordinate=0;
    private float StartXCoordinate;
    private float StartYCoordinate;
    private float EndXCoordinate;
    private float EndYCoordinate;
    private double steeringWheelValue;
    private float warningTextXCoordinate;
    private float warningTextYCoordinate;
  
    // JNI functions
    public native int prepareCamera(int videoid);
    public native int prepareCameraWithBase(int videoid, int camerabase);
    public native void processCamera();
    public native void stopCamera();
    public native void pixeltobmp(Bitmap bitmap);
    static {
        System.loadLibrary("ImageProc");
    }
    
	CameraPreview(Context context) {
		super(context);
		this.context = context;
		if(DEBUG) Log.d("WebCam","CameraPreview constructed");
		setFocusable(true);
		
		holder = getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);	
	}
	
    @Override
    public void run() {
    	if (cameraExists) {
    	   while (true && cameraExists) {
    		       		   
    		   //get steering wheel angle
    		   steeringWheelValue = (float)VehicleMonitoringService.SteeringWheelAngle;
    		   
    		   // obtaining a camera image (pixel data are stored in an array in JNI).
    		   processCamera();
    		   
    		   // camera image to bmp
    		   pixeltobmp(bmpVideoFeed);
         	
    		   Canvas canvas = getHolder().lockCanvas();
    		   
    		   if (canvas != null) {
    			             		
    			  
    			   //obtain screen dimensions
    			   getScreenHeight();
    			   getScreenWidth();
    //video feed
    			   //compute ratio between screen and bmpVideoFeed
    			   computeScreenToFeedWidthRatio();
    			   computeScreenToFeedHeightRatio();

    			   //adjust bmpVideoFeed to match screen size
    			   computeNewBmpVideoFeedWidth();
    			   computeNewBmpVideoFeedHeight();
    			   
    			   //adjust size and flip bmpVideoFeed horizontally with matrix
    			   Matrix videoFeedMatrix = createVideoFeedMatrix();    			   
            	
     //overlay
    			   //compute ratio between screen and overlay lines
    			   computeScreenToOverlayWidthRatio();
    			   computeScreenToOverlayHeightRatio();
    			   
    			   //compute adjusted dimensions of bmpOverlayLines
    			   computeAdjustedOverlayWidth();
    			   computeAdjustedOverlayHeight();
    			  
    			   //compute translation of bmpOverlaylines
    			   computeOverlayVerticalTranslation();
    			   computeOverlayHorizontalTranslation();
    			   
    			   //adjust bmpOverlaylines according to above calculations
    			   Matrix overlayMatrix = createOverlayMatrix();
    			       			       			   
     //ibook
    			   //compute ratio between screen and Ibook icon
    			   computeScreenToIbookHeightRatio();
    			   computeScreenToIbookWidthRatio();
    			   
    			   //compute adjusted dimensions of Ibook icon
    			   computeAdjustedIbookWidth();
    			   computedAdjustedIbookHeight();
    			   
    			   //compute translation of Ibook icon
    			   computeIbookHorizontalTranslation();
    			   computeIbookVerticalTranslation();
    			   
    			   //adjust Ibook icon according to above calculations
    			   Matrix ibookMatrix = createIbookMatrix();
    		
    //warning text			   
    			   //place warning text (adjusted according to ibook icon placement)
    			   
    			   getWarningTextXCoordinate();
    			   getWarningTextYCoordinate();
    			   
    			   Paint warningTextPaint = createWarningTextPaint();
    			   
    			   //create Outline
    			   
    			   Paint warningTextOutlinePaint = createWarningTextOutlinePaint(warningTextPaint);
    			   
    			   
    			   //make overlay fade with steering wheel angle
    			   Paint overlayPaint = new Paint();
    			   //overlayPaint.setAlpha(255-(int)steeringWheelValue);
    			   
    			   // draw bitmaps to canvas
    			   canvas.drawBitmap(bmpVideoFeed, videoFeedMatrix, null);
    			   canvas.drawBitmap(bmpOverlayLines, overlayMatrix, overlayPaint);
    			   canvas.drawBitmap(bmpIbook, ibookMatrix, null);
    			   
    			   //must draw outline paint first, otherwise yields thick black text with small white inside
    			   canvas.drawText("Please Check Surroundings for Safety", warningTextXCoordinate, 
    					   warningTextYCoordinate, warningTextOutlinePaint);
    			   canvas.drawText("Please Check Surroundings for Safety", warningTextXCoordinate, 
    					   warningTextYCoordinate, warningTextPaint);
    			   
    			   
    			   Matrix bendingLinesMatrix = new Matrix();
    			   bendingLinesMatrix.preScale(screenToOverlayWidthRatio, screenToOverlayHeightRatio);
    			   bendingLinesMatrix.postTranslate(overlayHorizontalTranslation + (float)steeringWheelValue, 
    					   overlayVerticalTranslation);
    			   
    			   int x;
    			   
    			   for (x = 0; x < adjustedOverlayHeight ;x++);{
    			   
    			   
    			   canvas.drawBitmap(bmpBendingLines, bendingLinesMatrix, null);
    			   
    			   }
    			   //get coordinates of overlay
    			   overlayBottomCoordinate = adjustedOverlayHeight + overlayVerticalTranslation;
    			   overlayTopCoordinate = overlayVerticalTranslation;
    			   overlayLeftCoordinate = (float)0.5*(screenWidth-adjustedOverlayWidth);
    			   overlayRightCoordinate = overlayLeftCoordinate + adjustedOverlayWidth;

    			   
    			   getHolder().unlockCanvasAndPost(canvas); 
    		   }

    		   if(shouldStop){
    			   shouldStop = false;  
    			   break;
    		   }	        
    	   }
     	}
       	else {
    	   Intent noCameraDetectedIntent = new Intent(NO_CAMERA_DETECTED);
    	   context.sendBroadcast(noCameraDetectedIntent);
    	   Log.i(TAG, "No Camera Detected Intent Sent");
       	}
    }

	
	
	private Paint createWarningTextOutlinePaint(Paint warningTextPaint) {
		Paint warningTextOutlinePaint = new Paint();
		warningTextOutlinePaint.setStrokeWidth(4);
		warningTextOutlinePaint.setStyle(Paint.Style.STROKE);
		warningTextOutlinePaint.setTextSize(warningTextPaint.getTextSize());
			return warningTextOutlinePaint;
	}
	private Paint createWarningTextPaint() {
		Paint warningTextPaint = new Paint();
		warningTextPaint.setColor(Color.WHITE);
		warningTextPaint.setTextSize(50);
			return warningTextPaint;
	}
	private float getWarningTextYCoordinate() {
		warningTextYCoordinate = ibookVerticalTranslation + adjustedIbookHeight;
			return warningTextYCoordinate;
	}
	private float getWarningTextXCoordinate() {
		warningTextXCoordinate = ibookHorizontalTranslation + (float)1.5*adjustedIbookWidth;
			return warningTextXCoordinate;
	}
	private Matrix createWarningTextMatrix() {
		Matrix warningTextMatrix = new Matrix();
		warningTextHorizontalTranslation = ibookHorizontalTranslation + (float)1.5*adjustedIbookWidth;
		warningTextVerticalTranslation = ibookVerticalTranslation;
		warningTextMatrix.preScale((float)0.5, (float)0.5);
		warningTextMatrix.postTranslate(warningTextHorizontalTranslation, warningTextVerticalTranslation);
			return warningTextMatrix;
	}
	private Matrix createIbookMatrix() {
		Matrix ibookMatrix = new Matrix();
		ibookMatrix.preScale(screenToIbookWidthRatio, screenToIbookHeightRatio);
		ibookMatrix.postTranslate(ibookHorizontalTranslation, ibookVerticalTranslation);
			return ibookMatrix;
	}
	private float computeIbookVerticalTranslation() {
		ibookVerticalTranslation = (float)(0.02*screenHeight);
			return ibookVerticalTranslation;
	}
	private float computeIbookHorizontalTranslation() {
		ibookHorizontalTranslation = (float)0.02*screenWidth;
			return ibookHorizontalTranslation;
	}
	private float computedAdjustedIbookHeight() {
		adjustedIbookHeight = screenToIbookHeightRatio*bmpIbook.getHeight();
		return adjustedIbookHeight;
	}
	private float computeAdjustedIbookWidth() {
		adjustedIbookWidth = screenToIbookWidthRatio*bmpIbook.getWidth();
			return adjustedIbookWidth;
	}
	private float computeScreenToIbookWidthRatio() {
		screenToIbookWidthRatio = screenWidth/bmpIbook.getWidth()/20;
			return screenToIbookWidthRatio;
	}
	private float computeScreenToIbookHeightRatio() {
		screenToIbookHeightRatio = screenHeight/bmpIbook.getHeight()/20;
			return screenToIbookHeightRatio;
	}
	private Matrix createOverlayMatrix() {
		Matrix overlayMatrix = new Matrix();
		overlayMatrix.preScale(screenToOverlayWidthRatio, screenToOverlayHeightRatio);
		overlayMatrix.postTranslate(overlayHorizontalTranslation, 
		overlayVerticalTranslation);
		return overlayMatrix;
	}
	private float computeOverlayHorizontalTranslation() {
		overlayHorizontalTranslation = (float)(0.5*screenWidth)-(float)(0.5*adjustedOverlayWidth);
			return overlayHorizontalTranslation;
	}
	private float computeOverlayVerticalTranslation() {
		overlayVerticalTranslation = (float)(0.5*screenHeight)-(float)(0.35*adjustedOverlayHeight);
		return overlayVerticalTranslation;
	}
	private float computeAdjustedOverlayHeight() {
		adjustedOverlayHeight = screenToOverlayHeightRatio*bmpOverlayLines.getHeight();
			return adjustedOverlayHeight;
	}
	private float computeAdjustedOverlayWidth() {
		adjustedOverlayWidth = screenToOverlayWidthRatio*bmpOverlayLines.getWidth();
			return adjustedOverlayWidth;
	}
	private float computeScreenToOverlayHeightRatio() {
		screenToOverlayHeightRatio = (float)0.5*screenHeight/(float)bmpOverlayLines.getHeight();
			return screenToOverlayHeightRatio;
	}
	private float computeScreenToOverlayWidthRatio() {
		screenToOverlayWidthRatio = (float)0.75*screenWidth/(float)bmpOverlayLines.getWidth();
			return screenToOverlayWidthRatio;
	}
	private Matrix createVideoFeedMatrix() {
		Matrix videoFeedMatrix = new Matrix();
		videoFeedMatrix.preScale(-screenToFeedWidthRatio, screenToFeedHeightRatio);
		videoFeedMatrix.postTranslate((float)(0.5*screenWidth)+(float)(0.5*newBmpVideoFeedWidth),
 			(float)(0.5*screenHeight)-(float)(0.5*newBmpVideoFeedHeight));
			return videoFeedMatrix;
	}
	private float computeNewBmpVideoFeedHeight() {
		newBmpVideoFeedHeight = screenToFeedHeightRatio*bmpVideoFeed.getHeight();
			return newBmpVideoFeedHeight;
	}
	private float computeNewBmpVideoFeedWidth() {
		newBmpVideoFeedWidth = screenToFeedWidthRatio*bmpVideoFeed.getWidth();
			return newBmpVideoFeedWidth;
	}
	private float computeScreenToFeedHeightRatio() {
		screenToFeedHeightRatio = screenHeight/(float)bmpVideoFeed.getHeight();
			return screenToFeedHeightRatio;
	}
	private float computeScreenToFeedWidthRatio() {
		screenToFeedWidthRatio = screenWidth/(float)bmpVideoFeed.getWidth();
			return screenToFeedWidthRatio;
	}
	private float getScreenHeight() {
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		screenHeight= metrics.heightPixels;
			return screenHeight;
	}
	private float getScreenWidth() {
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		screenWidth = metrics.widthPixels;
			return screenWidth;
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if(DEBUG) Log.d("WebCam", "surfaceCreated");
		if(bmpVideoFeed==null){
			
			bmpVideoFeed = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ARGB_8888);

		}
		if(bmpOverlayLines==null){
			
			bmpOverlayLines = BitmapFactory.decodeResource(getResources(), R.drawable.linesoverlay3); 
		}
		
		if(bmpIbook==null){
			
			bmpIbook = BitmapFactory.decodeResource(getResources(), R.drawable.ibook);
		}
		
		if(bmpBendingLines==null){
			
			bmpBendingLines = BitmapFactory.decodeResource(getResources(), R.drawable.bendinglines);
		}
		
		// /dev/videox (x=cameraId + cameraBase) is used
		int ret = prepareCameraWithBase(cameraId, cameraBase);
		
		if(ret!=-1) {
			cameraExists = true;
		}
		
        mainLoop = new Thread(this);
        mainLoop.start();		
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if(DEBUG) Log.d("WebCam", "surfaceChanged");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if(DEBUG) Log.d("WebCam", "surfaceDestroyed");
		if(cameraExists){
			shouldStop = true;
			while(shouldStop){
				try{ 
					Thread.sleep(100); // wait for thread stopping
				}catch(Exception e){}
			}
		}
		stopCamera();
	}   
}

