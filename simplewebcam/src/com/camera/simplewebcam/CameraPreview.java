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

    		   // obtaining a camera image (pixel data are stored in an array in JNI).
    		   processCamera();
    		   
    		   // camera image to bmp
    		   pixeltobmp(bmpVideoFeed);
         	
    		   Canvas canvas = getHolder().lockCanvas();
    		   
    		   if (canvas != null) {
    		
     			   // draw bitmaps to canvas
    			   drawVideoFeedBitmap(canvas);
    			   drawOverlayLinesBitmap(canvas);
    			   drawIbookBitmap(canvas);
    			   drawBendingLinesBitmap(canvas);
    			   
    			   //must draw outline paint first, otherwise yields thick black text with small white inside
    			   drawWarningTextOutline(canvas);
    			   drawWarningText(canvas);
    			   
    			   //unlock canvas
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
    
	//bitmap drawing methods
	private void drawVideoFeedBitmap(Canvas canvas) {
		Matrix videoFeedMatrix = createVideoFeedMatrix();	
		canvas.drawBitmap(bmpVideoFeed, videoFeedMatrix, null);
	}
	
	private void drawOverlayLinesBitmap(Canvas canvas) {
		Paint overlayPaint = createOverlayPaint();
		Matrix overlayMatrix = createOverlayMatrix();
		canvas.drawBitmap(bmpOverlayLines, overlayMatrix, overlayPaint);
	}
	
	private void drawIbookBitmap(Canvas canvas) {
		Matrix ibookMatrix = createIbookMatrix();
		canvas.drawBitmap(bmpIbook, ibookMatrix, null);
	}

	private void drawBendingLinesBitmap(Canvas canvas) {
		Paint bendingLinesPaint = createBendingLinesPaint();
		Matrix bendingLinesMatrix = createBendingLinesMatrix();
		canvas.drawBitmap(bmpBendingLines, bendingLinesMatrix, bendingLinesPaint);
	}
	
    //matrix creation methods
	private Matrix createIbookMatrix() {
		float screenToIbookHeightRatio = computeScreenToIbookHeightRatio();
		float screenToIbookWidthRatio = computeScreenToIbookWidthRatio();
		float ibookHorizontalTranslation = computeIbookHorizontalTranslation();
		float ibookVerticalTranslation = computeIbookVerticalTranslation();
		Matrix ibookMatrix = new Matrix();
		ibookMatrix.preScale(screenToIbookWidthRatio, screenToIbookHeightRatio);
		ibookMatrix.postTranslate(ibookHorizontalTranslation, ibookVerticalTranslation);
			return ibookMatrix;
	}
	
	private Matrix createVideoFeedMatrix() {
		float screenHeight = getScreenHeight();
		float screenWidth = getScreenWidth();
		float screenToFeedWidthRatio = computeScreenToFeedWidthRatio();
		float screenToFeedHeightRatio = computeScreenToFeedHeightRatio();
		float adjustedVideoFeedWidth = computeAdjustedVideoFeedWidth();
		float adjustedVideoFeedHeight = computeAdjustedVideoFeedHeight();
		Matrix videoFeedMatrix = new Matrix();
		videoFeedMatrix.preScale(-screenToFeedWidthRatio, screenToFeedHeightRatio);
		videoFeedMatrix.postTranslate((float)(0.5*screenWidth)+(float)(0.5*adjustedVideoFeedWidth),
 			(float)(0.5*screenHeight)-(float)(0.5*adjustedVideoFeedHeight));
			return videoFeedMatrix;
	}
	
	private Matrix createOverlayMatrix() {
		float screenToOverlayWidthRatio = computeScreenToOverlayWidthRatio();
		float screenToOverlayHeightRatio = computeScreenToOverlayHeightRatio();
		float overlayVerticalTranslation = computeOverlayVerticalTranslation();
		float overlayHorizontalTranslation = computeOverlayHorizontalTranslation();
		Matrix overlayMatrix = new Matrix();
		overlayMatrix.preScale(screenToOverlayWidthRatio, screenToOverlayHeightRatio);
		overlayMatrix.postTranslate(overlayHorizontalTranslation, 
		overlayVerticalTranslation);
			return overlayMatrix;
	}
	
	private Matrix createBendingLinesMatrix() {
		float screenToOverlayHeightRatio = computeScreenToOverlayHeightRatio();
		float screenToOverlayWidthRatio = computeScreenToOverlayWidthRatio();
		float overlayVerticalTranslation = computeOverlayVerticalTranslation();
		float overlayHorizontalTranslation = computeOverlayHorizontalTranslation();
		
		//place bending lines directly on top of overlay by using same translations/ratios
		float screenToBendingLinesHeightRatio = screenToOverlayHeightRatio;
		float screenToBendingLinesWidthRatio = screenToOverlayWidthRatio;
		float bendingLinesVerticalTranslation = overlayVerticalTranslation;
		float bendingLinesHorizontalTranslation = overlayHorizontalTranslation;
		
		//get steering wheel angle
		float steeringWheelValue = getSteeringWheelAngle(); 
		
		Matrix bendingLinesMatrix = new Matrix();
		bendingLinesMatrix.preScale(screenToBendingLinesWidthRatio, screenToBendingLinesHeightRatio);
		
		
		bendingLinesMatrix.postTranslate(bendingLinesHorizontalTranslation + (float)steeringWheelValue/2, 
				bendingLinesVerticalTranslation);
		
		bendingLinesMatrix.preSkew(-steeringWheelValue/100, steeringWheelValue/1000);
		return bendingLinesMatrix;
	}
	
	//text drawing methods
	private void drawWarningTextOutline(Canvas canvas) {
		Paint warningTextPaint = createWarningTextPaint();
		Paint warningTextOutlinePaint = createWarningTextOutlinePaint(warningTextPaint);
		float warningTextXCoordinate = getWarningTextXCoordinate();
		float warningTextYCoordinate = getWarningTextYCoordinate();
		canvas.drawText("Please Check Surroundings for Safety", warningTextXCoordinate, 
				   warningTextYCoordinate, warningTextOutlinePaint);
	}
	
	private void drawWarningText(Canvas canvas) {
		Paint warningTextPaint = createWarningTextPaint();
		float warningTextXCoordinate = getWarningTextXCoordinate();
		float warningTextYCoordinate = getWarningTextYCoordinate();
		canvas.drawText("Please Check Surroundings for Safety", warningTextXCoordinate, 
				   warningTextYCoordinate, warningTextPaint);
	}

	//paint creation methods
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
	
	private Paint createOverlayPaint(){
		float steeringWheelValue = getSteeringWheelAngle(); 
		Paint overlayPaint = new Paint();
		if (steeringWheelValue/2 >= 0 && steeringWheelValue/2 <=255) {
			
		overlayPaint.setAlpha(255-(int)steeringWheelValue/2);
		}
		else if (steeringWheelValue/2 < 0 && steeringWheelValue/2 > -255) {
			overlayPaint.setAlpha(255+(int)steeringWheelValue/2);
		}
		else {
			overlayPaint.setAlpha(0);
		}
			
		return overlayPaint;
	}
	
	private Paint createBendingLinesPaint() {
		float steeringWheelValue = getSteeringWheelAngle(); 
		Paint bendingLinesPaint = new Paint();
		if (steeringWheelValue >= 0 && steeringWheelValue < 255){
		bendingLinesPaint.setAlpha((int)steeringWheelValue);
		}
		else if (steeringWheelValue < 0 && steeringWheelValue > -255){
			bendingLinesPaint.setAlpha(-(int)steeringWheelValue);
		}
		else {
			bendingLinesPaint.setAlpha(255);
		}
		return bendingLinesPaint;
	}
	
	//get steeringWheelAngle
	private float getSteeringWheelAngle() {
		float steeringWheelValue = (float)VehicleMonitoringService.SteeringWheelAngle;
			return steeringWheelValue;
	}
	
	//get coordinates methods
	private float getWarningTextYCoordinate() {
		float adjustedIbookHeight = computeAdjustedIbookHeight();
		float ibookVerticalTranslation = computeIbookVerticalTranslation();
		float warningTextYCoordinate = ibookVerticalTranslation + adjustedIbookHeight;
			return warningTextYCoordinate;
	}
	
	private float getWarningTextXCoordinate() {
		float adjustedIbookWidth = computeAdjustedIbookWidth();
		float ibookHorizontalTranslation = computeIbookHorizontalTranslation();
		float warningTextXCoordinate = ibookHorizontalTranslation + (float)1.5*adjustedIbookWidth;
			return warningTextXCoordinate;
	}

    private float getOverlayBottomCoordinate() {
    	float adjustedOverlayHeight = computeAdjustedOverlayHeight();
    	float overlayVerticalTranslation = computeOverlayVerticalTranslation();
    	float overlayBottomCoordinate = adjustedOverlayHeight + overlayVerticalTranslation;
    	   return overlayBottomCoordinate;
	}
    
    private float getOverlayTopCoordinate() {
    	float overlayVerticalTranslation = computeOverlayVerticalTranslation();
    	float overlayTopCoordinate = overlayVerticalTranslation;
    		return overlayTopCoordinate;
    }
    
    private float getOverlayRightCoordinate() {
    	float overlayLeftCoordinate = getOverlayLeftCoordinate();
    	float adjustedOverlayWidth = computeAdjustedOverlayWidth();
    	float overlayRightCoordinate = overlayLeftCoordinate + adjustedOverlayWidth;
    		return overlayRightCoordinate;
    }
    
    private float getOverlayLeftCoordinate() {
    	float screenWidth = getScreenWidth();
    	float adjustedOverlayWidth = computeAdjustedOverlayWidth();
    	float overlayLeftCoordinate = (float)0.5*(screenWidth-adjustedOverlayWidth);
    		return overlayLeftCoordinate; 
    }
    
	//ibook translation computation methods
	private float computeIbookVerticalTranslation() {
		float screenHeight = getScreenHeight();
		float ibookVerticalTranslation = (float)(0.02*screenHeight);
			return ibookVerticalTranslation;
	}
	
	private float computeIbookHorizontalTranslation() {
		float screenWidth = getScreenWidth();
		float ibookHorizontalTranslation = (float)0.02*screenWidth;
			return ibookHorizontalTranslation;
	}
	
	//overlay translation computation methods
	private float computeOverlayHorizontalTranslation() {
		float screenWidth = getScreenWidth();
		float adjustedOverlayWidth = computeAdjustedOverlayWidth();
		float overlayHorizontalTranslation = (float)(0.5*screenWidth)-(float)(0.5*adjustedOverlayWidth);
			return overlayHorizontalTranslation;
	}
	
	private float computeOverlayVerticalTranslation() {
		float screenHeight = getScreenHeight();
		float adjustedOverlayHeight = computeAdjustedOverlayHeight();
		float overlayVerticalTranslation = (float)(0.5*screenHeight)-(float)(0.35*adjustedOverlayHeight);
		return overlayVerticalTranslation;
	}
	
	//screen to video feed ratio computation methods
	private float computeScreenToFeedHeightRatio() {
		float screenHeight = getScreenHeight();
		float screenToFeedHeightRatio = screenHeight/(float)bmpVideoFeed.getHeight();
			return screenToFeedHeightRatio;
	}
	
	private float computeScreenToFeedWidthRatio() {
		float screenWidth = getScreenWidth();
		float screenToFeedWidthRatio = screenWidth/(float)bmpVideoFeed.getWidth();
			return screenToFeedWidthRatio;
	}
	
	//screen to overlay ratio computation methods
	private float computeScreenToOverlayHeightRatio() {
		float screenHeight = getScreenHeight();
		float screenToOverlayHeightRatio = (float)0.5*screenHeight/(float)bmpOverlayLines.getHeight();
			return screenToOverlayHeightRatio;
	}
	
	private float computeScreenToOverlayWidthRatio() {
		float screenWidth = getScreenWidth();
		float screenToOverlayWidthRatio = (float)0.75*screenWidth/(float)bmpOverlayLines.getWidth();
			return screenToOverlayWidthRatio;
	}
	
	//screen to ibook ratio computation methods
	private float computeScreenToIbookWidthRatio() {
		float screenWidth = getScreenWidth();
		float screenToIbookWidthRatio = screenWidth/bmpIbook.getWidth()/20;
			return screenToIbookWidthRatio;
	}
	
	private float computeScreenToIbookHeightRatio() {
		float screenHeight = getScreenHeight();
		float screenToIbookHeightRatio = screenHeight/bmpIbook.getHeight()/20;
			return screenToIbookHeightRatio;
	}

	//adjusted video feed dimensions computation methods
	private float computeAdjustedVideoFeedHeight() {
		float screenToFeedHeightRatio = computeScreenToFeedHeightRatio();
		float adjustedVideoFeedHeight = screenToFeedHeightRatio*bmpVideoFeed.getHeight();
			return adjustedVideoFeedHeight;
	}
	
	private float computeAdjustedVideoFeedWidth() {
		float screenToFeedWidthRatio = computeScreenToFeedWidthRatio();
		float adjustedVideoFeedWidth = screenToFeedWidthRatio*bmpVideoFeed.getWidth();
			return adjustedVideoFeedWidth;
		}	
	
	//adjusted overlay dimensions computation methods
	private float computeAdjustedOverlayHeight() {
		float screenToOverlayHeightRatio = computeScreenToOverlayHeightRatio();
		float adjustedOverlayHeight = screenToOverlayHeightRatio*bmpOverlayLines.getHeight();
			return adjustedOverlayHeight;
	}
	private float computeAdjustedOverlayWidth() {
		float screenToOverlayWidthRatio = computeScreenToOverlayWidthRatio();
		float adjustedOverlayWidth = screenToOverlayWidthRatio*bmpOverlayLines.getWidth();
			return adjustedOverlayWidth;
	}
	
	//adjusted ibook dimensions computation methods
	private float computeAdjustedIbookHeight() {
		float screenToIbookHeightRatio = computeScreenToIbookHeightRatio();
		float adjustedIbookHeight = screenToIbookHeightRatio*bmpIbook.getHeight();
			return adjustedIbookHeight;
	}
		
	private float computeAdjustedIbookWidth() {
		float screenToIbookWidthRatio = computeScreenToIbookWidthRatio();
		float adjustedIbookWidth = screenToIbookWidthRatio*bmpIbook.getWidth();
			return adjustedIbookWidth;
		}
		
	//get screen dimensions methods
	private float getScreenHeight() {
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		float screenHeight= metrics.heightPixels;
			return screenHeight;
	}
	
	private float getScreenWidth() {
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		float screenWidth = metrics.widthPixels;
			return screenWidth;
	}
	
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if(DEBUG) Log.d("WebCam", "surfaceCreated");
		//if null, create each bitmap
		if(bmpVideoFeed==null){
			
			bmpVideoFeed = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ARGB_8888);

		}
		if(bmpOverlayLines==null){
			
			bmpOverlayLines = BitmapFactory.decodeResource(getResources(), R.drawable.overlay); 
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

